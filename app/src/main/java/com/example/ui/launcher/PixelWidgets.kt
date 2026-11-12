package com.example.ui.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LauncherCustomization
import com.example.model.SearchEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Authentic Pixel "At a Glance" Widget
 */
@Composable
fun PixelAtAGlanceWidget(
    onClick: () -> Unit,
    onCustomizeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val now = Date()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("pixel_at_a_glance_widget")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date
            Text(
                text = dateFormat.format(now),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.5).sp
            )

            if (onCustomizeClick != null) {
                IconButton(
                    onClick = onCustomizeClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Launcher Settings",
                        tint = Color(0xCCFFFFFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Weather Pill & Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0x2EFFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Weather",
                    tint = Color(0xFFFBBC05), // Google Yellow
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "28°C",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "•   Proot Ubuntu & Android Ready",
                color = Color(0xEEF0F0F0),
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Clean, Authentic AOSP / Pixel Launcher Google Search Bar
 * Minimal, frosted glass pill aesthetic with crisp Google 4-color 'G' and Pixel Voice/Lens affordances.
 */
@Composable
fun RealPixelGoogleSearchBar(
    customization: LauncherCustomization,
    onVoiceClick: () -> Unit,
    onLensClick: () -> Unit,
    onSearchSubmit: (String) -> Unit,
    onOpenCustomize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var queryText by remember { mutableStateOf("") }
    val cornerRadius = customization.searchBarCornerRadius.dpValue.dp
    val barBgColor = Color(0xFF303134).copy(alpha = customization.searchBarAlpha)

    Surface(
        color = barBgColor,
        shape = RoundedCornerShape(cornerRadius),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(
                width = 1.dp,
                color = Color(0x1FFFFFFF),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .testTag("pixel_google_search_pill")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Google 4-Color 'G' Logo on left
            Box(
                modifier = Modifier.padding(end = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                when (customization.searchEngine) {
                    SearchEngine.GOOGLE -> AospGoogleGLogo(size = 22.dp, parentBgColor = barBgColor)
                    SearchEngine.BING -> Text("b", color = Color(0xFF008373), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    SearchEngine.DUCKDUCKGO -> Text("🦆", fontSize = 16.sp)
                }
            }

            // 2. Clean AOSP Search Text Input Field
            TextField(
                value = queryText,
                onValueChange = { queryText = it },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("search_text_input"),
                placeholder = {
                    Text(
                        text = "Search phone and web...",
                        color = Color(0x99FFFFFF),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF8AB4F8),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (queryText.isNotBlank()) {
                            executeWebSearch(context, queryText.trim(), customization.searchEngine)
                            onSearchSubmit(queryText.trim())
                            queryText = ""
                        }
                    }
                )
            )

            // 3. Clear (X) button when user is typing
            AnimatedVisibility(
                visible = queryText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = { queryText = "" },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color(0xAAFFFFFF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 4. Pixel Google Mic Icon (4-color styled mic)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (queryText.isNotBlank()) {
                            executeWebSearch(context, queryText.trim(), customization.searchEngine)
                            onSearchSubmit(queryText.trim())
                            queryText = ""
                        } else {
                            onVoiceClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (queryText.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF8AB4F8),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    PixelGoogleMicIcon(size = 19.dp)
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 5. Pixel Google Lens Camera Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onLensClick() },
                contentAlignment = Alignment.Center
            ) {
                PixelGoogleLensIcon(size = 19.dp)
            }
        }
    }
}

private fun executeWebSearch(context: Context, query: String, engine: SearchEngine) {
    try {
        val encoded = Uri.encode(query)
        val url = "${engine.searchUrlPrefix}$encoded"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Handled gracefully in emulator
    }
}

/**
 * Pristine 4-Color AOSP Google "G" Logo Canvas
 * Uses proper cutout math with the actual parent background color so there are no ugly solid boxes.
 */
@Composable
fun AospGoogleGLogo(size: Dp, parentBgColor: Color = Color(0xFF303134)) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val c = Offset(w / 2f, h / 2f)
        val r = w / 2f

        // Red top segment (from 215° to 330°)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 210f,
            sweepAngle = 120f,
            useCenter = true
        )
        // Blue right segment
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -30f,
            sweepAngle = 80f,
            useCenter = true
        )
        // Green bottom segment
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 50f,
            sweepAngle = 90f,
            useCenter = true
        )
        // Yellow bottom-left segment
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 140f,
            sweepAngle = 70f,
            useCenter = true
        )

        // Center cutout matching bar background
        drawCircle(
            color = parentBgColor,
            radius = r * 0.52f,
            center = c
        )

        // Right side blue horizontal bar into center
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(c.x - r * 0.05f, c.y - r * 0.18f),
            size = Size(r * 0.95f, r * 0.36f)
        )

        // Triangle cutout to form the G mouth gap
        val mouthGap = Path().apply {
            moveTo(c.x, c.y)
            lineTo(c.x + r * 1.1f, c.y - r * 0.5f)
            lineTo(c.x + r * 1.1f, c.y - r * 0.18f)
            close()
        }
        drawPath(mouthGap, parentBgColor)
    }
}

/**
 * Authentic Pixel Google 4-Color Voice Search Microphone Canvas
 */
@Composable
fun PixelGoogleMicIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val c = Offset(w / 2f, h * 0.42f)
        val micW = w * 0.32f
        val micH = h * 0.48f

        // Mic Top Capsule Body (Blue)
        drawRoundRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(c.x - micW / 2f, c.y - micH / 2f),
            size = Size(micW, micH * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(micW / 2f, micW / 2f)
        )
        // Mic Middle Body (Red)
        drawRect(
            color = Color(0xFFEA4335),
            topLeft = Offset(c.x - micW / 2f, c.y),
            size = Size(micW, micH * 0.25f)
        )
        // Mic Bottom Body (Yellow)
        drawRoundRect(
            color = Color(0xFFFBBC05),
            topLeft = Offset(c.x - micW / 2f, c.y + micH * 0.25f),
            size = Size(micW, micH * 0.25f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(0f, micW / 2f)
        )

        // Mic U-Shaped Stand (Green)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(c.x - micW * 0.85f, c.y - micH * 0.1f),
            size = Size(micW * 1.7f, micH * 0.8f),
            style = Stroke(width = 3.5f)
        )

        // Mic Stem Line (Green)
        drawLine(
            color = Color(0xFF34A853),
            start = Offset(c.x, c.y + micH * 0.7f),
            end = Offset(c.x, h * 0.9f),
            strokeWidth = 3.5f
        )
    }
}

/**
 * Authentic Pixel Google Lens Camera Icon Canvas
 */
@Composable
fun PixelGoogleLensIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val camW = w * 0.82f
        val camH = h * 0.72f
        val topLeft = Offset((w - camW) / 2f, (h - camH) / 2f)

        // Clean white camera frame
        drawRoundRect(
            color = Color(0xEEFFFFFF),
            topLeft = topLeft,
            size = Size(camW, camH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx(), 7.dp.toPx()),
            style = Stroke(width = 2.8f)
        )

        // Center lens circle (Blue accent)
        drawCircle(
            color = Color(0xFF4285F4),
            radius = w * 0.20f,
            center = Offset(w / 2f, h / 2f),
            style = Stroke(width = 2.8f)
        )

        // Top right color dot (Yellow dot)
        drawCircle(
            color = Color(0xFFFBBC05),
            radius = w * 0.07f,
            center = Offset(topLeft.x + camW * 0.76f, topLeft.y + camH * 0.25f)
        )
    }
}
