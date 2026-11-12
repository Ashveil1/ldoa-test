package com.example.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.SetupState

/** First-run gate: the real Ubuntu userland must be installed before the
 *  app is usable. Resumable download, explicit Retry; clean one-screen look. */
@Composable
fun FirstRunSetupScreen(state: SetupState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(Color(0xFF1C1C22), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(">", color = Color(0xFF4E9A06), fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("AetherLinux", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Setting up Ubuntu 24.04. This downloads about 30 MB once.",
            color = Color(0xFF9A9AA2), fontSize = 14.sp, textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (state) {
            is SetupState.Running -> {
                LinearProgressIndicator(
                    progress = { state.percent / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color(0xFF4E9A06),
                    trackColor = Color(0xFF26262E)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(state.status, color = Color(0xFF9A9AA2), fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            is SetupState.Failed -> {
                Text(
                    "Download failed: ${state.message}\nPartial progress is kept.",
                    color = Color(0xFFEF2929), fontSize = 13.sp, textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E9A06)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            else -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color(0xFF4E9A06),
                    trackColor = Color(0xFF26262E)
                )
            }
        }
    }
}
