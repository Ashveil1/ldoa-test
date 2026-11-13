package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CrossOsFileBridge
import com.example.engine.LinuxSubsystemEngine
import com.example.model.FileCategory
import com.example.model.GpuBackend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    @Test
    fun testGpuBackendSwitching() {
        val engine = LinuxSubsystemEngine()
        engine.setGpuBackend(GpuBackend.MESA_ZINK)
        assertEquals(GpuBackend.MESA_ZINK, engine.gpuBackend.value)

        engine.setFpsTarget(120)
        assertEquals(120, engine.fpsTarget.value)
    }

    @Test
    fun testCrossOsFileBridgeRealIo() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        // A minimal fake "rootfs" in app-private storage: the bridge must
        // list REAL files from it, not fabricated entries.
        val rootfs = File(app.filesDir, "test-rootfs").apply { deleteRecursively(); mkdirs() }
        val home = File(rootfs, "home/ubuntu").apply { mkdirs() }
        File(home, "real_note.txt").writeText("hello")

        val bridge = CrossOsFileBridge(app) { rootfs }
        bridge.refresh()

        // Real listing: the file we created on disk shows up with real size.
        val listed = bridge.files.value.firstOrNull { it.name == "real_note.txt" }
        assertTrue("bridge must list real rootfs files", listed != null)
        assertEquals(5L, listed!!.sizeBytes)
        assertTrue(listed.isLinuxSpace)
        assertEquals("/home/ubuntu/real_note.txt", listed.path)

        // createNewFile performs real I/O.
        bridge.createNewFile("test_script.py", FileCategory.CODE, true)
        assertTrue(File(home, "test_script.py").isFile)
        assertTrue(bridge.files.value.any { it.name == "test_script.py" })

        // copyAcrossBridge Linux->Android moves real bytes.
        bridge.copyAcrossBridge(listed)
        val exported = File(app.getExternalFilesDir(null), "linux_export/real_note.txt")
        assertTrue("export must produce a real file", exported.isFile)
        assertEquals("hello", exported.readText())
    }
}
