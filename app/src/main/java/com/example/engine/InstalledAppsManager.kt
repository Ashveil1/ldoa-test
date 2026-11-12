package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

    fun launchApp(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
