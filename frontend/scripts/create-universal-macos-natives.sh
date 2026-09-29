#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 3 ]]; then
    echo "Usage: $0 <mac-x64-javafx-graphics.jar> <mac-arm64-javafx-graphics.jar> <output-directory>" >&2
    exit 2
fi

mac_x64_jar=$1
mac_arm64_jar=$2
output_directory=$3

if ! command -v lipo >/dev/null 2>&1; then
    echo "lipo is required to build universal macOS JavaFX libraries." >&2
    exit 1
fi

working_directory=$(mktemp -d "${TMPDIR:-/tmp}/resolveit-universal-macos.XXXXXX")
trap 'rm -rf -- "$working_directory"' EXIT

mkdir -p "$working_directory/x64" "$working_directory/arm64"

unzip -q -j "$mac_x64_jar" '*.dylib' -d "$working_directory/x64"
unzip -q -j "$mac_arm64_jar" '*.dylib' -d "$working_directory/arm64"

find "$working_directory/x64" -maxdepth 1 -type f -name '*.dylib' -exec basename {} \; | sort > "$working_directory/x64-files.txt"
find "$working_directory/arm64" -maxdepth 1 -type f -name '*.dylib' -exec basename {} \; | sort > "$working_directory/arm64-files.txt"

if ! diff -u "$working_directory/x64-files.txt" "$working_directory/arm64-files.txt"; then
    echo "The Intel and Apple-Silicon JavaFX archives contain different native libraries." >&2
    exit 1
fi

rm -rf -- "$output_directory"
mkdir -p "$output_directory"

while IFS= read -r library_name; do
    [[ -n "$library_name" ]] || continue
    lipo -create \
        "$working_directory/x64/$library_name" \
        "$working_directory/arm64/$library_name" \
        -output "$output_directory/$library_name"
    lipo "$output_directory/$library_name" -verify_arch x86_64 arm64
done < "$working_directory/x64-files.txt"
