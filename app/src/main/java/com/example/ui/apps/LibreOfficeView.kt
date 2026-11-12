package com.example.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextFrostWhite
import com.example.ui.theme.TextMuted

@Composable
fun LibreOfficeView(
    modifier: Modifier = Modifier
) {
    var documentTitle by remember { mutableStateOf("Linux_Architecture_Specification.odt") }
    var documentContent by remember {
        mutableStateOf(
            """# AetherLinux Operating System Architecture

1. Executive Overview
AetherLinux establishes an integrated runtime environment allowing mobile Android hardware to operate as an enterprise-grade Ubuntu Linux workstation.

2. Hardware Acceleration
Using VirGL driver architecture, 3D graphics calls are bridged with near-native latency directly onto the mobile Adreno/Mali GPU shader cores.

3. Touch Ergonomics
Unlike legacy desktop ports requiring a virtual mouse trackpad, AetherLinux maps native multi-touch gestures directly to window handles, text manipulation, and UI elements.

4. Multi-Display Presentation
Connecting an external monitor automatically bifurcates tasks: the high-resolution external display hosts the primary desktop windowing canvas, while the phone touchscreen acts as a companion launcher dock.
"""
        )
    }

    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isSavedNotification by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("libreoffice_view")
    ) {
        // LibreOffice Menu & Formatting Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = NeonCyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "WRITER",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = documentTitle,
                color = TextFrostWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { isBold = !isBold },
                modifier = Modifier
                    .size(32.dp)
                    .background(if (isBold) ObsidianSurfaceVariant else Color.Transparent, RoundedCornerShape(4.dp))
            ) {
                Icon(
                    Icons.Default.FormatBold,
                    contentDescription = "Bold",
                    tint = if (isBold) NeonCyan else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { isItalic = !isItalic },
                modifier = Modifier
                    .size(32.dp)
                    .background(if (isItalic) ObsidianSurfaceVariant else Color.Transparent, RoundedCornerShape(4.dp))
            ) {
                Icon(
                    Icons.Default.FormatItalic,
                    contentDescription = "Italic",
                    tint = if (isItalic) NeonCyan else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = { isSavedNotification = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("libreoffice_save_button")
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "Save",
                    tint = ObsidianBg,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save", color = ObsidianBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Notification banner if saved
        if (isSavedNotification) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalGreen.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Document saved to /home/ubuntu/Documents/$documentTitle",
                    color = TerminalGreen,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text("Dismiss", color = TextFrostWhite, fontSize = 11.sp, modifier = Modifier.clickable { isSavedNotification = false })
            }
        }

        // Document Page Surface (mimicking desktop paper view with generous touch margin)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .background(Color(0xFF131820), RoundedCornerShape(6.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                .padding(12.dp)
        ) {
            TextField(
                value = documentContent,
                onValueChange = { documentContent = it },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("libreoffice_editor_field"),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                    color = TextFrostWhite
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = TextFrostWhite,
                    unfocusedTextColor = TextFrostWhite,
                    cursorColor = NeonCyan,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        // Status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Words: ${documentContent.split("\\s+".toRegex()).filter { it.isNotBlank() }.size}  |  Characters: ${documentContent.length}",
                color = TextMuted,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Format: ODF (.odt) - 100% Zoom",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
