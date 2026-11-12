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

# Shizuku v13.6's starter is the packaged native binary libshizuku.so
# in the app's extracted native lib dir - the app's "Start (in adb)"
# command runs it directly. No app launch or start.sh needed. Derive
# the path from pm path so it works regardless of install hash.
APKPATH=$(adb shell pm path moe.shizuku.privileged.api | head -1 | sed 's/^package://' | tr -d '\r')
echo "shizuku apk: $APKPATH"
STARTER="$(dirname "$APKPATH")/lib/x86_64/libshizuku.so"
adb shell ls -la "$STARTER"
adb shell "nohup $STARTER --apk=$APKPATH > /sdcard/shizuku.log 2>&1 &"
sleep 10
adb shell cat /sdcard/shizuku.log || true
adb shell ps -A | grep -i shizuku || true

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

# Evidence: state transitions from the smoke activity.
adb logcat -d -s VdSmoke:I > vd-logcat.txt || true
cat vd-logcat.txt
READY=$(grep -c 'state=READY' vd-logcat.txt || true)
echo "state=READY lines: $READY"

# PNG for the artifact + raw RGBA for the assert (ubuntu-24.04 runners
# have no ImageMagick; raw screencap needs no decoder).
adb exec-out screencap -p > vd-m1.png
adb exec-out screencap > vd-m1.raw
ls -la vd-m1.png vd-m1.raw
python3 - vd-m1.raw "$READY" <<'PY'
import struct, sys
d = open(sys.argv[1], 'rb').read()
ready = int(sys.argv[2])
w, h, fmt = struct.unpack('<III', d[:12])
px = d[12:]
n = len(px) // 4
total = 0
nonblack = 0
for i in range(0, n * 4, 4):
    v = px[i] + px[i+1] + px[i+2]
    total += v
    if v > 30:
        nonblack += 1
mean = total / (n * 3)
frac = nonblack / n
print(f"frame {w}x{h} fmt={fmt} mean={mean:.1f} nonblack={frac*100:.1f}% ready_lines={ready}")
# A rendered Settings window is mostly light: demand >=20% non-black so
# a title-bar-only frame (like the NEED_PERMISSION run, 9%) fails.
ok = ready > 0 and frac >= 0.20
print("PASS: VD rendered" if ok else "FAIL: no READY state or black panel")
sys.exit(0 if ok else 1)
PY
