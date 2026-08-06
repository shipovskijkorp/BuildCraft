#!/bin/sh
set -eu

GRADLE_VERSION=9.0.0
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BOOTSTRAP_DIR="$APP_HOME/.gradle-bootstrap"
GRADLE_HOME="$BOOTSTRAP_DIR/gradle-$GRADLE_VERSION"
ZIP_FILE="$BOOTSTRAP_DIR/gradle-$GRADLE_VERSION-bin.zip"
URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
    mkdir -p "$BOOTSTRAP_DIR"
    if [ ! -f "$ZIP_FILE" ]; then
        if command -v curl >/dev/null 2>&1; then
            curl -fL "$URL" -o "$ZIP_FILE"
        elif command -v wget >/dev/null 2>&1; then
            wget -O "$ZIP_FILE" "$URL"
        else
            echo "Neither curl nor wget is available to download Gradle $GRADLE_VERSION." >&2
            exit 1
        fi
    fi
    if command -v unzip >/dev/null 2>&1; then
        unzip -q -o "$ZIP_FILE" -d "$BOOTSTRAP_DIR"
    else
        echo "unzip is required to unpack Gradle $GRADLE_VERSION." >&2
        exit 1
    fi
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
