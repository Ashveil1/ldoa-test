package com.example.vd

import android.content.Context
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Thin handle to the shell-side service (same-process in the emulator dev path, Shizuku UserService binder on device). */
interface VdBackend {
    fun createDisplay(name: String, surface: Surface, w: Int, h: Int, dpi: Int): Int
    fun resize(id: Int, w: Int, h: Int, dpi: Int)
    fun release(id: Int)
    fun launch(id: Int, component: String): Int
    fun touch(id: Int, action: Int, x: Float, y: Float, downTime: Long)
}

/**
 * One panel = one SurfaceView = one VirtualDisplay. Call from the existing window frame.
 * [component] like "com.android.settings/.Settings".
 */
@Composable
fun VdPanel(ctx: Context, backend: VdBackend, component: String, dpi: Int, modifier: Modifier = Modifier) {
    AndroidView(modifier = modifier, factory = {
        SurfaceView(ctx).also { sv ->
            var id = -1
            var downTime = 0L
            sv.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(h: SurfaceHolder) {}
                override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) {
                    if (id < 0) {
                        id = backend.createDisplay("vd-panel", h.surface, w, hh, dpi)
                        if (id >= 0) backend.launch(id, component)
                    } else backend.resize(id, w, hh, dpi)
                }
                override fun surfaceDestroyed(h: SurfaceHolder) {
                    if (id >= 0) backend.release(id); id = -1
                }
            })
            // Panel pixels == display pixels (same w/h), so no coordinate scaling needed.
            sv.setOnTouchListener { _, e ->
                if (id < 0) return@setOnTouchListener false
                if (e.actionMasked == MotionEvent.ACTION_DOWN) downTime = e.eventTime
                backend.touch(id, e.actionMasked, e.x, e.y, downTime)
                true
            }
        }
    })
}
