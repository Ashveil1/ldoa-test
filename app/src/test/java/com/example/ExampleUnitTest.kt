package com.example

import com.example.engine.CrossOsFileBridge
import com.example.engine.LinuxSubsystemEngine
import com.example.model.FileCategory
import com.example.model.GpuBackend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testLinuxEngineCommandExecution() {
        val engine = LinuxSubsystemEngine()
        val unameResult = engine.executeCommand("uname -a")
        assertTrue(unameResult.contains("Linux"))

        val freeResult = engine.executeCommand("free -h")
        assertTrue(freeResult.contains("Mem:"))

        val glResult = engine.executeCommand("virgl_test_server")
        assertTrue(glResult.contains("VirGL"))
    }

    @Test
    fun testGpuBackendSwitching() {
        val engine = LinuxSubsystemEngine()
        engine.setGpuBackend(GpuBackend.MESA_ZINK)
        assertEquals(GpuBackend.MESA_ZINK, engine.gpuBackend.value)

        engine.setFpsTarget(120)
        assertEquals(120, engine.fpsTarget.value)
    }

    @Test
    fun testCrossOsFileBridge() {
        val bridge = CrossOsFileBridge()
        val initialCount = bridge.files.value.size
        assertTrue(initialCount > 0)

        // Test creating a new code file
        bridge.createNewFile("test_script.py", FileCategory.CODE, true)
        assertEquals(initialCount + 1, bridge.files.value.size)

        val created = bridge.files.value.first()
        assertEquals("test_script.py", created.name)
        assertTrue(created.isLinuxSpace)

        // Test symlink toggling
        bridge.toggleSymlink(created.id)
        val updated = bridge.files.value.first()
        assertTrue(updated.isSymlinked)
    }
}
