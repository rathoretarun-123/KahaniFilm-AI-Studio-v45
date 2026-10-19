#!/usr/bin/env bash
# Builds the Kahani Film APK without Gradle (works offline with Debian/Ubuntu android tools).
#   sudo apt-get install -y android-sdk-platform-23 aapt dalvik-exchange zipalign apksigner openjdk-17-jdk
# Output: release/KahaniFilm-v47.apk (and a KahaniFilm.apk symlink for older automation)
set -euo pipefail
cd "$(dirname "$0")"

# Ensure the committed story packs are available to direct/local APK builds too.
if [ "${KAHANIFILM_ASSETS_PREPARED:-0}" != "1" ] && [[ -s Aakhiri_Chai_Assets_Part1.zip && -s Aakhiri_Chai_Assets_Part2.zip && -s Babu_Ke_Liye_Assets_Part1.zip && -s Babu_Ke_Liye_Assets_Part2.zip ]]; then
  bash tools/scripts/prepare_bundled_film_assets.sh
fi

ANDROID_JAR=${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}
# Full Android 14 API for compiling (Robolectric's android-all jar from Maven Central).
ALL_JAR=${ALL_JAR:-$HOME/.cache/kahani/android-all-14.jar}
if [ ! -f "$ALL_JAR" ]; then
  mkdir -p "$(dirname "$ALL_JAR")"
  curl -fL -o "$ALL_JAR" https://repo1.maven.org/maven2/org/robolectric/android-all/14-robolectric-10818077/android-all-14-robolectric-10818077.jar
fi
# The android-23 jar has no java.util.function (API 24), which some Android 14 signatures name: the JDK's own
# classes of it, for compiling only (never dexed).
FN_JAR=$HOME/.cache/kahani/java-util-function.jar
if [ ! -f "$FN_JAR" ] && command -v jimage >/dev/null; then
  rm -rf build/fnx && mkdir -p build/fnx
  jimage extract --dir build/fnx --include 'regex:/java.base/java/util/function/.*' "$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")/lib/modules"
  (cd build/fnx/java.base && jar cf "$FN_JAR" java/util/function)
fi
BOOT="$ALL_JAR:$ANDROID_JAR"; [ -f "$FN_JAR" ] && BOOT="$BOOT:$FN_JAR"
# v47.13: ONNX Runtime for the face detector (FaceModel): its Java classes and its native libraries for phones
ORT_V=1.20.0
ORT_AAR=$HOME/.cache/kahani/onnxruntime-android-$ORT_V.aar
if [ ! -s "$ORT_AAR" ]; then
  mkdir -p "$(dirname "$ORT_AAR")"
  for i in 1 2 3 4; do
    curl -fL -o "$ORT_AAR" https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/$ORT_V/onnxruntime-android-$ORT_V.aar && break
    sleep $((i * 8))
  done
fi
# v47.13 (the user: "include full size kokoro in apk"): sherpa-onnx (Apache-2.0) to run Kokoro-82M on the phone, the
# Kotlin runtime its classes need, and the full Kokoro model (Apache-2.0, 350 MB: fetched at build time — GitHub keeps
# no file over 100 MB in a repository)
fetch() { [ -s "$2" ] && return 0; mkdir -p "$(dirname "$2")"; for i in 1 2 3 4; do curl -fL -o "$2.part" "$1" && mv "$2.part" "$2" && return 0; sleep $((i * 8)); done; return 1; }
SHERPA_V=1.13.8
SHERPA_AAR=$HOME/.cache/kahani/sherpa/sherpa-onnx-$SHERPA_V.aar
KOTLIN_JAR=$HOME/.cache/kahani/kotlin-stdlib-2.0.21.jar
KOKORO_TAR=$HOME/.cache/kahani/kokoro/kokoro-multi-lang-v1_0.tar.bz2
WITH_KOKORO=${WITH_KOKORO:-1}
if [ "$WITH_KOKORO" = "1" ]; then
  fetch https://github.com/k2-fsa/sherpa-onnx/releases/download/v$SHERPA_V/sherpa-onnx-$SHERPA_V.aar "$SHERPA_AAR" || WITH_KOKORO=0
  fetch https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/2.0.21/kotlin-stdlib-2.0.21.jar "$KOTLIN_JAR" || WITH_KOKORO=0
  fetch https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2 "$KOKORO_TAR" || WITH_KOKORO=0
fi
echo "kokoro in the APK: $WITH_KOKORO"
VERSION_CODE=$(grep -o 'versionCode="[0-9]*"' app/src/main/AndroidManifest.xml | grep -o '[0-9]*')
VERSION_NAME=$(grep -o 'versionName="[^"]*"' app/src/main/AndroidManifest.xml | cut -d'"' -f2)
# every CI build is a higher version than the one before it, so a new APK always installs as an update over the app
# already on the phone (same package, same committed signing key, a larger version code)
if [ -n "${GITHUB_RUN_NUMBER:-}" ]; then
  VERSION_CODE=$((VERSION_CODE * 10000 + GITHUB_RUN_NUMBER))
  VERSION_NAME="$VERSION_NAME.$GITHUB_RUN_NUMBER"
fi
echo "version $VERSION_NAME ($VERSION_CODE)"

