# feat/vdisplay - real Android apps inside in-app panels (VirtualDisplay)

Separate track. Floating path (v21, docs/FREEFORM.md) stays untouched and is the fallback.

## Branch / build
- branch `feat/vdisplay`; product flavor `vd`: applicationId suffix `.vd` (installs beside v21), versionNameSuffix `-vd`.
- APK `AetherLinux-vd-<n>.apk`, GitHub release tag `vd-v<n>`. Never touch `ci-v*` tags.
- Same fixed debug key. New code lives under `app/src/vd/` + `com.example.vd` package; only hook into main code through one entry (a "Real Android app in panel" action), so merges stay small.

## Architecture
1. Privilege: Shizuku (dev.rikka.shizuku:api/provider 13.x). App binds a Shizuku UserService (`VdService`, runs as shell uid 2000).
2. UserService creates the display. Shell can create TRUSTED, OWN_CONTENT_ONLY virtual displays through hidden API DisplayManagerGlobal.createVirtualDisplay (use HiddenApiBypass, already in the app). Flags: PUBLIC | OWN_CONTENT_ONLY | SUPPORTS_TOUCH | DESTROY_CONTENT_ON_REMOVAL (+ TRUSTED where API allows). Fallback if hidden call fails: `cmd display`-less path, plain DisplayManager.createVirtualDisplay from the UserService context.
3. Surface: the panel (Compose AndroidView TextureView or SurfaceView) owns the Surface; pass it to the service over AIDL (Surface is Parcelable). Panel size change -> `VirtualDisplay.resize(w,h,dpi)` and Surface buffer size update.
4. Launch: UserService runs `am start --display <id> --windowingMode 1 -n <component>` (or startActivity with ActivityOptions.setLaunchDisplayId via IActivityTaskManager). Pick component from PackageManager launcher intent.
5. Input: panel touch events -> translate to display coords -> inject MotionEvent via UserService (InputManager.injectInputEvent with event.displayId = id, INJECT_INPUT_EVENT_MODE_ASYNC). Back/Home: KeyEvent injected on that display.
6. Multi-app: one VirtualDisplay per panel (display = panel). Panels are the existing window frames of the launcher.
7. Lifecycle: on panel close -> `am force-stop`-free: just release display (DESTROY_CONTENT_ON_REMOVAL). On Shizuku death or screen lock: panel shows "Shizuku stopped - tap to retry" and offers floating launch (v21 path).
8. UI states: Shizuku not installed / not running / permission not granted / ready. Plain-Thai strings in panel.

## Milestones (each ships as vd-vN, emulator first)
M1 Emulator proof: Shizuku not available on emulator, so provide a dev path: `adb shell` starts VdService via `app_process` (Shizuku's `rish`-less starter) - exact same shell uid. Prove: display created, Settings app renders in a panel, screenshot of panel, touch tap works. CI emulator test asserts non-black frame.
M2 Two panels, two apps, independent touch, resize.
M3 Shizuku integration (real device path), state screens.
M4 Measure fps (SurfaceView frame counter) and report honestly.
Device test only after Ash's explicit "ได้".

## Risks to report up-front
- Extra copy -> lower fps than floating; heat/battery.
- HyperOS kills wireless adb on lock -> Shizuku dies -> all panels go blank.
- Re-pair after each reboot.
- Secure-flag apps (banking) render black; some apps refuse non-default displays.
