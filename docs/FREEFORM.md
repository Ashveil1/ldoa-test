# Floating (freeform) Android apps — how it works here

Verified working on a POCO M7 4G running HyperOS OS3.0 (Android 16), build
`AetherLinux-debug-v21.apk` (commit `bed9856`, 2026-11-12): apps launched
from the desktop grid open as real floating windows.

## The launch path

`InstalledAppsManager.launchAppFreeform` mirrors the launch mechanics of
[Taskbar](https://github.com/farmerbb/taskbar) by Braden Farmer (Apache 2.0),
the app we diffed against because its floating launches worked on the target
device while ours opened fullscreen:

- Explicit `ComponentName` + `ACTION_MAIN` / `CATEGORY_LAUNCHER`
- Flags `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_SINGLE_TOP` (no
  `MULTIPLE_TASK`)
- `ActivityOptions.setLaunchBounds(...)` for the window rectangle
- Hidden `ActivityOptions.setLaunchWindowingMode(5)` (freeform), called by
  plain reflection after
  [LSPosed HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass)
  (Apache 2.0) lifts hidden-API enforcement
  (`addHiddenApiExemptions("")` in `InstalledAppsManager.init`).
  Meta-reflection is kept as a fallback.

## Device prerequisites

- `Settings.Global.enable_freeform_support = 1` and
  `force_resizable_activities = 1`. The app writes both itself when it holds
  `WRITE_SECURE_SETTINGS` (one-time `pm grant`, same grant Taskbar asks for);
  the in-app panel explains the grant when missing.
- HyperOS reads freeform settings at boot: if apps keep opening fullscreen
  after enabling, restart the phone once (Taskbar documents the same step).
- Process role: `AetherLaunchService` keeps a foreground service running
  while the desktop is open, and the panel offers the "Display over other
  apps" grant — Taskbar runs with both, and some ROMs only honor freeform
  requests from a process with that profile.

## Diagnostics

The panel's result line reports whether the windowingMode=5 hint was
actually attached ("hint applied" vs "ROM blocked the windowing hint"),
which distinguishes a ROM overriding the request from the reflection being
blocked. `adb logcat -s InstalledApps` carries the same signal.
