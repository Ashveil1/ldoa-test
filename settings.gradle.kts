pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "AetherLinux"

include(":app")

// Vendored termux-x11 (GPLv3), pinned commit 29d4460 (same pin as LDOA main).
// :lorie is an Android library module; the app launches its MainActivity for
// the X desktop.
include(":lorie")
project(":lorie").projectDir = file("third_party/termux-x11/lorie")
include(":shell-loader:stub")
project(":shell-loader:stub").projectDir = file("third_party/termux-x11/shell-loader/stub")

// Vendored patch (from LDOA): CmdEntryPoint.initEntryPoint loads libXlorie
// via a classloader resource URL, which only works with extractNativeLibs=false.
// We keep extractNativeLibs=true (W^X blocks filesDir exec at targetSdk 34+),
// so add an XSERVER_LIB env preload.
run {
    val f = File(rootDir, "third_party/termux-x11/lorie/src/main/java/com/termux/x11/CmdEntryPoint.java")
    if (f.exists()) {
        val orig = "        ctx = createContext();\n"
        val patched = "        ctx = createContext();\n\n        String xserverLib = System.getenv(\"XSERVER_LIB\");\n        if (xserverLib != null && !xserverLib.isEmpty()) { System.load(xserverLib); return; }\n"
        val text = f.readText()
        if (orig in text && "XSERVER_LIB" !in text) f.writeText(text.replace(orig, patched, ignoreCase = false))
        check("XSERVER_LIB" in f.readText()) { "CmdEntryPoint XSERVER_LIB preload patch failed to apply" }
    }
}
// Vendored patch (from LDOA): vendored modules compile at SDK 34, but their
// current androidx deps require compileSdk 35. Bump compileSdk only.
for (rel in listOf("third_party/termux-x11/lorie/build.gradle", "third_party/termux-x11/shell-loader/stub/build.gradle")) {
    val f = File(rootDir, rel)
    if (f.exists()) {
        val text = f.readText()
        if ("compileSdkVersion 34" in text) f.writeText(text.replace("compileSdkVersion 34", "compileSdkVersion 35"))
        check("compileSdkVersion 35" in f.readText()) { "compileSdk 35 bump failed for $rel" }
    }
}
