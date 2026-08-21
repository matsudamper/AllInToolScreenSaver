#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ANDROID_SDK_DIR="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="13114758"
ANDROID_PLATFORM="platforms;android-37.0"
ANDROID_BUILD_TOOLS="build-tools;37.0.0"

# Gradle と本体ビルド/テストは最新 LTS で動かす。CI も JDK 25 を使用している。
JDK_LTS_MAJOR="25"
JDKS_DIR="$HOME/.jdks"

find_lts_jdk_home() {
  ls -d "$JDKS_DIR/jdk-${JDK_LTS_MAJOR}"* 2>/dev/null | head -1
}

find_detekt_jdk_home() {
  ls -d /usr/lib/jvm/*-21-openjdk* /usr/lib/jvm/*21*openjdk* 2>/dev/null | head -1
}

# 最新 LTS(Temurin) を foojay Disco API から取得する。リポジトリの
# .claude/hooks/session-start.sh と同じ供給元に揃えている。
ensure_lts_jdk() {
  local home
  home="$(find_lts_jdk_home)"
  if [ -n "$home" ] && [ -x "$home/bin/java" ]; then
    LTS_JAVA_HOME="$home"
    return
  fi
  mkdir -p "$JDKS_DIR"
  local api_url="https://api.foojay.io/disco/v3.0/packages?distro=temurin&architecture=x64&archive_type=tar.gz&operating_system=linux&libc_type=glibc&package_type=jdk&javafx_bundled=false&version=${JDK_LTS_MAJOR}&latest=overall"
  local pkg_id
  pkg_id="$(curl -fsSL "$api_url" | python3 -c 'import sys, json; print(json.load(sys.stdin)["result"][0]["id"])')"
  local tmp_tar
  tmp_tar="$(mktemp --suffix=.tar.gz)"
  curl -fsSL -o "$tmp_tar" "https://api.foojay.io/disco/v3.0/ids/${pkg_id}/redirect"
  tar -xzf "$tmp_tar" -C "$JDKS_DIR"
  rm -f "$tmp_tar"
  LTS_JAVA_HOME="$(find_lts_jdk_home)"
}

# detekt(1.23.8) は JDK 25 のバージョン表記を解釈できず失敗するため、
# detekt 実行用に JDK 21 を確保しておく(CI も detekt だけ JDK 21 を使う)。
ensure_detekt_jdk() {
  local home
  home="$(find_detekt_jdk_home)"
  if [ -z "$home" ]; then
    sudo apt-get update
    sudo apt-get install -y --no-install-recommends openjdk-21-jdk
    home="$(find_detekt_jdk_home)"
  fi
  DETEKT_JAVA_HOME="$home"
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
  printf 'sdk.dir=%s\n' "$ANDROID_SDK_DIR" > "$REPO_ROOT/local.properties"
}

# Gradle デーモンのデフォルト JDK を最新 LTS に固定する。detekt を動かすときだけ
# コマンドラインで -Dorg.gradle.java.home に JDK 21 を渡して上書きする。
configure_gradle_launcher_jdk() {
  local gradle_props="$HOME/.gradle/gradle.properties"
  mkdir -p "$HOME/.gradle"
  touch "$gradle_props"
  grep -v '^org.gradle.java.home=' "$gradle_props" > "$gradle_props.tmp" 2>/dev/null || true
  mv "$gradle_props.tmp" "$gradle_props"
  printf 'org.gradle.java.home=%s\n' "$LTS_JAVA_HOME" >> "$gradle_props"
}

ensure_lts_jdk
ensure_detekt_jdk
ensure_android_sdk
write_local_properties
configure_gradle_launcher_jdk

cd "$REPO_ROOT"
export JAVA_HOME="$LTS_JAVA_HOME"
./gradlew assembleDebug
