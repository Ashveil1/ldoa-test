vd flavor only (build.gradle.kts):
  buildFeatures { aidl = true }   // put IVdService.aidl in app/src/vd/aidl/com/example/vd/
  "vdImplementation"("dev.rikka.shizuku:api:13.1.5")
  "vdImplementation"("dev.rikka.shizuku:provider:13.1.5")
Manifest (src/vd/AndroidManifest.xml):
  <provider android:name="rikka.shizuku.ShizukuProvider" android:authorities="${applicationId}.shizuku" android:exported="true" android:multiprocess="false" android:permission="android.permission.INTERACT_ACROSS_USERS_FULL"/>
Emulator M1 without Shizuku: same VdService via `adb shell CLASSPATH=$(pm path com.aistudio.aetherlinux.pxkdmv.vd) app_process / com.example.vd.DevMain`; a Surface cannot cross that path, so for M1 instead run the Shizuku server on the emulator (`adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh`, Shizuku APK installed) - same code path as device.
