package com.example.ui.apps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TerminalAmber
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TextFrostWhite
import com.example.ui.theme.TextMuted

data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@Composable
fun GimpCanvasView(
    modifier: Modifier = Modifier
) {
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var currentColor by remember { mutableStateOf(NeonCyan) }
    var currentStrokeWidth by remember { mutableStateOf(6f) }
    val currentPoints = remember { mutableStateListOf<Offset>() }

    val palette = listOf(
        NeonCyan,
        ElectricViolet,
        TerminalGreen,
        TerminalAmber,
        TerminalRed,
        TextFrostWhite,
        Color(0xFFFF4081)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("gimp_canvas_view")
    ) {
        // GIMP Tool Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = ElectricViolet.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "GIMP 2.10",
                    color = ElectricViolet,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(Icons.Default.Brush, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Touch Brush", color = TextFrostWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.width(12.dp))

            // Color Palette Chips
            palette.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color, CircleShape)
                        .border(
                            if (currentColor == color) 2.dp else 1.dp,
                            if (currentColor == color) TextFrostWhite else BorderSubtle,
                            CircleShape
                        )
                        .clickable { currentColor = color }
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { strokes.clear() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Clear Canvas",
                    tint = TerminalRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Drawing Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color(0xFF0F141C), RoundedCornerShape(8.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints.clear()
                            currentPoints.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPoints.add(change.position)
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                strokes.add(
                                    DrawStroke(
                                        points = currentPoints.toList(),
                                        color = currentColor,
                                        strokeWidth = currentStrokeWidth
                                    )
                                )
                                currentPoints.clear()
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Render committed strokes
                for (stroke in strokes) {
                    for (i in 0 until stroke.points.size - 1) {
                        drawLine(
                            color = stroke.color,
                            start = stroke.points[i],
                            end = stroke.points[i + 1],
                            strokeWidth = stroke.strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Render active in-progress stroke
                for (i in 0 until currentPoints.size - 1) {
                    drawLine(
                        color = currentColor,
                        start = currentPoints[i],
                        end = currentPoints[i + 1],
                        strokeWidth = currentStrokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            if (strokes.isEmpty() && currentPoints.isEmpty()) {
                Text(
                    text = "Touch anywhere to draw with finger or wireless stylus",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }

        // Canvas Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Canvas: 1920x1080 RGBA | Direct Multi-touch 120Hz",
                color = TextMuted,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Layers: 1 (Hardware GLES Accelerated)",
                color = NeonCyan,
                fontSize = 10.sp
            )
        }
    }
}
