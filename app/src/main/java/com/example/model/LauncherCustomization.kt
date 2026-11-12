package com.example.model

enum class SearchEngine(val displayName: String, val searchUrlPrefix: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=")
}

enum class SearchBarCornerRadius(val label: String, val dpValue: Int) {
    PILL("Pill (28dp)", 28),
    ROUNDED("Rounded (16dp)", 16),
    RECTANGLE("Slight Curve (8dp)", 8)
}

data class LauncherCustomization(
    val gridColumns: Int = 4,
    val iconSizeDp: Int = 54,
    val showIconLabels: Boolean = true,
    val showAtAGlanceWidget: Boolean = true,
    val showSearchBar: Boolean = true,
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val searchBarCornerRadius: SearchBarCornerRadius = SearchBarCornerRadius.PILL,
    val searchBarAlpha: Float = 0.85f,
    val wallpaperDimLevel: Float = 0.20f, // 0.0 to 0.7
    val enableAppSwipeUp: Boolean = true,
    val openAppsInFloatingWindow: Boolean = true,
    val defaultLinuxShell: String = "Ubuntu 24.04 PRoot"
)
