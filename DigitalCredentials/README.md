# Digital Credentials Sample

This is a sample repository for the **Digital Credentials API** integration in Android, showcasing how to request and parse verifiable credentials using the Credential Manager.

The Digital Credentials Sample is a functional Android app built with Kotlin and Jetpack Compose. It is designed to help developers understand the workflow for retrieving and presenting digital credentials, such as Mobile Driver's Licenses (mDL) and Verified Emails, using the OpenID4VP and DCQL standards.

## Features

This sample app implements the following use cases:

* **Get Digital Credential (mdoc)**: Request a Mobile Driver's License (mDL) or other ISO 18013-5 compliant credentials.
* **Get Verified Email**: Request a verified email address using the SD-JWT (Selective Disclosure JWT) format.
* **OpenID4VP & DCQL Support**: Demonstrates how to construct modern, simplified Digital Credential Query Language (DCQL) requests.
* **CBOR & SD-JWT Parsing**: Includes a reference implementation for decoding CBOR-based mDoc data and parsing selectively disclosed claims from SD-JWTs.

## Requirements

* Latest release of [Android Studio](https://developer.android.com/studio)
* Java 17 or higher
* A physical device or emulator running Android 9 (API level 28) or higher.
* A digital wallet app (like [CMWallet](https://github.com/digitalcredentialsdev/CMWallet)) installed and registered on the device to test the generic retrieval flow. This is not needed to test email verification functionality.

## Typical Workflow

* **Launch the app**: The main screen presents two primary actions for credential retrieval.
* **Request a Credential**: Tap on "Get Digital Credential (mdoc)" or "Get Verified Email".
* **Credential Selection**: The Credential Manager bottom sheet will appear, listing available credentials from registered wallets.
* **User Consent**: Once a credential is selected, the wallet app will handle user consent and authentication.
* **Result Display**: The app receives the response, validates it locally (simulated), parses the individual claims (e.g., Given Name, Email), and displays them in the UI.

## Architecture & Design

### Credential Manager Integration

Credential Manager calls are centralized in `CredentialManagerUtil.kt`. The app uses `GetDigitalCredentialOption` to pass JSON requests formatted according to the OpenID4VP specification.

### Data Requests

The `Requests.kt` file contains the logic for generating dynamic JSON requests. It supports:
- **DCQL Query Structure**: Simplified claim requests using paths.
- **Client Metadata**: Specifies supported algorithms for secure credential exchange.

### Parsing Logic

Since digital credentials come in various formats, the app includes robust parsing utilities:
- **CBOR Decoding**: A dedicated `Cbor.kt` utility handles the binary mDoc format, including manual unwrapping of Tag 24 items.
- **SD-JWT Parsing**: Logic to extract claims from Selective Disclosure JWTs by decoding individual disclosures.

### UI Layer

Built with **Jetpack Compose**, the UI follows a simple MVI-like pattern:
- `MainScreen.kt`: Handles the UI layout and interaction events.
- `MainViewModel.kt`: Manages the state of the credential request (Initial, Loading, Success, or Error).
- `MainUiState.kt`: Defines the data model for the UI states and extracted claims.

## License

This sample is distributed under the terms of the Apache License (Version 2.0). See the [LICENSE](LICENSE) file for more information.
