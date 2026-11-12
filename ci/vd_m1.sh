#!/usr/bin/env bash
# M1 proof script (docs/VDISPLAY_SPEC.md): real Shizuku server on an
# emulator, Settings into a VdPanel, non-black screencap assert.
# Lives in a file because android-emulator-runner executes each script
# line as its own `sh -c`, so loops/heredocs cannot live inline.
set -x

adb wait-for-device
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
adb root
sleep 3
adb wait-for-device

adb install -r app/build/outputs/apk/vd/debug/app-vd-debug.apk
adb install -r shizuku.apk

# The Shizuku app extracts its starter (start.sh + binary) on first
# launch; the file does not exist before that. Launch, then poll for it.
adb shell monkey -p moe.shizuku.privileged.api -c android.intent.category.LAUNCHER 1
FOUND=0
for i in $(seq 1 15); do
  if adb shell ls /sdcard/Android/data/moe.shizuku.privileged.api/start.sh; then
    FOUND=1
    break
  fi
  sleep 2
done
echo "start.sh present: $FOUND"

# Start the Shizuku server as root (adb root on the emulator - the same
# start.sh users run through wireless adb on device).
adb shell 'nohup sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh > /sdcard/shizuku.log 2>&1 &'
sleep 10
adb shell cat /sdcard/shizuku.log || true

adb shell am start -n com.aistudio.aetherlinux.pxkdmv.vd/com.example.vd.VdSmokeActivity
sleep 12

# Grant the Shizuku permission dialog if it is showing.
for i in 1 2 3 4; do
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1
  B=$(grep -o 'text="Allow all the time"[^>]*bounds="[^"]*"' ui.xml | grep -o 'bounds="[^"]*"' | head -1)
  [ -z "$B" ] && B=$(grep -o 'text="Allow"[^>]*bounds="[^"]*"' ui.xml | grep -o 'bounds="[^"]*"' | head -1)
  [ -z "$B" ] && break
  C=$(echo "$B" | sed 's/bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]"/echo $(( (\1+\3)\/2 )) $(( (\2+\4)\/2 ))/e')
  echo "tapping allow at $C"
  adb shell input tap $C
  sleep 4
done

# Let the service bind, the display be created and Settings draw.
sleep 20

adb exec-out screencap -p > vd-m1.png
ls -la vd-m1.png
MEAN=$(identify -format "%[mean]" vd-m1.png)
echo "screenshot mean pixel value: $MEAN"
# An all-black frame has mean ~0; a rendered Settings window is far
# above. 500/65535 (~0.8% white) still counts a mostly-dark frame with
# real content as non-black.
python3 - "$MEAN" <<'PY'
import sys
mean = float(sys.argv[1])
print("PASS: non-black frame" if mean > 500 else "FAIL: frame is black", "mean =", mean)
sys.exit(0 if mean > 500 else 1)
PY
