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
import com.google.credentialmanager.sample.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    private lateinit var listener: MainFragmentCallback
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as MainFragmentCallback
        } catch (castException: ClassCastException) {
            /** The activity does not implement the listener.  */
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.signUp.setOnClickListener {
            listener.signup()
        }

        binding.signIn.setOnClickListener {
            listener.signIn()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface MainFragmentCallback {
        fun signup()
        fun signIn()
    }
}
