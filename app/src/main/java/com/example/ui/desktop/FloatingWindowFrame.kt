package com.example.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.engine.InstalledAppsManager
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.AppType
import com.example.model.SnapState
import com.example.model.WindowSession
import com.example.ui.apps.CodeStudioView
import com.example.ui.apps.GimpCanvasView
import com.example.ui.apps.GpuAccelerationPanel
import com.example.ui.apps.LibreOfficeView
import com.example.ui.apps.LinuxTerminalView
import com.example.ui.apps.SmartFileOrganizerView
import com.example.ui.theme.UbuntuAubergine
import com.example.ui.theme.UbuntuOrange
import com.example.ui.theme.YaruBorder
import com.example.ui.theme.YaruTextMuted
import com.example.ui.theme.YaruTextWhite
import com.example.ui.theme.YaruWindowBody
import com.example.ui.theme.YaruWindowButtonClose
import com.example.ui.theme.YaruWindowButtonMinMax
import com.example.ui.theme.YaruWindowHeader
import com.example.viewmodel.AetherDesktopViewModel
import kotlin.math.roundToInt

@Composable
fun FloatingWindowFrame(
    window: WindowSession,
    viewModel: AetherDesktopViewModel,
    parentWidthDp: Float,
    parentHeightDp: Float,
    modifier: Modifier = Modifier
) {
    if (window.isMinimized) return

    val density = LocalDensity.current

    // Local drag offset applied directly via hardware GPU graphicsLayer
    var dragDeltaX by remember { mutableFloatStateOf(0f) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }

    // Local resize offset applied during touch
    var resizeDeltaW by remember { mutableFloatStateOf(0f) }
    var resizeDeltaH by remember { mutableFloatStateOf(0f) }

    // Account for Ubuntu top bar (30dp) and left dock (52dp)
    val availableWidth = (parentWidthDp - 52f).coerceAtLeast(320f)
    val availableHeight = (parentHeightDp - 30f).coerceAtLeast(240f)

    val (effectiveOffset, effectiveWidth, effectiveHeight) = when {
        window.isMaximized || window.snapState == SnapState.FULLSCREEN -> {
            Triple(IntOffset(with(density) { 52.dp.roundToPx() }, with(density) { 30.dp.roundToPx() }), availableWidth.dp, availableHeight.dp)
        }
        window.snapState == SnapState.LEFT_HALF -> {
            Triple(IntOffset(with(density) { 52.dp.roundToPx() }, with(density) { 30.dp.roundToPx() }), (availableWidth / 2f).dp, availableHeight.dp)
        }
        window.snapState == SnapState.RIGHT_HALF -> {
            val halfW = availableWidth / 2f
            val halfPx = with(density) { (52f + halfW).dp.roundToPx() }
            val topPx = with(density) { 30.dp.roundToPx() }
            Triple(IntOffset(halfPx, topPx), halfW.dp, availableHeight.dp)
        }
        else -> {
            val wDp = (window.size.width.value + resizeDeltaW)
                .coerceIn(240f, 900f)
                .coerceAtMost(parentWidthDp)
                .dp
            val hDp = (window.size.height.value + resizeDeltaH)
                .coerceIn(180f, 800f)
                .coerceAtMost(parentHeightDp)
                .dp
            // Clamp on-screen: the +52/+30 Ubuntu desktop offsets can push a
            // window past the right edge on the phone launcher (real-device
            // bug: Photos panel cut off at the screen edge).
            val maxX = (parentWidthDp - wDp.value).coerceAtLeast(0f)
            val maxY = (parentHeightDp - hDp.value).coerceAtLeast(0f)
            val xPx = with(density) { (window.position.x + 52f).coerceIn(0f, maxX).dp.roundToPx() }
            val yPx = with(density) { (window.position.y + 30f).coerceIn(0f, maxY).dp.roundToPx() }
            Triple(IntOffset(xPx, yPx), wDp, hDp)
        }
    }

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    effectiveOffset.x + dragDeltaX.roundToInt(),
                    effectiveOffset.y + dragDeltaY.roundToInt()
                )
            }
            .width(effectiveWidth)
            .height(effectiveHeight)
            .zIndex(window.zIndex)
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(8.dp)
            }
            .clip(RoundedCornerShape(8.dp))
            .background(YaruWindowBody)
            .border(
                width = 1.dp,
                color = if (window.isFocused) Color(0xFF4C4C4C) else YaruBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { viewModel.focusWindow(window.id) }
            .testTag("floating_window_${window.internalAppKey}")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Authentic Ubuntu GNOME / Yaru Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(if (window.isFocused) YaruWindowHeader else Color(0xFF222222))
                    .pointerInput(window.id, window.isMaximized) {
                        detectDragGestures(
                            onDragStart = {
                                viewModel.focusWindow(window.id)
                                dragDeltaX = 0f
                                dragDeltaY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (!window.isMaximized && window.snapState == SnapState.NONE) {
                                    dragDeltaX += dragAmount.x
                                    dragDeltaY += dragAmount.y
                                }
                            },
                            onDragEnd = {
                                if (dragDeltaX != 0f || dragDeltaY != 0f) {
                                    with(density) {
                                        viewModel.updateWindowPosition(
                                            window.id,
                                            androidx.compose.ui.geometry.Offset(
                                                dragDeltaX.toDp().value,
                                                dragDeltaY.toDp().value
                                            )
                                        )
                                    }
                                }
                                dragDeltaX = 0f
                                dragDeltaY = 0f
                            },
                            onDragCancel = {
                                dragDeltaX = 0f
                                dragDeltaY = 0f
                            }
                        )
                    }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                Icon(
                    imageVector = resolveAppIcon(window.iconName, window.appType),
                    contentDescription = null,
                    tint = if (window.appType == AppType.ANDROID_APP) Color(0xFF34A853) else UbuntuOrange,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Title (Yaru theme clean text)
                Text(
                    text = window.title,
                    color = if (window.isFocused) YaruTextWhite else YaruTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // Ubuntu GNOME Window Control Buttons (Minimize, Maximize, Close)
                // Minimize
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF383838), CircleShape)
                        .clickable { viewModel.minimizeWindow(window.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Minimize",
                        tint = YaruTextWhite,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Maximize / Restore
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF383838), CircleShape)
                        .clickable { viewModel.toggleMaximizeWindow(window.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (window.isMaximized) Icons.Default.OpenInFull else Icons.Default.CropSquare,
                        contentDescription = "Maximize",
                        tint = YaruTextWhite,
                        modifier = Modifier.size(11.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Close Button (Ubuntu Orange / Red circle)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(YaruWindowButtonClose, CircleShape)
                        .clickable { viewModel.closeWindow(window.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Window Content Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (window.internalAppKey) {
                    "terminal" -> LinuxTerminalView(
                        prootEngine = viewModel.prootEngine,
                        realEngine = viewModel.realTerminalEngine
                    )
                    "editor" -> CodeStudioView()
                    "office" -> LibreOfficeView()
                    "gimp" -> GimpCanvasView()
                    "files" -> SmartFileOrganizerView(bridge = viewModel.fileBridge)
                    "gpu" -> GpuAccelerationPanel(
                        engine = viewModel.linuxEngine,
                        displayManager = viewModel.displayManager,
                        telemetry = viewModel.telemetry.value
                    )
                    else -> {
                        AndroidAppFloatingView(
                            packageName = window.internalAppKey,
                            appName = window.title,
                            appsManager = viewModel.appsManager
                        )
                    }
                }
            }
        }

        // Corner Resize Handle (Clean, subtle 48x48dp touch target)
        if (!window.isMaximized && window.snapState == SnapState.NONE) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.BottomEnd)
                    .pointerInput(window.id) {
                        detectDragGestures(
                            onDragStart = {
                                resizeDeltaW = 0f
                                resizeDeltaH = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                with(density) {
                                    resizeDeltaW += dragAmount.x.toDp().value
                                    resizeDeltaH += dragAmount.y.toDp().value
                                }
                            },
                            onDragEnd = {
                                if (resizeDeltaW != 0f || resizeDeltaH != 0f) {
                                    viewModel.updateWindowSize(window.id, resizeDeltaW, resizeDeltaH)
                                }
                                resizeDeltaW = 0f
                                resizeDeltaH = 0f
                            },
                            onDragCancel = {
                                resizeDeltaW = 0f
                                resizeDeltaH = 0f
                            }
                        )
                    },
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(12.dp)
                        .background(UbuntuOrange.copy(alpha = 0.5f), RoundedCornerShape(topStart = 4.dp))
                )
            }
        }
    }
}

