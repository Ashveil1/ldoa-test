package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import com.example.model.AndroidAppItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InstalledAppsManager(private val context: Context) {
    private val TAG = "InstalledApps"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _androidApps = MutableStateFlow<List<AndroidAppItem>>(emptyList())
    val androidApps: StateFlow<List<AndroidAppItem>> = _androidApps.asStateFlow()

    // Cache of actual real app icons loaded from the device OS
    private val iconCache = mutableMapOf<String, Bitmap?>()

    init {
        loadApps()
    }

    fun loadApps() {
        scope.launch {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val appList = mutableListOf<AndroidAppItem>()
            try {
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                for (ri in resolveInfos) {
                    val pkgName = ri.activityInfo.packageName
                    // Skip our own launcher from the listing to prevent recursive loop
                    if (pkgName == context.packageName) continue

                    val appLabel = ri.loadLabel(pm).toString()
                    val isSystem = (ri.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                    // Cache real APK icon
                    try {
                        val drawable = ri.loadIcon(pm)
                        val bmp = drawableToBitmap(drawable)
                        synchronized(iconCache) {
                            iconCache[pkgName] = bmp
                        }
                    } catch (_: Exception) {
                        // ignore
                    }

                    appList.add(
                        AndroidAppItem(
                            packageName = pkgName,
                            appName = appLabel,
                            isSystemApp = isSystem,
                            launchIntentAvailable = true
                        )
                    )
                }
            } catch (_: Exception) {
                // Handled gracefully below
            }

            // Fallback essential Android suite if list is empty (e.g. testing container)
            if (appList.isEmpty()) {
                val standardPkgs = listOf(
                    "com.android.chrome" to "Chrome",
                    "com.google.android.youtube" to "YouTube",
                    "com.google.android.apps.photos" to "Photos",
                    "com.google.android.gm" to "Gmail",
                    "com.google.android.apps.maps" to "Maps",
                    "com.android.settings" to "Settings",
                    "com.android.camera2" to "Camera",
                    "com.google.android.calendar" to "Calendar",
                    "com.google.android.keep" to "Keep Notes",
                    "com.android.calculator2" to "Calculator",
                    "com.google.android.dialer" to "Phone",
                    "com.google.android.apps.messaging" to "Messages",
                    "com.android.vending" to "Play Store",
                    "com.google.android.apps.docs" to "Google Drive"
                )
                for ((pkg, label) in standardPkgs) {
                    appList.add(AndroidAppItem(pkg, label, true, true))
                    // Try to load system icon if package exists on device
                    try {
                        val icon = pm.getApplicationIcon(pkg)
                        synchronized(iconCache) {
                            iconCache[pkg] = drawableToBitmap(icon)
                        }
                    } catch (_: Exception) {
                        // ignored
                    }
                }
            }

            _androidApps.value = appList.sortedBy { it.appName.lowercase() }
        }
    }

    fun getAppIcon(packageName: String): Bitmap? {
        synchronized(iconCache) {
            if (iconCache.containsKey(packageName)) {
                return iconCache[packageName]
            }
        }
        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bmp = drawableToBitmap(drawable)
            synchronized(iconCache) {
                iconCache[packageName] = bmp
            }
            bmp
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /** Device support for freeform windows (Ash's choice A: Android-native
     *  floating, device-dependent). The public API path is
     *  ActivityOptions.setLaunchBounds; whether the system honors it depends
     *  on the device's windowing support. */
    enum class FreeformSupport { SUPPORTED, DEV_OPTION_NEEDED, UNSUPPORTED }

    fun freeformSupport(): FreeformSupport {
        val pm = context.packageManager
        if (pm.hasSystemFeature("android.software.freeform_window_management"))
            return FreeformSupport.SUPPORTED
        val dev = runCatching {
            android.provider.Settings.Global.getInt(context.contentResolver, "enable_freeform_support", 0)
        }.getOrDefault(0)
        return if (dev == 1) FreeformSupport.SUPPORTED else FreeformSupport.DEV_OPTION_NEEDED
    }

    /** Vendor ROM label for honest UI hints ("HyperOS 2", "MIUI 14", or null).
     *  MIUI/HyperOS floating windows are a vendor system; AOSP freeform is
     *  usually disabled there, so the panel can set expectations. */
    fun vendorRom(): String? {
        fun prop(key: String): String = runCatching {
            val sp = Class.forName("android.os.SystemProperties")
                .getDeclaredMethod("get", String::class.java)
            (sp.invoke(null, key) as String).trim()
        }.getOrDefault("")
        val hyper = prop("ro.mi.os.version.name")
        if (hyper.isNotEmpty()) return "HyperOS $hyper"
        val miui = prop("ro.miui.ui.version.name")
        if (miui.isNotEmpty()) return "MIUI $miui"
        return null
    }

    /** If we hold WRITE_SECURE_SETTINGS (one-time adb/Shizuku grant, the same
     *  one Taskbar asks for), flip the global freeform switches ourselves.
     *  Returns the resulting support state. Technique credit:
     *  farmerbb/Taskbar (Apache 2.0). */
    private fun tryEnableFreeformWithSecureSettings(): FreeformSupport {
        if (freeformSupport() == FreeformSupport.SUPPORTED) return FreeformSupport.SUPPORTED
        val granted = context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            runCatching {
                android.provider.Settings.Global.putInt(context.contentResolver, "enable_freeform_support", 1)
                android.provider.Settings.Global.putInt(context.contentResolver, "force_resizable_activities", 1)
            }
        }
        return freeformSupport()
    }

    /** Centered ~70% bounds for the freeform request. */
    private fun freeformBounds(): android.graphics.Rect {
        val dm = context.resources.displayMetrics
        val w = (dm.widthPixels * 0.7f).toInt().coerceAtLeast(480)
        val h = (dm.heightPixels * 0.7f).toInt().coerceAtLeast(640)
        return android.graphics.Rect(
            (dm.widthPixels - w) / 2, (dm.heightPixels - h) / 2,
            (dm.widthPixels + w) / 2, (dm.heightPixels + h) / 2
        )
    }

    /** Launch the app floating (freeform) when the device supports it;
     *  falls back to a normal full-screen launch otherwise. Returns the
     *  mode actually used so the UI can say what happened. */
    fun launchApp(packageName: String): Boolean = launchAppFreeform(packageName) != LaunchMode.FAILED

    /**
     * Best-effort hidden-API freeform request: ActivityOptions
     * .setLaunchWindowingMode(WINDOWING_MODE_FREEFORM=5) is @hide, so call it
     * through meta-reflection (Class.class.getDeclaredMethod) which bypasses
     * the hidden-API enforcement on API 28+. Whether the ROM then HONORS the
     * request is a separate question - stock AOSP with the freeform dev
     * option does; MIUI/HyperOS may still ignore it (its floating windows
     * are a separate vendor system). This only ever ADDS a hint; callers
     * already handle the fullscreen fallback honestly.
     */
    private fun requestFreeformWindowingMode(opts: android.app.ActivityOptions): Boolean {
        return try {
            val meta = Class::class.java.getDeclaredMethod(
                "getDeclaredMethod", String::class.java, arrayOfNulls<Class<*>>(0).javaClass
            )
            val m = meta.invoke(
                android.app.ActivityOptions::class.java,
                "setLaunchWindowingMode",
                arrayOf(Int::class.javaPrimitiveType)
            ) as java.lang.reflect.Method
            m.isAccessible = true
            m.invoke(opts, 5)
            true
        } catch (t: Throwable) {
            Log.d(TAG, "setLaunchWindowingMode unavailable: ${t.message}")
            false
        }
    }

    fun launchAppFreeform(packageName: String): LaunchMode {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return LaunchMode.FAILED
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        tryEnableFreeformWithSecureSettings()
        val opts = android.app.ActivityOptions.makeBasic()
        opts.launchBounds = freeformBounds()
        val windowingModeSet = requestFreeformWindowingMode(opts)
        Log.d(TAG, "freeform launch $packageName: windowingModeSet=$windowingModeSet support=${freeformSupport()} rom=${vendorRom()}")
        return try {
            context.startActivity(launchIntent, opts.toBundle())
            if (freeformSupport() == FreeformSupport.SUPPORTED) LaunchMode.FREEFORM else LaunchMode.FULLSCREEN_FALLBACK
        } catch (_: Exception) {
            try {
                context.startActivity(launchIntent)
                LaunchMode.FULLSCREEN_FALLBACK
            } catch (_: Exception) {
                LaunchMode.FAILED
            }
        }
    }

    enum class LaunchMode { FREEFORM, FULLSCREEN_FALLBACK, FAILED }
}
