Android Identity Samples Repository
===================================

This repository contains a set of individual Android Studio projects to help you get
started writing/understanding Android identity features.

There are 2 main branches, `main` and `credman-compose`.

Branch `main` contains the primary samples for Credential Manager:
* `CredentialManager` - Simple app demonstrating how to use basic Credential Manager functions
* `CredentialProvider/MyVault` - Simple credential provider app implementation
* `SmsVerification` - Legacy sample using Play Service's SMS Verification APIs
* `WebView` - Contains 2 folders. `CredentialManagerWebView` contains code snippets to use
  Credential Manager with WebView. `WebkitWebView` contains a sample app that uses the Webkit
  library to implement passkey creation in WebView

Branch `credman-compose` notably contains 2 `Shrine` samples:
* `Shrine` is a more developed mobile app featuring best practices and recommended UX when 
using Credential Manager. The sample features multiple methods of sign-up, sign-in, settings 
configuration, and session management.
* `Shrinewear/appkt` is a Wear app that works together with the Shrine mobile app. It features
seamless authentication via passkey and Sign-in-with-Google logins.