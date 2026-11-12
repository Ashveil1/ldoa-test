package com.example.vd

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rikka.shizuku.Shizuku

/**
 * M1 dev entry (no launcher icon; started by the emulator workflow via
 * `am start`): one VdPanel running com.android.settings through the
 * Shizuku path, with the backend state printed above it and logged to
 * logcat tag VdSmoke. The workflow asserts state=READY in logcat and a
 * non-black screencap.
 *
 * ShizukuVdBackend.start() is single-shot: a permission grant or a
 * late-starting server is never retried, so this harness re-drives
 * start() on the permission result and while the binder is missing.
 */
class VdSmokeActivity : ComponentActivity() {

    private val stateText = mutableStateOf("starting")
    private lateinit var backend: ShizukuVdBackend
    private val handler = Handler(Looper.getMainLooper())

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        Log.i("VdSmoke", "permission result=$grantResult")
        if (grantResult == PackageManager.PERMISSION_GRANTED) backend.start()
    }

    private val retry = object : Runnable {
        override fun run() {
            val s = stateText.value
            if (s == ShizukuVdBackend.State.NOT_RUNNING.name) {
                Log.i("VdSmoke", "retrying start() - binder was missing")
                backend.start()
            }
            if (s != ShizukuVdBackend.State.READY.name) handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backend = ShizukuVdBackend(this) { s ->
            Log.i("VdSmoke", "state=${s.name}")
            stateText.value = s.name
        }
        setContent {
            Column(Modifier.fillMaxSize().background(Color(0xFF202020))) {
                Text(
                    "VD smoke - shizuku: ${stateText.value}",
                    color = Color.White, fontSize = 14.sp,
                    modifier = Modifier.padding(8.dp)
                )
                Box(Modifier.fillMaxWidth().height(0.dp).weight(1f)) {
                    VdPanel(
                        ctx = this@VdSmokeActivity,
                        backend = backend,
                        component = "com.android.settings/.Settings",
                        dpi = resources.displayMetrics.densityDpi,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(permListener)
        backend.start()
        handler.postDelayed(retry, 3000)
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener)
        handler.removeCallbacks(retry)
        super.onDestroy()
    }
}
