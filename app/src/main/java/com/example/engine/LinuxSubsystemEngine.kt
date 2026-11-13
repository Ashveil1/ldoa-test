package com.example.engine

import com.example.model.GpuBackend
import com.example.model.LinuxAppDefinition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SubsystemState {
    STOPPED,
    BOOTING,
    RUNNING,
    PAUSED
}

class LinuxSubsystemEngine {

    private val _state = MutableStateFlow(SubsystemState.RUNNING)
    val state: StateFlow<SubsystemState> = _state.asStateFlow()

    private val _gpuBackend = MutableStateFlow(GpuBackend.VIRGL_HARDWARE)
    val gpuBackend: StateFlow<GpuBackend> = _gpuBackend.asStateFlow()

    private val _fpsTarget = MutableStateFlow(60)
    val fpsTarget: StateFlow<Int> = _fpsTarget.asStateFlow()

    private val _installedApps = MutableStateFlow<List<LinuxAppDefinition>>(listOf(
        LinuxAppDefinition(
            id = "linux-desktop",
            name = "Ubuntu Desktop (real)",
            category = "System",
            description = "Real Ubuntu 24.04 GNOME session via embedded X server and proot",
            command = "gnome-session --session=ubuntu",
            iconName = "desktop_windows"
        ),
        LinuxAppDefinition(
            id = "terminal",
            name = "Ubuntu Terminal",
            category = "System",
            description = "Bash shell with rootfs proot sandbox and apt package management",
            command = "bash -l",
            iconName = "terminal"
        ),
        LinuxAppDefinition(
            id = "editor",
            name = "Code Studio (VS Code)",
            category = "Development",
            description = "Full featured desktop IDE with syntax highlighting and file tree",
            command = "code-server --bind-addr 127.0.0.1:8080",
            iconName = "code"
        ),
        LinuxAppDefinition(
            id = "office",
            name = "LibreOffice Suite",
            category = "Productivity",
            description = "Desktop office suite for documents, spreadsheets and presentations",
            command = "libreoffice --writer",
            iconName = "description"
        ),
        LinuxAppDefinition(
            id = "gimp",
            name = "GNU Image Manipulator (GIMP)",
            category = "Graphics",
            description = "Touch-adapted digital imaging, photo editing and layers tool",
            command = "gimp",
            iconName = "brush"
        ),
        LinuxAppDefinition(
            id = "browser",
            name = "Chromium / Firefox",
            category = "Internet",
            description = "Desktop web browser with tabbed browsing and devtools",
            command = "firefox -new-tab",
            iconName = "public"
        ),
        LinuxAppDefinition(
            id = "sysmon",
            name = "Ubuntu System Monitor",
            category = "System",
            description = "Real-time CPU, RAM, GPU, and process inspection",
            command = "gnome-system-monitor",
            iconName = "analytics"
        ),
        LinuxAppDefinition(
            id = "files",
            name = "Nautilus Files",
            category = "Utilities",
            description = "Linux file explorer with direct Android storage bridge",
            command = "nautilus /home/ubuntu",
            iconName = "folder"
        )
    ))
    val installedApps: StateFlow<List<LinuxAppDefinition>> = _installedApps.asStateFlow()

    /** User-selected target backend (a preference, not a measured state). */
    fun setGpuBackend(backend: GpuBackend) {
        _gpuBackend.value = backend
    }

    fun setFpsTarget(fps: Int) {
        _fpsTarget.value = fps
    }
}
