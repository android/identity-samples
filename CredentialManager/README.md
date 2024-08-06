![Credential Manager ]

Credentials Manager Sample App
==================

This is the repository for the Credential Manager api code integration. app. It is a **work in
progress** 🚧.

**Credential Manager Sample** is a fully functional Android app built entirely with Kotlin. This
sample app is built to share a working sample of Credential Manager APIs in Android and help
visualize the workflow. The intention is to help developers understand the workflow better and allow
them to estimate the changes required in their app easily.

# Features

This sample app implements the following use cases :

* Create an account using passkey.
* Sign up using using username and password, store the credentials in Google Password manager using Credential Manager API for future authentication purpose.
* Store the credentials for created accounts on the device.
* Sign in Flow with passkeys support.
* Sign in Flow with saved password support.
* Sign in using username and password.
* Logout from the account.

# Happy flow

* Launch the app. 
* On Main screen, there are two buttons : Sign up and Sign In. 
* Open Sign up screen. 
* On Sign up screen, 1st option : Sign up with passkey.
* Register user credentials using your fingerprint sensor and log in.
* Logout of the application.
* On Main screen, Go to sign in.
* You can either sign in through passkeys or username & password. Please note this sample app uses a
  mock server so username & password are not being validated. You can do that at your backend before
  letting the user in.
* Logout of the application.
* On Main screen, Go to sign up.
* Sign up using username and password, store the credentials in Google Password manager using Credential Manager API for future authentication purpose.
* On Main screen, Go to sign in.
* Try sign in with username and password. Note : Sign in with username and password is for demonstration purpose, so no validation is done in this sample app and user will be let in the app.
* Logout of the application.

# Requirements

* Android Studio
* Java 11
* Kotlin plugin version : 1.8.10.
* The sample app uses Credential Manager API version 1.3.0-alpha01
* Android device running Android 5 or higher. 
* This sample app requires digital asset linking to a website for credential manager api to validate the linking and proceed further, so the rp id used in the mock responses is from a mocked 3P server(Glitch.me). if you want to try your own mock response, try adding your app domain and dont forget to complete the digital asset linking as mentioned [here](https://developer.android.com/training/sign-in/passkeys#add-support-dal). 
* Use the same debug.keystore mentioned in the sample project to build debug and release variants in order to verify the digital asset linking of sample app package name and sha on your mock server. This is already being done for you in build.gradle. 


# Development Environment

This app uses the Gradle build system and can be imported directly into the latest stable version of
Android Studio (available [here](https://developer.android.com/studio)). 

# Understand Integration flow

## Initialization steps

Credential Manager needs to be initialized to use further for creating user credential and
authentication purposes. (This is done already for sample project)

## How to sign up with a passkey

* You need to register the passkey (credential). For this sample app, you will see a mock response
  returned and passed to createPasskey. (check fetchRegistrationJsonFromServer where you read the mock response from RegFromServer.txt file from assets)
* This is the same response you will get once the request is sent from the client to server.
* The API returns an ApiResult string with all the PublicKeyCredentialCreationOptions that the client needs
  to generate a new credential.

* Note : These are the parameters sent to the server : 

```
     HTTP Headers :
     Method:post
     Content-Type:application/json;
     X-Requested-With:XMLHttpRequest

     Parameters(POST,application/json)
      {
       timeout:Number,
       authenticatorSelection:{
       authenticatorAttachment:('platform'|'cross-platform'),
       requireResidentKey:Boolean,
       userVerification:('required'|'preferred'|'discouraged')
       },
       attestation:('none'|'indirect'|'direct')
       }

     Cookies:connect.sid
```

* Next, you need to create Passkey with the response received. (mock response from
  fetchRegistrationJsonFromServer()
* Give users the choice to enroll a passkey and use it for re-authentication, by registering a user
  credential using a CreatePublicKeyCredentialRequest() object.
* This method will call “createCredential()l” from Credential Manager apis, which helps Register a
  user credential that can be used to authenticate the user to the app in the future.
* This method execution launches framework UI flows for a user to view their registration options,
  grant consent, etc.
* Use your fingerprint or other auth. Methods from your device to register.
* After this, send the response back to your server to complete the registration process which returns a flag indicating if the registration is successful. 

```
    HTTP Headers
    Method: post
    Content-Type: application/json;
    X-Requested-With: XMLHttpRequest // Simple CSRF protection

    Parameters (POST, application/json)
    {
     id: String,
    type: 'public-key',
    rawId: String,
    response: {
    clientDataJSON: String,
    attestationObject: String,
    signature: String,
    userHandle: String
    }
    }

    Cookies : connect.sid
```

* For this sample app, we are using mock server implementation so we just returned true from the
  server saying the credentials has been registered on server.

## How to sign in with a passkey

You now have a credential registered on the app and the server. You can now use it to let the user
sign in.

* As similar to creating the passkey,  we want to request the server for required information and see if we can let the user sign in with Credential Manager Api.
* For this sample app, you will see a mock response returned and passed to getPasskey. (check fetchAuthJsonFromServer where you read the mock response from AuthFromServer.txt file from assets)
* This is the same response you will get once the request is sent from the client to server.
* Next, you need to prompt the user with all the retrieved stored credentials for
  his account. Check the “getPasskey()” method to call getCredentials().
* This calls GetCredentialRequest(). This method
  also takes a boolean isAutoSelectAllowed() which can be used if you want a credential entry to be
  automatically chosen if it is the only one, this is false by default.
* Pass PasswordOption & PublicKeyCredentialOptions as parameters to the above method to retrieve all
  credentials for this particular user. This call takes requestJson (which is the mock response string) which is already sent in
  required webauthn web json format.
* Next, call CredentialManager.getCredential() which launches the bottomsheet. This bottom sheet
  will show all the previously saved credentials and ask the user to provide consent.
* Users can select the credential to authenticate.

* Note : These are the parameters sent to the server :

```
    HTTP Headers
    Method: post
    Content-Type: application/json;
    X-Requested-With: XMLHttpRequest // Simple CSRF protection

    Parameters (POST, application/json)
    {
        userVerification: ('required'|'preferred'|'discouraged')
    }

    Parameters (query)
    credId: the credential id to sign-in to.

    Cookies
    connect.sid
```

* Next you need to send the response received back to the server so that the server can authenticate
  the user

```
    HTTP Headers
    Method: post
    Content-Type: application/json;
    X-Requested-With: XMLHttpRequest // Simple CSRF protection

    Parameters (POST, application/json)
    {
      id: String,
      type: 'public-key',
      rawId: String,
    response: {
        clientDataJSON: String,
       authenticatorData: String,
       signature: String,
       userHandle: String
    }
    }

    Cookies : connect.sid
```

* For this sample app, we assume this to be true .
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

# License

****Credential Manager Sample**** is distributed under the terms of the Apache License (Version 2.0)
. See the
[license](LICENSE) for more information.
