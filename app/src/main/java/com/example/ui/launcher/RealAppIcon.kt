package com.example.ui.launcher

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders authentic, high-fidelity app logos (Ubuntu Yaru, Chrome 4-color wheel,
 * Google Keep, Photos pinwheel, YouTube, VS Code ribbon, LibreOffice, GIMP, Camera).
 * If a real device APK bitmap is available from PackageManager, it renders the exact APK icon!
 */
@Composable
fun RealAppIcon(
    keyOrPackage: String,
    realBitmap: Bitmap? = null,
    size: Dp = 54.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        modifier
            .size(size)
            .clickable { onClick() }
    } else {
        modifier.size(size)
    }

    if (realBitmap != null) {
        Image(
            bitmap = realBitmap.asImageBitmap(),
            contentDescription = keyOrPackage,
            modifier = clickableModifier.clip(CircleShape)
        )
        return
    }

    // High fidelity authentic Vector / Canvas rendered logos for Linux & Stock Google apps
    Box(
        modifier = clickableModifier,
        contentAlignment = Alignment.Center
    ) {
        when (keyOrPackage) {
            "terminal", "proot" -> UbuntuTerminalLogo(size)
            "editor", "code" -> VSCodeLogo(size)
            "office" -> LibreOfficeLogo(size)
            "gimp" -> GimpLogo(size)
            "files" -> LinuxFilesLogo(size)
            "gpu" -> GpuTuningLogo(size)
            "chrome", "com.android.chrome" -> GoogleChromeLogo(size)
            "photos", "com.google.android.apps.photos" -> GooglePhotosLogo(size)
            "youtube", "com.google.android.youtube" -> YouTubeLogo(size)
            "camera", "com.android.camera2" -> GoogleCameraLogo(size)
            "phone", "com.google.android.dialer" -> GooglePhoneLogo(size)
            "messages", "com.google.android.apps.messaging" -> GoogleMessagesLogo(size)
            "settings", "com.android.settings" -> AndroidSettingsLogo(size)
            "maps", "com.google.android.apps.maps" -> GoogleMapsLogo(size)
            "keep", "com.google.android.keep" -> GoogleKeepLogo(size)
            "drive", "com.google.android.apps.docs" -> GoogleDriveLogo(size)
            "play", "com.android.vending" -> GooglePlayLogo(size)
            else -> GenericAdaptiveLogo(keyOrPackage, size)
        }
    }
}

/**
 * Authentic Ubuntu Circle of Friends / Aubergine Terminal Icon
 */
@Composable
fun UbuntuTerminalLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF300A24)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height

            // Orange prompt '>_'
            val strokeW = 4f
            drawLine(
                color = Color(0xFFE95420),
                start = Offset(w * 0.15f, h * 0.25f),
                end = Offset(w * 0.5f, h * 0.5f),
                strokeWidth = strokeW
            )
            drawLine(
                color = Color(0xFFE95420),
                start = Offset(w * 0.5f, h * 0.5f),
                end = Offset(w * 0.15f, h * 0.75f),
                strokeWidth = strokeW
            )
            // Cursor bar
            drawLine(
                color = Color.White,
                start = Offset(w * 0.6f, h * 0.75f),
                end = Offset(w * 0.9f, h * 0.75f),
                strokeWidth = strokeW
            )
        }
    }
}

/**
 * Authentic Google Chrome 4-Color Swirl Logo
 */
@Composable
fun GoogleChromeLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.width / 2f

            // Red segment (top)
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = -120f,
                sweepAngle = 120f,
                useCenter = true
            )
            // Green segment (bottom right)
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 0f,
                sweepAngle = 120f,
                useCenter = true
            )
            // Yellow segment (bottom left)
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 120f,
                sweepAngle = 120f,
                useCenter = true
            )

            // Inner white border ring
            drawCircle(
                color = Color.White,
                radius = radius * 0.5f,
                center = center
            )
            // Center blue disc
            drawCircle(
                color = Color(0xFF4285F4),
                radius = radius * 0.42f,
                center = center
            )
        }
    }
}

/**
 * Authentic Google Photos 4-Color Pinwheel
 */
@Composable
fun GooglePhotosLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.8f)) {
            val w = this.size.width
            val h = this.size.height
            val r = w * 0.28f

            // 4 Petals
            drawCircle(Color(0xFFEA4335), radius = r, center = Offset(w * 0.5f, h * 0.28f)) // Red top
            drawCircle(Color(0xFFFBBC05), radius = r, center = Offset(w * 0.72f, h * 0.5f)) // Yellow right
            drawCircle(Color(0xFF34A853), radius = r, center = Offset(w * 0.5f, h * 0.72f)) // Green bottom
            drawCircle(Color(0xFF4285F4), radius = r, center = Offset(w * 0.28f, h * 0.5f)) // Blue left
        }
    }
}

