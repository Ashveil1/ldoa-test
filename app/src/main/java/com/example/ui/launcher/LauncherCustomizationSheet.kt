package com.example.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SearchBarCornerRadius
import com.example.model.SearchEngine
import com.example.ui.theme.AndroidTextPrimary
import com.example.ui.theme.AndroidTextSecondary
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.UbuntuOrange
import com.example.viewmodel.AetherDesktopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherCustomizationSheet(
    viewModel: AetherDesktopViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val customization by viewModel.customization.collectAsState()
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E2127),
        tonalElevation = 8.dp,
        modifier = modifier.testTag("launcher_customization_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(scrollState)
        ) {
            // Drag handle & Header
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color(0xFF5F6368), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Launcher Customization",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize home screen, search bar & grid layout",
                        color = AndroidTextSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Home Grid & Icons
            Text(
                text = "HOME SCREEN & GRID",
                color = GoogleBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Grid Columns selector (4, 5, 6)
            Text(
                text = "Columns on Home Screen: ${customization.gridColumns} cols",
                color = Color.White,
                fontSize = 14.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(4, 5, 6).forEach { cols ->
                    val isSelected = customization.gridColumns == cols
                    Surface(
                        color = if (isSelected) GoogleBlue else Color(0xFF2C3038),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clickable {
                                viewModel.updateCustomization { it.copy(gridColumns = cols) }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$cols x 6",
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Icon Size Slider
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "App Icon Size", color = Color.White, fontSize = 14.sp)
                Text(text = "${customization.iconSizeDp} dp", color = AndroidTextSecondary, fontSize = 13.sp)
            }
            Slider(
                value = customization.iconSizeDp.toFloat(),
                onValueChange = { sizeVal ->
                    viewModel.updateCustomization { it.copy(iconSizeDp = sizeVal.toInt()) }
                },
                valueRange = 44f..68f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = GoogleBlue,
                    activeTrackColor = GoogleBlue,
                    inactiveTrackColor = Color(0xFF3C4048)
                )
            )

            // Show Icon Labels Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Show App Labels", color = Color.White, fontSize = 14.sp)
                    Text(text = "Display app names below icons", color = AndroidTextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = customization.showIconLabels,
                    onCheckedChange = { checked ->
                        viewModel.updateCustomization { it.copy(showIconLabels = checked) }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GoogleBlue)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFF33363F))
            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Google Search Bar Customization
            Text(
                text = "SEARCH BAR CUSTOMIZATION",
                color = GoogleBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Search Engine Picker
            Text(text = "Default Search Engine", color = Color.White, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchEngine.values().forEach { engine ->
                    val isSelected = customization.searchEngine == engine
                    Surface(
                        color = if (isSelected) GoogleBlue else Color(0xFF2C3038),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable {
                                viewModel.updateCustomization { it.copy(searchEngine = engine) }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = engine.displayName,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Search Bar Corner Radius Style
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Search Bar Shape", color = Color.White, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchBarCornerRadius.values().forEach { radius ->
                    val isSelected = customization.searchBarCornerRadius == radius
                    Surface(
                        color = if (isSelected) GoogleBlue else Color(0xFF2C3038),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable {
                                viewModel.updateCustomization { it.copy(searchBarCornerRadius = radius) }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = radius.label.substringBefore(" "),
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Search Bar Transparency Slider
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Search Bar Opacity", color = Color.White, fontSize = 14.sp)
                Text(text = "${(customization.searchBarAlpha * 100).toInt()}%", color = AndroidTextSecondary, fontSize = 13.sp)
            }
            Slider(
                value = customization.searchBarAlpha,
                onValueChange = { alphaVal ->
                    viewModel.updateCustomization { it.copy(searchBarAlpha = alphaVal) }
                },
                valueRange = 0.3f..1.0f,
                colors = SliderDefaults.colors(thumbColor = GoogleBlue, activeTrackColor = GoogleBlue)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFF33363F))
            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Gesture & Behavior
            Text(
                text = "GESTURES & LAUNCH BEHAVIOR",
                color = GoogleBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Swipe Up to Open App Drawer Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Swipe Up for All Apps", color = Color.White, fontSize = 14.sp)
                    Text(text = "Swipe up anywhere on home screen to view full app drawer", color = AndroidTextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = customization.enableAppSwipeUp,
                    onCheckedChange = { checked ->
                        viewModel.updateCustomization { it.copy(enableAppSwipeUp = checked) }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GoogleBlue)
                )
            }

            // Open Apps as Floating Multi-Windows Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Floating Multi-Window", color = Color.White, fontSize = 14.sp)
                    Text(text = "Open apps in floating desktop windows over home screen", color = AndroidTextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = customization.openAppsInFloatingWindow,
                    onCheckedChange = { checked ->
                        viewModel.updateCustomization { it.copy(openAppsInFloatingWindow = checked) }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GoogleBlue)
                )
            }

            // Wallpaper Dim Level Slider
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Wallpaper Dim Level", color = Color.White, fontSize = 14.sp)
                Text(text = "${(customization.wallpaperDimLevel * 100).toInt()}%", color = AndroidTextSecondary, fontSize = 13.sp)
            }
            Slider(
                value = customization.wallpaperDimLevel,
                onValueChange = { dim ->
                    viewModel.updateCustomization { it.copy(wallpaperDimLevel = dim) }
                },
                valueRange = 0.0f..0.7f,
                colors = SliderDefaults.colors(thumbColor = GoogleBlue, activeTrackColor = GoogleBlue)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
