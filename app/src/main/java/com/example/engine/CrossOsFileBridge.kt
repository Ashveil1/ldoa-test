package com.example.engine

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.model.FileCategory
import com.example.model.SmartBridgeFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Real cross-OS file bridge. Every entry and number comes from the actual
 * filesystem:
 * - Linux space = the installed Ubuntu rootfs (app-private, always readable
 *   from the host; shown with guest-style /home paths).
 * - Android space = storage this app can actually read without special
 *   permissions (app-specific external dir; shared storage only when the OS
 *   lets us list it).
 * - Free-space numbers come from StatFs on the real volumes.
 * - copyAcrossBridge and createNewFile perform real file I/O.
 */
class CrossOsFileBridge(
    private val ctx: Context,
    private val rootfsDir: () -> File
) {

    private val _files = MutableStateFlow<List<SmartBridgeFile>>(emptyList())
    val files: StateFlow<List<SmartBridgeFile>> = _files.asStateFlow()

    private val _androidStorageFreeGb = MutableStateFlow(0f)
    val androidStorageFreeGb: StateFlow<Float> = _androidStorageFreeGb.asStateFlow()

    private val _linuxRootfsFreeGb = MutableStateFlow(0f)
    val linuxRootfsFreeGb: StateFlow<Float> = _linuxRootfsFreeGb.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)

    /** Rescan both spaces from disk. Cheap (bounded walk); call on window
     *  open and after every mutation. */
    fun refresh() {
        val root = rootfsDir()
        _linuxRootfsFreeGb.value = statFsGb(root)
        _androidStorageFreeGb.value = statFsGb(Environment.getExternalStorageDirectory())
        _files.value = scanLinux(root) + scanAndroid()
    }

    private fun statFsGb(f: File): Float = runCatching {
        val bytes = StatFs(f.absolutePath).availableBytes
        Math.round(bytes / 1_073_741_824f * 10f) / 10f
    }.getOrDefault(0f)

    private fun scanLinux(root: File): List<SmartBridgeFile> {
        val home = File(root, "home")
        if (!home.isDirectory) return emptyList()
        val out = mutableListOf<SmartBridgeFile>()
        walk(home, home, out, maxDepth = 6)
        return out
    }

    private fun walk(dir: File, homeRoot: File, out: MutableList<SmartBridgeFile>, maxDepth: Int, depth: Int = 0) {
        if (depth > maxDepth || out.size >= MAX_ENTRIES) return
        val kids = dir.listFiles() ?: return
        for (f in kids.sortedBy { it.name }) {
            if (out.size >= MAX_ENTRIES) return
            if (f.name.startsWith(".") && depth == 0) continue // hide dotfiles at home level
            if (f.isDirectory) {
                if (f.name in SKIP_DIRS) continue
                walk(f, homeRoot, out, maxDepth, depth + 1)
            } else {
                out.add(toBridgeFile(f, linuxSpace = true, guestPrefix = homeRoot.absolutePath))
            }
        }
    }

    private fun scanAndroid(): List<SmartBridgeFile> {
        val roots = buildList<File> {
            ctx.getExternalFilesDir(null)?.let { add(it) }
            // Shared Download: readable on some setups, returns null listing
            // where scoped storage blocks us - in that case we simply show
            // nothing rather than fabricated entries.
            add(File(Environment.getExternalStorageDirectory(), "Download"))
        }
        val out = mutableListOf<SmartBridgeFile>()
        for (r in roots) {
            if (out.size >= MAX_ENTRIES) break
            r.listFiles()?.filter { it.isFile }?.sortedBy { it.name }?.forEach { f ->
                if (out.size < MAX_ENTRIES) out.add(toBridgeFile(f, linuxSpace = false, guestPrefix = null))
            }
        }
        return out
    }

    private fun toBridgeFile(f: File, linuxSpace: Boolean, guestPrefix: String?): SmartBridgeFile {
        val displayPath = if (linuxSpace && guestPrefix != null) {
            "/home" + f.absolutePath.removePrefix(guestPrefix)
        } else f.absolutePath
        val linked = runCatching { f.canonicalPath != f.absolutePath }.getOrDefault(false)
        return SmartBridgeFile(
            id = f.absolutePath, // stable across refreshes
            name = f.name,
            path = displayPath,
            sizeBytes = f.length(),
            modifiedFormatted = dateFormat.format(Date(f.lastModified())),
            category = categorize(f.name),
            isLinuxSpace = linuxSpace,
            isSymlinked = linked,
            extension = f.name.substringAfterLast('.', "")
        )
    }

    private fun categorize(name: String): FileCategory {
        return when (name.substringAfterLast('.', "").lowercase()) {
            "py", "sh", "kt", "kts", "java", "c", "cpp", "h", "js", "ts", "rs", "go",
            "json", "xml", "yml", "yaml", "toml", "md", "dockerfile" -> FileCategory.CODE
            "odt", "ods", "odp", "doc", "docx", "pdf", "txt", "rtf" -> FileCategory.DOCUMENTS
            "png", "jpg", "jpeg", "gif", "webp", "svg", "mp4", "mkv", "mp3", "wav" -> FileCategory.MEDIA
            "zip", "gz", "xz", "bz2", "7z", "rar", "tar", "tgz" -> FileCategory.ARCHIVES
            "deb" -> FileCategory.LINUX_DEB
            else -> FileCategory.DOCUMENTS
        }
    }

    /** Real copy: Linux -> Android app-visible export dir, or Android ->
     *  rootfs /home/ubuntu/shared_android/. No in-memory pretense: the bytes
     *  move and the list refreshes from disk. */
    fun copyAcrossBridge(file: SmartBridgeFile) {
        val root = rootfsDir()
        runCatching {
            if (file.isLinuxSpace) {
                // Reconstruct the host path from the guest-style display path.
                val host = File(root, file.path.removePrefix("/"))
                if (!host.isFile) return@runCatching
                val destDir = File(ctx.getExternalFilesDir(null), "linux_export").apply { mkdirs() }
                host.copyTo(File(destDir, file.name), overwrite = true)
            } else {
                val src = File(file.path)
                if (!src.isFile) return@runCatching
                val destDir = File(root, "home/ubuntu/shared_android").apply { mkdirs() }
                src.copyTo(File(destDir, file.name), overwrite = true)
            }
        }
        refresh()
    }

    /** Creates a REAL empty file (Linux space: rootfs home; Android space:
     *  app-specific external dir). */
    fun createNewFile(name: String, category: FileCategory, isLinux: Boolean) {
        val clean = name.substringAfterLast('/').substringAfterLast('\\')
        if (clean.isBlank() || clean.startsWith(".")) return
        runCatching {
            val dir = if (isLinux) File(rootfsDir(), "home/ubuntu").apply { mkdirs() }
                      else File(ctx.getExternalFilesDir(null), "documents").apply { mkdirs() }
            File(dir, clean).createNewFile()
        }
        refresh()
    }

    private companion object {
        const val MAX_ENTRIES = 2_000
        val SKIP_DIRS = setOf(".cache", ".config", ".local", "snap", "__pycache__")
    }
}
