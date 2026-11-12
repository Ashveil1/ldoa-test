package com.example.vd

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import android.view.Surface
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Runs as shell uid (Shizuku UserService, or app_process in the emulator dev path).
 * One instance can own many displays (one per panel).
 */
class VdService {
    private val TAG = "VdService"
    private val displays = HashMap<Int, VirtualDisplay>()

    private fun dm(ctx: Context) = ctx.getSystemService(DisplayManager::class.java)

    /** Returns the new display id, or -1 on failure. */
    fun createDisplay(ctx: Context, name: String, surface: Surface, w: Int, h: Int, dpi: Int): Int = try {
        HiddenApiBypass.addHiddenApiExemptions("L")
        val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
            (1 shl 6) /* SUPPORTS_TOUCH */ or
            (1 shl 8) /* DESTROY_CONTENT_ON_REMOVAL */
        val vd = dm(ctx).createVirtualDisplay(name, w, h, dpi, surface, flags)
        displays[vd.display.displayId] = vd
        Log.i(TAG, "virtual display created id=${vd.display.displayId} ${w}x$h dpi=$dpi")
        vd.display.displayId
    } catch (t: Throwable) {
        Log.e(TAG, "createVirtualDisplay failed", t)
        -1
    }

    fun resize(id: Int, w: Int, h: Int, dpi: Int) { displays[id]?.resize(w, h, dpi) }

    fun release(id: Int) { displays.remove(id)?.release() }

    /** am start on the target display. Windowing mode 1 = fullscreen inside that display. */
    fun launch(id: Int, component: String): Int {
        val p = Runtime.getRuntime().exec(arrayOf("/system/bin/am", "start", "--display", id.toString(), "-n", component))
        val out = (p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()).trim()
        val rc = p.waitFor()
        Log.i(TAG, "launch $component on display $id -> rc=$rc ${out.take(200)}")
        return rc
    }

    /** Inject touch in display coordinates (already scaled by the panel). */
    fun touch(id: Int, action: Int, x: Float, y: Float, downTime: Long) {
        val ev = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
        ev.source = InputDevice.SOURCE_TOUCHSCREEN
        // hidden: MotionEvent.setDisplayId(int)
        MotionEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType).invoke(ev, id)
        inject(ev)
        ev.recycle()
    }

    private fun inject(ev: InputEvent) {
        val imgClass = Class.forName("android.hardware.input.InputManagerGlobal")
            .let { runCatching { it.getMethod("getInstance").invoke(null) }.getOrNull() }
            ?: Class.forName("android.hardware.input.InputManager").getMethod("getInstance").invoke(null)
        imgClass!!.javaClass.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
            .invoke(imgClass, ev, 0 /* ASYNC */)
    }
}
