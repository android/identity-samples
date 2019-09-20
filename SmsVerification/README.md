# SMS Verify App

This sample demonstrates the use of Play Service's SMS Verification APIs to get
access to specially-tagged text messages (the tags associate the message with
the APK) without needing full SMS retrieval permission (SMS_READ).

## Configuration

To build the application, you will need to acquire or generate at least three
(potentially four), files. These files are not distributed with this source as
they contain sensitive keys, etc.

`./app/google-services.json` (configuration for Google API client).
([Online tool to generate
`google-services.json`](https://developers.google.com/mobile/add?platform=android&cntapi=signin&cnturl=https:%2F%2Fdevelopers.google.com%2Fidentity%2Fsign-in%2Fandroid%2Fsign-in%3Fconfigured%3Dtrue&cntlbl=Continue%20Adding%20Sign-In).
The package name is `com.google.samples.smartlock.sms_verify`.)

`./app/src/main/res/values/sensitive.xml` (configures
the HTTP endpoint the app uses to send the SMS). The format is:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
  <string name="server_client_secret">0000000000</string>
  <string name="server_client_secret_v8">0000000000</string>
  <string name="url_verify">https://your-server.com/api/verify</string>
  <string name="url_request">https://your-server.com/api/request</string>
  <string name="url_reset">https://your-server.com/api/reset</string>
</resources>
```

`./debug.keystore` (keystore used to sign the APK).

`./appengine/credentials.py` (configures the SMS itself).
(Optional—this is only necessary if deploying/changing the appengine server-side
component.) Use the script `sms-verification/bin/sms_retriever_hash_v9.sh` to
generate the hash if required:

```sh
$ ./bin/sms_retriever_hash_v9.sh --package com.google.samples.smartlock.sms_verify --keystore sms-verification/android/debug.keystore 
```

These three files have dependencies on each other; they together ensure that the
text messages sent by the server can be read by Google Play Services.
