Android Identity Samples Repository
===================================

This repository contains a set of individual Android Studio projects to help you get
started writing/understanding Android identity features.

There are two main branches, `main` and `credman_codelab`.

Branch `main` contains the primary samples for Credential Manager:
* `CredentialManager` - Simple app demonstrating how to use basic Credential Manager functions
* `Shrine` is a more developed mobile app featuring best practices and recommended UX when
  using Credential Manager. The sample features multiple methods of sign-up, sign-in, settings
  configuration, and session management.
* `Shrine/wear` is a Wear app that works together with the Shrine mobile app. It features
  seamless authentication via passkey, password, and Sign-in-with-Google logins.
* `CredentialProvider/MyVault` - Simple credential provider app implementation
* `SmsVerification` - Legacy sample using Play Service's SMS Verification APIs
* `WebView` - Contains 2 folders. `CredentialManagerWebView` contains code snippets to use
  Credential Manager with WebView. `WebkitWebView` contains a sample app that uses the Webkit
  library to implement passkey creation in WebView

Branch `credman_codelab` contains the starting codebase for the
[Credential Manager Codelab](https://codelabs.developers.google.com/credential-manager-api-for-android#0)
in the `CredentialManager` app.

