# Credential Manager Sample App

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
* Generate a new passkey for an existing account
* Store the credentials for created accounts in the user's Google Password Manager account.
* Sign in flow with passkeys support
* Sign in flow with restore credentials support
* Logout from the account.

## Requirements

* Latest release of [Android Studio](https://developer.android.com/studio)
* Java 11 or higher
* A web browser with the ability to access [Glitch](https://glitch.com/).

## Typical account creation and login flow

* Launch the app
* Create an account by sending any username to the server
* Set a session by sending a password in step 2
* Register user credentials using your fingerprint sensor
* From the list of passkeys options shown in the bottom sheet, select the correct passkey option to login
* Logout of the application and close the app
* [Optional] Create multiple accounts and switch accounts to test the implementation


## How to setup your own Glitch.me server

The Shrine app sends requests to a Glitch.me server, and out of the box this code example has been
configured to use a Glitch instance that we've created. To use your own Glitch-hosted backend,
follow these steps. The backend code uses your Android package and SHA fingerprint, and you will
update these on the server.

1.  Go to the edit page of the website at [https://glitch.com/edit/#!/credential-manager-app-test](https://glitch.com/edit/#!/credential-manager-app-test)

2.  Find the ***"Remix to Edit"*** button at the top right corner. By pressing the button, you can fork the code and continue this tutorial with your own version of the project and services.

3.  To use the API on an Android app, you need to associate it with a website and share credentials between them. To set this up, you'll use [Digital Asset Links](https://developer.android.com/training/sign-in/passkeys#add-support-dal). Digital Asset Links files are used to declare associations by hosting a JSON file on your website, and adding a link to this file to your app's manifest. Normally, you'll define an association between your app and the website by creating a JSON file and put it at `.well-known/assetlinks.json` on your HTTPS server. **For this demo, we have a server code that creates an `assetlinks.json` file automatically, just by adding the following environment params to the `.env` file in Glitch:**

  1. In the Glitch left nav Files section, click on the `.env` file. This opens up your project's Environment Config. Fill in the following values:

  2.  `HOSTNAME`: The name of your newly created Glitch service. The project name is found on top left of your Glitch project screen. It'll be something like `peaceful-banana-fern`. Paste or type in the name of your Glitch project into the `HOSTNAME` section.

  3.  `ANDROID_PACKAGENAME`: The package name of your app, such as `com.google.credentialmanager.sample`. You can find the package name in your project's app-level `build.gradle` file as the value of the `applicationId` property within the `android` block.

  4.  `ANDROID_SHA256HASH`: SHA-256 hash of your signing certificate. To get the SHA-256 hash of your developer signing certificate, use the following command: `keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore`. The default password of the debug keystore is "android". The SHA256 value appears under Certificate fingerprints. (`75:89:78:74:...`)


4.  In your `build.gradle`'s `android` block, find the fields for `buildConfigField` and `resValue`, and update the following values.

    1.  `buildConfigField / API_BASE_URL`: The URL of your new Glitch server's API. It'll be the full URL + /path appended to the end. For example: `https://peaceful-banana-fern.glitch.me/auth`

    2.   `resValue / host`: The root URL for your server. For example: [https://peaceful-banana-fern.glitch.me](https://peaceful-banana-fern.glitch.me)


5.  Sync your `build.gradle` changes by running **File > Sync Project with Gradle Files**.

6.  Test building your app. Run a physical or emulated device that has a valid and passkey-enabled Google account set up, then run your app on it. You should see the Shrine app home screen appear, with Sign In and Sign Up buttons. Don't click anything just yet, you'll do that in the next step.


## Integration

Follow these steps to test Credential Manager integration. In the app, look for a toast to appear to indicate a response success or failure on each step.

### Create an account for username on the server

1.  When your app runs the first time, you should see a screen with buttons for Sign In and Sign Up. Click the **Sign Up** button. The **Create Account** screen appears.

2.  Enter an email address and unique password and click the **Submit** button.

3.  You should now see a **Create a passkey** screen. Click **Create a passkey**.

4.  You should see a Google Password Manager bottom sheet appear, offering to save your credentials. Click **Continue**.

5.  The Shrine app should then show the **Create a passkey** screen. Click the **Create a passkey** button and a Google Password Manager bottom sheet should appear that offers to create a passkey for your app. Click **Continue**. You should now see the Shrine main menu.

6.  Click **Step 1: Send Username** after adding a username and email in the 1st field. For demo purposes, the app and the server will accept any username.

7.  Check the username, and create a new account if it doesn't exist.

8.  Set a `username` in the session.

9.  Wait for the toast to appear saying "Username verified successfully". If you don't see toast, check the logs for errors.


### **Set a session on server**

This step demonstrates if developers want to do additional authentication (2FA). This step shows how 2FA can be done while using passkeys for authentication.

1.  Check **Step 2: Send Password**. Above that field, add any password. Type and send the request.

2.  Verify the user credential and let the user sign-in. No preceding registration is required.

3.  Wait for the toast to appear that says "Session-id stored successfully, Do register!"

4.  If you don't see toast, check the logs for errors.


### **Pass required information to a passkey creation prompt**

This section describes how to send a registration request to the server and pass the required information to a passkey creation prompt.

1.  Register the passkey credential. Inside `AuthRepository.kt`, find `registerRequest`.

2.  Once the request is sent from the client, this method calls the server API `/auth/registerRequest`. The API returns an `ApiResult` with all the `PublicKeyCredentialCreationOptions` that the client needs to generate a new credential.


### **Create a passkey**

In this section, you'll create a passkey with the response received from `/registerRequest`.

1.  Parse the params as per needed for the create credentials call. Call `createPasskey()` from `Auth.kt`

2.  Give users the choice to enroll a passkey and use it for re-authentication by registering a user credential using a `CreatePublicKeyCredentialRequest()` object.

3.  This method calls `createCredential()` from *Credential Manager API*, which registers a user credential that can be used to authenticate the user to the app in the future. This method launches framework UI flows for a user to view their registration options, grant consent, etc.

4.  Use your fingerprint or other auth. Methods from your device to register.


### **Send the registration response and register a user credential**

This section describes how to send a registration response back to the server and register a user credential on a server.

-   Call `/registerResponse`. This `registerResponse` method is called after the user interface successfully generates a new credential, and you want to send it back to the server.

-   Use the response received from the `createPasskey()` call and pass it back to your server.

-   Remember the ID of your local key so you can distinguish it from other keys registered on the server. In the `PublicKeyCredential` object, use the `rawId` property.

-   The returned value contains a list of all the credentials registered on the server, including the new one.


### **How to retrieve previously stored credentials for user’s account**

You now have a credential registered on the app and the server. You can now use it to let the user sign in.

1.  Initiate a server check:

-   Open the file `AuthRepository.kt`.

-   Examine the `signinRequest` object.

-   Send a request to your server to confirm if Credential Manager APIs can be used for user sign-in.

-   Provide the `sessionId` and `credentialId` (which were stored locally) as data for this request.


2.  Prompt the user for stored credentials:

-   If the server request is successful, call the `getPasskey()` function from the `Auth.kt` file to display the user's stored credentials.

3.  Configure the retrieval request:

-   Create a `GetCredentialRequest()`.

-   Provide the previously created registration options to this request.

-   For the `isAutoSelectAllowed()` flag:


-   Set to `true` if you want a single stored credential to be automatically selected.

-   Set to `false` to require manual selection.


4.  Retrieve credentials:

-   Use `PublicKeyCredentialOptions` with the `GetCredentialRequest` to retrieve all the user's eligible credentials.

-   Ensure that the `requestJson` argument is in a valid WebAuthn JSON format.


5.  Display the credential selection interface:


-   Call `CredentialManager.getCredential()` to display a bottom sheet interface.

-   This UI displays a list of previously saved credentials. The user can then select a credential and provide consent to proceed with authentication.


### **Send a sign-in response and authenticate the user**

This section describes how to send a sign-in response to the server and authenticate your user.

1.  Call `signinResponse` from `AuthRespository.kt`. Pass the response and credential to the method as parameters.

2.  If successful, the user has been signed in and you can redirect them to the home screen.


### **Restore credentials of a returning user on a new device**

This section describes how to implement restore credentials

1. On a successful user authentication, create a Restore Key

   1. Call `AuthRepository`'s `registerPasskeyCreationRequest` method 
   
   2. With the PasskeyCreationRequest recieved from the above method, call `CredentialManagerUtils`'s `createRestoreKey` method 
   
   3. Then call `AuthRepository`'s `registerPasskeyCreationResponse` method


2. Once on a new device, check if there is any restore key present on the device or not (brought to the new device in the process of Backup and Restore)

   1. Call `AuthRepository`'s `signInWithPasskeysRequest` method 
   
   2. With the PasskeyCreationRequest recieved from the above method, call `CredentialManagerUtils`'s `getRestoreKey` method 
   
   3. If there is a RestoreKey present this will return a `GenericCredentialManagerResponse.GetPasskeySuccess` else this will return a `GenericCredentialManagerResponse.Error`


3. Sign in using the found Restore Key

   1. If a restore key is found in the above step, simply use it to sign-in using `AuthRepository`'s `signInWithPasskeysResponse` method


4. Delete a Restore Key

   1. If a user logs out of the app, make sure to clear the stored restore key by calling `CredentialManagerUtils`'s `deleteRestoreKey`


## **Specific use case handling**

-   When there are no passkeys associated with accounts registered, the developer should catch the exception code and let the user know that first he needs to create a passkey before they try to fetch it.

-   For Begin Sign In Failure: 16: Caller has been temporarily blocked due to too many canceled sign-in prompt errors: This is a FIDO-specific error.

-   For Begin Sign In Failure: 8: Unknown internal error. If the phone isn't set up properly with a Google account, the passkey JSON is being created incorrectly.

-   For `publickeycredential.CreatePublicKeyCredentialDomException`: The incoming request cannot be validated: This means your application package ID is not registered with your server. Validate this with your server code.


### **How to build a debug signed version**

To build a debug signed version of this sample app, you need to update the `API_BASE_URL` to `https://credential-manager-app-test.glitch.me/auth` and `host` field to `https://credman-glitch-sample.glitch.me` under `buildConfigField` in your `build.gradle` file.

## **License**

Shrine is distributed under the terms of the Apache License (Version 2.0). See the license for more information.

**
