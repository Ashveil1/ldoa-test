package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.apps.LinuxTerminalView
import com.example.ui.launcher.AndroidHomeLauncher
import com.example.ui.setup.FirstRunSetupScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AetherDesktopViewModel
import com.example.viewmodel.SetupState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Actual-pixel renders of the main surfaces for visual QA (no emulator).
 * PNGs land in app/build/outputs/robolectric-screens/.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiScreenshotRenderTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun save(name: String) {
        composeRule.waitForIdle()
        val root = composeRule.activity.window.decorView
        val density = composeRule.activity.resources.displayMetrics.density
        val w = (411 * density).toInt()
        val h = (891 * density).toInt()
        root.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, w, h)
        composeRule.waitForIdle()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bmp))
        val dir = File("build/outputs/robolectric-screens")
        dir.mkdirs()
        FileOutputStream(File(dir, "$name.png")).use {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun render_firstRunSetup_downloading() {
        composeRule.setContent {
            MyApplicationTheme {
                FirstRunSetupScreen(
                    state = SetupState.Running(62, "Downloading… 18391 / 29696 KiB"),
                    onRetry = {}
                )
            }
        }
        composeRule.waitForIdle()
        save("setup-downloading")
    }

    @Test
    fun render_firstRunSetup_failed() {
        composeRule.setContent {
            MyApplicationTheme {
                FirstRunSetupScreen(
                    state = SetupState.Failed("Connection aborted after 5 attempts."),
                    onRetry = {}
                )
            }
        }
        composeRule.waitForIdle()
        save("setup-failed")
    }

    @Test
    fun render_launcher_home() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = AetherDesktopViewModel(app)
        composeRule.setContent {
            MyApplicationTheme {
                AndroidHomeLauncher(viewModel = vm, modifier = Modifier.fillMaxSize())
            }
        }
        composeRule.waitForIdle()
        save("launcher-home")
    }

    @Test
    fun render_terminal() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = AetherDesktopViewModel(app)
        composeRule.setContent {
            MyApplicationTheme {
                LinuxTerminalView(
                    prootEngine = vm.prootEngine,
                    realEngine = vm.realTerminalEngine,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        composeRule.waitForIdle()
        save("terminal")
    }
}
