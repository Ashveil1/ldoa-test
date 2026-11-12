package com.example.ui.launcher

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.WallpaperHelper
import com.example.model.LinuxAppDefinition
import com.example.ui.desktop.FloatingWindowFrame
import com.example.ui.theme.GoogleGreen
import com.example.viewmodel.AetherDesktopViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HomeScreenAppEntry(
    val name: String,
    val keyOrPackage: String,
    val isLinux: Boolean = false,
    val linuxDef: LinuxAppDefinition? = null
)

@Composable
fun AndroidHomeLauncher(
    viewModel: AetherDesktopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val telemetry by viewModel.telemetry.collectAsState()
    val windows by viewModel.windows.collectAsState()
    val isDrawerOpen by viewModel.isAppDrawerOpen.collectAsState()
    val isCustomizationOpen by viewModel.isCustomizationOpen.collectAsState()
    val customization by viewModel.customization.collectAsState()

    var systemWallpaper by remember { mutableStateOf<Bitmap?>(null) }
    var totalVerticalDrag by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        systemWallpaper = WallpaperHelper.getSystemWallpaperBitmap(context)
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val now = Date()

    // Authentic apps grid with real brand logos / APK icons
    val homeApps = listOf(
        HomeScreenAppEntry("Terminal", "terminal", true, LinuxAppDefinition("terminal", "Terminal (Ubuntu PRoot)", "System", "", "bash", "terminal")),
        HomeScreenAppEntry("Files", "files", true, LinuxAppDefinition("files", "Files", "Utilities", "", "files", "folder")),
        HomeScreenAppEntry("Code Studio", "editor", true, LinuxAppDefinition("editor", "Code Studio", "Development", "", "code", "code")),
        HomeScreenAppEntry("LibreOffice", "office", true, LinuxAppDefinition("office", "LibreOffice", "Productivity", "", "office", "description")),
        HomeScreenAppEntry("GIMP Paint", "gimp", true, LinuxAppDefinition("gimp", "GIMP Paint", "Graphics", "", "gimp", "brush")),
        HomeScreenAppEntry("Chrome", "com.android.chrome"),
        HomeScreenAppEntry("Photos", "com.google.android.apps.photos"),
        HomeScreenAppEntry("Camera", "com.android.camera2"),
        HomeScreenAppEntry("YouTube", "com.google.android.youtube"),
        HomeScreenAppEntry("Maps", "com.google.android.apps.maps"),
        HomeScreenAppEntry("Keep Notes", "com.google.android.keep"),
        HomeScreenAppEntry("GPU Hardware", "gpu", true, LinuxAppDefinition("gpu", "GPU Hardware", "Hardware", "", "virgl", "speed")),
        HomeScreenAppEntry("Ubuntu Desktop", "linux-desktop", true, LinuxAppDefinition("linux-desktop", "Ubuntu Desktop (real)", "System", "", "gnome-session", "desktop_windows"))
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // Highly sensitive & reliable Swipe Up gesture anywhere on home screen
            .pointerInput(customization.enableAppSwipeUp) {
                if (customization.enableAppSwipeUp) {
                    detectDragGestures(
                        onDragStart = { totalVerticalDrag = 0f },
                        onDrag = { change, dragAmount ->
                            totalVerticalDrag += dragAmount.y
                            // If user drags up more than 28dp, open app drawer
                            if (totalVerticalDrag < -30f) {
                                change.consume()
                                viewModel.setAppDrawerOpen(true)
                            }
                        },
                        onDragEnd = { totalVerticalDrag = 0f },
                        onDragCancel = { totalVerticalDrag = 0f }
                    )
                }
            }
            .testTag("android_home_launcher")
    ) {
        val parentW = maxWidth.value
        val parentH = maxHeight.value

        // 1. Wallpaper Layer: Load Real System Wallpaper if available, else stock Pixel dark gradient
        if (systemWallpaper != null) {
            Image(
                bitmap = systemWallpaper!!.asImageBitmap(),
                contentDescription = "System Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Adjustable user scrim so real icons and typography pop vividly
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = customization.wallpaperDimLevel))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E242B),
                                Color(0xFF15191E),
                                Color(0xFF0F1216)
                            )
                        )
                    )
            )
        }

        // 2. Android Home Screen Foreground
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Clean AOSP/iOS-style clock + date
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setAppDrawerOpen(true) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeFormat.format(now),
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now),
                    color = Color(0xCCFFFFFF),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Android Custom Columns Apps Grid with Authentic Real Logos & APK Icons
            LazyVerticalGrid(
                columns = GridCells.Fixed(customization.gridColumns),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(homeApps) { app ->
                    val realIcon = viewModel.appsManager.getAppIcon(app.keyOrPackage)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (app.isLinux && app.linuxDef != null) {
                                    viewModel.openLinuxApp(app.linuxDef)
                                } else {
                                    if (customization.openAppsInFloatingWindow) {
                                        viewModel.openAndroidAppInWindow(app.keyOrPackage, app.name)
                                    } else {
                                        viewModel.appsManager.launchAppFreeform(app.keyOrPackage)
                                    }
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        RealAppIcon(
                            keyOrPackage = app.keyOrPackage,
                            realBitmap = realIcon,
                            size = customization.iconSizeDp.dp
                        )

                        if (customization.showIconLabels) {
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = app.name,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Minimized Floating Apps Bar
            val minimizedWindows = windows.filter { it.isMinimized }
            if (minimizedWindows.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(minimizedWindows) { win ->
                        Surface(
                            color = Color(0xCC20232A),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.clickable { viewModel.unminimizeWindow(win.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(GoogleGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = win.title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Slim swipe-up handle (all apps)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setAppDrawerOpen(true) }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "All apps",
                    tint = Color(0xAAFFFFFF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Stock Android 5-App Bottom Dock (Real Icons for Phone, Messages, Terminal, Chrome, Camera)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Phone
                RealAppIcon(
                    keyOrPackage = "phone",
                    realBitmap = viewModel.appsManager.getAppIcon("com.google.android.dialer"),
                    size = 52.dp,
                    onClick = { viewModel.appsManager.launchAppFreeform("com.google.android.dialer") }
                )

                // 2. Messages
                RealAppIcon(
                    keyOrPackage = "messages",
                    realBitmap = viewModel.appsManager.getAppIcon("com.google.android.apps.messaging"),
                    size = 52.dp,
                    onClick = { viewModel.appsManager.launchAppFreeform("com.google.android.apps.messaging") }
                )

                // 3. Center: Real Ubuntu Terminal (Floating window)
                RealAppIcon(
                    keyOrPackage = "terminal",
                    size = 52.dp,
                    onClick = {
                        viewModel.openLinuxApp(LinuxAppDefinition("terminal", "Terminal (Ubuntu PRoot)", "System", "", "bash", "terminal"))
                    }
                )

                // 4. Chrome
                RealAppIcon(
                    keyOrPackage = "chrome",
                    realBitmap = viewModel.appsManager.getAppIcon("com.android.chrome"),
                    size = 52.dp,
                    onClick = { viewModel.appsManager.launchAppFreeform("com.android.chrome") }
                )

                // 5. Camera
                RealAppIcon(
                    keyOrPackage = "camera",
                    realBitmap = viewModel.appsManager.getAppIcon("com.android.camera2"),
                    size = 52.dp,
                    onClick = { viewModel.appsManager.launchAppFreeform("com.android.camera2") }
                )
            }
        }

        // 3. Floating Windows Layer (Directly on top of Android Home Launcher)
        windows.forEach { window ->
            FloatingWindowFrame(
                window = window,
                viewModel = viewModel,
                parentWidthDp = parentW,
                parentHeightDp = parentH
            )
        }

        // 4. App Drawer Sheet (when swiped up or tapped)
        if (isDrawerOpen) {
            AppDrawerSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.setAppDrawerOpen(false) }
            )
        }

        // 5. Launcher Customization Sheet (when tapped settings)
        if (isCustomizationOpen) {
            LauncherCustomizationSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.setCustomizationOpen(false) }
            )
        }
    }
}
