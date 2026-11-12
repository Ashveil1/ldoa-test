package com.example.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * One real Ubuntu package entry for the package panel, read back from the
 * guest's own apt/dpkg metadata (no canned catalog).
 */
data class UbuntuPackage(
    val name: String,
    val version: String,
    val section: String,
    val sizeKb: Int,
    val description: String,
    val isInstalled: Boolean = false,
    val binaries: List<String> = emptyList()
)

/**
 * Real engine: a genuine Ubuntu 24.04 userland (official ubuntu-base tarball,
 * pinned + sha256-verified) executed by the Termux-built proot shipped in
 * jniLibs. Same StateFlow API the UI already consumes - only the internals
 * changed from simulated to real.
 *
 * The shell runs under `proot -0` (guest uid 0) so apt/dpkg work. Without a
 * pty there is no job control; a persistent dash session takes commands on
 * stdin and streams merged output back.
 */
class PRootUbuntuDistroEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val installer = DistroInstaller(context)
    private val distro = Distros.UBUNTU
    private val rootfsDir: File get() = installer.rootfsDir(distro)
    private var session: TerminalSession? = null
    private var delivered = 0

    private val _terminalOutput = MutableStateFlow<List<String>>(emptyList())
    val terminalOutput: StateFlow<List<String>> = _terminalOutput.asStateFlow()

    private val _currentWorkingDir = MutableStateFlow("/root")
    val currentWorkingDir: StateFlow<String> = _currentWorkingDir.asStateFlow()

    private val _currentUser = MutableStateFlow("root")
    val currentUser: StateFlow<String> = _currentUser.asStateFlow()

    private val _installedPackagesCount = MutableStateFlow(0)
    val installedPackagesCount: StateFlow<Int> = _installedPackagesCount.asStateFlow()

    private val _availablePackages = MutableStateFlow<Map<String, UbuntuPackage>>(emptyMap())
    val availablePackages: StateFlow<Map<String, UbuntuPackage>> = _availablePackages.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        scope.launch { bootRealGuest() }
    }

    private fun appendLines(lines: List<String>) {
        val merged = (_terminalOutput.value + lines)
        _terminalOutput.value = if (merged.size > 2000) merged.takeLast(1500) else merged
    }

    private fun promptLine(): String =
        "${_currentUser.value}@ubuntu-noble:${_currentWorkingDir.value}# "

    private suspend fun bootRealGuest() {
        appendLines(listOf(
            "AetherLinux real-engine boot",
            "distro: ${distro.displayName} (ubuntu-base ${distro.id}, pinned + sha256-verified)"
        ))
        if (!installer.isInstalled(distro)) {
            // The download keeps its partial file; every attempt resumes with
            // an HTTP Range request, so a dropped connection costs nothing.
            var ok = false
            var attempt = 0
            while (!ok && attempt < 5) {
                attempt++
                if (attempt > 1) {
                    appendLines(listOf("retrying download (attempt $attempt/5, resuming)…"))
                    delay(2_000L * attempt)
                }
                var lastPct = -1
                ok = installer.install(distro) { st ->
                    when (st) {
                        is InstallState.Downloading -> {
                            val pct = if (st.totalBytes > 0) (st.doneBytes * 100 / st.totalBytes).toInt() else 0
                            if (pct / 10 != lastPct / 10) {
                                lastPct = pct
                                appendLines(listOf("download: $pct% (${st.doneBytes / 1024} / ${st.totalBytes / 1024} KiB)"))
                            }
                        }
                        InstallState.Verifying -> appendLines(listOf("verifying sha256…"))
                        InstallState.Extracting -> appendLines(listOf("extracting rootfs…"))
                        InstallState.Installed -> appendLines(listOf("install complete."))
                        is InstallState.Failed -> appendLines(listOf("install attempt $attempt failed: ${st.message}"))
                        else -> {}
                    }
                }
            }
            if (!ok) {
                appendLines(listOf("Real guest unavailable after 5 attempts. Partial download is kept - type \"retry\" to resume."))
                return
            }
        }
        if (!Proot.binariesPresent(context)) {
            appendLines(listOf("proot binaries missing from nativeLibraryDir - cannot start guest."))
            return
        }
        startSession()
    }

    private fun sessionShellName(): String =
        if (java.io.File(rootfsDir, "bin/bash").exists()) "bash" else "dash"

    private fun startSession() {
        val s = TerminalSession(
            context, rootfsDir,
            fakeRoot = true, // apt/dpkg refuse to run as non-root
            ps1 = "" // the UI echoes its own prompt; a double prompt is noise
        ) { drainSessionOutput() }
        session = s
        if (!s.start()) {
            appendLines(listOf("session failed to start (see terminal-session.log)"))
            return
        }
        _ready.value = true
        appendLines(listOf("real guest shell up (proot -0, ${sessionShellName()}). Type commands below."))
        // apt needs its index once per fresh rootfs; run quietly in background.
        scope.launch {
            appendLines(listOf("refreshing apt index (first-boot, one time)…"))
            val r = Proot.runGuest(context, rootfsDir, listOf("apt-get", "update", "-qq"), fakeRoot = true)
            appendLines(listOf(if (r.exitCode == 0) "apt index ready." else "apt update exit=${r.exitCode}: ${r.stderr.take(200)}"))
            refreshPackageState()
        }
    }

    /** Pull newly-written bytes out of the session scrollback into the UI. */
    private fun drainSessionOutput() {
        val snap = session?.snapshot() ?: return
        if (snap.length <= delivered) return
        val fresh = snap.substring(delivered)
        delivered = snap.length
        // Strip the cwd marker lines before display; update cwd from them.
        val outLines = mutableListOf<String>()
        for (line in fresh.split("\n")) {
            val m = line.removePrefix("\r")
            if (m.startsWith(PWD_MARKER)) {
                _currentWorkingDir.value = m.removePrefix(PWD_MARKER).trim().ifEmpty { _currentWorkingDir.value }
            } else {
                outLines.add(line)
            }
        }
        appendLines(outLines)
    }

    fun executeCommand(rawCommand: String) {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return
        appendLines(listOf(promptLine() + rawCommand))
        if (cmd == "clear") {
            _terminalOutput.value = emptyList()
            return
        }
        if (cmd == "retry" && !_ready.value) {
            scope.launch { bootRealGuest() }
            return
        }
        val s = session
        if (s == null || !s.isRunning()) {
            appendLines(listOf("guest not ready yet - install/boot still in progress."))
            return
        }
        // The marker round-trip reports the real cwd after the command,
        // covering cd, pushd and scripts that change directory.
        s.send("$cmd\necho $PWD_MARKER\$PWD")
    }

    private fun refreshPackageState() {
        scope.launch {
            val count = Proot.runGuest(context, rootfsDir,
                listOf("/bin/sh", "-c", "dpkg-query -f 'x\n' -W 2>/dev/null | wc -l"), fakeRoot = true)
            _installedPackagesCount.value = count.stdout.trim().toIntOrNull() ?: 0
            // Package panel data: first 400 entries of the real apt catalog.
            val list = Proot.runGuest(context, rootfsDir,
                listOf("/bin/sh", "-c", "apt list 2>/dev/null | tail -n +2 | head -400"), fakeRoot = true)
            val installedNames = Proot.runGuest(context, rootfsDir,
                listOf("/bin/sh", "-c", "dpkg-query -f '\${Package}\n' -W 2>/dev/null"), fakeRoot = true)
                .stdout.lines().filter { it.isNotBlank() }.toSet()
            val catalog = mutableMapOf<String, UbuntuPackage>()
            for (line in list.stdout.lines()) {
                // format: name/suite,now version arch [status]
                val name = line.substringBefore('/'); if (name.isBlank()) continue
                val rest = line.substringAfter(' ', "")
                val version = rest.substringBefore(' ')
                catalog[name] = UbuntuPackage(
                    name = name, version = version, section = "",
                    sizeKb = 0, description = "",
                    isInstalled = installedNames.contains(name)
                )
            }
            _availablePackages.value = catalog
        }
    }

    companion object {
        private const val PWD_MARKER = "__AETHER_PWD:"
    }
}
