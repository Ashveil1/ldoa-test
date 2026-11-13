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
    // Absolute scrollback offset (survives front-eviction in TerminalSession;
    // offsets below session.droppedChars have expired).
    private var delivered = 0L

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
        "${_currentUser.value}@ubuntu-noble:${_currentWorkingDir.value.replace("/root", "~")}# "

    /** Shell echoes of lines WE sent (no pty, so `bash -i` echoes its
     *  stdin). Swallowed in drain order so the user sees each command once
     *  (our prompt-led local echo) instead of twice. */
    private val pendingEcho = ArrayDeque<String>()

    /** cwd travels out-of-band: the guest writes it to this file after each
     *  command; nothing about tracking ever enters the visible stream. */
    private val cwdFile: File
        get() = File(rootfsDir, "tmp/.aether_cwd")

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
        pendingEcho.clear()
        delivered = 0L // new session, new scrollback: resync the read offset
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
        val sess = session ?: return
        val snap = sess.snapshot()
        // Resync if the capped scrollback evicted content we never delivered.
        if (delivered < sess.droppedChars) delivered = sess.droppedChars
        val offset = (delivered - sess.droppedChars).toInt()
        if (snap.length <= offset) return
        val fresh = snap.substring(offset)
        // Only consume COMPLETE lines; a chunk can end mid-line and a
        // partial line must not be echo-matched or displayed half-formed.
        val lastNl = fresh.lastIndexOf('\n')
        if (lastNl < 0) return
        delivered += lastNl + 1
        val outLines = mutableListOf<String>()
        for (line in fresh.substring(0, lastNl + 1).split("\n")) {
            val clean = line.removePrefix("\r").trimEnd()
            if (pendingEcho.isNotEmpty() && clean == pendingEcho.first()) {
                pendingEcho.removeFirst()
                continue
            }
            outLines.add(line.removeSuffix("\r"))
        }
        appendLines(outLines)
        // Out-of-band cwd: written by the guest after every command.
        runCatching {
            val cwd = cwdFile.readText().trim()
            if (cwd.isNotEmpty()) _currentWorkingDir.value = cwd
        }
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
        var s = session
        if ((s == null || !s.isRunning()) && _ready.value) {
            // Session died under us (guest crash, OOM kill): restart once
            // instead of leaving the terminal permanently dead.
            appendLines(listOf("guest session ended - restarting…"))
            startSession()
            s = session
        }
        if (s == null || !s.isRunning()) {
            appendLines(listOf("guest not ready yet - install/boot still in progress."))
            return
        }
        // cwd goes out-of-band (file, not stdout), so no marker line can
        // ever leak into the terminal. The shell's echo of each sent line
        // is swallowed in drainSessionOutput via pendingEcho. With bash the
        // shell itself tracks cwd via PROMPT_COMMAND (set in TerminalSession),
        // so NOTHING extra is written to stdin - interactive programs (apt's
        // [Y/n], less, editors) used to eat the injected `pwd > ...` line as
        // their own input. The dash fallback still needs the injected line.
        cmd.split('\n').forEach { pendingEcho.addLast(it) }
        if (s.shellTracksCwd) {
            s.send(cmd)
        } else {
            pendingEcho.addLast("pwd > /tmp/.aether_cwd")
            s.send("$cmd\npwd > /tmp/.aether_cwd")
        }
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

}
