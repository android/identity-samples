package com.credmanwebtest.webhandler

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject


/**
This web listener looks for the 'postMessage()' call on the javascript web code, and when it
receives it, it will handle it in the manner dictated in this local codebase. This allows for
javascript on the web to interact with the local setup on device that contains more complex logic.

The embedded javascript can be found in CredentialManagerWebView/javascript/encode.js.
It can be modified depending on the use case. If you wish to minify, please use the following command
to call the toptal minifier API.
```
cat encode.js | grep -v '^let __webauthn_interface__;$' | \
curl -X POST --data-urlencode input@- \
https://www.toptal.com/developers/javascript-minifier/api/raw | tr '"' "'" | pbcopy
```
pbpaste should output the proper minimized code. In linux, you may have to alias as follows:
```
alias pbcopy='xclip -selection clipboard'
alias pbpaste='xclip -selection clipboard -o'
```
in your bashrc.
 */
class PasskeyWebListener(
    private val activity: Activity,
    private val coroutineScope: CoroutineScope,
    private val credentialManagerHandler: CredentialManagerHandler
) : WebViewCompat.WebMessageListener {

    /** havePendingRequest is true if there is an outstanding WebAuthn request. There is only ever
    one request outstanding at a time.*/
    private var havePendingRequest = false

    /** pendingRequestIsDoomed is true if the WebView has navigated since starting a request. The
    fido module cannot be cancelled, but the response will never be delivered in this case.*/
    private var pendingRequestIsDoomed = false

    /** replyChannel is the port that the page is listening for a response on. It
    is valid if `havePendingRequest` is true.*/
    private var replyChannel: ReplyChannel? = null

    /** Called by the page when it wants to do a WebAuthn `get` or 'post' request. */
    @UiThread
    override fun onPostMessage(
        view: WebView,
        message: WebMessageCompat,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        replyProxy: JavaScriptReplyProxy,
    ) {
        Log.i(TAG, "In Post Message : $message source: $sourceOrigin");
        val messageData = message.data ?: return
        onRequest(messageData, sourceOrigin, isMainFrame, JavaScriptReplyChannel(replyProxy))
    }

    private fun onRequest(
        msg: String,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        reply: ReplyChannel,
    ) {
        msg.let {
            val jsonObj = JSONObject(msg);
            val type = jsonObj.getString(TYPE_KEY)
            val message = jsonObj.getString(REQUEST_KEY)

            if (havePendingRequest) {
                postErrorMessage(reply, "request already in progress", type)
                return
            }
            replyChannel = reply
            if (!isMainFrame) {
                reportFailure("requests from subframes are not supported", type)
                return
            }

            val originScheme = sourceOrigin.scheme
            if (originScheme == null || originScheme.lowercase() != "https") {
                reportFailure("WebAuthn not permitted for current URL", type)
                return
            }

            // Verify that origin belongs to your website,
            // it's because the unknown origin may gain credential info.
            if (isUnknownOrigin(originScheme)) {
                return
            }

            havePendingRequest = true
            pendingRequestIsDoomed = false

            // Let’s use a temporary “replyCurrent” variable  to send the data back, while resetting
            // the main “replyChannel” variable to null so it’s ready for the next request.
            val replyCurrent = replyChannel
            if (replyCurrent == null) {
                Log.i(TAG, "reply channel was null, cannot continue")
                return;
            }

            when (type) {
                CREATE_UNIQUE_KEY ->
                    this.coroutineScope.launch {
                        handleCreateFlow(credentialManagerHandler, message, replyCurrent)
                    }
                GET_UNIQUE_KEY -> this.coroutineScope.launch {
                    handleGetFlow(credentialManagerHandler, message, replyCurrent)
                }
                else -> Log.i(TAG, "Incorrect request json")
            }
        }
    }

    // Handles the get flow in a less error-prone way
    private suspend fun handleGetFlow(
        credentialManagerHandler: CredentialManagerHandler,
        message: String,
        reply: ReplyChannel,
    ) {
        try {
            havePendingRequest = false
            pendingRequestIsDoomed = false
            val r = credentialManagerHandler.getPasskey(message)
            val successArray = ArrayList<Any>();
            successArray.add("success");
            successArray.add(JSONObject(
                (r.credential as PublicKeyCredential).authenticationResponseJson))
            successArray.add(GET_UNIQUE_KEY);
            reply.send(JSONArray(successArray).toString())
            replyChannel = null // setting initial replyChannel for next request given temp 'reply'
        } catch (e: GetCredentialException) {
            reportFailure("Error: ${e.errorMessage} w type: ${e.type} w obj: $e", GET_UNIQUE_KEY)
        } catch (t: Throwable) {
            reportFailure("Error: ${t.message}", GET_UNIQUE_KEY)
        }
    }

    // handles the create flow in a less error prone way
    private suspend fun handleCreateFlow(
        credentialManagerHandler: CredentialManagerHandler,
        message: String,
        reply: ReplyChannel,
    ) {
        try {
            havePendingRequest = false
            pendingRequestIsDoomed = false
            val response = credentialManagerHandler.createPasskey(message)
            val successArray = ArrayList<Any>();
            successArray.add("success");
            successArray.add(JSONObject(response.registrationResponseJson));
            successArray.add(CREATE_UNIQUE_KEY);
            reply.send(JSONArray(successArray).toString())
            replyChannel = null // setting initial replyChannel for next request given temp 'reply'
        } catch (e: CreateCredentialException) {
            reportFailure("Error: ${e.errorMessage} w type: ${e.type} w obj: $e",
                CREATE_UNIQUE_KEY)
        } catch (t: Throwable) {
            reportFailure("Error: ${t.message}", CREATE_UNIQUE_KEY)
        }
    }

    /** Invalidates any current request.  */
    fun onPageStarted() {
        if (havePendingRequest) {
            pendingRequestIsDoomed = true
        }
    }

    /** Sends an error result to the page.  */
    private fun reportFailure(message: String, type: String) {
        havePendingRequest = false
        pendingRequestIsDoomed = false
        val reply: ReplyChannel = replyChannel!! // verifies non null by throwing NPE
        replyChannel = null
        postErrorMessage(reply, message, type)
    }

    private fun postErrorMessage(reply: ReplyChannel, errorMessage: String, type: String) {
        Log.i(TAG, "Sending error message back to the page via replyChannel $errorMessage");
        val array: MutableList<Any?> = ArrayList()
        array.add("error")
        array.add(errorMessage)
        array.add(type)
        reply.send(JSONArray(array).toString())
        var toastMsg = errorMessage
        Toast.makeText(this.activity.applicationContext,  toastMsg, Toast.LENGTH_SHORT).show()
    }

    private class JavaScriptReplyChannel(private val reply: JavaScriptReplyProxy) :
        ReplyChannel {
        override fun send(message: String?) {
            try {
                reply.postMessage(message!!)
            }catch (t: Throwable) {
                Log.i(TAG, "Reply failure due to: " + t.message);
            }
        }
    }

    /** ReplyChannel is the interface over which replies to the embedded site are sent. This allows
    for testing because AndroidX bans mocking its objects.*/
    interface ReplyChannel {
        fun send(message: String?)
    }

    companion object {
        /** INTERFACE_NAME is the name of the MessagePort that must be injected into pages. */
        const val INTERFACE_NAME = "__webauthn_interface__"

        const val CREATE_UNIQUE_KEY = "create"
        const val GET_UNIQUE_KEY = "get"
        const val TYPE_KEY = "type"
        const val REQUEST_KEY = "request"

        /** INJECTED_VAL is the minified version of the JavaScript code described at this class
         * heading. The non minified form is found at credmanweb/javascript/encode.js.*/
        const val INJECTED_VAL = """
            var __webauthn_interface__,__webauthn_hooks__;!function(e){console.log("In the hook."),__webauthn_interface__.addEventListener("message",function e(n){var r=JSON.parse(n.data),t=r[2];"get"===t?o(r):"create"===t?u(r):console.log("Incorrect response format for reply")});var n=null,r=null,t=null,a=null;function o(e){if(null!==n&&null!==t){if("success"!=e[0]){var r=t;n=null,t=null,r(new DOMException(e[1],"NotAllowedError"));return}var a=i(e[1]),o=n;n=null,t=null,o(a)}}function l(e){var n=e.length%4;return Uint8Array.from(atob(e.replace(/-/g,"+").replace(/_/g,"/").padEnd(e.length+(0===n?0:4-n),"=")),function(e){return e.charCodeAt(0)}).buffer}function s(e){return btoa(Array.from(new Uint8Array(e),function(e){return String.fromCharCode(e)}).join("")).replace(/\+/g,"-").replace(/\//g,"_").replace(/=+${'$'}/,"")}function u(e){if(null===r||null===a){console.log("Here: "+r+" and reject: "+a);return}if(console.log("Output back: "+e),"success"!=e[0]){var n=a;r=null,a=null,n(new DOMException(e[1],"NotAllowedError"));return}var t=i(e[1]),o=r;r=null,a=null,o(t)}function i(e){return console.log("Here is the response from credential manager: "+e),e.rawId=l(e.rawId),e.response.clientDataJSON=l(e.response.clientDataJSON),e.response.hasOwnProperty("attestationObject")&&(e.response.attestationObject=l(e.response.attestationObject)),e.response.hasOwnProperty("authenticatorData")&&(e.response.authenticatorData=l(e.response.authenticatorData)),e.response.hasOwnProperty("signature")&&(e.response.signature=l(e.response.signature)),e.response.hasOwnProperty("userHandle")&&(e.response.userHandle=l(e.response.userHandle)),e.getClientExtensionResults=function e(){return{}},e}e.create=function n(t){if(!("publicKey"in t))return e.originalCreateFunction(t);var o=new Promise(function(e,n){r=e,a=n}),l=t.publicKey;if(l.hasOwnProperty("challenge")){var u=s(l.challenge);l.challenge=u}if(l.hasOwnProperty("user")&&l.user.hasOwnProperty("id")){var i=s(l.user.id);l.user.id=i}var c=JSON.stringify({type:"create",request:l});return __webauthn_interface__.postMessage(c),o},e.get=function r(a){if(!("publicKey"in a))return e.originalGetFunction(a);var o=new Promise(function(e,r){n=e,t=r}),l=a.publicKey;if(l.hasOwnProperty("challenge")){var u=s(l.challenge);l.challenge=u}var i=JSON.stringify({type:"get",request:l});return __webauthn_interface__.postMessage(i),o},e.onReplyGet=o,e.CM_base64url_decode=l,e.CM_base64url_encode=s,e.onReplyCreate=u}(__webauthn_hooks__||(__webauthn_hooks__={})),__webauthn_hooks__.originalGetFunction=navigator.credentials.get,__webauthn_hooks__.originalCreateFunction=navigator.credentials.create,navigator.credentials.get=__webauthn_hooks__.get,navigator.credentials.create=__webauthn_hooks__.create,window.PublicKeyCredential=function(){},window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable=function(){return Promise.resolve(!1)};
        """
        const val TAG = "PasskeyWebListener"
    }

}
