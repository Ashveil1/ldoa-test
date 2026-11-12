package com.example.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppType
import com.example.model.LinuxAppDefinition
import com.example.ui.theme.UbuntuAubergine
import com.example.ui.theme.UbuntuAubergineDark
import com.example.ui.theme.UbuntuOrange
import com.example.ui.theme.YaruBorder
import com.example.ui.theme.YaruTextMuted
import com.example.ui.theme.YaruTextWhite
import com.example.viewmodel.AetherDesktopViewModel

@Composable
fun DesktopWorkspace(
    viewModel: AetherDesktopViewModel,
    modifier: Modifier = Modifier
) {
    val windows by viewModel.windows.collectAsState()
    var isOverviewOpen by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // Authentic Ubuntu 24.04 Noble Aubergine Wallpaper
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF5E2750), // Ubuntu Noble Aubergine Mid
                        Color(0xFF2C001E), // Deep Canonical Aubergine
                        Color(0xFF160914)  // Outer Dark Vignette
                    ),
                    radius = 1400f
                )
            )
            .testTag("desktop_workspace")
    ) {
        val parentW = maxWidth.value
        val parentH = maxHeight.value

        Column(modifier = Modifier.fillMaxSize()) {
            // GNOME Top Panel (30dp)
            UbuntuTopPanel(
                viewModel = viewModel,
                onToggleActivities = { isOverviewOpen = !isOverviewOpen }
            )

            // Main Desktop Area: Left Dock + Canvas
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Ubuntu Left Dash Dock (52dp)
                UbuntuLeftDock(
                    viewModel = viewModel,
                    onShowAllApps = { isOverviewOpen = true }
                )

                // Desktop Surface with standard Ubuntu desktop icons
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    // Standard Desktop Icons (Home Folder, Trash, Storage)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UbuntuDesktopIcon(
                            title = "Home",
                            icon = Icons.Default.Folder,
                            tint = UbuntuOrange,
                            onClick = {
                                viewModel.openLinuxApp(
                                    LinuxAppDefinition("files", "Nautilus Files", "Utilities", "", "files", "folder")
                                )
                            }
                        )

                        UbuntuDesktopIcon(
                            title = "Trash",
                            icon = Icons.Default.Delete,
                            tint = Color(0xFFAAAAAA),
                            onClick = {
                                viewModel.openLinuxApp(
                                    LinuxAppDefinition("files", "Trash", "Utilities", "", "files", "folder")
                                )
                            }
                        )

                        UbuntuDesktopIcon(
                            title = "Terminal",
                            icon = Icons.Default.Terminal,
                            tint = Color(0xFF4E9A06),
                            onClick = {
                                viewModel.openLinuxApp(
                                    LinuxAppDefinition("terminal", "Ubuntu Terminal", "System", "", "bash", "terminal")
                                )
                            }
                        )
                    }
                }
            }
        }

        // Floating Windows Layer
        windows.forEach { window ->
            FloatingWindowFrame(
                window = window,
                viewModel = viewModel,
                parentWidthDp = parentW,
                parentHeightDp = parentH
            )
        }

        // GNOME Activities / Show All Apps Overlay
        if (isOverviewOpen) {
            UbuntuAppOverviewModal(
                viewModel = viewModel,
                onDismiss = { isOverviewOpen = false }
            )
        }
    }
}

@Composable
fun UbuntuDesktopIcon(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(68.dp)
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Surface(
            color = Color(0x33000000),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
