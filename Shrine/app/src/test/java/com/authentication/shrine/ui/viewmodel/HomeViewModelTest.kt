/*
 * Copyright 2025 The Android Open Source Project
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
package com.authentication.shrine.ui.viewmodel

import com.authentication.shrine.repository.AuthenticationRepository
import com.authentication.shrine.repository.FakeAuthRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private lateinit var homeViewModel: HomeViewModel
    private val fakeAuthRepository: AuthenticationRepository = FakeAuthRepository()

    @Before
    fun setup() {
        homeViewModel = HomeViewModel(fakeAuthRepository)
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @Test
    fun `log the user out when signOut is called`() {
        homeViewModel.signOut { }

        val state = homeViewModel.uiState.value
        assertThat(state.isSignedIn).isFalse()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
