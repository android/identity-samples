#!/bin/bash
set -euo pipefail

VENDOR_DIR="target/vendor_tmp"
OUTPUT_FILE="THIRD_PARTY_LICENSES.txt"

# 1. Clean previous run
rm -rf "$VENDOR_DIR"
rm -f "$OUTPUT_FILE"

# 2. Vendor dependencies (download source code to local folder)
echo "📦 Vendorizing dependencies..."
cargo vendor "$VENDOR_DIR"

echo "📝 Aggregating licenses..."
echo "This software uses the following open source packages:" > "$OUTPUT_FILE"

# 3. Loop through each package in the vendor directory
for package_path in "$VENDOR_DIR"/*; do
    if [ -d "$package_path" ]; then
        package_name=$(basename "$package_path")
        
        echo "----------------------------------------------------------------------------" >> "$OUTPUT_FILE"
        echo "PACKAGE: $package_name" >> "$OUTPUT_FILE"
        
        # Look for license files in order of preference (Apache 2.0 first)
        # Many Rust crates are dual licensed and include both LICENSE-APACHE and LICENSE-MIT.
        license_found=false
        
        # Check for specific Apache file
        if [ -f "$package_path/LICENSE-APACHE" ]; then
            echo "LICENSE: Apache-2.0" >> "$OUTPUT_FILE"
            echo "----------------------------------------------------------------------------" >> "$OUTPUT_FILE"
            cat "$package_path/LICENSE-APACHE" >> "$OUTPUT_FILE"
            license_found=true
        
        # Check for generic LICENSE file (often contains the text)
        elif [ -f "$package_path/LICENSE" ]; then
            echo "LICENSE: See text below" >> "$OUTPUT_FILE"
            echo "----------------------------------------------------------------------------" >> "$OUTPUT_FILE"
            cat "$package_path/LICENSE" >> "$OUTPUT_FILE"
            license_found=true
            
        # Check for MIT as fallback
        elif [ -f "$package_path/LICENSE-MIT" ]; then
             echo "LICENSE: MIT" >> "$OUTPUT_FILE"
             echo "----------------------------------------------------------------------------" >> "$OUTPUT_FILE"
             cat "$package_path/LICENSE-MIT" >> "$OUTPUT_FILE"
             license_found=true
        fi

        if [ "$license_found" = false ]; then
            echo "WARNING: No license file found for '$package_name' in source." >&2
            echo "WARNING: No license file found in source." >> "$OUTPUT_FILE"
        fi
        
        echo -e "\n\n" >> "$OUTPUT_FILE"
    fi
done

# 4. Cleanup
rm -rf "$VENDOR_DIR"
echo "✅ Done! Created $OUTPUT_FILE"