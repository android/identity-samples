# credentials-quickstart

## Get Started
This is a quickstart sample for the Android Credentials API, which is part of Smartlock for Passwords on Android.  Visit the following link for more information:

https://developers.google.com/identity/smartlock-passwords/android/get-started

## Optional
### Determining the SHA512 Hash of Your Keystore
This application demonstrates the use and verification of ID Tokens retrieved from Credentials which
is a great way to authenticate your client applications with your backend server. You will need to
determine the SHA512 hash of your application and update `MockServer.java` accordingly.

Run the quickstart and successfully load a credential.  Then in the `adb logcat` logs, search for
"IDToken Audience".  There should be a log message containing the audience of the ID Token in the
loaded credential which will have the following form:
```
android://<SHA512>@<PACKAGE_NAME>
```
Copy the SHA512 from that log message and update `MockServer.java`.

For example for this log message:
```
D/MockServer: IDToken Audience:android://_6Gx0cvSsFgTLF3NJCJIUs9BDaZMdSnXosAbPYYLiTqNWvR0IMc0C-UQehhmDu8t8l4fd3tEI6TlVCoqybFV5g==@com.google.example.credentialsbasic
```
The SHA512 hash is
```
_6Gx0cvSsFgTLF3NJCJIUs9BDaZMdSnXosAbPYYLiTqNWvR0IMc0C-UQehhmDu8t8l4fd3tEI6TlVCoqybFV5g==
```

