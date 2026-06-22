/*
 * Copyright 2024 The Android Open Source Project
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
package com.google.android.gms.identity.credentials.sample

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.text.Editable
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.identity.credentials.sample.databinding.ActivityMainBinding
import com.google.android.gms.identitycredentials.CredentialOption
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        lifecycleScope.launch {
            viewModel.state
                .collect { state ->
                    binding.buttonClearRegistry.isVisible = state.hasCredential
                    binding.buttonRegisterCredentials.isVisible = !state.hasCredential
                    if (state.hasCredential) {
                        binding.nameTextView.text.insert(0, state.credential.toString())
                    }
                }
        }

        binding.buttonRegisterCredentials.setOnClickListener {
            val credentialBytes = binding.nameTextView.text.toString().toByteArray()
            val matcherBytes = "".toByteArray()

            // IllegalArgumentException: Either type: default,
            // or requestType: default and protocolTypes: [] must be specified,
            // but all were blank, or for protocolTypes, empty or full of blank elements.
            viewModel.registerCredentials(
                credentialBytes,
                matcherBytes,
                "default",
                "",
                emptyList(),
                "1"
            )
        }

        binding.buttonGetCredential.setOnClickListener {
            val credentialOptions:  List<CredentialOption> = listOf()
            val data: Bundle = Bundle()
            val origin: String = ""
            val receiver: ResultReceiver = ResultReceiver(Handler(Looper.getMainLooper()))
            viewModel.launchCredentialSelector(credentialOptions,  data, origin, receiver)
        }

        binding.buttonClearRegistry.setOnClickListener {
            viewModel.clearRegistry()
        }
    }
}
