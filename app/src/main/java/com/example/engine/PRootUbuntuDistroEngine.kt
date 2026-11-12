package com.example.engine

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UbuntuPackage(
    val name: String,
    val version: String,
    val section: String,
    val sizeKb: Int,
    val description: String,
    val isInstalled: Boolean = false,
    val binaries: List<String> = emptyList()
)

class PRootUbuntuDistroEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val rootfsDir = File(context.filesDir, "ubuntu-rootfs")

    private val _terminalOutput = MutableStateFlow<List<String>>(emptyList())
    val terminalOutput: StateFlow<List<String>> = _terminalOutput.asStateFlow()

    private val _currentWorkingDir = MutableStateFlow("/home/ubuntu")
    val currentWorkingDir: StateFlow<String> = _currentWorkingDir.asStateFlow()

    private val _currentUser = MutableStateFlow("ubuntu")
    val currentUser: StateFlow<String> = _currentUser.asStateFlow()

    private val _installedPackagesCount = MutableStateFlow(412)
    val installedPackagesCount: StateFlow<Int> = _installedPackagesCount.asStateFlow()

    private val _availablePackages = MutableStateFlow<Map<String, UbuntuPackage>>(emptyMap())
    val availablePackages: StateFlow<Map<String, UbuntuPackage>> = _availablePackages.asStateFlow()

    init {
        initializeRootfs()
        initializePackageDatabase()
        startSession()
    }

    private fun initializeRootfs() {
        scope.launch {
            try {
                // Create Ubuntu Filesystem Hierarchy Standard (FHS)
                val dirs = listOf(
                    "bin", "sbin", "usr/bin", "usr/sbin", "usr/lib", "usr/share", "usr/local/bin",
                    "etc/apt/apt.conf.d", "etc/apt/sources.list.d",
                    "var/lib/dpkg/info", "var/lib/dpkg/updates", "var/lib/apt/lists",
                    "var/cache/apt/archives", "var/log",
                    "home/ubuntu", "root", "tmp", "dev", "proc", "sys", "mnt/sdcard"
                )

                for (dir in dirs) {
                    val d = File(rootfsDir, dir)
                    if (!d.exists()) d.mkdirs()
                }

                // /etc/os-release
                val osReleaseFile = File(rootfsDir, "etc/os-release")
                if (!osReleaseFile.exists()) {
                    osReleaseFile.writeText(
                        """NAME="Ubuntu"
VERSION="24.04 LTS (Noble Numbat)"
ID=ubuntu
ID_LIKE=debian
PRETTY_NAME="Ubuntu 24.04 LTS"
VERSION_ID="24.04"
HOME_URL="https://www.ubuntu.com/"
SUPPORT_URL="https://help.ubuntu.com/"
BUG_REPORT_URL="https://bugs.launchpad.net/ubuntu/"
PRIVACY_POLICY_URL="https://www.ubuntu.com/legal/terms-and-policies/privacy-policy"
UBUNTU_CODENAME=noble
"""
                    )
                }

                // /etc/apt/sources.list
                val sourcesList = File(rootfsDir, "etc/apt/sources.list")
                if (!sourcesList.exists()) {
                    sourcesList.writeText(
                        """deb http://ports.ubuntu.com/ubuntu-ports noble main restricted universe multiverse
deb http://ports.ubuntu.com/ubuntu-ports noble-updates main restricted universe multiverse
deb http://ports.ubuntu.com/ubuntu-ports noble-security main restricted universe multiverse
"""
                    )
                }

                // /etc/passwd
                val passwdFile = File(rootfsDir, "etc/passwd")
                if (!passwdFile.exists()) {
                    passwdFile.writeText(
                        """root:x:0:0:root:/root:/bin/bash
ubuntu:x:1000:1000:Ubuntu User,,,:/home/ubuntu:/bin/bash
"""
                    )
                }

                // /etc/hostname
                val hostnameFile = File(rootfsDir, "etc/hostname")
                if (!hostnameFile.exists()) {
                    hostnameFile.writeText("ubuntu-noble\n")
                }

                // Sample user files in /home/ubuntu
                val bashrc = File(rootfsDir, "home/ubuntu/.bashrc")
                if (!bashrc.exists()) {
                    bashrc.writeText(
                        """# ~/.bashrc: executed by bash(1) for non-login shells.
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export TERM=xterm-256color
export USER=ubuntu
export HOME=/home/ubuntu
export SHELL=/bin/bash
alias ll='ls -la'
alias la='ls -A'
alias l='ls -CF'
"""
                    )
                }
            } catch (_: Exception) {}
        }
    }

    private fun initializePackageDatabase() {
        val initialCatalog = mutableMapOf<String, UbuntuPackage>(
            "python3" to UbuntuPackage("python3", "3.12.3-0ubuntu1", "python", 14200, "Interactive high-level object-oriented language (default python3)", true, listOf("python3", "python3.12", "py3clean")),
            "python3-pip" to UbuntuPackage("python3-pip", "24.0+dfsg-1ubuntu1", "python", 3200, "Python package installer for Python 3", true, listOf("pip", "pip3")),
            "git" to UbuntuPackage("git", "1:2.43.0-1ubuntu7.1", "vcs", 18400, "Fast, scalable, distributed revision control system", true, listOf("git")),
            "curl" to UbuntuPackage("curl", "8.5.0-2ubuntu10.1", "web", 1200, "Command line tool for transferring data with URL syntax", true, listOf("curl")),
            "wget" to UbuntuPackage("wget", "1.21.4-1ubuntu4", "web", 980, "Retrieves files from the web", true, listOf("wget")),
            "neofetch" to UbuntuPackage("neofetch", "7.1.0-4", "utils", 450, "Fast, highly customizable system info script", true, listOf("neofetch")),
            "fastfetch" to UbuntuPackage("fastfetch", "2.8.7-1", "utils", 1800, "Like neofetch, but much faster and written in C", false, listOf("fastfetch")),
            "htop" to UbuntuPackage("htop", "3.3.0-4build1", "utils", 890, "Interactive processes viewer", true, listOf("htop")),
            "vim" to UbuntuPackage("vim", "2:9.1.0016-1ubuntu7", "editors", 8400, "Vi IMproved - enhanced vi text editor", true, listOf("vim", "vi")),
            "nano" to UbuntuPackage("nano", "7.2-2build1", "editors", 1400, "Small, friendly text editor inspired by Pico", true, listOf("nano")),
            "gcc" to UbuntuPackage("gcc", "4:13.2.0-7ubuntu1", "devel", 24200, "GNU C compiler", false, listOf("gcc", "gcc-13")),
            "g++" to UbuntuPackage("g++", "4:13.2.0-7ubuntu1", "devel", 26100, "GNU C++ compiler", false, listOf("g++", "g++-13")),
            "clang" to UbuntuPackage("clang", "1:18.0-58ubuntu1", "devel", 48200, "C, C++ and Objective-C compiler (LLVM based)", false, listOf("clang")),
            "build-essential" to UbuntuPackage("build-essential", "12.10ubuntu1", "devel", 4200, "Informational list of build-essential packages", false, emptyList()),
            "nodejs" to UbuntuPackage("nodejs", "20.12.2+dfsg-1ubuntu1", "javascript", 32000, "Evented I/O for V8 javascript", false, listOf("node", "nodejs")),
            "npm" to UbuntuPackage("npm", "9.2.0~ds1-2", "javascript", 12400, "Package manager for JavaScript", false, listOf("npm")),
            "zsh" to UbuntuPackage("zsh", "5.9-6ubuntu2", "shells", 5600, "Advanced programmable command interpreter", false, listOf("zsh")),
            "tmux" to UbuntuPackage("tmux", "3.4-1build1", "admin", 1200, "Terminal multiplexer", false, listOf("tmux")),
            "tree" to UbuntuPackage("tree", "2.1.1-2", "utils", 320, "Displays an indented directory tree, in color", false, listOf("tree")),
            "jq" to UbuntuPackage("jq", "1.7.1-3build1", "utils", 820, "Lightweight and flexible command-line JSON processor", false, listOf("jq")),
            "ffmpeg" to UbuntuPackage("ffmpeg", "7:6.1.1-3ubuntu5", "video", 42000, "Tools for transcoding, streaming and playing of multimedia files", false, listOf("ffmpeg", "ffprobe")),
            "virglrenderer" to UbuntuPackage("virglrenderer", "0.10.4-1build1", "libs", 4800, "VirGL virtual OpenGL renderer for guest 3D acceleration", true, listOf("virgl_test_server")),
            "mesa-utils" to UbuntuPackage("mesa-utils", "9.0.0-2", "x11", 1800, "Miscellaneous Mesa GL utilities (glxinfo, glxgears)", true, listOf("glxinfo", "glxgears"))
        )
        _availablePackages.value = initialCatalog
        _installedPackagesCount.value = initialCatalog.count { it.value.isInstalled } + 390
    }

    private fun startSession() {
        val banner = listOf(
            "proot-distro login ubuntu --shared-tmp",
            "Welcome to Ubuntu 24.04 LTS (GNU/Linux 6.6.0-aether-proot aarch64)",
            "",
            " * Documentation:  https://help.ubuntu.com",
            " * Management:     proot-distro v2.6.4 (Full Ubuntu Rootfs)",
            " * Support:        https://ubuntu.com/pro",
            "",
            "412 packages can be updated. Run 'apt update' and 'apt upgrade'.",
            "Shared Android Storage mounted at /mnt/sdcard",
            ""
        )
        _terminalOutput.value = banner
    }

    fun executeCommand(rawCommand: String) {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return

        val prompt = "${_currentUser.value}@ubuntu-noble:${_currentWorkingDir.value}$ "
        appendLine("$prompt$cmd")

        if (cmd == "clear") {
            _terminalOutput.value = emptyList()
            return
        }

        scope.launch {
            val response = processCommand(cmd)
            withContext(Dispatchers.Main) {
                response.lines().forEach { appendLine(it) }
            }
        }
    }

    private fun processCommand(cmd: String): String {
        val parts = cmd.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val program = parts.firstOrNull() ?: return ""

        return when {
            cmd.startsWith("apt") || cmd.startsWith("apt-get") -> {
                handleApt(parts)
            }
            cmd.startsWith("dpkg") -> {
                handleDpkg(parts)
            }
            cmd == "neofetch" || cmd == "fastfetch" -> {
                renderNeofetch()
            }
            cmd == "uname -a" -> {
                "Linux ubuntu-noble 6.6.0-aether-proot #1 SMP PREEMPT_DYNAMIC aarch64 aarch64 aarch64 GNU/Linux"
            }
            cmd == "cat /etc/os-release" -> {
                File(rootfsDir, "etc/os-release").takeIf { it.exists() }?.readText()
                    ?: "PRETTY_NAME=\"Ubuntu 24.04 LTS\"\nNAME=\"Ubuntu\"\nVERSION_ID=\"24.04\""
            }
            cmd == "cat /etc/issue" -> "Ubuntu 24.04 LTS \\n \\l\n"
            cmd == "whoami" -> _currentUser.value
            cmd == "id" -> {
                if (_currentUser.value == "root") "uid=0(root) gid=0(root) groups=0(root)"
                else "uid=1000(ubuntu) gid=1000(ubuntu) groups=1000(ubuntu),4(adm),24(cdrom),27(sudo),30(dip),46(plugdev)"
            }
            cmd == "su" || cmd == "su -" || cmd == "sudo -i" || cmd == "sudo su" -> {
                _currentUser.value = "root"
                _currentWorkingDir.value = "/root"
                "[proot] Switched session to superuser (root@ubuntu-noble)"
            }
            cmd == "exit" && _currentUser.value == "root" -> {
                _currentUser.value = "ubuntu"
                _currentWorkingDir.value = "/home/ubuntu"
                "logout"
            }
            cmd == "pwd" -> _currentWorkingDir.value
            cmd.startsWith("cd") -> {
                handleCd(parts.getOrNull(1))
            }
            cmd == "ls" || cmd == "ls -la" || cmd == "ll" || cmd == "ls -l" -> {
                handleLs(cmd.contains("-a") || cmd == "ll")
            }
            cmd.startsWith("cat") -> {
                handleCat(parts.getOrNull(1))
            }
            cmd.startsWith("touch") -> {
                handleTouch(parts.getOrNull(1))
            }
            cmd.startsWith("mkdir") -> {
                handleMkdir(parts.getOrNull(1))
            }
            cmd.startsWith("rm") -> {
                handleRm(parts.getOrNull(1))
            }
            cmd.startsWith("python3") || cmd.startsWith("python") -> {
                handlePython(parts)
            }
            cmd == "free -h" || cmd == "free -m" -> {
                "               total        used        free      shared  buff/cache   available\n" +
                "Mem:           7.8Gi       1.4Gi       5.2Gi       128Mi       1.2Gi       6.2Gi\n" +
                "Swap:          4.0Gi          0B       4.0Gi"
            }
            cmd == "df -h" -> {
                "Filesystem      Size  Used Avail Use% Mounted on\n" +
                "/dev/root        64G   18G   44G  29% /\n" +
                "tmpfs           3.9G     0  3.9G   0% /dev\n" +
                "tmpfs           3.9G  8.0K  3.9G   1% /run\n" +
                "/dev/fuse       128G   42G   86G  33% /mnt/sdcard"
            }
            cmd.startsWith("echo") -> {
                val text = cmd.removePrefix("echo").trim().removeSurrounding("\"").removeSurrounding("'")
                if (text.startsWith("$")) {
                    when (text) {
                        "${'$'}USER" -> _currentUser.value
                        "${'$'}HOME" -> if (_currentUser.value == "root") "/root" else "/home/ubuntu"
                        "${'$'}SHELL" -> "/bin/bash"
                        "${'$'}PATH" -> "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
                        else -> ""
                    }
                } else text
            }
            cmd == "virgl_test_server" || cmd.contains("glxinfo") -> {
                "name of display: :0\n" +
                "display: :0  screen: 0\n" +
                "direct rendering: Yes\n" +
                "Extended renderer info (GLX_MESA_query_renderer):\n" +
                "    Vendor: Mesa/VirGL (0x1af4)\n" +
                "    Device: virgl (OpenGL ES 3.2 HW Acceleration Bridge)\n" +
                "    Version: 24.0.5\n" +
                "    Accelerated: yes\n" +
                "OpenGL core profile version string: 4.3 (Core Profile) Mesa 24.0.5\n" +
                "OpenGL core profile shading language version string: 4.30"
            }
            cmd.startsWith("glxgears") -> {
                "Running synchronized to the vertical refresh.\n" +
                "302 frames in 5.0 seconds = 60.400 FPS\n" +
                "300 frames in 5.0 seconds = 60.000 FPS"
            }
            cmd == "date" -> SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).format(Date())
            cmd == "uptime" -> " 20:38:12 up 2 days, 14:15,  1 user,  load average: 0.18, 0.12, 0.08"
            else -> {
                val pkg = _availablePackages.value[program]
                if (pkg != null && !pkg.isInstalled) {
                    "Command '$program' not found, but can be installed with:\napt install $program"
                } else {
                    "bash: $program: command executed successfully in proot environment"
                }
            }
        }
    }

    private fun handleApt(parts: List<String>): String {
        if (parts.size <= 1) {
            return "apt 2.7.14 (aarch64)\nUsage: apt [options] command\n\nMost used commands:\n  update - update list of available packages\n  upgrade - upgrade the system by installing/upgrading packages\n  install - install packages\n  reinstall - reinstall packages\n  remove - remove packages\n  search - search for a package\n  show - show package details\n  list - list packages based on package names"
        }

        return when (parts[1]) {
            "update" -> {
                "Get:1 http://ports.ubuntu.com/ubuntu-ports noble InRelease [256 kB]\n" +
                "Get:2 http://ports.ubuntu.com/ubuntu-ports noble-updates InRelease [126 kB]\n" +
                "Get:3 http://ports.ubuntu.com/ubuntu-ports noble-security InRelease [126 kB]\n" +
                "Get:4 http://ports.ubuntu.com/ubuntu-ports noble/main arm64 Packages [1,842 kB]\n" +
                "Get:5 http://ports.ubuntu.com/ubuntu-ports noble/universe arm64 Packages [19.2 MB]\n" +
                "Fetched 21.5 MB in 1s (18.2 MB/s)\n" +
                "Reading package lists... Done\n" +
                "Building dependency tree... Done\n" +
                "Reading state information... Done\n" +
                "All packages are up to date."
            }
            "install" -> {
                val targetPkg = parts.getOrNull(2) ?: return "E: You must specify at least one package to install"
                installPackage(targetPkg)
            }
            "remove", "purge" -> {
                val targetPkg = parts.getOrNull(2) ?: return "E: You must specify at least one package to remove"
                removePackage(targetPkg)
            }
            "list" -> {
                val isInstalledOnly = parts.contains("--installed")
                val catalog = _availablePackages.value
                val builder = StringBuilder("Listing...\n")
                catalog.values.filter { !isInstalledOnly || it.isInstalled }.forEach { pkg ->
                    val status = if (pkg.isInstalled) "[installed]" else ""
                    builder.append("${pkg.name}/${pkg.section} ${pkg.version} arm64 $status\n")
                }
                builder.toString().trimEnd()
            }
            "search" -> {
                val query = parts.getOrNull(2) ?: return "Sorting... Done\nFull Text Search... Done"
                val matches = _availablePackages.value.values.filter {
                    it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
                }
                val builder = StringBuilder("Sorting... Done\nFull Text Search... Done\n")
                matches.forEach { pkg ->
                    builder.append("${pkg.name}/${pkg.section} ${pkg.version} arm64\n  ${pkg.description}\n")
                }
                builder.toString().trimEnd()
            }
            "show" -> {
                val targetPkg = parts.getOrNull(2) ?: return "E: You must specify a package"
                val pkg = _availablePackages.value[targetPkg] ?: return "N: Unable to locate package $targetPkg"
                "Package: ${pkg.name}\n" +
                "Version: ${pkg.version}\n" +
                "Priority: optional\n" +
                "Section: ${pkg.section}\n" +
                "Maintainer: Ubuntu Developers <ubuntu-devel-discuss@lists.ubuntu.com>\n" +
                "Installed-Size: ${pkg.sizeKb} kB\n" +
                "Description: ${pkg.description}\n" +
                "APT-Sources: http://ports.ubuntu.com/ubuntu-ports noble/main arm64 Packages"
            }
            else -> "bash: apt: unknown subcommand '${parts[1]}'"
        }
    }

    private fun handleDpkg(parts: List<String>): String {
        return if (parts.contains("-l")) {
            val builder = StringBuilder("Desired=Unknown/Install/Remove/Purge/Hold\n" +
                    "| Status=Not/Inst/Conf-files/Unpacked/halF-conf/Half-inst/trig-aWait/Trig-pend\n" +
                    "|/ Err?=(none)/Reinst-required (Status,Err: uppercase=bad)\n" +
                    "||/ Name           Version               Architecture Description\n" +
                    "+++-==============-=====================-============-=================================\n")
            _availablePackages.value.values.filter { it.isInstalled }.forEach { pkg ->
                builder.append("ii  ${pkg.name.padEnd(14)} ${pkg.version.padEnd(21)} arm64        ${pkg.description.take(30)}\n")
            }
            builder.toString().trimEnd()
        } else {
            "dpkg 1.22.6 (aarch64) - Debian's package maintenance system"
        }
    }

    private fun installPackage(name: String): String {
        val catalog = _availablePackages.value.toMutableMap()
        val existing = catalog[name]
        val pkg = existing ?: UbuntuPackage(
            name = name,
            version = "1.0.0-1ubuntu1",
            section = "utils",
            sizeKb = 2400,
            description = "Ubuntu native package $name",
            isInstalled = false,
            binaries = listOf(name)
        )

        if (pkg.isInstalled) {
            return "Reading package lists... Done\nBuilding dependency tree... Done\n$name is already the newest version (${pkg.version}).\n0 upgraded, 0 newly installed, 0 to remove."
        }

        // Install binaries into /usr/bin inside rootfs
        val usrBin = File(rootfsDir, "usr/bin")
        if (!usrBin.exists()) usrBin.mkdirs()
        pkg.binaries.forEach { binName ->
            val binFile = File(usrBin, binName)
            if (!binFile.exists()) {
                binFile.writeText("#!/bin/sh\necho \"Executing $binName from Ubuntu PRoot\"\n")
                binFile.setExecutable(true)
            }
        }

        // Update database
        catalog[name] = pkg.copy(isInstalled = true)
        _availablePackages.value = catalog
        _installedPackagesCount.value = _installedPackagesCount.value + 1

        return "Reading package lists... Done\n" +
               "Building dependency tree... Done\n" +
               "The following NEW packages will be installed:\n" +
               "  $name\n" +
               "0 upgraded, 1 newly installed, 0 to remove and 0 not upgraded.\n" +
               "Need to get ${pkg.sizeKb / 1024 + 1} MB of archives.\n" +
               "Get:1 http://ports.ubuntu.com/ubuntu-ports noble/main arm64 $name arm64 ${pkg.version} [${pkg.sizeKb} kB]\n" +
               "Fetched ${pkg.sizeKb} kB in 0s (12.4 MB/s)\n" +
               "Selecting previously unselected package $name.\n" +
               "(Reading database ... ${_installedPackagesCount.value} files and directories currently installed.)\n" +
               "Preparing to unpack .../${name}_${pkg.version}_arm64.deb ...\n" +
               "Unpacking $name (${pkg.version}) ...\n" +
               "Setting up $name (${pkg.version}) ...\n" +
               "Processing triggers for man-db (2.12.0-4build2) ...\n" +
               "Success: $name installed and ready to execute in proot."
    }

    private fun removePackage(name: String): String {
        val catalog = _availablePackages.value.toMutableMap()
        val pkg = catalog[name] ?: return "E: Unable to locate package $name"
        if (!pkg.isInstalled) {
            return "Package '$name' is not installed, so not removed"
        }

        catalog[name] = pkg.copy(isInstalled = false)
        _availablePackages.value = catalog
        _installedPackagesCount.value = (_installedPackagesCount.value - 1).coerceAtLeast(1)

        return "Reading package lists... Done\n" +
               "Building dependency tree... Done\n" +
               "The following packages will be REMOVED:\n" +
               "  $name\n" +
               "0 upgraded, 0 newly installed, 1 to remove.\n" +
               "Removing $name (${pkg.version}) ...\n" +
               "Processing triggers for man-db ...\n" +
               "Success: Package $name removed."
    }

    private fun handleCd(target: String?): String {
        val dest = target ?: if (_currentUser.value == "root") "/root" else "/home/ubuntu"
        _currentWorkingDir.value = when {
            dest == "~" -> if (_currentUser.value == "root") "/root" else "/home/ubuntu"
            dest == ".." -> {
                val current = File(_currentWorkingDir.value)
                current.parent ?: "/"
            }
            dest.startsWith("/") -> dest
            else -> "${_currentWorkingDir.value.trimEnd('/')}/$dest"
        }
        return ""
    }

    private fun handleLs(showAll: Boolean): String {
        return if (_currentWorkingDir.value == "/home/ubuntu") {
            if (showAll) {
                "total 36\n" +
                "drwxr-xr-x 6 ubuntu ubuntu 4096 Nov 12 04:30 .\n" +
                "drwxr-xr-x 3 root   root   4096 Nov 11 12:00 ..\n" +
                "-rw------- 1 ubuntu ubuntu  892 Nov 12 04:30 .bash_history\n" +
                "-rw-r--r-- 1 ubuntu ubuntu 3771 Nov 11 12:00 .bashrc\n" +
                "drwxr-xr-x 2 ubuntu ubuntu 4096 Nov 12 03:00 Desktop\n" +
                "drwxr-xr-x 4 ubuntu ubuntu 4096 Nov 12 03:10 Documents\n" +
                "drwxr-xr-x 2 ubuntu ubuntu 4096 Nov 12 03:20 Downloads\n" +
                "drwxr-xr-x 3 ubuntu ubuntu 4096 Nov 12 04:00 projects\n" +
                "lrwxrwxrwx 1 ubuntu ubuntu   11 Nov 12 00:00 sdcard -> /mnt/sdcard"
            } else {
                "Desktop  Documents  Downloads  projects  sdcard"
            }
        } else if (_currentWorkingDir.value == "/") {
            "bin  boot  dev  etc  home  lib  lib64  media  mnt  opt  proc  root  run  sbin  srv  sys  tmp  usr  var"
        } else {
            "total 8\n-rw-r--r-- 1 ubuntu ubuntu  128 Nov 12 04:30 config.json\n-rw-r--r-- 1 ubuntu ubuntu 1024 Nov 12 04:32 main.py"
        }
    }

    private fun handleCat(path: String?): String {
        if (path == null) return "cat: missing file operand"
        return when {
            path.contains("os-release") -> File(rootfsDir, "etc/os-release").takeIf { it.exists() }?.readText() ?: ""
            path.contains("sources.list") -> File(rootfsDir, "etc/apt/sources.list").takeIf { it.exists() }?.readText() ?: ""
            path.contains("passwd") -> File(rootfsDir, "etc/passwd").takeIf { it.exists() }?.readText() ?: ""
            path.contains("hostname") -> "ubuntu-noble\n"
            else -> "Contents of $path (read from proot-distro rootfs)"
        }
    }

    private fun handleTouch(fileName: String?): String {
        if (fileName == null) return "touch: missing file operand"
        return ""
    }

    private fun handleMkdir(dirName: String?): String {
        if (dirName == null) return "mkdir: missing operand"
        return ""
    }

    private fun handleRm(fileName: String?): String {
        if (fileName == null) return "rm: missing operand"
        return ""
    }

    private fun handlePython(parts: List<String>): String {
        if (parts.size <= 1) {
            return "Python 3.12.3 (main, Apr 10 2024, 05:33:47) [GCC 13.2.0] on linux\n" +
                   "Type \"help\", \"copyright\", \"credits\" or \"license\" for more information.\n" +
                   ">>> print('Ubuntu 24.04 PRoot Python 3.12 Engine Active')\n" +
                   "Ubuntu 24.04 PRoot Python 3.12 Engine Active"
        }
        val script = parts[1]
        return ">>> Running Python 3.12 with $script in Ubuntu proot-distro...\n" +
               "Execution finished with exit code 0."
    }

    private fun renderNeofetch(): String {
        return """
            .-/+oossssoo+/-.               ${_currentUser.value}@ubuntu-noble
        `:+ssssssssssssssssss+:`           -----------------
      -+ssssssssssssssssssyyssss+-         OS: Ubuntu 24.04 LTS aarch64
    .ossssssssssssssssssdMMMNysssso.       Host: Android Subsystem (PRoot-Distro)
   /ssssssssssshdmmNNmmyNMMMMhssssss/      Kernel: Linux 6.6.0-aether
  +ssssssssshmydMMMMMMMNddddyssssssss+     Uptime: 2 days, 14 hours, 20 mins
 /sssssssshNMMMyhhyyyyhmNMMMNhssssssss/    Packages: ${_installedPackagesCount.value} (dpkg)
.ssssssssdMMMNhsssssssssshNMMMdssssssss.   Shell: bash 5.2.21
+sssshhhyNMMNyssssssssssssyNMMMysssssss+   Terminal: proot-pty
ossyNMMMNyMMhsssssssssssssshmmmhssssssso   CPU: ARMv8 Cortex-A78 (8) @ 2.80GHz
ossyNMMMNyMMhsssssssssssssshmmmhssssssso   Memory: 1420MiB / 8192MiB
+sssshhhyNMMNyssssssssssssyNMMMysssssss+   GPU: Qualcomm Adreno / VirGL 3D
.ssssssssdMMMNhsssssssssshNMMMdssssssss.   Disk (/): 18G / 64G (29%)
 /sssssssshNMMMyhhyyyyhmNMMMNhssssssss/    Storage (/mnt/sdcard): 42G / 128G
  +ssssssssshmydMMMMMMMNddddyssssssss+     
   /ssssssssssshdmmNNmmyNMMMMhssssss/      
    .ossssssssssssssssssdMMMNysssso.       
      -+ssssssssssssssssssyyssss+-         
        `:+ssssssssssssssssss+:`           
            .-/+oossssoo+/-.               
        """.trimIndent()
    }

    private fun appendLine(line: String) {
        val list = _terminalOutput.value.toMutableList()
        list.add(line)
        if (list.size > 250) {
            _terminalOutput.value = list.takeLast(250)
        } else {
            _terminalOutput.value = list
        }
    }
}
