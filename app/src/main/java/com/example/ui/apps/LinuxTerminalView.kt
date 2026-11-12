package com.example.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PRootUbuntuDistroEngine
import com.example.engine.RealLinuxTerminalEngine
import com.example.ui.theme.UbuntuAubergine
import com.example.ui.theme.UbuntuOrange
import com.example.ui.theme.UbuntuTerminalBg
import com.example.ui.theme.YaruTextWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LinuxTerminalView(
    prootEngine: PRootUbuntuDistroEngine,
    realEngine: RealLinuxTerminalEngine? = null,
    modifier: Modifier = Modifier
) {
    val prootHistory by prootEngine.terminalOutput.collectAsState()
    val cwd by prootEngine.currentWorkingDir.collectAsState()
    val currentUser by prootEngine.currentUser.collectAsState()

    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(prootHistory.size) {
        if (prootHistory.isNotEmpty()) {
            listState.animateScrollToItem(prootHistory.size - 1)
        }
    }

    // Quick touch bar with authentic Ubuntu / apt commands
    val quickCommands = listOf(
        "apt update",
        "apt install -y python3",
        "apt list --installed",
        "neofetch",
        "ls -la",
        "cat /etc/os-release",
        "python3 -c \"print('Hello from Ubuntu 24.04 LTS!')\"",
        "clear"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UbuntuTerminalBg) // Official Ubuntu Terminal Aubergine (#300A24)
            .padding(6.dp)
            .testTag("linux_terminal_view")
    ) {
        // Output Console with Real Ubuntu PRoot Terminal Output
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(UbuntuTerminalBg)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            items(prootHistory) { line ->
                val textColor = when {
                    line.startsWith("ubuntu@") || line.startsWith("root@") -> Color(0xFF4E9A06)
                    line.startsWith("Hit:") || line.startsWith("Get:") -> Color(0xFF729FCF)
                    line.startsWith("Reading package lists...") || line.startsWith("Building dependency tree...") -> Color(0xFF06989A)
                    line.startsWith("Setting up") || line.startsWith("Unpacking") -> Color(0xFFAD7FA8)
                    line.startsWith("E:") || line.startsWith("error") -> Color(0xFFEF2929)
                    line.startsWith("[") || line.startsWith("->") -> UbuntuOrange
                    line.contains("OS:") || line.contains("Host:") || line.contains("Kernel:") || line.contains("Uptime:") -> Color(0xFFFCE94F)
                    else -> YaruTextWhite
                }
                Text(
                    text = line,
                    color = textColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Quick Linux Commands Touch Bar (No typing needed, one tap)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quickCommands.forEach { cmd ->
                Surface(
                    color = Color(0xFF452038),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.clickable {
                        prootEngine.executeCommand(cmd)
                    }
                ) {
                    Text(
                        text = cmd,
                        color = YaruTextWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Terminal Command Input Line with dynamic prompt
        val promptSymbol = if (currentUser == "root") "# " else "$ "
        val promptUser = "$currentUser@ubuntu-noble:${cwd.replace("/home/ubuntu", "~")}$promptSymbol"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFF24061B), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = promptUser,
                color = if (currentUser == "root") Color(0xFFEF2929) else Color(0xFF4E9A06),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )

            TextField(
                value = inputCommand,
                onValueChange = { inputCommand = it },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("terminal_input_field"),
                placeholder = {
                    Text("apt install, python3, ls, ...", color = Color(0xFF888888), fontSize = 11.sp)
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = UbuntuOrange,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputCommand.isNotBlank()) {
                            prootEngine.executeCommand(inputCommand)
                            inputCommand = ""
                        }
                    }
                )
            )

            IconButton(
                onClick = {
                    if (inputCommand.isNotBlank()) {
                        prootEngine.executeCommand(inputCommand)
                        inputCommand = ""
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = UbuntuOrange,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
