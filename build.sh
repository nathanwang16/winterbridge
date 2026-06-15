#!/usr/bin/env bash
#
# Build winterbridge with JDK 21 and install the jar into the Minecraft mods
# folder. Minecraft 1.21.x / NeoForge run on Java 21, so this pins JAVA_HOME to
# the Homebrew openjdk@21 keg (your default `java` is newer and not supported by
# the Gradle/NeoGradle toolchain).
#
# Usage:
#   ./build.sh              # build + install into mods/
#   ./build.sh --no-install # build only, skip the copy
#   any extra args are passed straight to gradle, e.g. ./build.sh clean build
#
set -euo pipefail
cd "$(dirname "$0")"

# --- locate JDK 21 (keg-only Homebrew formula, not on PATH) ---
JDK21="$(brew --prefix openjdk@21 2>/dev/null)/libexec/openjdk.jdk/Contents/Home"
if [ ! -x "$JDK21/bin/java" ]; then
  echo "error: JDK 21 not found. Install it with:  brew install openjdk@21" >&2
  exit 1
fi
export JAVA_HOME="$JDK21"
echo "Using JAVA_HOME=$JAVA_HOME"

# --- parse our one flag, pass the rest to gradle ---
INSTALL=1
GRADLE_ARGS=()
for arg in "$@"; do
  if [ "$arg" = "--no-install" ]; then INSTALL=0; else GRADLE_ARGS+=("$arg"); fi
done
[ ${#GRADLE_ARGS[@]} -eq 0 ] && GRADLE_ARGS=(build)

# --- build ---
./gradlew --no-daemon --console=plain "${GRADLE_ARGS[@]}"

# --- install into Minecraft mods folder ---
JAR="$(ls -t build/libs/winterbridge-*.jar 2>/dev/null | head -1 || true)"
MODS="$HOME/Library/Application Support/minecraft/mods"
if [ "$INSTALL" -eq 1 ] && [ -n "$JAR" ]; then
  mkdir -p "$MODS"
  cp "$JAR" "$MODS/"
  echo "Installed $(basename "$JAR") -> $MODS/"
elif [ -n "$JAR" ]; then
  echo "Built $JAR (skipped install)"
fi
