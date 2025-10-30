# Wear OS Shrine Sample

The Wear OS Shrine Sample provides a look at the standard Credential Manager feature set in a
functioning Wear OS app.

## Functionality

A Credential Manager Wear OS implementation will have the following functionalities:

* Authenticate with unified methods: Passkeys, Passwords, Sign in With Google
* Authenticate with fallback Methods
   * This sample demonstrates fallback methods: OAuth, Legacy Sign in With Google

## Limitations

Wear OS Credential Manager has the following limitations:

* Cannot create passkeys on Wear, must use companion mobile device
* Restore Credentials is not supported
* Does not work on Wear 3.0
* Hybrid flows not supported

## Prerequisites

* Android Studio with the latest SDK and build tools
* A Wear OS emulator or physical device on Wear 4.0 or higher
* Created and Stored credentials synced by the mobile Shrine app-- see 'Syncing' and 'Creating Testing Credentials'
* Your Credential provider (e.g. Google Password Manager) on Wear must be **the same** as the one 
  you used to create credentials for your app on another device type (of course if you are using
  a third-party provider, they will need to have implemented a Wear OS app).
* A device lock (pin or pattern) in order to use passkeys

## Usage

This sample demonstrates the flow of signing in with Credential manager.

If a user dismisses Credential Manager, the sample falls back to the Wear OS legacy authentication
methods.

Once a user is authenticated, a screen will be shown to log out and restart the process.

### Pre-reading: Syncing

Since Wear OS cannot yet create credentials, any Wear OS app implementing Credential
Manager must rely on credentials created and synced from companion apps on other device types with
Credential Manager integrations in order to sign in.  

While Credential Providers like Google Password Manager are partially responsible for storing and syncing
these credentials from other other devices, implementing apps also need to participate.

Passwords, federated identity tokens like Sign in with Google, and the public halves of
passkeys must be stored and synced in an app credentials server implemented and owned by you, the 
Credential Manager implementor.  Sign in with Google Credentials also require that you set up a 
Google Cloud Console project, see instructions here: 
https://developer.android.com/identity/sign-in/credential-manager-siwg

This app sample is linked to a sample live credentials server to store and sync credentials.  It
will work with any Credential Provider out of the box for passwords and passkeys, but requires a
separate cloud console for Sign in with Google, as noted above.

You may want to remind users to verify their provider if their attempts to login result in
an empty credentials list, see
[NoCredentialsException.](https://developer.android.com/reference/kotlin/androidx/credentials/exceptions/NoCredentialException)

### Prerequisite: Creating Testing Credentials

Follow these instructions to create credentials that you can use for testing using an Android phone
or emulator that runs Android 13+ with the mobile Shrine sample, and a Wear OS device or emulator
running Wear OS 4.0 or greater.

1. [Mobile] Setup google account.
2. [Mobile] Settings->Passwords, passkeys & autofill, confirm preferred service is
   “Google password Manager”, or your preferred provider.
3. [Mobile] Open Shrine app, Click create account and follow instructions. Each account must
   have a unique username. 
4. [Mobile] When prompted, save password to “Google password manager”, and create a passkey
5. [Mobile] Transfer the same google account on your phone to the watch via the phone Wear OS app.
6. [Wear OS] Install the Shrine wear app.
7. [Wear OS] Confirm that Google Password Manager (or the provider you chose on mobile)
   is chosen as the default provider in the passwords settings menu.

### Instructions

1. Launch the application on your Wear OS device or emulator.
2. Tap the checkmark to dismiss the intro, or just wait for the Credential Selector screen to launch.
3. Interact with Credential selector screen
   a. Try Passkey login, inputting device lock to authenticate passkey signing.
   b. Try Passwords and Sign in with Google.
   b. Tap Sign-in Options to see a list of all usable credentials. Try them all.
4. Fall back to legacy auth methods
   a. Scroll to the bottom of the Credential selector screen and tap "Dismiss"
   b. You will be redirected to a screen with the legacy Wear OS authentication methods, OAuth and
   Sign in with Google (standalone button).
   c. Try them both.
5. Tap "Logout" when finished.

## OAuth Instructions

The sample demonstrates a device authorization grant OAuth 2.0 flow between a Wear OS app and the
Android Wear companion app. The sample uses the Google OAuth 2.0 server, but you can use any OAuth
server instead.

### Device Authorization Grant OAuth ([RFC 8628](https://datatracker.ietf.org/doc/html/rfc8628)]

The flow starts by tapping the "authenticate" button in the Wear OS app. This retrieves a
verification URL from the OAuth server, and opens this URL on the paired phone. It then continues to
poll the OAuth server for OAuth tokens. After the user accepts and provides consent, the OAuth
tokens are filled, and the result is used to make an authenticated call to the Google OAuth 2.0 API.

### Getting Started

1. Download and install the latest Android Wear companion app

2. In the Google API console, select an existing or create a new project and register it as an OAuth
   2.0 client.

   * Follow the instructions for
     ["TV and limited input"](https://developers.google.com/identity/protocols/oauth2/limited-input-device#creatingcred).
   * Make sure to register https://wear.googleapis.com/3p_auth/<package_name> as the redirect URI
     for your client for this sample to work.

3. Update the AuthViewModel.kt file to include the client_id and client_secret from the Google API
   project you previously selected to configure your OAuth client.

### Special Notes

In this sample, the Wear app makes direct HTTP calls to retrieve auth tokens, and thus the client_id
and client_secret are both exposed in the Wear APK.

This IS NOT the standard practice. Instead, you would normally work with an intermediary server.
That server in turn will call the OAuth server to authenticate. This way, the client id and secret
are stored on your intermediary server.

For the simplicity of this sample, the watch is handling the token exchange.

## Support

Stack Overflow: https://stackoverflow.com/questions/tagged/wear-os

If you've found an error in this sample, please file an issue:
https://github.com/android/wear-os-samples

Patches are encouraged, and may be submitted by forking this project and submitting a pull request
through GitHub. Please see CONTRIBUTING.md for more details.