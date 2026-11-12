package com.example.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveMode
import com.example.model.AppType
import com.example.model.WindowSession
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassTaskbar
import com.example.ui.theme.GlassWindowHeader
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TerminalAmber
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextFrostWhite
import com.example.ui.theme.TextMuted
import com.example.viewmodel.AetherDesktopViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskbarDock(
    viewModel: AetherDesktopViewModel,
    modifier: Modifier = Modifier
) {
    val windows by viewModel.windows.collectAsState()
    val focusedId by viewModel.focusedWindowId.collectAsState()
    val isStartOpen by viewModel.isStartMenuOpen.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val monitorStatus by viewModel.displayManager.monitorStatus.collectAsState()

    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.US).format(Date())
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Start Menu Popup (if open)
        if (isStartOpen) {
            StartMenuPopup(
                viewModel = viewModel,
                onDismiss = { viewModel.closeStartMenu() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 54.dp)
            )
        }

        // Bottom Taskbar Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(50.dp)
                .background(GlassTaskbar)
                .border(width = 1.dp, color = BorderSubtle)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start Menu Button
            Surface(
                color = if (isStartOpen) NeonCyan else ObsidianSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .size(38.dp)
                    .clickable { viewModel.toggleStartMenu() }
                    .testTag("taskbar_start_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Start Menu",
                        tint = if (isStartOpen) ObsidianBg else NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Switch to Home Launcher button
            IconButton(
                onClick = { viewModel.setActiveMode(ActiveMode.HOME_LAUNCHER) },
                modifier = Modifier
                    .size(36.dp)
                    .background(ObsidianSurfaceVariant, RoundedCornerShape(8.dp))
                    .testTag("taskbar_home_button")
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Android Home",
                    tint = TextFrostWhite,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(BorderSubtle)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Running Window Tabs (Pills)
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(windows, key = { it.id }) { win ->
                    val isFocused = win.id == focusedId && !win.isMinimized
                    Surface(
                        color = when {
                            isFocused -> NeonCyan.copy(alpha = 0.25f)
                            win.isMinimized -> ObsidianSurfaceVariant.copy(alpha = 0.5f)
                            else -> ObsidianSurfaceVariant
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .clickable {
                                if (win.isMinimized) {
                                    viewModel.unminimizeWindow(win.id)
                                } else if (win.id == focusedId) {
                                    viewModel.minimizeWindow(win.id)
                                } else {
                                    viewModel.focusWindow(win.id)
                                }
                            }
                            .border(
                                1.dp,
                                if (isFocused) NeonCyan else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = resolveAppIcon(win.iconName, win.appType),
                                contentDescription = null,
                                tint = if (win.appType == AppType.ANDROID_APP) TerminalGreen else NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = win.title,
                                color = if (isFocused) TextFrostWhite else TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // System Tray Indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // External display indicator
                if (monitorStatus.isConnected) {
                    Surface(
                        color = TerminalGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Docked", color = TerminalGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // FPS Counter
                Surface(
                    color = ObsidianSurfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${telemetry.gpuFps} FPS",
                        color = TerminalGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Icon(Icons.Default.Wifi, contentDescription = "Wi-Fi", tint = TextFrostWhite, modifier = Modifier.size(16.dp))
                Icon(Icons.Default.BatteryFull, contentDescription = "Battery", tint = TerminalGreen, modifier = Modifier.size(16.dp))

                Text(
                    text = currentTime,
                    color = TextFrostWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun StartMenuPopup(
    viewModel: AetherDesktopViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val linuxApps by viewModel.linuxEngine.installedApps.collectAsState()
    val androidApps by viewModel.appsManager.androidApps.collectAsState()
    var filterQuery by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .width(360.dp)
            .height(460.dp)
            .shadow(20.dp, RoundedCornerShape(12.dp))
            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .testTag("start_menu_popup"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWindowHeader)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // User Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("U", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ubuntu@aether-box",
                        color = TextFrostWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ubuntu 24.04.1 LTS (VirGL 3D)",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            TextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Search Linux & Android apps...", color = TextMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface,
                    focusedTextColor = TextFrostWhite,
                    unfocusedTextColor = TextFrostWhite,
                    cursorColor = NeonCyan,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Apps List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("LINUX WORKSTATION APPS", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))

                linuxApps
                    .filter { it.name.contains(filterQuery, ignoreCase = true) || it.category.contains(filterQuery, ignoreCase = true) }
                    .forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.openLinuxApp(app)
                                    onDismiss()
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(ObsidianSurfaceVariant, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(resolveAppIcon(app.iconName, AppType.LINUX_APP), contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(app.name, color = TextFrostWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(app.description, color = TextMuted, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }

                Spacer(modifier = Modifier.height(10.dp))
                Text("BRIDGED ANDROID APPS", color = TerminalGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))

                androidApps
                    .filter { it.appName.contains(filterQuery, ignoreCase = true) }
                    .take(6)
                    .forEach { androidApp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.openAndroidAppInWindow(androidApp.packageName, androidApp.appName)
                                    onDismiss()
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(TerminalGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Android, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(androidApp.appName, color = TextFrostWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderSubtle)

            // Start Menu Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ObsidianSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable {
                        viewModel.setActiveMode(ActiveMode.HOME_LAUNCHER)
                        onDismiss()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = TextFrostWhite, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mobile Launcher", color = TextFrostWhite, fontSize = 11.sp)
                    }
                }

                Surface(
                    color = ObsidianSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable {
                        viewModel.openLinuxApp(
                            com.example.model.LinuxAppDefinition(
                                id = "gpu",
                                name = "GPU Settings",
                                category = "Hardware",
                                description = "VirGL & Display",
                                command = "virgl",
                                iconName = "speed"
                            )
                        )
                        onDismiss()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GPU Tuning", color = NeonCyan, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
