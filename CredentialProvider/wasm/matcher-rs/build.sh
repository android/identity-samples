# 1. Build with maximum rustc optimizations
CARGO_PROFILE_RELEASE_PANIC=immediate-abort \
CARGO_PROFILE_RELEASE_OPT_LEVEL="z" \
CARGO_PROFILE_RELEASE_CODEGEN_UNITS=1 \
CARGO_PROFILE_RELEASE_STRIP=true \
CARGO_PROFILE_RELEASE_LTO=true \
cargo +nightly build \
  -Z panic-immediate-abort \
  -Z build-std \
  --target wasm32-unknown-unknown \
  --release

# 2. Further shrink using wasm-opt (if available)
for wasm in target/wasm32-unknown-unknown/release/*.wasm; do
  wasm-opt -Oz --strip-debug --enable-bulk-memory --enable-sign-ext "$wasm" -o "$wasm"
done