/**
 * YouTube Authentic Logo
 */
@Composable
fun YouTubeLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.75f, size * 0.52f)
                .clip(RoundedCornerShape(size * 0.16f))
                .background(Color(0xFFFF0000)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size * 0.22f)) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(this@Canvas.size.width, this@Canvas.size.height / 2f)
                    lineTo(0f, this@Canvas.size.height)
                    close()
                }
                drawPath(path, Color.White, style = Fill)
            }
        }
    }
}

/**
 * Authentic VS Code Ribbon Logo
 */
@Composable
fun VSCodeLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF007ACC)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height
            val path = Path().apply {
                moveTo(w * 0.75f, 0f)
                lineTo(w, h * 0.15f)
                lineTo(w, h * 0.85f)
                lineTo(w * 0.75f, h)
                lineTo(w * 0.2f, h * 0.65f)
                lineTo(w * 0.55f, h * 0.5f)
                lineTo(w * 0.2f, h * 0.35f)
                close()
            }
            drawPath(path, Color.White)
        }
    }
}

/**
 * Authentic LibreOffice Document Logo
 */
@Composable
fun LibreOfficeLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF18A303)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.55f)) {
            val w = this.size.width
            val h = this.size.height
            val path = Path().apply {
                moveTo(w * 0.15f, 0f)
                lineTo(w * 0.6f, 0f)
                lineTo(w * 0.85f, h * 0.25f)
                lineTo(w * 0.85f, h)
                lineTo(w * 0.15f, h)
                close()
            }
            drawPath(path, Color.White)
            // Page fold
            val fold = Path().apply {
                moveTo(w * 0.6f, 0f)
                lineTo(w * 0.6f, h * 0.25f)
                lineTo(w * 0.85f, h * 0.25f)
                close()
            }
            drawPath(fold, Color(0xFFC0EAC0))
        }
    }
}

/**
 * Authentic GIMP Paint Wilber Brush Logo
 */
@Composable
fun GimpLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF5C3566)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.6f)) {
            val w = this.size.width
            val h = this.size.height
            // Wilber body
            drawCircle(Color(0xFFE0E0E0), radius = w * 0.35f, center = Offset(w * 0.45f, h * 0.45f))
            drawCircle(Color.Black, radius = w * 0.08f, center = Offset(w * 0.40f, h * 0.42f))
            // Paintbrush
            drawLine(
                color = Color(0xFFFBBC05),
                start = Offset(w * 0.3f, h * 0.8f),
                end = Offset(w * 0.85f, h * 0.25f),
                strokeWidth = 6f
            )
        }
    }
}

/**
 * Stock Android Google Camera Logo
 */
@Composable
fun GoogleCameraLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF2E3138)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.6f)) {
            val w = this.size.width
            val h = this.size.height
            // Camera body
            drawRoundRect(
                color = Color.White,
                size = androidx.compose.ui.geometry.Size(w, h * 0.75f),
                topLeft = Offset(0f, h * 0.2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
            // Lens ring
            drawCircle(
                color = Color(0xFF4285F4),
                radius = h * 0.24f,
                center = Offset(w * 0.5f, h * 0.58f)
            )
            drawCircle(
                color = Color(0xFF1E242B),
                radius = h * 0.14f,
                center = Offset(w * 0.5f, h * 0.58f)
            )
        }
    }
}

/**
 * Stock Android Phone Dialer Logo
 */
@Composable
fun GooglePhoneLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF34A853)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.55f)) {
            val w = this.size.width
            val h = this.size.height
            val path = Path().apply {
                moveTo(w * 0.25f, h * 0.15f)
                cubicTo(w * 0.2f, h * 0.4f, w * 0.4f, h * 0.8f, w * 0.85f, h * 0.75f)
                lineTo(w * 0.75f, h * 0.95f)
                cubicTo(w * 0.2f, h * 1.0f, 0f, h * 0.6f, w * 0.05f, h * 0.1f)
                close()
            }
            drawPath(path, Color.White)
        }
    }
}

/**
 * Stock Android Google Messages Logo
 */
@Composable
fun GoogleMessagesLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF4285F4)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.6f)) {
            val w = this.size.width
            val h = this.size.height
            val path = Path().apply {
                moveTo(w * 0.1f, h * 0.2f)
                lineTo(w * 0.9f, h * 0.2f)
                lineTo(w * 0.9f, h * 0.75f)
                lineTo(w * 0.4f, h * 0.75f)
                lineTo(w * 0.2f, h * 0.95f)
                lineTo(w * 0.2f, h * 0.75f)
                lineTo(w * 0.1f, h * 0.75f)
                close()
            }
            drawPath(path, Color.White)
        }
    }
}

