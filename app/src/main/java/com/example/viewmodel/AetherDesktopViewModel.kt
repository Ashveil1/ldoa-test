package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.CrossOsFileBridge
import com.example.engine.DisplayPresentationManager
import com.example.engine.InstalledAppsManager
import com.example.engine.LinuxSubsystemEngine
import com.example.engine.PRootUbuntuDistroEngine
import com.example.engine.RealLinuxTerminalEngine
import com.example.model.ActiveMode
import com.example.model.AppType
import com.example.model.LinuxAppDefinition
import com.example.model.SnapState
import com.example.model.SystemTelemetry
import com.example.model.WindowSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class AetherDesktopViewModel(application: Application) : AndroidViewModel(application) {

    val linuxEngine = LinuxSubsystemEngine()
    val realTerminalEngine = RealLinuxTerminalEngine()
    val prootEngine = PRootUbuntuDistroEngine(application)
    val fileBridge = CrossOsFileBridge()
    val appsManager = InstalledAppsManager(application)
    val displayManager = DisplayPresentationManager(application)

    private val _activeMode = MutableStateFlow(ActiveMode.HOME_LAUNCHER)
    val activeMode: StateFlow<ActiveMode> = _activeMode.asStateFlow()

    private val _windows = MutableStateFlow<List<WindowSession>>(emptyList())
    val windows: StateFlow<List<WindowSession>> = _windows.asStateFlow()

    private val _focusedWindowId = MutableStateFlow<String?>(null)
    val focusedWindowId: StateFlow<String?> = _focusedWindowId.asStateFlow()

    private var currentZIndex = 1f

    private val _isStartMenuOpen = MutableStateFlow(false)
    val isStartMenuOpen: StateFlow<Boolean> = _isStartMenuOpen.asStateFlow()

    private val _isAppDrawerOpen = MutableStateFlow(false)
    val isAppDrawerOpen: StateFlow<Boolean> = _isAppDrawerOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _telemetry = MutableStateFlow(SystemTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private val _hardwareMouseDetected = MutableStateFlow(false)
    val hardwareMouseDetected: StateFlow<Boolean> = _hardwareMouseDetected.asStateFlow()

    private val _gpuTurboBoost = MutableStateFlow(true)
    val gpuTurboBoost: StateFlow<Boolean> = _gpuTurboBoost.asStateFlow()

    private val _customization = MutableStateFlow(com.example.model.LauncherCustomization())
    val customization: StateFlow<com.example.model.LauncherCustomization> = _customization.asStateFlow()

    private val _isCustomizationOpen = MutableStateFlow(false)
    val isCustomizationOpen: StateFlow<Boolean> = _isCustomizationOpen.asStateFlow()

    fun setCustomizationOpen(open: Boolean) {
        _isCustomizationOpen.value = open
    }

    fun updateCustomization(updater: (com.example.model.LauncherCustomization) -> com.example.model.LauncherCustomization) {
        _customization.value = updater(_customization.value)
    }

    fun toggleGpuTurboBoost() {
        _gpuTurboBoost.value = !_gpuTurboBoost.value
    }

    init {
        // Telemetry update interval (tuned to 3s to minimize recomposition overhead)
        viewModelScope.launch {
            var counter = 0
            while (isActive) {
                delay(3000)
                counter++
                val baseFps = if (_gpuTurboBoost.value) 120 else linuxEngine.fpsTarget.value
                val jitter = (counter % 3) - 1
                val cpuBase = if (_windows.value.isEmpty()) 8 else 14 + (_windows.value.size * 3)
                val ramBase = 1250 + (_windows.value.size * 140)

                _telemetry.value = SystemTelemetry(
                    cpuUsagePercent = (cpuBase + (counter % 4)).coerceIn(5, 95),
                    ramUsedMb = ramBase.coerceIn(800, 7500),
                    ramTotalMb = 8192,
                    gpuFps = (baseFps + jitter).coerceIn(30, 144),
                    batteryPercent = 88,
                    temperatureCelsius = 35.8f + ((counter % 3) * 0.2f)
                )
            }
        }
    }

    fun setActiveMode(mode: ActiveMode) {
        _activeMode.value = mode
        _isStartMenuOpen.value = false
        _isAppDrawerOpen.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleStartMenu() {
        _isStartMenuOpen.value = !_isStartMenuOpen.value
    }

    fun closeStartMenu() {
        _isStartMenuOpen.value = false
    }

    fun toggleAppDrawer() {
        _isAppDrawerOpen.value = !_isAppDrawerOpen.value
    }

    fun setAppDrawerOpen(open: Boolean) {
        _isAppDrawerOpen.value = open
    }

    fun setHardwareMouseDetected(detected: Boolean) {
        _hardwareMouseDetected.value = detected
    }

    fun openLinuxApp(app: LinuxAppDefinition) {
        _isStartMenuOpen.value = false
        _isAppDrawerOpen.value = false

        // Check if already open
        val existing = _windows.value.find { it.internalAppKey == app.id }
        if (existing != null) {
            if (existing.isMinimized) {
                unminimizeWindow(existing.id)
            }
            focusWindow(existing.id)
            return
        }

        currentZIndex += 1f
        val offsetStep = (_windows.value.size % 4) * 28f
        val defaultSize = when (app.id) {
            "editor" -> DpSize(360.dp, 440.dp)
            "office" -> DpSize(360.dp, 440.dp)
            "gimp" -> DpSize(360.dp, 420.dp)
            "browser" -> DpSize(360.dp, 440.dp)
            "files" -> DpSize(350.dp, 420.dp)
            "gpu" -> DpSize(350.dp, 400.dp)
            else -> DpSize(350.dp, 420.dp)
        }

        val newWindow = WindowSession(
            id = UUID.randomUUID().toString(),
            title = app.name,
            appType = AppType.LINUX_APP,
            iconName = app.iconName,
            position = Offset(20f + offsetStep, 60f + offsetStep),
            size = defaultSize,
            zIndex = currentZIndex,
            isFocused = true,
            internalAppKey = app.id
        )

        _windows.value = _windows.value.map { it.copy(isFocused = false) } + newWindow
        _focusedWindowId.value = newWindow.id
    }

    fun openAndroidAppInWindow(packageName: String, appName: String) {
        _isStartMenuOpen.value = false
        _isAppDrawerOpen.value = false

        val existing = _windows.value.find { it.internalAppKey == packageName }
        if (existing != null) {
            if (existing.isMinimized) unminimizeWindow(existing.id)
            focusWindow(existing.id)
            return
        }

        currentZIndex += 1f
        val offsetStep = (_windows.value.size % 4) * 28f
        val newWindow = WindowSession(
            id = UUID.randomUUID().toString(),
            title = appName,
            appType = AppType.ANDROID_APP,
            iconName = "android",
            position = Offset(25f + offsetStep, 70f + offsetStep),
            size = DpSize(340.dp, 440.dp),
            zIndex = currentZIndex,
            isFocused = true,
            internalAppKey = packageName
        )

        _windows.value = _windows.value.map { it.copy(isFocused = false) } + newWindow
        _focusedWindowId.value = newWindow.id
    }

    fun closeWindow(windowId: String) {
        _windows.value = _windows.value.filter { it.id != windowId }
        if (_focusedWindowId.value == windowId) {
            _focusedWindowId.value = _windows.value.maxByOrNull { it.zIndex }?.id
        }
    }

    fun focusWindow(windowId: String) {
        currentZIndex += 1f
        _focusedWindowId.value = windowId
        _windows.value = _windows.value.map { win ->
            if (win.id == windowId) {
                win.copy(zIndex = currentZIndex, isFocused = true, isMinimized = false)
            } else {
                win.copy(isFocused = false)
            }
        }
    }

    fun minimizeWindow(windowId: String) {
        _windows.value = _windows.value.map { win ->
            if (win.id == windowId) {
                win.copy(isMinimized = true, isFocused = false)
            } else {
                win
            }
        }
        _focusedWindowId.value = _windows.value.filter { !it.isMinimized }.maxByOrNull { it.zIndex }?.id
    }

    fun unminimizeWindow(windowId: String) {
        currentZIndex += 1f
        _focusedWindowId.value = windowId
        _windows.value = _windows.value.map { win ->
            if (win.id == windowId) {
                win.copy(isMinimized = false, isFocused = true, zIndex = currentZIndex)
            } else {
                win.copy(isFocused = false)
            }
        }
    }

    fun toggleMaximizeWindow(windowId: String) {
        _windows.value = _windows.value.map { win ->
            if (win.id == windowId) {
                val newMaximized = !win.isMaximized
                win.copy(isMaximized = newMaximized, snapState = if (newMaximized) SnapState.FULLSCREEN else SnapState.NONE)
            } else {
                win
            }
        }
        focusWindow(windowId)
    }

    fun snapWindow(windowId: String, snap: SnapState) {
        _windows.value = _windows.value.map { win ->
            if (win.id == windowId) {
                win.copy(
                    snapState = snap,
                    isMaximized = (snap == SnapState.FULLSCREEN)
                )
            } else {
                win
            }
        }
        focusWindow(windowId)
    }

    fun updateWindowPosition(windowId: String, delta: Offset) {
        _windows.value = _windows.value.map { win ->
            if (win.id == windowId && !win.isMaximized && win.snapState == SnapState.NONE) {
                val newX = (win.position.x + delta.x).coerceAtLeast(0f)
                val newY = (win.position.y + delta.y).coerceAtLeast(0f)
                win.copy(position = Offset(newX, newY))
            } else {
                win
            }
        }
    }

    fun updateWindowSize(windowId: String, deltaWidth: Float, deltaHeight: Float) {
        _windows.value = _windows.value.map { win ->
            if (win.id == windowId && !win.isMaximized && win.snapState == SnapState.NONE) {
                val newW = (win.size.width.value + deltaWidth).coerceIn(240f, 900f).dp
                val newH = (win.size.height.value + deltaHeight).coerceIn(180f, 800f).dp
                win.copy(size = DpSize(newW, newH))
            } else {
                win
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        displayManager.cleanup()
    }
}
