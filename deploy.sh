#!/usr/bin/env bash
set -e

export JAVA_HOME="/Users/macbook/android-toolchain/jdk-17/Contents/Home"
export ANDROID_HOME="/Users/macbook/android-toolchain/android-sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:/Applications/Genymotion.app/Contents/MacOS/tools:$PATH"

ADB="/Applications/Genymotion.app/Contents/MacOS/tools/adb"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "🔨 Building debug APK locally..."
./gradlew assembleDebug

# Get all online devices / emulators
DEVICES=$($ADB devices | grep -w "device" | awk '{print $1}')

if [ -z "$DEVICES" ]; then
  echo "⚠️ No connected devices or emulators found! Make sure Genymotion is running."
  exit 1
fi

COUNT=$(echo "$DEVICES" | wc -l | tr -d ' ')
echo "📲 Found $COUNT connected device(s). Deploying..."

for DEV in $DEVICES; do
  echo "  -> Installing to $DEV..."
  $ADB -s "$DEV" install -r "$APK_PATH"
  echo "  -> Granting runtime permissions on $DEV..."
  $ADB -s "$DEV" shell pm grant com.fyntryx.homease android.permission.RECORD_AUDIO > /dev/null 2>&1 || true
  echo "  -> Restarting HomEase on $DEV..."
  $ADB -s "$DEV" shell am force-stop com.fyntryx.homease
  $ADB -s "$DEV" shell monkey -p com.fyntryx.homease -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
done

echo "✅ App deployed and running on all $COUNT device(s)!"
