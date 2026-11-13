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

sealed interface SetupState {
    data object Idle : SetupState
    data class Running(val percent: Int, val status: String) : SetupState
    data class Failed(val message: String) : SetupState
    data object Done : SetupState
}

class AetherDesktopViewModel(application: Application) : AndroidViewModel(application) {

    val linuxEngine = LinuxSubsystemEngine()
    val prootEngine = PRootUbuntuDistroEngine(application)
    val fileBridge = CrossOsFileBridge(application) { distroInstaller.rootfsDir(com.example.engine.Distros.UBUNTU) }
    val appsManager = InstalledAppsManager(application)

    private val distroInstaller = com.example.engine.DistroInstaller(application)
    private val _setupState = MutableStateFlow<SetupState>(
        if (distroInstaller.isInstalled(com.example.engine.Distros.UBUNTU)) SetupState.Done else SetupState.Idle
    )
    val setupState: StateFlow<SetupState> = _setupState.asStateFlow()

    fun runSetup() {
        if (_setupState.value is SetupState.Running || _setupState.value == SetupState.Done) return
        viewModelScope.launch {
            var ok = false
            var attempt = 0
            while (!ok && attempt < 5) {
                attempt++
                ok = distroInstaller.install(com.example.engine.Distros.UBUNTU) { st ->
                    _setupState.value = when (st) {
                        is com.example.engine.InstallState.Downloading -> SetupState.Running(
                            if (st.totalBytes > 0) (st.doneBytes * 100 / st.totalBytes).toInt() else 0,
                            "Downloading… ${st.doneBytes / 1024} / ${st.totalBytes / 1024} KiB" +
                                if (attempt > 1) " (attempt $attempt/5)" else ""
                        )
                        com.example.engine.InstallState.Verifying -> SetupState.Running(100, "Verifying download…")
                        com.example.engine.InstallState.Extracting -> SetupState.Running(100, "Installing…")
                        com.example.engine.InstallState.Installed -> SetupState.Done
                        is com.example.engine.InstallState.Failed -> SetupState.Failed(st.message)
                        else -> _setupState.value
                    }
                }
                if (!ok && attempt < 5) {
                    _setupState.value = SetupState.Running(0, "Retrying (attempt ${attempt + 1}/5)…")
                    kotlinx.coroutines.delay(2_000L * attempt)
                }
            }
            _setupState.value = if (ok) SetupState.Done
                else SetupState.Failed("5 attempts failed. Check the network and retry.")
        }
    }
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
        startRealTelemetry()
    }

    // Real UI frame counter: Choreographer fires once per frame our UI
    // actually renders; idle UI honestly reads 0.
    private val frameCounter = java.util.concurrent.atomic.AtomicInteger(0)
    @Volatile private var measuredFps = 0

    private fun startRealTelemetry() {
        viewModelScope.launch {
            val choreographer = android.view.Choreographer.getInstance()
            val callback = object : android.view.Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    frameCounter.incrementAndGet()
                    choreographer.postFrameCallback(this)
                }
            }
            choreographer.postFrameCallback(callback)
            var lastCount = 0
            while (isActive) {
                delay(2000)
                val now = frameCounter.get()
                measuredFps = (now - lastCount) / 2
                lastCount = now
            }
        }
        viewModelScope.launch {
            val app = getApplication<Application>()
            val am = app.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            var prevTotal = -1L
            var prevIdle = -1L
            while (isActive) {
                delay(2000)
                val mi = android.app.ActivityManager.MemoryInfo()
                am.getMemoryInfo(mi)
                val bat = app.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                val level = bat?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = bat?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
                val tempTenths = bat?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
                val tempC = if (tempTenths != Int.MIN_VALUE) tempTenths / 10f else 0f
                // CPU: /proc/stat aggregate delta between samples.
                var cpu = 0
                runCatching {
                    val fields = java.io.File("/proc/stat").useLines { it.first() }
                        .split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
                    if (fields.size >= 5) {
                        val idle = fields[3] + fields[4]
                        val total = fields.sum()
                        if (prevTotal >= 0 && total > prevTotal) {
                            cpu = (100.0 * (1.0 - (idle - prevIdle).toDouble() / (total - prevTotal))).toInt().coerceIn(0, 100)
                        }
                        prevTotal = total
                        prevIdle = idle
                    }
                }
                _telemetry.value = SystemTelemetry(
                    cpuUsagePercent = cpu,
                    ramUsedMb = ((mi.totalMem - mi.availMem) / (1024 * 1024)).toInt(),
                    ramTotalMb = (mi.totalMem / (1024 * 1024)).toInt(),
                    gpuFps = measuredFps,
                    batteryPercent = pct,
                    temperatureCelsius = tempC
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

        if (app.id == "linux-desktop") {
            // Real path: vendored lorie X server + Ubuntu GNOME in proot.
            com.example.engine.XServer.openDesktopApp(getApplication(), com.example.engine.Distros.UBUNTU)
            return
        }

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

    /** Ash's call (2026-11-12): tapping an Android app opens it directly -
     *  floating where the device honors it, fullscreen where it doesn't.
     *  The guidance panel is the FALLBACK, shown only when the launch
     *  itself fails. */
    fun openAndroidAppSmart(packageName: String, appName: String) {
        val result = appsManager.launchAppFreeform(packageName)
        if (result == com.example.engine.InstalledAppsManager.LaunchMode.FAILED) {
            openAndroidAppInWindow(packageName, appName)
        }
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

    fun updateWindowPosition(windowId: String, delta: Offset, parentWidthDp: Float = 0f, parentHeightDp: Float = 0f) {
        _windows.value = _windows.value.map { win ->
            if (win.id == windowId && !win.isMaximized && win.snapState == SnapState.NONE) {
                // Clamp the STORED position to the visible parent too, not only
                // at render: otherwise a window dragged far past the right/bottom
                // edge keeps an oversized stored offset and needs an equally long
                // drag back before it visibly moves ("stuck window" feel).
                val maxX = if (parentWidthDp > 0f) (parentWidthDp - win.size.width.value).coerceAtLeast(0f) else Float.MAX_VALUE
                val maxY = if (parentHeightDp > 0f) (parentHeightDp - win.size.height.value).coerceAtLeast(0f) else Float.MAX_VALUE
                val newX = (win.position.x + delta.x).coerceIn(0f, maxX)
                val newY = (win.position.y + delta.y).coerceIn(0f, maxY)
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
