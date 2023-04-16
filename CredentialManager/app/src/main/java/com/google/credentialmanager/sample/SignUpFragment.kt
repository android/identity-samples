/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.credentialmanager.sample

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.CreateCustomCredentialException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.credentialmanager.sample.databinding.FragmentSignUpBinding
import kotlinx.coroutines.launch
import java.security.SecureRandom

class SignUpFragment : Fragment() {

    private lateinit var mCredMan: CredentialManager
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private lateinit var listener: SignUpFragmentCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as SignUpFragmentCallback
        } catch (castException: ClassCastException) {
            /** The activity does not implement the listener.  */
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mCredMan = CredentialManager.create(requireActivity())

        //TODO : Add signUp onClick()

        binding.signUpWithPassword.setOnClickListener(signUpWithPassword())
    }

    private fun signUpWithPassword(): View.OnClickListener {
        return View.OnClickListener {
            binding.password.visibility = View.VISIBLE

            if (binding.username.text.isNullOrEmpty()) {
                binding.username.error = "User name required"
                binding.username.requestFocus()
            } else if (binding.password.text.isNullOrEmpty()) {
                binding.password.error = "Password required"
                binding.password.requestFocus()
            } else {
                lifecycleScope.launch {

                    configureViews(View.VISIBLE, false)

                    //TODO : Save the user credential password with their password provider

                    simulateServerDelayAndLogIn()

                }
            }
        }
    }

    private fun simulateServerDelayAndLogIn() {
        Handler(Looper.getMainLooper()).postDelayed({

            Utils.setSignedInThroughPasskeys(false)

            configureViews(View.INVISIBLE, true)

            listener.showHome()
        }, 2000)
    }

    private fun signUpWithPasskeys(): View.OnClickListener {
        return View.OnClickListener {

            binding.password.visibility = View.GONE

            if (binding.username.text.isNullOrEmpty()) {
                binding.username.error = "User name required"
                binding.username.requestFocus()
            } else {
                lifecycleScope.launch {
                    configureViews(View.VISIBLE, false)

                    //TODO : Call createPasskey() to sign up with passkey

                    configureViews(View.INVISIBLE, true)

                    //TODO : complete the registration process after sending public key credential to your server and let the user in

                }
            }
        }
    }

    private fun fetchRegistrationJsonFromServer(): String {

        //TODO fetch registration mock response

        return ""
    }

    private fun getEncodedUserId(): String {
        val random = SecureRandom()
        val bytes = ByteArray(64)
        random.nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
    }

    private suspend fun createPassword(): String {

        //TODO : CreatePasswordRequest with entered username and password

        //TODO : Create credential with created password request

        return ""
    }

    private suspend fun createPasskey(): CreatePublicKeyCredentialResponse? {
        var ret: CreatePublicKeyCredentialResponse? = null

        //TODO create a CreatePublicKeyCredentialRequest() with necessary registration json from server

        //TODO call createCredential() with createPublicKeyCredentialRequest

        return ret
    }

    private fun configureViews(visibility: Int, flag: Boolean) {
        configureProgress(visibility)
        binding.signUp.isEnabled = flag
        binding.signUpWithPassword.isEnabled = flag
    }

    private fun configureProgress(visibility: Int) {
        binding.textProgress.visibility = visibility
        binding.circularProgressIndicator.visibility = visibility
    }

    //Demonstration purpose : These are type of errors during passkey creation, handle this in your code wisely.
    private fun handleFailure(e: CreateCredentialException) {
        var msg = ""
        when (e) {
            is CreatePublicKeyCredentialDomException -> {
                // Handle the passkey DOM errors thrown according to the
                // WebAuthn spec using e.domError
                msg =
                    "An error occurred while creating a passkey, please check logs for additional details"
            }
            is CreateCredentialCancellationException -> {
                // The user intentionally canceled the operation and chose not
                // to register the credential.
                msg =
                    "The user intentionally canceled the operation and chose not to register the credential. , please check logs for additional details"
            }
            is CreateCredentialInterruptedException -> {
                msg =
                    "The operation was interrupted, please retry the call. Check logs for additional details"
                // Retry-able error. Consider retrying the call.
            }
            is CreateCredentialProviderConfigurationException -> {
                // Your app is missing the provider configuration dependency.
                // Most likely, you're missing the
                // "credentials-play-services-auth" module.
                msg =
                    "Your app is missing the provider configuration dependency. Check logs for additional details"
            }
            is CreateCredentialUnknownException -> {
                msg =
                    "An unknown error occurred while creating passkey. Check logs for additional details"
                Log.w("Auth", e.message.toString())
            }
            is CreateCustomCredentialException -> {
                // You have encountered an error from a 3rd-party SDK. If you
                // make the API call with a request object that's a subclass of
                // CreateCustomCredentialRequest using a 3rd-party SDK, then you
                // should check for any custom exception type constants within
                // that SDK to match with e.type. Otherwise, drop or log the
                // exception.
                msg =
                    "An unknown error occurred from a 3rd party SDK. Check logs for additional details"
            }
            else -> Log.w("Auth", "Unexpected exception type ${e::class.java.name}")
        }
        Log.e("Auth", " Exception Message" + e.message.toString())
        Utils.showErrorAlert(activity, msg)
    }

    private fun registerResponse(): Boolean {
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        configureProgress(View.INVISIBLE)
        _binding = null
    }

    interface SignUpFragmentCallback {
        fun showHome()
    }
}
