# Output Configuration
LICENSE_FILE := THIRD_PARTY_LICENSES.txt
WASM_TARGET  := wasm32-unknown-unknown
BUILD_DIR    := target/$(WASM_TARGET)/release
WASM_OUTPUT  := $(BUILD_DIR)/credential_manager.wasm

# Phony targets
.PHONY: all build licenses clean setup

all: build licenses

# 1. Build the Wasm binary
build:
	@echo "🚧 Building Release Binary..."
	cargo build --target $(WASM_TARGET) --release
	@echo "✅ Build complete: $(WASM_OUTPUT)"

# 2. Generate License Attribution (Using built-in cargo vendor)
licenses:
	@echo "📝 Generating License Attribution..."
	@chmod +x generate_license.sh
	@./generate_license.sh

# Helper to install the Wasm target if missing
setup:
	@echo "⚙️ Adding Wasm target to Rustup..."
	rustup target add $(WASM_TARGET)

# Clean build artifacts
clean:
	cargo clean
	rm -f $(LICENSE_FILE)