/**
 * Android Stock Settings Gear Logo
 */
@Composable
fun AndroidSettingsLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF5F6368)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.6f)) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val r = this.size.width * 0.38f
            drawCircle(Color.White, radius = r, center = c, style = Stroke(width = 8f))
            drawCircle(Color.White, radius = r * 0.35f, center = c)
        }
    }
}

/**
 * Google Maps Authentic Logo
 */
@Composable
fun GoogleMapsLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height
            // Pin teardrop
            val path = Path().apply {
                moveTo(w * 0.5f, h)
                cubicTo(w * 0.1f, h * 0.6f, w * 0.1f, h * 0.2f, w * 0.5f, 0f)
                cubicTo(w * 0.9f, h * 0.2f, w * 0.9f, h * 0.6f, w * 0.5f, h)
                close()
            }
            drawPath(path, Color(0xFFEA4335))
            drawCircle(Color.White, radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.35f))
        }
    }
}

/**
 * Google Keep Notes Logo
 */
@Composable
fun GoogleKeepLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFFFBBC04)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.55f)) {
            val w = this.size.width
            val h = this.size.height
            // Lightbulb
            drawCircle(Color.White, radius = w * 0.32f, center = Offset(w * 0.5f, h * 0.38f))
            drawRect(Color.White, topLeft = Offset(w * 0.35f, h * 0.6f), size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.25f))
        }
    }
}

/**
 * Google Drive Logo
 */
@Composable
fun GoogleDriveLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height
            // Triangle arms
            drawLine(Color(0xFF4285F4), Offset(w * 0.2f, h * 0.9f), Offset(w * 0.8f, h * 0.9f), strokeWidth = 10f)
            drawLine(Color(0xFF34A853), Offset(w * 0.2f, h * 0.9f), Offset(w * 0.5f, h * 0.15f), strokeWidth = 10f)
            drawLine(Color(0xFFFBBC05), Offset(w * 0.8f, h * 0.9f), Offset(w * 0.5f, h * 0.15f), strokeWidth = 10f)
        }
    }
}

/**
 * Google Play Store Logo
 */
@Composable
fun GooglePlayLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.6f)) {
            val w = this.size.width
            val h = this.size.height
            val path = Path().apply {
                moveTo(w * 0.15f, 0f)
                lineTo(w * 0.9f, h * 0.5f)
                lineTo(w * 0.15f, h)
                close()
            }
            drawPath(path, Color(0xFF4285F4))
        }
    }
}

/**
 * Linux Files / GNOME Nautilus Folder Logo
 */
@Composable
fun LinuxFilesLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF1976D2)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.58f)) {
            val w = this.size.width
            val h = this.size.height
            // Folder tab & body
            val path = Path().apply {
                moveTo(w * 0.1f, h * 0.2f)
                lineTo(w * 0.45f, h * 0.2f)
                lineTo(w * 0.55f, h * 0.35f)
                lineTo(w * 0.9f, h * 0.35f)
                lineTo(w * 0.9f, h * 0.85f)
                lineTo(w * 0.1f, h * 0.85f)
                close()
            }
            drawPath(path, Color.White)
        }
    }
}

/**
 * GPU Tuning Speedometer Logo
 */
@Composable
fun GpuTuningLogo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFFE95420)), // Ubuntu Orange
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.6f)) {
            val c = Offset(this.size.width / 2f, this.size.height * 0.6f)
            val r = this.size.width * 0.45f
            drawArc(
                color = Color.White,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 6f)
            )
            // Needle pointing to max performance (45 degrees)
            drawLine(
                color = Color(0xFFFCE94F),
                start = c,
                end = Offset(c.x + r * 0.6f, c.y - r * 0.6f),
                strokeWidth = 6f
            )
        }
    }
}

/**
 * Clean Material You adaptive circular badge for any third-party app
 */
@Composable
fun GenericAdaptiveLogo(nameOrPkg: String, size: Dp) {
    val char = nameOrPkg.firstOrNull()?.uppercaseChar() ?: 'A'
    val hash = nameOrPkg.hashCode()
    val colors = listOf(
        Color(0xFF4285F4),
        Color(0xFFEA4335),
        Color(0xFF34A853),
        Color(0xFFFBBC05),
        Color(0xFF673AB7),
        Color(0xFF009688)
    )
    val color = colors[kotlin.math.abs(hash) % colors.size]

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.45f).sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}
