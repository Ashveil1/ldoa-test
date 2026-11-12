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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppType
import com.example.ui.desktop.resolveAppIcon
import com.example.ui.theme.AndroidSearchBarBg
import com.example.ui.theme.AndroidTextPrimary
import com.example.ui.theme.AndroidTextSecondary
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.UbuntuOrange
import com.example.viewmodel.AetherDesktopViewModel

enum class DrawerFilter {
    ALL,
    LINUX,
    ANDROID
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerSheet(
    viewModel: AetherDesktopViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val linuxApps by viewModel.linuxEngine.installedApps.collectAsState()
    val androidApps by viewModel.appsManager.androidApps.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var currentFilter by remember { mutableStateOf(DrawerFilter.ALL) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E2127), // Pixel Dark Sheet color
        tonalElevation = 6.dp,
        modifier = modifier.testTag("app_drawer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Drag handle & title
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color(0xFF5F6368), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar in Drawer
            Surface(
                color = AndroidSearchBarBg,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AndroidTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search apps or commands...", color = AndroidTextSecondary, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = AndroidTextPrimary,
                            unfocusedTextColor = AndroidTextPrimary,
                            cursorColor = GoogleBlue,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = AndroidTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DrawerFilter.values().forEach { filter ->
                    val isSelected = currentFilter == filter
                    val (label, activeColor) = when (filter) {
                        DrawerFilter.ALL -> Pair("All (${linuxApps.size + androidApps.size})", GoogleBlue)
                        DrawerFilter.LINUX -> Pair("Ubuntu Linux (${linuxApps.size})", UbuntuOrange)
                        DrawerFilter.ANDROID -> Pair("Android (${androidApps.size})", GoogleGreen)
                    }

                    Surface(
                        color = if (isSelected) activeColor.copy(alpha = 0.2f) else Color(0xFF2C3038),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { currentFilter = filter }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) activeColor else AndroidTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Apps Grid with Real Logos
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Linux Ubuntu Apps
                if (currentFilter == DrawerFilter.ALL || currentFilter == DrawerFilter.LINUX) {
                    val filteredLinux = linuxApps.filter {
                        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                    }
                    items(filteredLinux) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.openLinuxApp(app)
                                    onDismiss()
                                }
                                .padding(2.dp)
                        ) {
                            RealAppIcon(
                                keyOrPackage = app.id,
                                size = 52.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = app.name,
                                color = AndroidTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Android Apps with real APK icon loaded from PackageManager
                if (currentFilter == DrawerFilter.ALL || currentFilter == DrawerFilter.ANDROID) {
                    val filteredAndroid = androidApps.filter {
                        searchQuery.isBlank() || it.appName.contains(searchQuery, ignoreCase = true)
                    }
                    items(filteredAndroid) { androidApp ->
                        val realIcon = viewModel.appsManager.getAppIcon(androidApp.packageName)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.openAndroidAppInWindow(androidApp.packageName, androidApp.appName)
                                    onDismiss()
                                }
                                .padding(2.dp)
                        ) {
                            RealAppIcon(
                                keyOrPackage = androidApp.packageName,
                                realBitmap = realIcon,
                                size = 52.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = androidApp.appName,
                                color = AndroidTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
