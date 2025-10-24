# Shrine Sample App

This is the repository for the Credential Manager API code integration app,
also known as the **"Shrine"** app.

The Shrine app is a fully functional Android app built with Kotlin and Jetpack Compose.
This sample app is built to share a working sample of Credential Manager APIs in Android
and help visualize the workflow. This sample code is designed to help you understand the
workflow better and allow you to estimate the level of effort needed to incorporate
Credential Manager with your own apps.

## Features

This sample app implements the following use cases:

* Create an account using username and set the session using password.
* Generate a new passkey for an existing account.
* Create a passkey-only account using username.
* Store the credentials for created accounts in the user's Google Password Manager account.
* Sign in flow with passkeys and password support.
* Sign in flow with Sign in with Google.
* Sign in flow with restore credentials support.
* Passkey management in settings.
* Internal session management.

## Requirements

* Latest release of [Android Studio](https://developer.android.com/studio)
* Java 11 or higher

## Typical account creation and login flow

* Launch the app.
* Create an account with a passkey or password, or directly log in with your Google account
* When creating a passkey, you will need to use your lock screen credentials. This means you will
  need to enable a lock screen if you don't have one.
* Once an account is created, you can sign in with the method you chose.
* For passkey login, select the passkey from the list of passkeys options shown in the bottom sheet.
  For password login, select the respective password option shown in the bottom sheet.
* Manage your passkeys by going into settings.
* Log out of the application to restart the flow.
* [Optional] Create multiple accounts and switch accounts to test the implementation.

## Design

This section will cover the general components of the Shrine app. The typical app flow would look
like the following:

1. User interacts with a screen, i.e. to create a credential via the "Sign up" button on
   `RegisterScreen.kt`.
2. A callback is triggered, usually tied to a View Model function, i.e.
   `viewModel.onPasskeyRegister()`.
3. The View Model function calls the appropriate `AuthRepository.kt` function to send a request to
   the server, i.e. `repository.registerUsername()` to first register the username with the server.
4. `AuthRepository.kt` contains an injected `AuthApiService` instance which implements Retrofit
   calls to the Project Sesame (app) server. Server requests and responses are handled in
   `AuthRepository.kt` and then passed to the calling View Model. Internal session states are also
   updated as needed.
5. The View Model handles the returned response and makes other server calls as needed, i.e.
   `createPasskey()`. In this case, `createPasskey()` also contains a callback with
   `credentialManager.createCredential()` once initial server communication succeeds, via a
   `CredentialManagerUtils` object that was passed from the Screen.
6. The View Model updates the UI with success or failure, and navigates the user to the next screen
   as appropriate.

### Server

This app utilizes the server at https://project-sesame-426206.appspot.com/ which is specified in the
`app/build.gradle.kts` file. This server handles all requests related to passwords and passkeys (
creation, authentication, deletion), as well as Sign in with Google. You can see the implementation
of this server at https://github.com/GoogleChromeLabs/project-sesame. The app uses Retrofit to
handle web communications with the server, with endpoints specified in`api/AuthApiService.kt` and
called in `repository/AuthRepository.kt`.

### Screens

The frontend elements are contained in the various Screens in the `ui` package. Each Screen utilizes
a View Model that generally has the same name, with a few exceptions. These Screens primarily handle
frontend element placements and error message displays.

Some Screens utilize Credential Manager calls (i.e. `RegisterScreen.kt`). As a Credential Manager
object requires an Activity, the corresponding Context is passed in through the Screen to the
`CredentialManagerUtils.kt` file which creates and manages the actual Credential Manager object.

### View Models

The View Models generally manage sending, receiving, and processing responses between the Screens
and app server. Server communication is handled via an injected `AuthRepository` object. Uistate is
also updated in the View Model, which is in turn monitored and updated in the Screen.

### Credential Manager

Credential manager calls are made in `CredentialManagerUtils.kt`. The usage is fairly
straightforward, i.e. `.createCredential()` and `.getCredential()` for creating and retrieving
credentials, respectively. Please note the handling of exceptions, as some may be caused by fairly
common usage, i.e. user cancelling the selector.

### Requests and responses

The requests and responses sent and received from the server are defined in the `model` package.
These consist of various data classes that are defined based on the expected response from the
Project Sesame server, i.e. the data class `RegisterRequestResponse` defines fields expected from
server response from requesting passkey registration options. This is then used in `registerRequest`
in `AuthApiService.kt`, and the server's returned JSON is expected to have those fields. You can
verify this by looking at the logs in Logcat after making any server request.

## **License**

Shrine is distributed under the terms of the Apache License (Version 2.0). See the license for more
information.

**
