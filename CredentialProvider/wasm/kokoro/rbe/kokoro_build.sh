#!/bin/bash

# Fail on any error.
set -e

# Display commands being run.
# WARNING: please only enable 'set -x' if necessary for debugging, and be very
#  careful if you handle credentials (e.g. from Keystore) with 'set -x':
#  statements like "export VAR=$(cat /tmp/keystore/credentials)" will result in
#  the credentials being printed in build logs.
#  Additionally, recursive invocation with credentials as command-line
#  parameters, will print the full command, with credentials, in the build logs.
# set -x

# Install dependencies
apt-get update
apt-get install -y make build-essential curl binaryen

# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
source "$HOME/.cargo/env"
rustup target add wasm32-unknown-unknown
rustup toolchain install nightly
rustup target add wasm32-unknown-unknown --toolchain nightly
rustup component add rust-src --toolchain nightly

# Code under repo is checked out to ${KOKORO_ARTIFACTS_DIR}/github.
# The final directory name in this path is determined by the scm name specified
# in the job configuration.

# Build matcher-rs
cd "${KOKORO_ARTIFACTS_DIR}/github/wasm/CredentialProvider/wasm/matcher-rs"
bash ./build.sh
bash ./generate_license.sh
mv target/THIRD_PARTY_LICENSES "${KOKORO_ARTIFACTS_DIR}/github/wasm/CredentialProvider/wasm/matcher-rs/target/wasm32-unknown-unknown/release/"
mv Cargo.lock "${KOKORO_ARTIFACTS_DIR}/github/wasm/CredentialProvider/wasm/matcher-rs/target/wasm32-unknown-unknown/release/"

# Build credential_transfer
cd "${KOKORO_ARTIFACTS_DIR}/github/wasm/CredentialProvider/wasm/credential_transfer"
bash ./build.sh

mv THIRD_PARTY_LICENSES "${KOKORO_ARTIFACTS_DIR}/github/wasm/CredentialProvider/wasm/credential_transfer/target/wasm32-unknown-unknown/release/"
# Move Cargo.lock to artifacts directory for SBOM generation
mv Cargo.lock "${KOKORO_ARTIFACTS_DIR}/github/wasm/CredentialProvider/wasm/credential_transfer/target/wasm32-unknown-unknown/release/"

