# Set Up Instructions
Assuming the GlitchMe site and server is up and running (the glitch me site can
be found [here](https://glitch.com/edit/#!/alder-sunny-bicycle)), this app should
be fairly easy to setup. There is one process that requires ensuring your app
signature is known to the server, which will be in the steps below.

1. Clone this repo anywhere on your machine. Ensure you have AndroidStudio and
   all the setup with that is correct (sdk/etc..).
2. Ensure your phone can connect to Android Studio. This often involves ensuring
   debug mode is on and adb is installed.
3. Double check there is a 'debug.keystore' file at the root. This is important
as the server expects your app to have the SHA256 from this keystore.
4. 'Run' the app via the 'run' button (the green play triangle).
5. Enjoy!

## Running your own server
If you'd like to manage a server directly, you can choose to Remix this project on glitch.me which
will clone the current project to management under your account. Note that you will likely need to
change the env variables to your app's package name and sha-256 hash, as well as update the 
expectedOrigin variables in server.mjs with your updated apk-key-hash (covert your app's sha-256 to
base 64).

# Notice
Please note that accounts created with this sample app are stored on glitch.me. While this does not
impact the security of your Google accounts, please be wary of putting sensitive information in your
username or password fields.

## Known Issues
- We have noticed the GlitchMe service sometimes has reliability issues.
  - See status [here](https://status.glitch.com/).