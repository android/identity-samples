# Credential Transfer WASM Module

This directory contains the Rust source code for the Credential Transfer WebAssembly module. This module is responsible for filtering and matching credentials based on request criteria provided by the host environment (e.g., Credential Manager).

## Overview

The module acts as a bridge between the raw credential data and the host application. It:
1.  **Parses Requests**: Reads a JSON request from the host specifying required credential types.
2.  **Compares Exporter**: Compares against the registered exporter and matches based on the request.
3.  **Exports Matches**: Returns the match back to the host via specific WASM imports (`AddExportEntry`).

## Prerequisites

Before building, ensure you have Rust installed and the `wasm32-unknown-unknown` target available.

```bash
# Install the WASM target
rustup target add wasm32-unknown-unknown
```

## Building

You can build the project using the provided `Makefile` or `build.sh` script.

### Using Make

```bash
# Build the WASM binary and generate license attribution
make

# Or to just build the binary
make build
```

This will produce the WASM file at:
`target/wasm32-unknown-unknown/release/credential_manager.wasm`

### Using Build Script

```bash
./build.sh
```

## Development

### Project Structure

- **`src/lib.rs`**: Main entry point and logic. Handles memory allocation for the host, JSON parsing, and matching logic.
- **`Cargo.toml`**: Rust project configuration and dependencies (`serde`, `serde_json`).
- **`Makefile`**: Build automation for compiling to WASM and bundling licenses.
- **`generate_license.sh`**: Script to aggregate licenses from dependencies.

### WASM Interface

The module exports `main` (and `_start` for standalone WASM) and expects the following imports from the `credman` module:

- `GetRequestSize` / `GetRequestBuffer`: To retrieve the filtering request.
- `GetCredentialsSize` / `ReadCredentialsBuffer`: To retrieve the credential store.
- `AddExportEntry`: To send matched credentials back to the host.

## License

See [LICENSE](LICENSE) for details.
