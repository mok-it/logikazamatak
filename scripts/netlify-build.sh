#!/usr/bin/env bash
set -euo pipefail

java_major_version() {
  java -version 2>&1 | awk -F '"' '/version/ {
    split($2, parts, ".")
    if (parts[1] == "1") {
      print parts[2]
    } else {
      print parts[1]
    }
  }'
}

install_jdk_21() {
  local arch
  case "$(uname -m)" in
    x86_64 | amd64) arch="x64" ;;
    aarch64 | arm64) arch="aarch64" ;;
    *) echo "Unsupported CPU architecture: $(uname -m)" >&2; exit 1 ;;
  esac

  local install_dir="${NETLIFY_BUILD_BASE:-$PWD}/.netlify/jdk-21"
  local archive
  archive="$(mktemp)"

  mkdir -p "$install_dir"
  if [ ! -x "$install_dir/bin/java" ]; then
    curl -fsSL \
      "https://api.adoptium.net/v3/binary/latest/21/ga/linux/${arch}/jdk/hotspot/normal/eclipse?project=jdk" \
      -o "$archive"
    tar -xzf "$archive" -C "$install_dir" --strip-components=1
  fi

  export JAVA_HOME="$install_dir"
  export PATH="$JAVA_HOME/bin:$PATH"
}

current_java="$(java_major_version)"
if [ "$current_java" != "21" ]; then
  echo "Netlify Java $current_java detected; switching Gradle to JDK 21."
  install_jdk_21
fi

java -version
./gradlew :composeApp:wasmJsBrowserDistribution
