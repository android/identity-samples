#!/bin/bash
set -euo pipefail

VENDOR_DIR="target/vendor_tmp"
OUTPUT_FILE="target/THIRD_PARTY_LICENSES"

# 1. Clean previous run
rm -rf "$VENDOR_DIR"
rm -f "$OUTPUT_FILE"

# 2. Vendor dependencies (download source code to local folder)
echo "📦 Vendorizing dependencies..."
cargo vendor "$VENDOR_DIR"

# Special override for nanoserde and nanoserde-derive licenses
NANOSERDE_LICENSE_TMP="target/NANOSERDE_LICENSE_APACHE"
echo "📥 Downloading nanoserde license..."
curl -sSL "https://raw.githubusercontent.com/not-fl3/nanoserde/master/LICENSE-APACHE" -o "$NANOSERDE_LICENSE_TMP"

for pkg in "nanoserde" "nanoserde-derive"; do
    if [ -d "$VENDOR_DIR/$pkg" ]; then
        cp "$NANOSERDE_LICENSE_TMP" "$VENDOR_DIR/$pkg/LICENSE-APACHE"
    fi
done
rm -f "$NANOSERDE_LICENSE_TMP"

echo "📝 Aggregating licenses..."
echo "This software uses the following open source packages:" > "$OUTPUT_FILE"

# 3. Loop through each package in the vendor directory
for package_path in "$VENDOR_DIR"/*; do
    if [ -d "$package_path" ]; then
        package_name=$(basename "$package_path")
        
        license_path=""
        license_type=""

        # Look for license files in order of preference (Apache 2.0 or Public Domain)
        if [ -f "$package_path/LICENSE-APACHE" ]; then
            license_path="$package_path/LICENSE-APACHE"
            license_type="Apache-2.0"
        elif [ -f "$package_path/UNLICENSE" ]; then
            license_path="$package_path/UNLICENSE"
            license_type="Unlicensed (Public Domain)"
        elif [ -f "$package_path/LICENSE" ]; then
            if grep -qi "Apache License" "$package_path/LICENSE"; then
                license_path="$package_path/LICENSE"
                license_type="Apache-2.0"
            elif grep -qi "Public Domain" "$package_path/LICENSE"; then
                license_path="$package_path/LICENSE"
                license_type="Public Domain"
            else
                echo "ERROR: LICENSE file for '$package_name' is neither Apache nor Public Domain." >&2
                exit 1
            fi
        fi

        if [ -z "$license_path" ]; then
            echo "ERROR: No Apache or Public Domain license found for '$package_name'." >&2
            exit 1
        fi

        if [ ! -s "$license_path" ]; then
            echo "ERROR: License file for '$package_name' is empty." >&2
            exit 1
        fi

        echo "----------------------------------------------------------------------------" >> "$OUTPUT_FILE"
        echo "PACKAGE: $package_name" >> "$OUTPUT_FILE"
        echo "LICENSE: $license_type" >> "$OUTPUT_FILE"
        echo "----------------------------------------------------------------------------" >> "$OUTPUT_FILE"
        cat "$license_path" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
    fi
done

# 4. Cleanup
rm -rf "$VENDOR_DIR"
echo "✅ Done! Created $OUTPUT_FILE"