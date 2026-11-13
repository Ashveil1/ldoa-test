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
            name = "Code Studio",
            category = "Development",
            description = "Edit real files in the Ubuntu rootfs and run them in proot",
            command = "internal-editor",
            iconName = "code"
        ),
        // NOTE: office/gimp/browser/sysmon entries were removed - they opened
        // placeholder mock windows or dead launches. They come back when the
        // real X-desktop path can actually run them.
        LinuxAppDefinition(
            id = "files",
            name = "Files (Cross-OS Bridge)",
            category = "Utilities",
            description = "Real file explorer over the Ubuntu rootfs and accessible Android storage",
            command = "internal-files",
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
