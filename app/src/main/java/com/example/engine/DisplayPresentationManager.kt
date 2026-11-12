package com.example.engine

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import com.example.model.ExternalMonitorStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DisplayPresentationManager(private val context: Context) {

    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private val _monitorStatus = MutableStateFlow(
        ExternalMonitorStatus(
            isConnected = false,
            displayName = "No External Display",
            resolution = "1920x1080 @ 60Hz",
            isMirrored = false
        )
    )
    val monitorStatus: StateFlow<ExternalMonitorStatus> = _monitorStatus.asStateFlow()

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            checkDisplays()
        }

        override fun onDisplayRemoved(displayId: Int) {
            checkDisplays()
        }

        override fun onDisplayChanged(displayId: Int) {
            checkDisplays()
        }
    }

    init {
        displayManager.registerDisplayListener(displayListener, null)
        checkDisplays()
    }

    fun checkDisplays() {
        val displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        if (displays.isNotEmpty()) {
            val ext = displays[0]
            val mode = ext.mode
            _monitorStatus.value = ExternalMonitorStatus(
                isConnected = true,
                displayName = ext.name ?: "External Monitor (HDMI/USB-C)",
                resolution = "${mode.physicalWidth}x${mode.physicalHeight} @ ${mode.refreshRate.toInt()}Hz",
                isMirrored = false
            )
        } else {
            // Keep current value if manually simulated for demonstration/testing
            if (!_monitorStatus.value.isConnected) {
                _monitorStatus.value = ExternalMonitorStatus(
                    isConnected = false,
                    displayName = "No External Display",
                    resolution = "Disconnected",
                    isMirrored = false
                )
            }
        }
    }

    /**
     * Allows user to simulate/toggle external display dock mode for testing without a physical HDMI cable
     */
    fun toggleSimulatedExternalDisplay() {
        val current = _monitorStatus.value
        if (current.isConnected) {
            _monitorStatus.value = ExternalMonitorStatus(
                isConnected = false,
                displayName = "No External Display",
                resolution = "Disconnected",
                isMirrored = false
            )
        } else {
            _monitorStatus.value = ExternalMonitorStatus(
                isConnected = true,
                displayName = "Dell UltraSharp 4K (USB-C Dock)",
                resolution = "3840x2160 @ 60Hz",
                isMirrored = false
            )
        }
    }

    fun setMirrored(mirrored: Boolean) {
        _monitorStatus.value = _monitorStatus.value.copy(isMirrored = mirrored)
    }

    fun cleanup() {
        displayManager.unregisterDisplayListener(displayListener)
    }
}
