![Credential Manager ]


Credentials Manager Sample App
==================

This is the repository for the Credential Manager api code integration. app. It is a **work in
progress** 🚧.

**Credential Manager Sample** is a fully functional Android app built entirely with Kotlin and
Jetpack Compose. This sample app is built to share a working sample of Credential Manager APIs in
Android and help visualize the workflow. The intention is to help developers understand the workflow
better and allow them to estimate the changes required in their app easily.

# Features

This sample app implements the following use cases :

* Create an account using username and set the session using password.
* Generate a new passkey for an account
* Store the credentials for created accounts on the device.
* Sign in Flow with passkeys support
* Logout from the account.

# Happy flow

* Launch the app
* Create an account by sending any username to the server.
* Sets session by sending Password in Step 2.
* Register user credentials using your fingerprint sensor.
* From the list of passkeys options shown in bottomsheet select the correct passkey option to login
  .
* Logout of the application and close the app
* [Optional] Create multiple accounts and switch accounts to test the implementation.

# Requirements

* Android Studio
* Java 11
* Clone your own Glitchme server from this
  link : https://glitch.com/edit/#!/credential-manager-app-test using Remix to edit and add package
  and SHA for your application.
* Use your own debug.keystore if needed for signed builds sample project -app testing

# How to setup your own Glitchme server 

The app sends requests to a server https://credential-manager-app-test.glitch.me/

You are going to work on your own version of the Glitch backend. The backend code uses your Android
package and SHA which needs to be updated on the server.

To create your own version of the Glitch backend. The backend code uses your Android
package and SHA which needs to be updated on the server.

* Go to the edit page of the website at https://glitch.com/edit/#!/credential-manager-app-test
* Find the "Remix to Edit" button at the top right corner. By pressing the button, you can "fork"
  the code and continue with your own version along with a new project URL.
* Copy the project name on top left (you may modify it as you want).
* Paste it to the .env file's HOSTNAME section in glitch.
* To use the API on an Android app, associate it with a website and share credentials between them.
  To do so, leverage the Digital Asset Links. You can declare associations by hosting a Digital
  Asset Links JSON file on your website, and adding a link to the Digital Asset Link file to your
  app's manifest. Host .well-known/assetlinks.json at your domain
* You can define an association between your app and the website by creating a JSON file and put it
  at .well-known/assetlinks.json. Luckily, we have a server code that displays assetlinks.json file
  automatically, just by adding following environment params to the .env file in glitch:
* ANDROID_PACKAGENAME: Package name of your app (com.example.android.fido2)
* ANDROID_SHA256HASH: SHA256 Hash of your signing certificate
* In order to get the SHA256 hash of your developer signing certificate, use the command below. The
  default password of the debug keystore is "android".
* $ keytool -exportcert -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore
* In sample app’s build gradle, under android {}, find buildConfigField for API_BASE_URL
  and resValue for  host and change this as per your new hostname you got after remixing the glitchMe server..


# Development Environment

