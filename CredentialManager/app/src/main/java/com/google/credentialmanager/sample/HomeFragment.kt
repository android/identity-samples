/*
 * Copyright 2023 Google LLC
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.credentialmanager.sample.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var listener: HomeFragmentCallback

    companion object {
        private const val LOGGED_IN_THROUGH_PASSWORD = "Logged in successfully through password"
        private const val LOGGED_IN_THROUGH_PASSKEYS = "Logged in successfully through passkeys"
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as HomeFragmentCallback
        } catch (castException: ClassCastException) {
            /** The activity does not implement the listener.  */
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureSignedInText()

        binding.logout.setOnClickListener {
            listener.logout()
        }
    }

    private fun configureSignedInText() {
        if (DataProvider.isSignedInThroughPasskeys()) {
            binding.signedInText.text = LOGGED_IN_THROUGH_PASSKEYS
        } else {
            binding.signedInText.text = LOGGED_IN_THROUGH_PASSWORD
        }
    }

    interface HomeFragmentCallback {
        fun logout()
    }
}
