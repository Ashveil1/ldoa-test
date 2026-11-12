package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class AppType {
    LINUX_APP,
    ANDROID_APP
}

enum class ActiveMode {
    HOME_LAUNCHER,
    DESKTOP_WORKSPACE,
    FILES_ORGANIZER,
    GPU_SETTINGS
}

enum class GpuBackend(val displayName: String, val description: String) {
    VIRGL_HARDWARE("VirGL Hardware Acceleration", "Translates Desktop OpenGL 4.3 to Mobile OpenGL ES 3.2 for optimal FPS"),
    MESA_ZINK("Mesa Zink (Vulkan Bridge)", "Runs full OpenGL on top of Vulkan drivers for newer Adreno/Mali chips"),
    SOFTWARE_RASTER("Software Rasterizer (LLVMpipe)", "Fallback CPU-based rendering for maximum compatibility")
}

data class LinuxAppDefinition(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val command: String,
    val iconName: String,
    val isInstalled: Boolean = true,
    val version: String = "1.0.0"
)

data class AndroidAppItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val launchIntentAvailable: Boolean = true
)

data class WindowSession(
    val id: String,
    val title: String,
    val appType: AppType,
    val iconName: String,
    val position: Offset = Offset(60f, 60f),
    val size: DpSize = DpSize(420.dp, 360.dp),
    val zIndex: Float = 1f,
    val isMinimized: Boolean = false,
    val isMaximized: Boolean = false,
    val isFocused: Boolean = true,
    val snapState: SnapState = SnapState.NONE,
    val internalAppKey: String = "" // e.g. "terminal", "editor", "office", "gimp", "files", "gpu"
)

enum class SnapState {
    NONE,
    LEFT_HALF,
    RIGHT_HALF,
    FULLSCREEN
}

enum class FileCategory(val label: String) {
    ALL("All Files"),
    CODE("Code & Scripts"),
    DOCUMENTS("Documents"),
    MEDIA("Media"),
    ARCHIVES("Archives"),
    LINUX_DEB("Linux Packages")
}

data class SmartBridgeFile(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val modifiedFormatted: String,
    val category: FileCategory,
    val isLinuxSpace: Boolean, // true if in /home/ubuntu, false if in /sdcard
    val isSymlinked: Boolean = false,
    val extension: String = ""
)

data class ExternalMonitorStatus(
    val isConnected: Boolean = false,
    val displayName: String = "No External Display",
    val resolution: String = "1920x1080 @ 60Hz",
    val isMirrored: Boolean = false
)

data class SystemTelemetry(
    val cpuUsagePercent: Int = 18,
    val ramUsedMb: Int = 1420,
    val ramTotalMb: Int = 8192,
    val gpuFps: Int = 60,
    val batteryPercent: Int = 88,
    val temperatureCelsius: Float = 36.5f
)
