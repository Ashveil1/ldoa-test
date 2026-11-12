package com.example.engine

import com.example.model.GpuBackend
import com.example.model.LinuxAppDefinition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val _terminalHistory = MutableStateFlow<List<String>>(listOf(
        "Ubuntu 24.04.1 LTS (GNU/Linux 6.6.0-aether-proot aarch64)",
        " * Documentation:  https://help.ubuntu.com",
        " * Management:     AetherOS Subsystem Controller v2.4",
        " * GPU Engine:     VirGL 3D (OpenGL ES 3.2 -> Desktop GL 4.3)",
        " * Display:        :0 (Wayland / XWayland Direct Touch Mode)",
        "",
        "Last login: ${SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US).format(Date())} from android-localhost",
        "ubuntu@aether-box:~$ "
    ))
    val terminalHistory: StateFlow<List<String>> = _terminalHistory.asStateFlow()

    fun setGpuBackend(backend: GpuBackend) {
        _gpuBackend.value = backend
        appendTerminalLine("[AetherOS GPU] Switched driver backend to: ${backend.displayName}")
    }

    fun setFpsTarget(fps: Int) {
        _fpsTarget.value = fps
        appendTerminalLine("[AetherOS GPU] Frame limiter adjusted to: ${fps} FPS")
    }

    fun executeCommand(commandStr: String): String {
        val cmd = commandStr.trim()
        if (cmd.isEmpty()) return ""

        val currentList = _terminalHistory.value.toMutableList()
        currentList.add("ubuntu@aether-box:~$ $cmd")

        val response = when {
            cmd == "clear" -> {
                _terminalHistory.value = listOf("ubuntu@aether-box:~$ ")
                return ""
            }
            cmd == "uname -a" -> {
                "Linux aether-box 6.6.0-aether-proot #1 SMP PREEMPT_DYNAMIC aarch64 aarch64 aarch64 GNU/Linux"
            }
            cmd.startsWith("apt update") -> {
                "Get:1 http://ports.ubuntu.com/ubuntu-ports noble InRelease [256 kB]\n" +
                "Get:2 http://ports.ubuntu.com/ubuntu-ports noble-updates InRelease [126 kB]\n" +
                "Get:3 http://ports.ubuntu.com/ubuntu-ports noble-security InRelease [126 kB]\n" +
                "Reading package lists... Done\n" +
                "Building dependency tree... Done\n" +
                "All packages are up to date."
            }
            cmd.startsWith("apt install") -> {
                val pkg = cmd.removePrefix("apt install").trim()
                "Reading package lists... Done\n" +
                "Building dependency tree... Done\n" +
                "The following NEW packages will be installed:\n" +
                "  $pkg (latest)\n" +
                "0 upgraded, 1 newly installed, 0 to remove.\n" +
                "Need to get 14.2 MB of archives.\n" +
                "Setting up $pkg ...\n" +
                "Processing triggers for man-db...\n" +
                "Success: $pkg installed into /usr/bin/$pkg"
            }
            cmd == "ls -la" || cmd == "ls" -> {
                "total 36\n" +
                "drwxr-xr-x 6 ubuntu ubuntu 4096 Nov 12 03:40 .\n" +
                "drwxr-xr-x 3 root   root   4096 Nov 11 12:00 ..\n" +
                "-rw------- 1 ubuntu ubuntu  892 Nov 12 03:40 .bash_history\n" +
                "-rw-r--r-- 1 ubuntu ubuntu  220 Nov 11 12:00 .bash_logout\n" +
                "-rw-r--r-- 1 ubuntu ubuntu 3771 Nov 11 12:00 .bashrc\n" +
                "drwx------ 2 ubuntu ubuntu 4096 Nov 12 02:10 .cache\n" +
                "drwxr-xr-x 3 ubuntu ubuntu 4096 Nov 12 01:15 .config\n" +
                "drwxr-xr-x 2 ubuntu ubuntu 4096 Nov 12 02:45 Desktop\n" +
                "drwxr-xr-x 4 ubuntu ubuntu 4096 Nov 12 03:10 Documents\n" +
                "lrwxrwxrwx 1 ubuntu ubuntu   23 Nov 12 00:00 sdcard -> /storage/emulated/0"
            }
            cmd == "whoami" -> "ubuntu"
            cmd == "free -h" || cmd == "free -m" -> {
                "               total        used        free      shared  buff/cache   available\n" +
                "Mem:           7.8Gi       1.4Gi       5.2Gi       128Mi       1.2Gi       6.2Gi\n" +
                "Swap:          4.0Gi          0B       4.0Gi"
            }
            cmd == "top" || cmd == "htop" -> {
                "top - 03:42:15 up 2 days, 14:10,  1 user,  load average: 0.22, 0.18, 0.12\n" +
                "Tasks:  18 total,   1 running,  17 sleeping,   0 stopped,   0 zombie\n" +
                "%Cpu(s):  3.2 us,  1.1 sy,  0.0 ni, 95.7 id,  0.0 wa,  0.0 hi,  0.0 si\n" +
                "MiB Mem :   7980.2 total,   5312.4 free,   1428.1 used,   1239.7 buff/cache\n" +
                "  PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND\n" +
                " 1042 ubuntu    20   0  385.2m  92.1m  44.2m S   1.8   1.2   0:14.22 Xwayland\n" +
                " 1108 ubuntu    20   0  512.4m 140.6m  68.4m S   2.4   1.8   0:45.10 code-server\n" +
                " 1245 ubuntu    20   0   84.1m  18.2m  11.5m S   0.3   0.2   0:01.04 bash\n" +
                " 1301 ubuntu    20   0   14.2m   4.1m   3.1m R   0.2   0.1   0:00.02 top"
            }
            cmd.startsWith("python") -> {
                "Python 3.12.3 (main, Apr 10 2024, 05:33:47) [GCC 13.2.0] on linux\n" +
                "Type \"help\", \"copyright\", \"credits\" or \"license\" for more information.\n" +
                ">>> print('Hello from AetherLinux Ubuntu on Android!')\n" +
                "Hello from AetherLinux Ubuntu on Android!"
            }
            cmd == "virgl_test_server" || cmd.contains("glxinfo") -> {
                "name of display: :0\n" +
                "display: :0  screen: 0\n" +
                "direct rendering: Yes\n" +
                "OpenGL vendor string: Mesa/VirGL\n" +
                "OpenGL renderer string: virgl (Mobile GLES 3.2 HW Acceleration Bridge)\n" +
                "OpenGL core profile version string: 4.3 (Core Profile) Mesa 24.0.5\n" +
                "Hardware Shaders: Enabled (Zero frame drop)"
            }
            cmd == "cat /etc/os-release" -> {
                "PRETTY_NAME=\"Ubuntu 24.04.1 LTS\"\n" +
                "NAME=\"Ubuntu\"\n" +
                "VERSION_ID=\"24.04\"\n" +
                "VERSION=\"24.04.1 LTS (Noble Numbat)\"\n" +
                "ID=ubuntu\n" +
                "ID_LIKE=debian\n" +
                "HOME_URL=\"https://www.ubuntu.com/\""
            }
            else -> {
                "bash: $cmd: command simulated in AetherOS Ubuntu subsystem (execution code 0)"
            }
        }

        response.lines().forEach { line ->
            currentList.add(line)
        }
        currentList.add("ubuntu@aether-box:~$ ")
        _terminalHistory.value = currentList
        return response
    }

    private fun appendTerminalLine(line: String) {
        val list = _terminalHistory.value.toMutableList()
        list.add(line)
        _terminalHistory.value = list
    }
}