@Composable
fun AndroidAppFloatingView(
    packageName: String,
    appName: String,
    appsManager: InstalledAppsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val support = remember { appsManager.freeformSupport() }
    val rom = remember { appsManager.vendorRom() }
    var launchResult by remember { mutableStateOf<InstalledAppsManager.LaunchMode?>(null) }
    // Auto-launch floating only when the device actually supports freeform;
    // auto-yanking the user into a fullscreen app would be worse than the panel.
    LaunchedEffect(packageName) {
        if (support == InstalledAppsManager.FreeformSupport.SUPPORTED) {
            launchResult = appsManager.launchAppFreeform(packageName)
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YaruWindowBody)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color(0xFF1E3A24), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Android, contentDescription = null, tint = Color(0xFF34A853), modifier = Modifier.size(34.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = appName,
            color = YaruTextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = packageName,
            color = YaruTextMuted,
            fontSize = 11.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF282828)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Real Android app - Android windowing",
                    color = YaruTextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (support) {
                        InstalledAppsManager.FreeformSupport.SUPPORTED ->
                            "This device supports floating windows. The app opens floating over the desktop."
                        InstalledAppsManager.FreeformSupport.DEV_OPTION_NEEDED ->
                            "Floating windows need a developer option on this device (force activities to be resizable / freeform). Enable it, then relaunch."
                        InstalledAppsManager.FreeformSupport.UNSUPPORTED ->
                            "This device does not support floating windows; the app will open fullscreen."
                    },
                    color = YaruTextMuted,
                    fontSize = 11.sp
                )
                if (support == InstalledAppsManager.FreeformSupport.DEV_OPTION_NEEDED) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "One-time fix (same grant Taskbar uses): with wireless debugging paired, run `pm grant com.aistudio.aetherlinux.pxkdmv android.permission.WRITE_SECURE_SETTINGS` - then this app enables floating by itself, permanently.",
                        color = YaruTextMuted,
                        fontSize = 11.sp
                    )
                }
                if (rom != null && support != InstalledAppsManager.FreeformSupport.SUPPORTED) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$rom: its built-in floating windows are a separate vendor feature - open Recents, long-press the app's icon, tap the floating-window icon. AOSP freeform (what apps can request) is usually disabled on this ROM.",
                        color = YaruTextMuted,
                        fontSize = 11.sp
                    )
                }
                launchResult?.let { r ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (r) {
                            InstalledAppsManager.LaunchMode.FREEFORM -> if (appsManager.lastWindowingModeHintApplied) "Floating requested (windowing hint applied). If it opened fullscreen anyway, this ROM overrode it - try restarting the phone once." else "Floating requested, but this ROM blocked the windowing hint."
                            InstalledAppsManager.LaunchMode.FULLSCREEN_FALLBACK -> "Device ignored floating - opened fullscreen."
                            InstalledAppsManager.LaunchMode.FAILED -> "Launch failed."
                        },
                        color = if (r == InstalledAppsManager.LaunchMode.FREEFORM) Color(0xFF34A853) else Color(0xFFE8A33D),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { launchResult = appsManager.launchAppFreeform(packageName) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853)),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("launch_native_android_app_button")
        ) {
            Icon(Icons.Default.Launch, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (support == InstalledAppsManager.FreeformSupport.UNSUPPORTED) "Open Fullscreen" else "Open Floating",
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }
        if (rom != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "On $rom, freeform settings are read at boot: if apps keep opening fullscreen, restart the phone once after enabling floating windows.",
                color = Color(0xFFAAAAAA), fontSize = 10.sp
            )
        }
        if (support == InstalledAppsManager.FreeformSupport.DEV_OPTION_NEEDED) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                Text("Open Developer Settings", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

fun resolveAppIcon(iconName: String, appType: AppType): ImageVector {
    if (appType == AppType.ANDROID_APP) return Icons.Default.Android
    return when (iconName) {
        "terminal" -> Icons.Default.Terminal
        "code" -> Icons.Default.Code
        "description" -> Icons.Default.Description
        "office" -> Icons.Default.Description
        "brush" -> Icons.Default.Brush
        "gimp" -> Icons.Default.Brush
        "folder" -> Icons.Default.Folder
        "files" -> Icons.Default.Folder
        "speed" -> Icons.Default.Speed
        "gpu" -> Icons.Default.Speed
        "analytics" -> Icons.Default.Analytics
        "public" -> Icons.Default.Public
        "browser" -> Icons.Default.Public
        else -> Icons.Default.Terminal
    }
}
