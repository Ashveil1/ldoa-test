package com.example.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveMode
import com.example.ui.theme.UbuntuAubergine
import com.example.ui.theme.UbuntuOrange
import com.example.ui.theme.YaruBorder
import com.example.ui.theme.YaruTextMuted
import com.example.ui.theme.YaruTextWhite
import com.example.ui.theme.YaruTopPanelBg
import com.example.ui.theme.YaruWindowHeader
import com.example.viewmodel.AetherDesktopViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UbuntuTopPanel(
    viewModel: AetherDesktopViewModel,
    onToggleActivities: () -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()
    var isQuickSettingsOpen by remember { mutableStateOf(false) }

    // Ticking top-panel clock: remember{} alone froze it at composition.
    var formattedDate by remember {
        mutableStateOf(SimpleDateFormat("EEE MMM dd  HH:mm", Locale.US).format(Date()))
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            formattedDate = SimpleDateFormat("EEE MMM dd  HH:mm", Locale.US).format(Date())
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // GNOME Top Panel Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(YaruTopPanelBg)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Activities
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .clickable { onToggleActivities() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Activities",
                    color = YaruTextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Center: Date & Time
            Text(
                text = formattedDate,
                color = YaruTextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            // Right: System Status & Quick Settings Pill
            Surface(
                color = if (isQuickSettingsOpen) Color(0xFF282828) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .clickable { isQuickSettingsOpen = !isQuickSettingsOpen }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("ubuntu_quick_settings_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = YaruTextWhite, modifier = Modifier.size(13.dp))
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = YaruTextWhite, modifier = Modifier.size(13.dp))
                    Icon(Icons.Default.BatteryFull, contentDescription = null, tint = YaruTextWhite, modifier = Modifier.size(14.dp))
                    Text(
                        text = "${telemetry.batteryPercent}%",
                        color = YaruTextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // GNOME 46 Quick Settings Menu Dropdown
        if (isQuickSettingsOpen) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 34.dp, end = 8.dp)
                    .width(280.dp)
                    .shadow(12.dp, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header: User & Battery
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(UbuntuOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("U", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ubuntu", color = YaruTextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Ubuntu 24.04 LTS (Noble)", color = YaruTextMuted, fontSize = 10.sp)
                        }
                        IconButton(
                            onClick = {
                                viewModel.setActiveMode(ActiveMode.HOME_LAUNCHER)
                                isQuickSettingsOpen = false
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Return to Android Home", tint = UbuntuOrange, modifier = Modifier.size(18.dp))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = YaruBorder)

                    // Quick Toggle Buttons (Wi-Fi, Bluetooth, Performance, Android Mode)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = UbuntuOrange.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = UbuntuOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Wi-Fi", color = YaruTextWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Connected", color = YaruTextMuted, fontSize = 9.sp)
                                }
                            }
                        }

                        Surface(
                            color = Color(0xFF333333),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = YaruTextWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Bluetooth", color = YaruTextWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Keyboard/Mouse", color = YaruTextMuted, fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Switch to Android Home Launcher Button
                    Surface(
                        color = Color(0xFF333333),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setActiveMode(ActiveMode.HOME_LAUNCHER)
                                isQuickSettingsOpen = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Android, contentDescription = null, tint = Color(0xFF34A853), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Switch to Android Phone Launcher", color = YaruTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Return to handheld mobile home screen", color = YaruTextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
