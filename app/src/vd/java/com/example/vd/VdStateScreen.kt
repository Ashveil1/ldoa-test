package com.example.vd

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.os.SystemClock
import android.view.Choreographer

/** Thai state screen shown in place of VdPanel until the backend is READY. */
@Composable
fun VdStateScreen(ctx: Context, state: ShizukuVdBackend.State, installed: Boolean, onRetry: () -> Unit, onFloatingFallback: () -> Unit) {
    val (title, body, action) = when {
        !installed -> Triple("ยังไม่ได้ติดตั้ง Shizuku", "แอพจริงในกรอบนี้ต้องใช้ Shizuku", "ติดตั้ง Shizuku" to {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) })
        state == ShizukuVdBackend.State.NOT_RUNNING -> Triple("Shizuku ยังไม่ทำงาน", "เปิด Wireless debugging แล้วเปิด Shizuku ใหม่ (ต้องทำทุกครั้งหลังรีสตาร์ท)", "ลองอีกครั้ง" to onRetry)
        state == ShizukuVdBackend.State.NEED_PERMISSION -> Triple("ต้องอนุญาตสิทธิ์", "กดอนุญาตในหน้าต่าง Shizuku", "ขออนุญาตอีกครั้ง" to onRetry)
        state == ShizukuVdBackend.State.DIED -> Triple("Shizuku หยุดทำงาน", "มักเกิดตอนล็อกจอ แอพในกรอบถูกปิด", "เชื่อมต่อใหม่" to onRetry)
        else -> Triple("กำลังเชื่อมต่อ...", "", null)
    }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (body.isNotEmpty()) Text(body, Modifier.padding(top = 8.dp))
        action?.let { (label, fn) -> Button(onClick = fn, modifier = Modifier.padding(top = 16.dp)) { Text(label) } }
        if (state != ShizukuVdBackend.State.CONNECTING)
            TextButton(onClick = onFloatingFallback) { Text("เปิดแบบหน้าต่างลอยแทน") }
    }
}

/** Counts frames the panel's Choreographer delivers while a display is live; log 1/s as "VD_FPS n". Display-side fps needs a Surface frame listener (M4b). */
class VdFpsMeter {
    private var frames = 0; private var last = SystemClock.uptimeMillis()
    private val cb = object : Choreographer.FrameCallback {
        override fun doFrame(t: Long) {
            frames++
            val now = SystemClock.uptimeMillis()
            if (now - last >= 1000) { android.util.Log.i("VD_FPS", "ui=${frames * 1000 / (now - last)}"); frames = 0; last = now }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }
    fun start() = Choreographer.getInstance().postFrameCallback(cb)
    fun stop() = Choreographer.getInstance().removeFrameCallback(cb)
}
