package com.example.engine

import com.example.model.FileCategory
import com.example.model.SmartBridgeFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CrossOsFileBridge {

    private val _files = MutableStateFlow<List<SmartBridgeFile>>(generateInitialFiles())
    val files: StateFlow<List<SmartBridgeFile>> = _files.asStateFlow()

    private val _androidStorageFreeGb = MutableStateFlow(84.5f)
    val androidStorageFreeGb: StateFlow<Float> = _androidStorageFreeGb.asStateFlow()

    private val _linuxRootfsFreeGb = MutableStateFlow(28.2f)
    val linuxRootfsFreeGb: StateFlow<Float> = _linuxRootfsFreeGb.asStateFlow()

    private fun generateInitialFiles(): List<SmartBridgeFile> {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
        val now = Date()

        return listOf(
            // Linux Workspace Files
            SmartBridgeFile(
                id = UUID.randomUUID().toString(),
                name = "main.py",
                path = "/home/ubuntu/projects/ai_engine/main.py",
                sizeBytes = 14200,
                modifiedFormatted = dateFormat.format(now),
                category = FileCategory.CODE,
                isLinuxSpace = true,
                isSymlinked = true,
                extension = "py"
            ),
            SmartBridgeFile(
                id = UUID.randomUUID().toString(),
                name = "Dockerfile",
                path = "/home/ubuntu/projects/ai_engine/Dockerfile",
                sizeBytes = 2800,
                modifiedFormatted = dateFormat.format(now),
                category = FileCategory.CODE,
                isLinuxSpace = true,
                extension = "dockerfile"
            ),
            SmartBridgeFile(
                id = UUID.randomUUID().toString(),
                name = "Project_Report_Q4.odt",
                path = "/home/ubuntu/Documents/Project_Report_Q4.odt",
                sizeBytes = 540200,
                modifiedFormatted = dateFormat.format(now),
                category = FileCategory.DOCUMENTS,
                isLinuxSpace = true,
                extension = "odt"
            ),
            SmartBridgeFile(
                id = UUID.randomUUID().toString(),
                name = "virglrenderer-dev_0.10.4_arm64.deb",
                path = "/home/ubuntu/packages/virglrenderer-dev_0.10.4_arm64.deb",
                sizeBytes = 8900400,
                modifiedFormatted = dateFormat.format(now),
                category = FileCategory.LINUX_DEB,
                isLinuxSpace = true,
                extension = "deb"
            ),
            SmartBridgeFile(
                id = UUID.randomUUID().toString(),
                name = "benchmark_gl.sh",
                path = "/home/ubuntu/scripts/benchmark_gl.sh",
                sizeBytes = 3500,
                modifiedFormatted = dateFormat.format(now),
                category = FileCategory.CODE,
                isLinuxSpace = true,
                extension = "sh"
            ),

            // Android Storage Files (Bridged into Linux)
            SmartBridgeFile(
                id = UUID.randomUUID().toString(),
                name = "Research_Notes.pdf",
                path = "/sdcard/Documents/Research_Notes.pdf",
                sizeBytes = 2100400,
                modifiedFormatted = dateFormat.format(now),
                category = FileCategory.DOCUMENTS,
                isLinuxSpace = false,
                isSymlinked = true,
                extension = "pdf"
            ),
            SmartBridgeFile(
                id = UUID.randomUUID().toString(),
                name = "dataset_weights_v2.tar.gz",
                path = "/sdcard/Download/dataset_weights_v2.tar.gz",
                sizeBytes = 48500000,
                modifiedFormatted = dateFormat.format(now),
                category = FileCategory.ARCHIVES,
                isLinuxSpace = false,
                isSymlinked = true,
                extension = "tar.gz"
            ),
            SmartBridgeFile(
                id = UUID.randomUUID().toString(),
                name = "design_system_preview.png",
                path = "/sdcard/Pictures/design_system_preview.png",
                sizeBytes = 1420500,
                modifiedFormatted = dateFormat.format(now),
                category = FileCategory.MEDIA,
                isLinuxSpace = false,
                isSymlinked = false,
                extension = "png"
            ),
            SmartBridgeFile(
                id = UUID.randomUUID().toString(),
                name = "code_backup.zip",
                path = "/sdcard/Download/code_backup.zip",
                sizeBytes = 12400300,
                modifiedFormatted = dateFormat.format(now),
                category = FileCategory.ARCHIVES,
                isLinuxSpace = false,
                extension = "zip"
            )
        )
    }

    fun toggleSymlink(fileId: String) {
        _files.value = _files.value.map { file ->
            if (file.id == fileId) {
                file.copy(isSymlinked = !file.isSymlinked)
            } else {
                file
            }
        }
    }

    fun copyAcrossBridge(file: SmartBridgeFile) {
        val targetIsLinux = !file.isLinuxSpace
        val targetPath = if (targetIsLinux) {
            "/home/ubuntu/shared_android/${file.name}"
        } else {
            "/sdcard/Download/linux_export/${file.name}"
        }

        val newFile = file.copy(
            id = UUID.randomUUID().toString(),
            path = targetPath,
            isLinuxSpace = targetIsLinux,
            isSymlinked = true,
            modifiedFormatted = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date())
        )
        _files.value = listOf(newFile) + _files.value
    }

    fun createNewFile(name: String, category: FileCategory, isLinux: Boolean) {
        val ext = name.substringAfterLast('.', "")
        val path = if (isLinux) "/home/ubuntu/$name" else "/sdcard/Documents/$name"
        val newFile = SmartBridgeFile(
            id = UUID.randomUUID().toString(),
            name = name,
            path = path,
            sizeBytes = 1024,
            modifiedFormatted = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date()),
            category = category,
            isLinuxSpace = isLinux,
            isSymlinked = false,
            extension = ext
        )
        _files.value = listOf(newFile) + _files.value
    }
}
