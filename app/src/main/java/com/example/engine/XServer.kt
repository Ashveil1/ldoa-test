package com.example.engine

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import kotlin.concurrent.thread

/**
 * Starts the vendored termux-x11 X server in a separate process via
 * app_process + com.termux.x11.CmdEntryPoint, then opens its MainActivity.
 *
 * Verified topology (vendored source + CI):
 * - The server needs XKB_CONFIG_ROOT with real keymap data (XkbData).
 * - libXlorie.so is preloaded via the XSERVER_LIB env hook (vendored patch
 *   c): under extractNativeLibs=true the classloader-resource path upstream
 *   uses would resolve to a compressed in-APK entry that cannot be dlopened.
 * - Socket: $TMPDIR/.X11-unix/X0; guests reach it through a proot bind.
 */
object XServer {
    private const val TAG = "PhoneLinuxX"
    private var process: Process? = null
    @Volatile var preparing = false
        private set

    fun tmpDir(ctx: Context): File = File(ctx.filesDir, "usr/tmp")
    fun socketDir(ctx: Context): File = File(tmpDir(ctx), ".X11-unix")

    @Synchronized
    fun start(ctx: Context) {
        if (process?.isAlive == true) return
        XkbData.ensure(ctx)
        socketDir(ctx).mkdirs()
        val lib = File(ctx.applicationInfo.nativeLibraryDir, "libXlorie.so")
        check(lib.isFile) { "libXlorie.so missing from nativeLibraryDir" }
        val log = File(ctx.filesDir, "xserver.log")
        val pb = ProcessBuilder("/system/bin/app_process", "/", "com.termux.x11.CmdEntryPoint", ":0")
        val env = pb.environment()
        env["CLASSPATH"] = ctx.applicationInfo.sourceDir
        env["TMPDIR"] = tmpDir(ctx).absolutePath
        env["XKB_CONFIG_ROOT"] = XkbData.root(ctx).absolutePath
        env["XSERVER_LIB"] = lib.absolutePath
        env["LD_LIBRARY_PATH"] = ctx.applicationInfo.nativeLibraryDir
        pb.redirectErrorStream(true)
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log))
        process = pb.start()
        Log.i(TAG, "X server spawned; log=$log")
    }


    /** Activity-free entry for ViewModel callers: same boot path, launches
     *  the X activity with FLAG_ACTIVITY_NEW_TASK from the app context. */
    fun openDesktopApp(app: Application, distro: Distro) {
        if (preparing) return
        preparing = true
        thread(name = "xserver-start") {
            try {
                start(app)
                startGuestGui(app, distro)
            } catch (t: Throwable) {
                Log.e(TAG, "X server start failed", t)
            }
            preparing = false
            val i = Intent(app, com.termux.x11.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(i)
        }
    }

    /** The on-device GUI milestone: one real X app (xterm) on the lorie
     *  display. Deliberately NOT GNOME - its apt set is hundreds of MB and
     *  its feasibility here is unproven; prove the chain with ~15 MB first.
     *  Evidence trail: guest-gui-smoke.log. */
    private fun startGuestGui(ctx: Context, distro: Distro) {
        val installer = DistroInstaller(ctx)
        if (!installer.isInstalled(distro)) return
        val rootfs = installer.rootfsDir(distro)
        if (!GuestUbuntu.hasXterm(rootfs)) {
            Log.i(TAG, "xterm not provisioned; apt install xterm x11-apps")
            val add = GuestUbuntu.provisionGuiSmoke(ctx, rootfs)
            Log.i(TAG, "gui-smoke provision exit=" + add.exitCode)
        }
        if (GuestUbuntu.hasXterm(rootfs)) {
            Log.i(TAG, "launching xterm GUI smoke")
            GuestUbuntu.launchXterm(ctx, rootfs)
        } else {
            Log.e(TAG, "xterm unavailable; X display will be empty")
        }
    }

    fun openDesktop(activity: Activity, distro: Distro) {
        if (preparing) return
        preparing = true
        thread(name = "xserver-start") {
            val ctx = activity.applicationContext
            try {
                start(ctx)
                startGuestGui(ctx, distro)
            } catch (t: Throwable) {
                Log.e(TAG, "X server start failed", t)
            }
            Handler(Looper.getMainLooper()).post {
                preparing = false
                activity.startActivity(Intent(activity, com.termux.x11.MainActivity::class.java))
            }
        }
    }
}
