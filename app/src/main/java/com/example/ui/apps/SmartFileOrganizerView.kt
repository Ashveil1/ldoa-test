package com.example.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CrossOsFileBridge
import com.example.model.FileCategory
import com.example.model.SmartBridgeFile
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TerminalAmber
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextFrostWhite
import com.example.ui.theme.TextMuted

@Composable
fun SmartFileOrganizerView(
    bridge: CrossOsFileBridge,
    modifier: Modifier = Modifier
) {
    val allFiles by bridge.files.collectAsState()
    val androidFreeGb by bridge.androidStorageFreeGb.collectAsState()
    val linuxFreeGb by bridge.linuxRootfsFreeGb.collectAsState()

    // Real data: scan the rootfs and accessible Android storage on open.
    LaunchedEffect(Unit) { bridge.refresh() }

    var selectedSpace by remember { mutableStateOf<Boolean?>(null) } // null = All, true = Linux, false = Android
    var selectedCategory by remember { mutableStateOf(FileCategory.ALL) }
    var searchKeyword by remember { mutableStateOf("") }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }

    val filteredFiles = allFiles.filter { file ->
        val matchesSpace = when (selectedSpace) {
            true -> file.isLinuxSpace
            false -> !file.isLinuxSpace
            null -> true
        }
        val matchesCategory = selectedCategory == FileCategory.ALL || file.category == selectedCategory
        val matchesSearch = searchKeyword.isBlank() || file.name.contains(searchKeyword, ignoreCase = true) || file.path.contains(searchKeyword, ignoreCase = true)
        matchesSpace && matchesCategory && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(10.dp)
            .testTag("smart_file_organizer_view")
    ) {
        // Storage Overview Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface, RoundedCornerShape(8.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Linux Rootfs: ${linuxFreeGb} GB free", color = TextFrostWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Android Storage: ${androidFreeGb} GB free", color = TextMuted, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar & Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                placeholder = { Text("Search files, scripts, packages...", color = TextMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("file_search_field"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface,
                    focusedTextColor = TextFrostWhite,
                    unfocusedTextColor = TextFrostWhite,
                    cursorColor = NeonCyan,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { showNewFileDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("new_file_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New File", tint = ObsidianBg, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // OS Space Selector Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val spaceOptions = listOf(
                Pair(null, "All Spaces (${allFiles.size})"),
                Pair(true, "Linux Ubuntu (/home)"),
                Pair(false, "Android Storage (/sdcard)")
            )

            spaceOptions.forEach { (spaceKey, label) ->
                val isSelected = selectedSpace == spaceKey
                Surface(
                    color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else ObsidianSurface,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { selectedSpace = spaceKey }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) NeonCyan else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Category Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(FileCategory.values()) { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    color = if (isSelected) ElectricViolet.copy(alpha = 0.25f) else ObsidianSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat.label,
                        color = if (isSelected) ElectricViolet else TextFrostWhite,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // File List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredFiles, key = { it.id }) { file ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    when (file.category) {
                                        FileCategory.CODE -> NeonCyan.copy(alpha = 0.15f)
                                        FileCategory.DOCUMENTS -> ElectricViolet.copy(alpha = 0.15f)
                                        FileCategory.MEDIA -> TerminalGreen.copy(alpha = 0.15f)
                                        FileCategory.ARCHIVES -> TerminalAmber.copy(alpha = 0.15f)
                                        FileCategory.LINUX_DEB -> Color(0xFFFF5252).copy(alpha = 0.15f)
                                        else -> ObsidianSurfaceVariant
                                    },
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (file.category) {
                                    FileCategory.CODE -> Icons.Default.Code
                                    FileCategory.DOCUMENTS -> Icons.Default.Description
                                    FileCategory.MEDIA -> Icons.Default.Image
                                    FileCategory.ARCHIVES -> Icons.Default.Archive
                                    FileCategory.LINUX_DEB -> Icons.Default.Folder
                                    else -> Icons.Default.Folder
                                },
                                contentDescription = null,
                                tint = when (file.category) {
                                    FileCategory.CODE -> NeonCyan
                                    FileCategory.DOCUMENTS -> ElectricViolet
                                    FileCategory.MEDIA -> TerminalGreen
                                    FileCategory.ARCHIVES -> TerminalAmber
                                    FileCategory.LINUX_DEB -> Color(0xFFFF5252)
                                    else -> TextFrostWhite
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = file.name,
                                    color = TextFrostWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (file.isSymlinked) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = TerminalGreen.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Link, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Linked", color = TerminalGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Text(
                                text = file.path,
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                            Text(
                                text = "${file.sizeBytes / 1024} KB • ${file.modifiedFormatted} • ${if (file.isLinuxSpace) "Ubuntu" else "Android"}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }

                        // Bridge Transfer Action Button
                        IconButton(
                            onClick = { bridge.copyAcrossBridge(file) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(ObsidianSurfaceVariant, RoundedCornerShape(6.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = if (file.isLinuxSpace) "Export to Android" else "Bridge to Linux",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // New File Creation Dialog
        if (showNewFileDialog) {
            AlertDialog(
                onDismissRequest = { showNewFileDialog = false },
                containerColor = ObsidianSurface,
                title = { Text("Create New File", color = TextFrostWhite, fontSize = 16.sp) },
                text = {
                    Column {
                        Text("Enter file name with extension (e.g. script.py, notes.md, data.json):", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            placeholder = { Text("script.py", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFileName.isNotBlank()) {
                                bridge.createNewFile(newFileName, FileCategory.CODE, true)
                                newFileName = ""
                                showNewFileDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Create", color = ObsidianBg, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFileDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            )
        }
    }
}
