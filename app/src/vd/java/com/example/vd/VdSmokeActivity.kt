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

/**
 * M1 dev entry (no launcher icon; started by the emulator workflow via
 * `am start`): one VdPanel running com.android.settings through the
 * Shizuku path, with the backend state printed above it and logged to
 * logcat tag VdSmoke. The workflow asserts state=READY in logcat and a
 * non-black screencap. The backend owns the Shizuku lifecycle
 * (binder/permission/death listeners re-drive it), so this activity
 * only starts and stops it.
 */
class VdSmokeActivity : ComponentActivity() {

    private val stateText = mutableStateOf("starting")
    private lateinit var backend: ShizukuVdBackend

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
        backend.start()
    }

    override fun onDestroy() {
        backend.stop()
        super.onDestroy()
    }
}
