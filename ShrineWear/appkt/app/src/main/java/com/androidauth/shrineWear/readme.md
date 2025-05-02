# Wear OS Shrine Sample

The Wear OS Shrine Sample provides an look at how different features are integrated into a 
functioning Wear app.

Current functionality:
  * Credential Manager Authentication API
    * Passkeys, Passwords, Sign in With Google
    * Fallback Methods: OAuth, Legacy Sign in With Google

## Credential Manager

The Credential Manager API integration demonstrates the flow of signing in with Credential manager.

If a user dismisses Credential Manager, the sample falls back to the Wear OS legacy authentication
methods.

Once a user is authenticated, a screen will be shown to log out and restart the process.

### How it Works: Credential Syncing

Since Wear OS cannot yet create passkeys or password, any Wear OS app implementing Credential
Manager must rely on passkeys and passwords synced from other devices in order to sign in. 
Credential Manager's Sign in With Google integration does not have this dependency.

In other words, in order to provide passkeys and passwords on Wear OS, you must have a Credential
Manager implementation on a device type which allows users to create credentials (probably
mobile). 

Credential Providers like Google Password Manager are mostly responsible for storing and syncing
these credentials from other other devices. The public half of passkey pairs, however, can only be
stored and synced by you, the Credential Manager integrator.  We recommend an encrypted credentials
server for this purpose, see [authenticationServer::loginWithPasskey](AuthenticationServer.kt#L68).

To use a credential created on another device, your provider on Wear must be **the same** as the one
you used to create credentials for your app on another device type.

You may want to remind users to verify their provider if their attempts to login result in
an empty credentials list, see
[NoCredentialsException.](https://developer.android.com/reference/kotlin/androidx/credentials/exceptions/NoCredentialException)

### Prerequisites

* Android Studio with the latest SDK and build tools.
* A Wear OS emulator or physical device on Wear 5.1 or higher.
* Created and Stored credentials synced by the mobile Shrine app. Instructions next section.

### Creating Testing Credentials

Follow these instructions to create credentials that you can use for testing using an Android phone
that runs Android 13+ with the mobile Shrine sample.

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

### Usage

1. Launch the application on your Wear OS device or emulator.
2. Tap 'Login'
3. Interact with Credential Manager
   a. Try Passkey login, inputting device credentials to authenticate passkey signing.
   b. Tap Sign-in Options to see a list of all usable credentials. Try them all.
4. Fall back to legacy auth methods
   a. After tapping 'login' on the splash page, scroll to the botton of the Credential Manager UI
   and tap "Dismiss"
   b. You will be redirected to a screen with the legacy Wear OS authentication methods, OAuth and
   Sign in with Google (standalone button).
   c. Try them both.
5. Tap "Logout" when finished.