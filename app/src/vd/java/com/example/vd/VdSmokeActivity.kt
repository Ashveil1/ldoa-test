package com.example.vd

import android.os.Bundle
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
import com.example.engine.InstalledAppsManager

/**
 * M1 dev entry (no launcher icon; started by the emulator workflow via
 * `am start`): one VdPanel running com.android.settings through the
 * Shizuku path, with the backend state printed above it and logged to
 * logcat tag VdSmoke. The workflow asserts state=READY in logcat and a
 * non-black screencap. The backend owns the Shizuku lifecycle
 * (binder/permission/death listeners re-drive it), so this activity
 * only starts and stops it. While the backend is not READY the Thai
 * VdStateScreen stands in for the panel; VD_FPS logs the UI frame rate.
 */
class VdSmokeActivity : ComponentActivity() {

    private val stateText = mutableStateOf("starting")
    private lateinit var backend: ShizukuVdBackend
    private val fpsMeter = VdFpsMeter()
    private var fpsRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backend = ShizukuVdBackend(this) { s ->
            Log.i("VdSmoke", "state=${s.name}")
            stateText.value = s.name
            if (s == ShizukuVdBackend.State.READY && !fpsRunning) {
                fpsRunning = true
                fpsMeter.start()
            }
        }
        setContent {
            Column(Modifier.fillMaxSize().background(Color(0xFF202020))) {
                Text(
                    "VD smoke - shizuku: ${stateText.value}",
                    color = Color.White, fontSize = 14.sp,
                    modifier = Modifier.padding(8.dp)
                )
                Box(Modifier.fillMaxWidth().height(0.dp).weight(1f)) {
                    val state = runCatching { ShizukuVdBackend.State.valueOf(stateText.value) }.getOrNull()
                    if (state == ShizukuVdBackend.State.READY) {
                        VdPanel(
                            ctx = this@VdSmokeActivity,
                            backend = backend,
                            component = "com.android.settings/.Settings",
                            dpi = resources.displayMetrics.densityDpi,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        VdStateScreen(
                            ctx = this@VdSmokeActivity,
                            state = state ?: ShizukuVdBackend.State.CONNECTING,
                            installed = isShizukuInstalled(),
                            onRetry = { backend.start() },
                            onFloatingFallback = {
                                InstalledAppsManager(this@VdSmokeActivity).launchAppFreeform("com.android.settings")
                            }
                        )
                    }
                }
            }
        }
        backend.start()
    }

    private fun isShizukuInstalled() = runCatching {
        packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
    }.isSuccess

    override fun onDestroy() {
        if (fpsRunning) fpsMeter.stop()
        backend.stop()
        super.onDestroy()
    }
}