B=build
rm -rf $B && mkdir -p $B/gen $B/classes $B/ort
unzip -q -o "$ORT_AAR" classes.jar 'jni/arm64-v8a/*' 'jni/armeabi-v7a/*' -d $B/ort
EXTRA_A=""
DEX_EXTRA=""
if [ "$WITH_KOKORO" = "1" ]; then
  mkdir -p $B/sherpa $B/extra/kokoro
  unzip -q -o "$SHERPA_AAR" classes.jar 'jni/arm64-v8a/*' 'jni/armeabi-v7a/*' -d $B/sherpa
  tar xjf "$KOKORO_TAR" -C $B/extra/kokoro --strip-components=1 kokoro-multi-lang-v1_0/model.onnx kokoro-multi-lang-v1_0/voices.bin \
    kokoro-multi-lang-v1_0/tokens.txt kokoro-multi-lang-v1_0/lexicon-us-en.txt kokoro-multi-lang-v1_0/lexicon-gb-en.txt \
    kokoro-multi-lang-v1_0/LICENSE kokoro-multi-lang-v1_0/espeak-ng-data
  cp tools/tts/KOKORO_NOTICE.txt $B/extra/kokoro/NOTICE.txt
  EXTRA_A="-A $B/extra"
  # (the old dx cannot read Java 9 module descriptors: the jars are dexed without them)
  cp "$KOTLIN_JAR" $B/sherpa/kotlin-stdlib.jar
  zip -q -d $B/sherpa/kotlin-stdlib.jar 'META-INF/versions/*' 'module-info.class' >/dev/null 2>&1 || true
  zip -q -d $B/sherpa/classes.jar 'META-INF/versions/*' 'module-info.class' >/dev/null 2>&1 || true
  DEX_EXTRA="$B/sherpa/classes.jar $B/sherpa/kotlin-stdlib.jar"
fi
# v47.13 (the user: "include all the pictures uploaded till now … in apk library"): the user's own pictures, kept on this
# machine only (private_assets/ is never committed — the repository is public), go into an APK built here
if [ -d private_assets ]; then EXTRA_A="$EXTRA_A -A private_assets"; echo "the user's own pictures in the APK: $(find private_assets -type f | wc -l) files"; fi
echo "[1/6] resources"
aapt2 compile --dir app/src/main/res -o $B/res.zip
aapt2 link -o $B/base.apk -I "$ALL_JAR" --manifest app/src/main/AndroidManifest.xml \
  -A app/src/main/assets $EXTRA_A --java $B/gen --min-sdk-version 26 --target-sdk-version 34 \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" -0 jpg -0 png -0 onnx -0 bin -0 ogg $B/res.zip
echo "[2/6] compile java"
if ! javac -nowarn -source 8 -target 8 -encoding UTF-8 -bootclasspath "$BOOT" -classpath $B/ort/classes.jar -d $B/classes \
  $(find app/src/main/java -name '*.java') $(find $B/gen -name '*.java') > $B/javac.log 2>&1; then
  grep -v "warning\|^Note\|JAVA_TOOL" $B/javac.log
  echo "BUILD FAILED: java compile errors" >&2
  exit 1
fi
test -f $B/classes/com/tarun/kahani/app/MainActivity.class
echo "[3/6] dex"
# D8 (Android SDK build-tools) is preferred; Ubuntu/Debian machines without the SDK fall back to the
# packaged dalvik-exchange (dx), which still accepts the Java 8 bytecode javac produced above.
mkdir -p "$B/dex"
jar cf "$B/classes.jar" -C "$B/classes" .
if command -v d8 >/dev/null 2>&1; then
  d8 --min-api 26 --lib "$ALL_JAR" --output "$B/dex" "$B/classes.jar" "$B/ort/classes.jar" $DEX_EXTRA
elif command -v dalvik-exchange >/dev/null 2>&1; then
  echo "d8 not found; using dalvik-exchange (dx)"
  dalvik-exchange --dex --multi-dex --min-sdk-version=26 --output="$B/dex" "$B/classes.jar" "$B/ort/classes.jar" $DEX_EXTRA
elif command -v dx >/dev/null 2>&1; then
  echo "d8 not found; using dx"
  dx --dex --multi-dex --min-sdk-version=26 --output="$B/dex" "$B/classes.jar" "$B/ort/classes.jar" $DEX_EXTRA
else
  echo "BUILD FAILED: neither d8 nor dalvik-exchange/dx is installed" >&2
  exit 1
fi
cp "$B"/dex/classes*.dex "$B/"
echo "[4/6] package"
cp $B/base.apk $B/unsigned.apk
(cd $B && zip -q -j unsigned.apk classes*.dex)
# the runtime's native libraries, as lib/<abi>/ in the package
mkdir -p $B/native/lib && cp -r $B/ort/jni/* $B/native/lib/
# (with Kokoro: sherpa-onnx's libraries, and its ONNX Runtime 1.28 in place of 1.20's — the newer runtime serves the
# older Java API of the face and pose models as well)
if [ "$WITH_KOKORO" = "1" ]; then cp -r $B/sherpa/jni/* $B/native/lib/; fi
(cd $B/native && zip -q -r ../unsigned.apk lib)
echo "[5/6] align"
zipalign -f 4 $B/unsigned.apk $B/aligned.apk
echo "[6/6] sign"
mkdir -p release
apksigner sign --ks tools/debug.keystore --ks-pass pass:android --key-pass pass:android --ks-key-alias kahani \
  --out release/KahaniFilm-v47.apk $B/aligned.apk
apksigner verify release/KahaniFilm-v47.apk
# v47: a stable alias so older scripts and the CI step that still looks for KahaniFilm.apk keep working
cp -f release/KahaniFilm-v47.apk release/KahaniFilm.apk
ls -la release/KahaniFilm-v47.apk release/KahaniFilm.apk
