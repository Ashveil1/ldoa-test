package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import com.example.viewmodel.SetupState
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.ActiveMode
import com.example.model.LinuxAppDefinition
import com.example.ui.apps.GpuAccelerationPanel
import com.example.ui.apps.SmartFileOrganizerView
import com.example.ui.desktop.DesktopWorkspace
import com.example.ui.launcher.AndroidHomeLauncher
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.viewmodel.AetherDesktopViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AetherDesktopViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        setContent {
            MyApplicationTheme {
                val setupState by viewModel.setupState.collectAsState()
                if (setupState != SetupState.Done) {
                    LaunchedEffect(Unit) { viewModel.runSetup() }
                    com.example.ui.setup.FirstRunSetupScreen(
                        state = setupState,
                        onRetry = { viewModel.runSetup() }
                    )
                } else {
                    AetherMainContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AetherMainContent(
    viewModel: AetherDesktopViewModel
) {
    val activeMode by viewModel.activeMode.collectAsState()
    val isStartOpen by viewModel.isStartMenuOpen.collectAsState()
    val isDrawerOpen by viewModel.isAppDrawerOpen.collectAsState()
    val windows by viewModel.windows.collectAsState()
    val focusedId by viewModel.focusedWindowId.collectAsState()

    // Handle Android system back gesture / back button
    BackHandler(enabled = isStartOpen || isDrawerOpen || activeMode != ActiveMode.HOME_LAUNCHER) {
        when {
            isStartOpen -> viewModel.closeStartMenu()
            isDrawerOpen -> viewModel.setAppDrawerOpen(false)
            activeMode != ActiveMode.HOME_LAUNCHER -> viewModel.setActiveMode(ActiveMode.HOME_LAUNCHER)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .safeDrawingPadding()
            // Keyboard shortcut handling for Bluetooth/USB hardware keyboards
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when {
                        // Super / Windows key toggles Start Menu
                        keyEvent.key == Key.MetaLeft || keyEvent.key == Key.MetaRight -> {
                            viewModel.toggleStartMenu()
                            true
                        }
                        // Alt + Tab cycles through open windows
                        keyEvent.isAltPressed && keyEvent.key == Key.Tab -> {
                            if (windows.isNotEmpty()) {
                                val currentIndex = windows.indexOfFirst { it.id == focusedId }
                                val nextIndex = (currentIndex + 1) % windows.size
                                viewModel.focusWindow(windows[nextIndex].id)
                            }
                            true
                        }
                        // Ctrl + Alt + T opens Ubuntu Terminal
                        keyEvent.isCtrlPressed && keyEvent.isAltPressed && keyEvent.key == Key.T -> {
                            viewModel.openLinuxApp(
                                LinuxAppDefinition(
                                    id = "terminal",
                                    name = "Ubuntu Terminal",
                                    category = "System",
                                    description = "Bash shell",
                                    command = "bash",
                                    iconName = "terminal"
                                )
                            )
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            // Pointer detection for wireless mice
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Main)
                        val hasMouse = event.changes.any { it.type == PointerType.Mouse }
                        if (hasMouse && !viewModel.hardwareMouseDetected.value) {
                            viewModel.setHardwareMouseDetected(true)
                        }
                    }
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (activeMode) {
                ActiveMode.HOME_LAUNCHER -> {
                    AndroidHomeLauncher(viewModel = viewModel)
                }
                ActiveMode.DESKTOP_WORKSPACE -> {
                    DesktopWorkspace(viewModel = viewModel)
                }
                ActiveMode.FILES_ORGANIZER -> {
                    SmartFileOrganizerView(bridge = viewModel.fileBridge)
                }
                ActiveMode.GPU_SETTINGS -> {
                    GpuAccelerationPanel(
                        engine = viewModel.linuxEngine,
                        displayManager = viewModel.displayManager,
                        telemetry = viewModel.telemetry.value
                    )
                }
            }
        }
    }
}
