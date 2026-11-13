package com.example.engine

import android.content.Context
import android.util.Log
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * A persistent guest shell: one proot process running the guest shell inside
 * the installed rootfs, stdout+stderr merged into a bounded scrollback
 * buffer. Lines written via [send] go to the shell's stdin.
 *
 * Robustness notes:
 * - Output is decoded through a streaming UTF-8 reader so multibyte
 *   characters split across pipe reads (Thai, box-drawing) never corrupt.
 * - The scrollback is capped; [droppedChars] counts characters evicted from
 *   the front so consumers tracking an absolute offset stay correct.
 * - The debug session log is capped instead of growing without bound.
 */
class TerminalSession(
    private val ctx: Context,
    private val rootfs: File,
    private val fakeRoot: Boolean = false,
    private val ps1: String = "\\$ ",
    private val onUpdate: () -> Unit
) {
    private val TAG = "PhoneLinuxTerm"
    private var proc: Process? = null
    private val buf = StringBuilder()
    private val sessionLog by lazy { File(ctx.filesDir, "terminal-session.log") }
    private val running = AtomicBoolean(false)
    private val lock = Any()

    /** Characters evicted from the front of the scrollback so far. Consumers
     *  holding an absolute read offset must treat offsets < droppedChars as
     *  expired and resync. */
    @Volatile var droppedChars: Long = 0L
        private set

    /** bash when the guest has it (ubuntu-base does), else sh/dash. */
    val guestShell: String =
        if (File(rootfs, "bin/bash").exists()) "/bin/bash" else "/bin/sh"

    /** True when the shell tracks cwd itself (bash PROMPT_COMMAND writes
     *  /tmp/.aether_cwd after every prompt), so callers must NOT inject a
     *  trailing `pwd > ...` line that interactive programs would eat as
     *  input. */
    val shellTracksCwd: Boolean get() = guestShell == "/bin/bash"

    fun start(): Boolean {
        if (running.get()) return true
        val nd = ctx.applicationInfo.nativeLibraryDir
        val argv = listOf(
            File(nd, "libproot.so").absolutePath,
            "--link2symlink"
        ) + (if (fakeRoot) listOf("-0") else emptyList()) + listOf(
            "-r", rootfs.absolutePath,
            "-b", "/dev", "-b", "/proc", "-b", "/sys",
            "-w", "/root",
            // -i: no pty is available through ProcessBuilder, and a
            // non-interactive busybox sh never prints a prompt, leaving the
            // terminal looking dead. -i gives prompt + echo; it prints one
            // "can't access tty" job-control notice, which is authentic.
            guestShell, "-i"
        )
        return try {
            val tmp = File(ctx.filesDir, "usr/tmp").apply { mkdirs() }
            val pb = ProcessBuilder(argv).redirectErrorStream(true)
            pb.environment().putAll(mutableMapOf(
                "PROOT_TMP_DIR" to tmp.absolutePath,
                "TMPDIR" to tmp.absolutePath,
                "PROOT_LOADER" to File(nd, "libproot_loader.so").absolutePath,
                "PROOT_LOADER_32" to File(nd, "libproot_loader32.so").absolutePath,
                "LD_LIBRARY_PATH" to nd,
                "HOME" to "/root",
                "TERM" to "xterm-256color",
                "PATH" to "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                "PS1" to ps1
            ).apply {
                if (shellTracksCwd) {
                    // bash runs this before every prompt: cwd tracking happens
                    // inside the shell itself, so no extra stdin line is ever
                    // sent to (or eaten by) interactive programs.
                    put("PROMPT_COMMAND", "printf %s \"\$PWD\" > /tmp/.aether_cwd")
                }
            })
            pb.directory(File(ctx.filesDir, "work").apply { mkdirs() })
            val p = pb.start()
            proc = p
            running.set(true)
            thread(isDaemon = true, name = "term-reader") {
                try {
                    // InputStreamReader decodes UTF-8 incrementally: a
                    // multibyte character split across two pipe reads is held
                    // until complete instead of becoming U+FFFD pairs.
                    val reader = InputStreamReader(p.inputStream, Charsets.UTF_8)
                    val chunk = CharArray(4096)
                    while (running.get()) {
                        val n = reader.read(chunk)
                        if (n < 0) break
                        appendToBuf(String(chunk, 0, n))
                        onUpdate()
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "reader ended: ${t.message}")
                    appendSessionLog("READER ENDED: ${t.message}\n")
                } finally {
                    running.set(false); onUpdate()
                }
            }
            Log.i(TAG, "session started pid=$p shell=$guestShell")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "start failed", t)
            synchronized(lock) { buf.append("[session failed: ${t.message}]\n") }
            false
        }
    }

    private fun appendToBuf(s: String) {
        synchronized(lock) {
            buf.append(s)
            if (buf.length > MAX_SCROLLBACK) {
                var cut = buf.length - MAX_SCROLLBACK
                val nl = buf.indexOf('\n', cut)
                if (nl >= 0) cut = nl + 1
                buf.delete(0, cut)
                droppedChars += cut
            }
        }
    }

    private var logAppendsSinceCheck = 0
    private fun appendSessionLog(s: String) {
        runCatching {
            // Bound the log: stat every 64 appends, truncate to the tail when
            // it grows past the cap. Unbounded append-per-chunk filled storage
            // on long sessions.
            if (++logAppendsSinceCheck >= 64) {
                logAppendsSinceCheck = 0
                if (sessionLog.length() > MAX_SESSION_LOG) {
                    val tail = sessionLog.readText().takeLast(MAX_SESSION_LOG / 2)
                    sessionLog.writeText(tail)
                }
            }
            sessionLog.appendText(s)
        }
    }

    fun send(line: String) {
        val p = proc ?: return
        if (!running.get()) return
        try {
            p.outputStream.write((line + "\n").toByteArray(Charsets.UTF_8))
            p.outputStream.flush()
            appendSessionLog("SENT: " + line + "\n")
        } catch (t: Throwable) {
            Log.w(TAG, "write failed: ${t.message}")
        }
    }

    fun snapshot(): String = synchronized(lock) { buf.toString() }

    fun isRunning(): Boolean = running.get()

    fun close() {
        running.set(false)
        try { proc?.destroy() } catch (_: Throwable) {}
        proc = null
    }

    private companion object {
        /** Scrollback cap in characters (~256K): hours of normal shell
         *  output, small enough to never pressure the heap. */
        const val MAX_SCROLLBACK = 256 * 1024
        /** Debug log cap in bytes. */
        const val MAX_SESSION_LOG = 1024 * 1024
    }
}
