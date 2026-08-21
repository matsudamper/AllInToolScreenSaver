#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ANDROID_SDK_DIR="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="13114758"
ANDROID_PLATFORM="platforms;android-37.0"
ANDROID_BUILD_TOOLS="build-tools;37.0.0"

ensure_jdk() {
  if command -v java >/dev/null 2>&1; then
    return
  fi
  sudo apt-get update
  sudo apt-get install -y --no-install-recommends openjdk-21-jdk
}

ensure_android_sdk() {
  local sdkmanager_bin="$ANDROID_SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
  if [ ! -x "$sdkmanager_bin" ]; then
    mkdir -p "$ANDROID_SDK_DIR/cmdline-tools"
    local tmp_zip
    tmp_zip="$(mktemp)"
    curl -fsSL -o "$tmp_zip" \
      "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
    rm -rf "$ANDROID_SDK_DIR/cmdline-tools/latest"
    unzip -q "$tmp_zip" -d "$ANDROID_SDK_DIR/cmdline-tools"
    mv "$ANDROID_SDK_DIR/cmdline-tools/cmdline-tools" "$ANDROID_SDK_DIR/cmdline-tools/latest"
    rm -f "$tmp_zip"
  fi
  set +o pipefail
  yes | "$sdkmanager_bin" --sdk_root="$ANDROID_SDK_DIR" --licenses >/dev/null
  set -o pipefail
  "$sdkmanager_bin" --sdk_root="$ANDROID_SDK_DIR" \
    "platform-tools" "$ANDROID_PLATFORM" "$ANDROID_BUILD_TOOLS"
}

write_local_properties() {
  export ANDROID_HOME="$ANDROID_SDK_DIR"
  printf 'sdk.dir=%s\n' "$ANDROID_SDK_DIR" > "$REPO_ROOT/local.properties"
}

ensure_jdk
ensure_android_sdk
write_local_properties

cd "$REPO_ROOT"
./gradlew assembleDebug