This app uses the Gradle build system and can be imported directly into the latest stable version of
Android Studio (available [here](https://developer.android.com/studio)). The `debug`
build can be built and run using the default configuration.

# Integration Steps

## Initialization steps

Initialize your Credential Manager to use further for creating user credential and authentication
purposes.

Note : Wait for the Toast to appear for respose success/failure on eac step.

## How to create an account for username on server

* Click on the “Step 1: Send Username” button after adding a username/email in the 1st field. For
  the purpose of demonstration, the app and the server accept any username. Just type something. (
  call sendUsername() request from AuthRepository )
* Check username, create a new account if it doesn't exist.
* Set a `username` in the session.
* Wait for the toast to appear saying "Username verified successfully".
* If you don't see toast, check the logs for error.

## How to set a session on server

* Next, check “Step 2 : Send Password”. Above that, add any password (This is for demonstration
  purpose only). Type and send the request (call password() request from AuthRepository )
* This step is to demonstrate if developers want to do additional authentication (2FA). This step
  shows how 2FA can be done while using Passkeys for authentication.
* Verifies user credential and let the user sign-in.
* No preceding registration required.
* Wait for the toast to appear saying "Session-id stored successfully, Do register!"
* If you don't see toast, check the logs for error. 

## How to send Register Request to server and get req info to be passed to Create Passkey prompt

* Next, we need to register the passkey (credential). Inside AuthRepository.kt, you will find
  registerRequest.
* Once the request is sent from the client, this method calls the server API /auth/registerRequest.
  The API returns an ApiResult with all the PublicKeyCredentialCreationOptions that the client needs
  to generate a new credential.

## How to create a Passkey

* Next, you need to create Passkey with the response received from /registerRequest
* For that, you need to first parse the params as per needed for the Create Credentials call.  (Call
  “createPasskey()” from Auth.kt)
* Give users the choice to enroll a passkey and use it for re-authentication, by registering a user
  credential using a CreatePublicKeyCredentialRequest() object.
* This method will call “createCredential()l” from Credential Manager apis, which helps Register a
  user credential that can be used to authenticate the user to the app in the future.
* This method execution launches framework UI flows for a user to view their registration options,
  grant consent, etc.
* Use your fingerprint or other auth. Methods from your device to register.

## How to send Register Response back to server and register user credential on server

* After this, you need to call /registerResponse. This registerReponse method is called after the UI
  successfully generated a new credential, and we want to send it back to the server.
* Now you will use the response received from “ceatePasskey()” call, and pass back to your server.
* you now want to remember the ID of your local key so you can distinguish it from other keys
  registered on the server. In the PublicKeyCredential object, take its rawId property to be sent.
* The returned value contains a list of all the credentials registered on the server, including the
  new one.

## How to retrieve previously stored credentials for user’s account

You now have a credential registered on the app and the server. You can now use it to let the user
sign in.

* As similar to creating the passkey, Open AuthRepository.kt and check signinRequest. Here, we want
  to request the server and see if we can let the user sign in with Credential Manager Apis.This
  method takes sessionId and credentialId stored locally.
* Once the request is successful, you need to prompt the user with all the stored credentials for
  his account. Call “getPasskey()” from Auth.kt.
* This calls GetCredentialRequest():  Pass these registration options to this method . This method
  also takes a boolean isAutoSelectAllowed() which can be used if you want a credential entry to be
  automatically chosen if it is the only one, this is false by default.
* For now, Pass PublicKeyCredentialOptions as parameters to the above method to retrieve all
  credentials for this particular user. This takes requestJson which is already sent in required
  webauthn web json format.
* Next, call CredentialManager.getCredential() which launches the bottomsheet. This bottom sheet
  will show all the previously saved credentials and ask the user to provide consent.
* Users can select the credential to authenticate.

## How to send Signin Response back to server and authenticate the user

* Next you need to send the response received back to the server so that the server can authenticate
  the user.
* Call signinResponse from AuthRespostory.kt. Pass response and credentialed to the method as
  params.
* Once successful, the user has been signed in successfully. You can redirect them to the home
  screen.

# Specific use cases handling

* When there are no passkeys associated with accounts registered, the developer should catch the
  exception code and let the user know that first he needs to create a passkey before he tries to
  fetch.
* On Begin Sign In Failure:  16: Caller has been temporarily blocked due to too many canceled
  sign-in prompt - They are fido specific errors.
* On Begin Sign In Failure:  8: Unknown internal error. a) if the phone isn't setup properly with
  the google account . b) the passkey json is being created incorrectly, these errors should pop up.
* androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException: The
  incoming request cannot be validated - This means your application package id is not registered
  with your server. Validate this with your server code(instructions below).

## How to build a debug signed version
To build a debug signed version of this sample app, you need to update the API_BASE_URL  to "https://credential-manager-app-test.glitch.me/auth" and host field to "https://credman-glitch-sample.glitch.me" under buildConfigField in your app-> build.gradle


# License

****Credential Manager Sample**** is distributed under the terms of the Apache License (Version 2.0)
. See the
[license](LICENSE) for more information.
