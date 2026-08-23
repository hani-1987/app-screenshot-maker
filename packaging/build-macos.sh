#!/usr/bin/env bash
# Builds the macOS native app bundle for Screenshot Maker using jpackage.
# Must be run ON macOS with a JDK 17+ installed (jpackage ships with the JDK) --
# jpackage cannot cross-build a macOS bundle from Windows or Linux, and this
# repo's Windows build (packaging/build-windows.ps1) cannot produce it either.
#
# Usage:
#   chmod +x packaging/build-macos.sh
#   ./packaging/build-macos.sh
#
# Note: only the website capture engine (--mode web) works on macOS. Desktop
# application capture (--mode desktop) is Windows-only, since it relies on
# WinAppDriver / Windows UI Automation, which has no macOS equivalent in this tool.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STAGING_DIR="$ROOT_DIR/packaging/macos-input"
DIST_DIR="$ROOT_DIR/packaging/dist-macos"

echo "==> Building the shaded CLI jar (mvn package)"
(cd "$ROOT_DIR" && mvn -q -DskipTests package)

FAT_JAR="$ROOT_DIR/cli/target/screenshot-maker.jar"
if [ ! -f "$FAT_JAR" ]; then
  echo "Expected shaded jar not found at $FAT_JAR" >&2
  exit 1
fi

echo "==> Staging a clean jpackage input directory (fat jar only)"
rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR"
cp "$FAT_JAR" "$STAGING_DIR/"

rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

echo "==> Running jpackage (--type app-image)"
jpackage \
  --type app-image \
  --name ScreenshotMaker \
  --app-version 1.0.0 \
  --vendor "Screenshot Maker" \
  --description "Walks every reachable page of a website and captures a screenshot of each one." \
  --input "$STAGING_DIR" \
  --main-jar screenshot-maker.jar \
  --main-class com.screenshotmaker.cli.Main \
  --dest "$DIST_DIR"

echo
echo "==> Done. Output in $DIST_DIR"
echo "    Run it with: $DIST_DIR/ScreenshotMaker.app/Contents/MacOS/ScreenshotMaker --mode web --target https://example.com"
