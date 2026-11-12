package com.example.ui.desktop

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppType
import com.example.ui.theme.UbuntuAubergineDark
import com.example.ui.theme.UbuntuOrange
import com.example.ui.theme.YaruTextMuted
import com.example.ui.theme.YaruTextWhite
import com.example.viewmodel.AetherDesktopViewModel

@Composable
fun UbuntuAppOverviewModal(
    viewModel: AetherDesktopViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val linuxApps by viewModel.linuxEngine.installedApps.collectAsState()
    val androidApps by viewModel.appsManager.androidApps.collectAsState()
    var searchKeyword by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE61A0C18)) // GNOME Overview translucent aubergine backdrop
            .clickable { onDismiss() }
            .testTag("ubuntu_app_overview_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .clickable(enabled = false) {}, // Prevent clicks inside from closing
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Search Bar at Top (GNOME Shell Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(44.dp)
                    .background(Color(0xFF282828), RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = YaruTextMuted, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchKeyword,
                    onValueChange = { searchKeyword = it },
                    placeholder = { Text("Type to search...", color = YaruTextMuted, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = YaruTextWhite,
                        unfocusedTextColor = YaruTextWhite,
                        cursorColor = UbuntuOrange,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                if (searchKeyword.isNotBlank()) {
                    IconButton(onClick = { searchKeyword = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = YaruTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Grid (Linux & Android Apps)
            val filteredLinux = linuxApps.filter {
                searchKeyword.isBlank() || it.name.contains(searchKeyword, ignoreCase = true)
            }
            val filteredAndroid = androidApps.filter {
                searchKeyword.isBlank() || it.appName.contains(searchKeyword, ignoreCase = true)
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.85f)
            ) {
                items(filteredLinux) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                viewModel.openLinuxApp(app)
                                onDismiss()
                            }
                            .padding(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFF2E2E2E),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = resolveAppIcon(app.iconName, AppType.LINUX_APP),
                                    contentDescription = app.name,
                                    tint = UbuntuOrange,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = app.name,
                            color = YaruTextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 14.sp
                        )
                    }
                }

                items(filteredAndroid) { androidApp ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                viewModel.openAndroidAppInWindow(androidApp.packageName, androidApp.appName)
                                onDismiss()
                            }
                            .padding(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFF1E3A24),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = androidApp.appName,
                                    tint = Color(0xFF34A853),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = androidApp.appName,
                            color = YaruTextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
