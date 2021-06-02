/*
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.example.credentialsbasic;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResponse;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.credentials.CredentialsOptions;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.auth.api.credentials.IdToken;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * A minimal example of saving and loading username/password credentials from the Credentials API.
 * @author samstern@google.com
 */
public class MainActivity extends AppCompatActivity implements
        View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_IS_RESOLVING = "is_resolving";
    private static final int RC_SAVE = 1;
    private static final int RC_HINT = 2;
    private static final int RC_READ = 3;

    private EditText mEmailField;
    private EditText mPasswordField;

    private CredentialsClient mCredentialsClient;
    private Credential mCurrentCredential;
    private boolean mIsResolving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        // Fields
        mEmailField = findViewById(R.id.edit_text_email);
        mPasswordField = findViewById(R.id.edit_text_password);

        // Buttons
        findViewById(R.id.button_save_credential).setOnClickListener(this);
        findViewById(R.id.button_load_credentials).setOnClickListener(this);
        findViewById(R.id.button_load_hint).setOnClickListener(this);
        findViewById(R.id.button_delete_loaded_credential).setOnClickListener(this);

        // Instance state
        if (savedInstanceState != null) {
            mIsResolving = savedInstanceState.getBoolean(KEY_IS_RESOLVING);
        }

        // Instantiate client for interacting with the credentials API. For this demo
        // application we forcibly enable the SmartLock save dialog, which is sometimes
        // disabled when it would conflict with the Android autofill API.
        CredentialsOptions options = new CredentialsOptions.Builder()
                .forceEnableSaveDialog()
                .build();
        mCredentialsClient = Credentials.getClient(this, options);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Attempt auto-sign in.
        if (!mIsResolving) {
            requestCredentials();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_RESOLVING, mIsResolving);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
        hideProgress();

        switch (requestCode) {
            case RC_HINT:
                // Drop into handling for RC_READ
            case RC_READ:
                if (resultCode == RESULT_OK) {
                    boolean isHint = (requestCode == RC_HINT);
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    processRetrievedCredential(credential, isHint);
                } else {
                    Log.e(TAG, "Credential Read: NOT OK");
                    showToast("Credential Read Failed");
                }
                
                mIsResolving = false;
                break;
            case RC_SAVE:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Credential Save: OK");
                    showToast("Credential Save Success");
                } else {
                    Log.e(TAG, "Credential Save: NOT OK");
                    showToast("Credential Save Failed");
                }

                mIsResolving = false;
                break;
        }
    }

    /**
     * Called when the save button is clicked.  Reads the entries in the email and password
     * fields and attempts to save a new Credential to the Credentials API.
     */
    private void saveCredentialClicked() {
        String email = mEmailField.getText().toString();
        String password = mPasswordField.getText().toString();

        // Create a Credential with the user's email as the ID and storing the password.  We
        // could also add 'Name' and 'ProfilePictureURL' but that is outside the scope of this
        // minimal sample.
        Log.d(TAG, "Saving Credential:" + email + ":" + anonymizePassword(password));
        final Credential credential = new Credential.Builder(email)
                .setPassword(password)
                .build();

        showProgress();

        // NOTE: this method unconditionally saves the Credential built, even if all the fields
        // are blank or it is invalid in some other way.  In a real application you should contact
        // your app's back end and determine that the credential is valid before saving it to the
        // Credentials backend.
        showProgress();

        mCredentialsClient.save(credential).addOnCompleteListener(
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            showToast("Credential saved.");
                            hideProgress();
                            return;
                        }

                        Exception e = task.getException();
                        if (e instanceof ResolvableApiException) {
                            // The first time a credential is saved, the user is shown UI
                            // to confirm the action. This requires resolution.
                            ResolvableApiException rae = (ResolvableApiException) e;
                            resolveResult(rae, RC_SAVE);
                        } else {
                            // Save failure cannot be resolved.
                            Log.w(TAG, "Save failed.", e);
                            showToast("Credential Save Failed");
                            hideProgress();
                        }
                    }
                });
    }

    /**
     * Called when the Load Credentials button is clicked. Attempts to read the user's saved
     * Credentials from the Credentials API.  This may show UX, such as a credential picker
     * or an account picker.
     *
     * <b>Note:</b> in a normal application loading credentials should happen without explicit user
     * action, this is only connected to a 'Load Credentials' button for easier demonstration
     * in this sample.  Make sure not to load credentials automatically if the user has clicked
     * a "sign out" button in your application in order to avoid a sign-in loop. You can do this
     * with the function <code>Auth.CredentialsApi.disableAuthSignIn(...)</code>.
     */
    private void loadCredentialsClicked() {
        requestCredentials();
    }

    /**
     * Called when the Load Hints button is clicked. Requests a Credential "hint" which will
     * be the basic profile information and an ID token for an account on the device. This is useful
     * to auto-fill sign-up forms with an email address, picture, and name or to do password-free
     * authentication with a server by providing an ID Token.
     */
    private void loadHintClicked() {
        HintRequest hintRequest = new HintRequest.Builder()
                .setHintPickerConfig(new CredentialPickerConfig.Builder()
                        .setShowCancelButton(true)
                        .build())
                .setIdTokenRequested(shouldRequestIdToken())
                .setEmailAddressIdentifierSupported(true)
                .setAccountTypes(IdentityProviders.GOOGLE)
                .build();

;
        PendingIntent intent = mCredentialsClient.getHintPickerIntent(hintRequest);
        try {
            startIntentSenderForResult(intent.getIntentSender(), RC_HINT, null, 0, 0, 0);
            mIsResolving = true;
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Could not start hint picker Intent", e);
            mIsResolving = false;
        }
    }

    /**
     * Request Credentials from the Credentials API.
     */
    private void requestCredentials() {
        // Request all of the user's saved username/password credentials.  We are not using
        // setAccountTypes so we will not load any credentials from other Identity Providers.
        CredentialRequest request = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setIdTokenRequested(shouldRequestIdToken())
                .build();

        showProgress();

        mCredentialsClient.request(request).addOnCompleteListener(
                new OnCompleteListener<CredentialRequestResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<CredentialRequestResponse> task) {
                        hideProgress();

                        if (task.isSuccessful()) {
                            // Successfully read the credential without any user interaction, this
                            // means there was only a single credential and the user has auto
                            // sign-in enabled.
                            processRetrievedCredential(task.getResult().getCredential(), false);
                            return;
                        }

                        Exception e = task.getException();
                        if (e instanceof ResolvableApiException) {
                            // This is most likely the case where the user has multiple saved
                            // credentials and needs to pick one. This requires showing UI to
                            // resolve the read request.
                            ResolvableApiException rae = (ResolvableApiException) e;
                            resolveResult(rae, RC_READ);
                            return;
                        }

                        if (e instanceof ApiException) {
                            ApiException ae = (ApiException) e;
                            if (ae.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
                                // This means only a hint is available, but we are handling that
                                // elsewhere so no need to act here.
                            } else {
                                Log.w(TAG, "Unexpected status code: " + ae.getStatusCode());
                            }
                        }
                    }
                });
    }

    /**
     * Called when the delete credentials button is clicked.  This deletes the last Credential
     * that was loaded using the load button.
     */
    private void deleteLoadedCredentialClicked() {
        if (mCurrentCredential == null) {
            showToast("Error: no credential to delete");
            return;
        }

        showProgress();

        mCredentialsClient.delete(mCurrentCredential).addOnCompleteListener(
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        hideProgress();

                        if (task.isSuccessful()) {
                            // Credential delete succeeded, disable the delete button because we
                            // cannot delete the same credential twice. Clear text fields.
                            showToast("Credential Delete Success");
                            ((EditText) findViewById(R.id.edit_text_email)).setText("");
                            ((EditText) findViewById(R.id.edit_text_password)).setText("");
                            mCurrentCredential = null;
                        } else {
                            // Credential deletion either failed or was cancelled, this operation
                            // never gives a 'resolution' so we can display the failure message
                            // immediately.
                            Log.e(TAG, "Credential Delete: NOT OK", task.getException());
                            showToast("Credential Delete Failed");
                        }
                    }
                });
    }

    /**
     * Attempt to resolve a non-successful result from an asynchronous request.
     * @param rae the ResolvableApiException to resolve.
     * @param requestCode the request code to use when starting an Activity for result,
     *                    this will be passed back to onActivityResult.
     */
    private void resolveResult(ResolvableApiException rae, int requestCode) {
        // We don't want to fire multiple resolutions at once since that can result
        // in stacked dialogs after rotation or another similar event.
        if (mIsResolving) {
            Log.w(TAG, "resolveResult: already resolving.");
            return;
        }
        
        Log.d(TAG, "Resolving: " + rae);
        try {
            rae.startResolutionForResult(MainActivity.this, requestCode);
            mIsResolving = true;
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "STATUS: Failed to send resolution.", e);
            hideProgress();
        }
    }

    /**
     * Process a Credential object retrieved from a successful request.
     * @param credential the Credential to process.
     * @param isHint true if the Credential is hint-only, false otherwise.
     */
    private void processRetrievedCredential(Credential credential, boolean isHint) {
        Log.d(TAG, "Credential Retrieved: " + credential.getId() + ":" +
                anonymizePassword(credential.getPassword()));

        // If the Credential is not a hint, we should store it an enable the delete button.
        // If it is a hint, skip this because a hint cannot be deleted.
        if (!isHint) {
            showToast("Credential Retrieved");
            mCurrentCredential = credential;
            findViewById(R.id.button_delete_loaded_credential).setEnabled(true);
        } else {
            showToast("Credential Hint Retrieved");
        }

        mEmailField.setText(credential.getId());
        mPasswordField.setText(credential.getPassword());

        if (!credential.getIdTokens().isEmpty()) {
            IdToken idToken = credential.getIdTokens().get(0);

            // For the purposes of this sample we are using a background service in place of a real
            // web server. The client sends the Id Token to the server which uses the Google
            // APIs Client Library for Java to verify the token and gain a signed assertion
            // of the user's email address. This can be used to confirm the user's identity
            // and sign the user in even without providing a password.
            Intent intent = new Intent(this, MockServer.class)
                    .putExtra(MockServer.EXTRA_IDTOKEN, idToken.getIdToken());
            startService(intent);
        } else {
            // This state is reached if non-Google accounts are added to Gmail:
            // https://support.google.com/mail/answer/6078445
            Log.d(TAG, "Credential does not contain ID Tokens.");
        }
    }

    /**
     * Determine if we should request an ID token with Hints/Credentials. The default behavior
     * is to not request an ID token (for speed purposes) but by setting this value to true
     * an ID token will be returned with Hints/Credentials when possible.
     */
    private boolean shouldRequestIdToken() {
        return ((CheckBox) findViewById(R.id.checkbox_request_idtoken)).isChecked();
    }

    /** Make a password into asterisks of the right length, for logging. **/
    private String anonymizePassword(String password) {
        if (password == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < password.length(); i++) {
            sb.append('*');
        }
        return sb.toString();
    }

    /** Display a short Toast message **/
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /** Show progress spinner and disable buttons **/
    private void showProgress() {
        findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);

        // Disable all buttons while progress indicator shows.
        setViewsEnabled(false, R.id.button_load_credentials, R.id.button_load_hint,
                R.id.button_save_credential, R.id.button_delete_loaded_credential);
    }

    /** Hide progress spinner and enable buttons **/
    private void hideProgress() {
        findViewById(R.id.progress_bar).setVisibility(View.INVISIBLE);

        // Enable buttons once progress indicator is hidden.
        setViewsEnabled(true, R.id.button_load_credentials, R.id.button_load_hint,
                R.id.button_save_credential);
    }

    /** Enable or disable multiple views **/
    private void setViewsEnabled(boolean enabled, int... ids) {
        for (int id : ids) {
            findViewById(id).setEnabled(enabled);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_save_credential:
                saveCredentialClicked();
                break;
            case R.id.button_load_credentials:
                loadCredentialsClicked();
                break;
            case R.id.button_load_hint:
                loadHintClicked();
                break;
            case R.id.button_delete_loaded_credential:
                deleteLoadedCredentialClicked();
                break;
        }
    }
}
