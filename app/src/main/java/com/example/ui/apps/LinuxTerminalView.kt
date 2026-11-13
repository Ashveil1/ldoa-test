package com.example.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PRootUbuntuDistroEngine

/**
 * Terminal emulator surface: one continuous monochrome console, prompt-led
 * input line, no chrome. Colors follow the classic xterm palette.
 */
@Composable
fun LinuxTerminalView(
    prootEngine: PRootUbuntuDistroEngine,
    modifier: Modifier = Modifier
) {
    val history by prootEngine.terminalOutput.collectAsState()
    val cwd by prootEngine.currentWorkingDir.collectAsState()
    val currentUser by prootEngine.currentUser.collectAsState()

    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1)
    }

    val termFont = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("linux_terminal_view")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(history) { line ->
                val color = when {
                    line.startsWith("root@") || line.startsWith("ubuntu@") -> Color(0xFF4E9A06)
                    line.startsWith("E:") || line.startsWith("error", ignoreCase = true) ||
                        line.contains("FAILED", ignoreCase = true) -> Color(0xFFEF2929)
                    line.startsWith("download:") || line.startsWith("install") ||
                        line.startsWith("retrying") -> Color(0xFF729FCF)
                    else -> Color(0xFFDDDDDD)
                }
                Text(text = line, color = color, style = termFont)
            }
        }

        // Prompt-led input, visually part of the console.
        val promptSymbol = if (currentUser == "root") "#" else "$"
        val prompt = "$currentUser@ubuntu-noble:${cwd.replace("/root", "~")}$promptSymbol"
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = prompt,
                color = if (currentUser == "root") Color(0xFFEF2929) else Color(0xFF4E9A06),
                style = termFont,
                fontWeight = FontWeight.Bold
            )
            Box(modifier = Modifier.weight(1f)) {
                TextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    modifier = Modifier.fillMaxWidth().testTag("terminal_input_field"),
                    textStyle = termFont.copy(color = Color.White, fontWeight = FontWeight.Bold),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = Color(0xFFDDDDDD),
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
            }
        }
    }
}
