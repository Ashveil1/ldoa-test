package com.example.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TerminalAmber
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextFrostWhite
import com.example.ui.theme.TextMuted

data class CodeFile(
    val name: String,
    val language: String,
    val initialContent: String
)

@Composable
fun CodeStudioView(
    modifier: Modifier = Modifier
) {
    val files = remember {
        listOf(
            CodeFile(
                name = "main.py",
                language = "Python",
                initialContent = """# AetherLinux Desktop Python Engine
import os
import sys

def benchmark_subsystem():
    print("Ubuntu 24.04 Environment Initialized")
    print(f"Python Platform: {sys.platform}")
    print("VirGL Acceleration: ACTIVE (OpenGL ES 3.2)")
    
    total = sum([x**2 for x in range(100000)])
    print(f"Computation benchmark result: {total}")

if __name__ == "__main__":
    benchmark_subsystem()
"""
            ),
            CodeFile(
                name = "server.js",
                language = "Node.js",
                initialContent = """// Node.js Local Linux Server
const http = require('http');
const server = http.createServer((req, res) => {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'Online', engine: 'AetherOS' }));
});
server.listen(3000, () => console.log('Listening on port 3000'));
"""
            ),
            CodeFile(
                name = "config.yaml",
                language = "YAML",
                initialContent = """subsystem:
  distro: ubuntu-noble
  gpu_driver: virgl
  touch_mode: direct_gesture
  external_monitor: auto_detect
  display_scaling: 100%
"""
            )
        )
    }

    var selectedFileIndex by remember { mutableStateOf(0) }
    var codeContent by remember(selectedFileIndex) { mutableStateOf(files[selectedFileIndex].initialContent) }
    var terminalOutput by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("code_studio_view")
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Code Studio",
                color = TextFrostWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            // File Tabs
            files.forEachIndexed { index, file ->
                val isSelected = index == selectedFileIndex
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) ObsidianSurfaceVariant else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { selectedFileIndex = index }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = file.name,
                        color = if (isSelected) NeonCyan else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    isRunning = true
                    terminalOutput = ">>> Running ${files[selectedFileIndex].name} in Ubuntu proot environment...\n" +
                            "[stdout] Python 3.12.3 executed successfully.\n" +
                            "[stdout] Ubuntu 24.04 Environment Initialized\n" +
                            "[stdout] Python Platform: linux-aarch64\n" +
                            "[stdout] VirGL Acceleration: ACTIVE (OpenGL ES 3.2)\n" +
                            "[stdout] Computation benchmark result: 333328333350000\n" +
                            "Process exited with code 0."
                    isRunning = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("code_run_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run Code",
                    tint = ObsidianBg,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Run", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Editor Body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            TextField(
                value = codeContent,
                onValueChange = { codeContent = it },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("code_editor_field"),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextFrostWhite
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ObsidianBg,
                    unfocusedContainerColor = ObsidianBg,
                    focusedTextColor = TextFrostWhite,
                    unfocusedTextColor = TextFrostWhite,
                    cursorColor = NeonCyan,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        // Run Console Output Panel (collapsible/visible when run)
        if (terminalOutput.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF070A0F))
                    .border(1.dp, BorderSubtle)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TERMINAL EXECUTION OUTPUT",
                        color = TerminalAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Clear",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { terminalOutput = "" }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = terminalOutput,
                    color = TerminalGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
