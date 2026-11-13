package com.example.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.Proot
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TerminalAmber
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextFrostWhite
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Real code editor: files live in the installed Ubuntu rootfs at
 * /home/ubuntu/projects and Run executes them in proot for real
 * (python3/bash/node when present - a missing interpreter shows the real
 * guest error, not a canned success banner).
 */
@Composable
fun CodeStudioView(
    rootfsDir: File,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val workspace = remember(rootfsDir) {
        File(rootfsDir, "home/ubuntu/projects").apply { mkdirs() }
    }

    var fileNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var codeContent by remember { mutableStateOf("") }
    var savedContent by remember { mutableStateOf("") }
    var terminalOutput by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }

    fun refreshFiles(select: String? = null) {
        // Seed one REAL runnable starter file so a fresh workspace is not
        // a dead end; it is written to disk and runs in proot like any other.
        if (workspace.listFiles()?.isEmpty() != false) {
            File(workspace, "hello.py").writeText(
                "# Runs for real in the Ubuntu guest via proot\n" +
                    "import platform, sys\n" +
                    "print(\"python\", sys.version.split()[0], \"on\", platform.machine())\n" +
                    "print(\"sum of squares 0..99999:\", sum(x*x for x in range(100000)))\n"
            )
        }
        fileNames = workspace.listFiles()
            ?.filter { it.isFile }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
        val target = select ?: selectedFile
        if (target != null && target in fileNames) {
            selectedFile = target
        } else {
            selectedFile = fileNames.firstOrNull()
        }
    }

    LaunchedEffect(workspace) { refreshFiles() }

    // Load the selected file's real bytes.
    LaunchedEffect(selectedFile) {
        val name = selectedFile ?: return@LaunchedEffect
        val text = withContext(Dispatchers.IO) {
            runCatching { File(workspace, name).readText() }.getOrDefault("")
        }
        codeContent = text
        savedContent = text
    }

    fun saveCurrent() {
        val name = selectedFile ?: return
        runCatching { File(workspace, name).writeText(codeContent) }
        savedContent = codeContent
    }

    fun runCurrent() {
        val name = selectedFile ?: return
        saveCurrent()
        val interpreter = when (name.substringAfterLast('.', "").lowercase()) {
            "py" -> "python3"
            "sh" -> "bash"
            "js" -> "node"
            "rb" -> "ruby"
            else -> null
        }
        if (interpreter == null) {
            terminalOutput = "No interpreter mapping for .$name files - run it from the terminal window instead."
            return
        }
        isRunning = true
        terminalOutput = "$ $interpreter /home/ubuntu/projects/$name\n"
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    Proot.runGuest(
                        context, rootfsDir,
                        listOf(interpreter, "/home/ubuntu/projects/$name"),
                        fakeRoot = false
                    )
                }.getOrElse { Proot.Result(127, "", "host exec failed: ${it.message}", 0) }
            }
            terminalOutput = buildString {
                append("$ $interpreter /home/ubuntu/projects/$name\n")
                if (result.stdout.isNotBlank()) append(result.stdout.trimEnd()).append('\n')
                if (result.stderr.isNotBlank()) append(result.stderr.trimEnd()).append('\n')
                append("[exit ${result.exitCode} in ${result.wallMs} ms]")
            }
            isRunning = false
        }
    }

    val dirty = codeContent != savedContent

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("code_studio_view")
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Code Studio",
                color = TextFrostWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            // Real file tabs
            Row(modifier = Modifier.weight(1f)) {
                fileNames.take(6).forEach { name ->
                    val isSelected = name == selectedFile
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) ObsidianSurfaceVariant else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { if (dirty) saveCurrent(); selectedFile = name }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isSelected && dirty) "$name *" else name,
                            color = if (isSelected) NeonCyan else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            // New real file
            IconButton(
                onClick = {
                    var i = 1
                    while (File(workspace, "untitled$i.py").exists()) i++
                    val name = "untitled$i.py"
                    File(workspace, name).writeText("")
                    refreshFiles(select = name)
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New file", tint = TextMuted, modifier = Modifier.size(16.dp))
            }

            IconButton(
                onClick = { saveCurrent() },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "Save",
                    tint = if (dirty) NeonCyan else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = { if (!isRunning) runCurrent() },
                colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("code_run_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run Code",
                    tint = ObsidianBg,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (isRunning) "Running" else "Run",
                    color = ObsidianBg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Editor Body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (selectedFile == null) {
                Text(
                    "No files in /home/ubuntu/projects yet - tap + to create one.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            } else {
                TextField(
                    value = codeContent,
                    onValueChange = { codeContent = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("code_editor_field"),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextFrostWhite
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg,
                        focusedTextColor = TextFrostWhite,
                        unfocusedTextColor = TextFrostWhite,
                        cursorColor = NeonCyan,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }

        // Run Console Output Panel: real guest stdout/stderr/exit
        if (terminalOutput.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF070A0F))
                    .border(1.dp, BorderSubtle)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GUEST EXECUTION OUTPUT (real proot)",
                        color = TerminalAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Clear",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { terminalOutput = "" }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = terminalOutput,
                    color = TerminalGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
