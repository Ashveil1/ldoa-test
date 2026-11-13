package com.example.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppType
import com.example.model.LinuxAppDefinition
import com.example.ui.theme.UbuntuAubergine
import com.example.ui.theme.UbuntuOrange
import com.example.ui.theme.YaruDockBg
import com.example.ui.theme.YaruTextWhite
import com.example.viewmodel.AetherDesktopViewModel

@Composable
fun UbuntuLeftDock(
    viewModel: AetherDesktopViewModel,
    onShowAllApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val windows by viewModel.windows.collectAsState()
    val focusedId by viewModel.focusedWindowId.collectAsState()

    val dockApps = listOf(
        DockItem("terminal", "Ubuntu Terminal", Icons.Default.Terminal, Color(0xFF300A24), Color.White),
        DockItem("files", "Nautilus Files", Icons.Default.Folder, Color(0xFFE95420), Color.White),
        DockItem("editor", "Code Studio", Icons.Default.Code, Color(0xFF007ACC), Color.White),
        DockItem("gpu", "GPU Hardware Settings", Icons.Default.Speed, Color(0xFF77216F), Color.White),
        DockItem("android_hub", "Android Bridge Apps", Icons.Default.Android, Color(0xFF34A853), Color.White)
    )

    Column(
        modifier = modifier
            .width(52.dp)
            .fillMaxHeight()
            .background(YaruDockBg)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dockApps.forEach { item ->
                val isRunning = windows.any { it.internalAppKey == item.key }
                val isFocused = windows.any { it.internalAppKey == item.key && it.id == focusedId && !it.isMinimized }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            if (item.key == "android_hub") {
                                viewModel.setAppDrawerOpen(true)
                            } else {
                                val openWin = windows.find { it.internalAppKey == item.key }
                                if (openWin != null) {
                                    if (openWin.isMinimized) viewModel.unminimizeWindow(openWin.id)
                                    else viewModel.focusWindow(openWin.id)
                                } else {
                                    viewModel.openLinuxApp(
                                        LinuxAppDefinition(
                                            id = item.key,
                                            name = item.title,
                                            category = "System",
                                            description = "",
                                            command = item.key,
                                            iconName = item.key
                                        )
                                    )
                                }
                            }
                        }
                        .padding(horizontal = 2.dp)
                ) {
                    // Authentic Ubuntu Orange Running Dot / Pill on the left edge
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(if (isFocused) 16.dp else if (isRunning) 6.dp else 0.dp)
                            .background(if (isRunning) UbuntuOrange else Color.Transparent, RoundedCornerShape(2.dp))
                    )

                    Spacer(modifier = Modifier.width(3.dp))

                    // Square / Rounded App Icon
                    Surface(
                        color = item.bgColor,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = item.tintColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // GNOME 9-Dots App Grid Launcher at bottom of dock
        Surface(
            color = Color(0xFF222222),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .size(40.dp)
                .clickable { onShowAllApps() }
                .testTag("ubuntu_show_apps_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Show Applications",
                    tint = YaruTextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

data class DockItem(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val bgColor: Color,
    val tintColor: Color
)
