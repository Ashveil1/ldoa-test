package com.example.vd

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * One panel = one VirtualDisplay rendered into this SurfaceView.
 *
 * The backend may not be READY when the surface first appears (Shizuku
 * bind / permission flow happens in parallel). createDisplay fails soft
 * with -1 in that state, so surfaceChanged retries once per second
 * (bounded) instead of silently leaving a permanent black panel.
 */
@Composable
fun VdPanel(ctx: Context, backend: VdBackend, component: String, dpi: Int, modifier: Modifier = Modifier) {
    AndroidView(modifier = modifier, factory = {
        SurfaceView(ctx).also { sv ->
            var id = -1
            var downTime = 0L
            var retries = 0
            sv.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(h: SurfaceHolder) {}
                override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) {
                    if (id >= 0) { backend.resize(id, w, hh, dpi); return }
                    id = backend.createDisplay("vd-panel", h.surface, w, hh, dpi)
                    if (id >= 0) {
                        Log.i("VdPanel", "display created id=$id ${w}x$hh")
                        backend.launch(id, component)
                    } else if (retries++ < 30) {
                        Log.i("VdPanel", "createDisplay not ready, retry $retries")
                        sv.postDelayed({ surfaceChanged(h, f, w, hh) }, 1000)
                    } else {
                        Log.e("VdPanel", "createDisplay gave up after $retries retries")
                    }
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
