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
        homeViewModel.signOut {  }

        val state = homeViewModel.uiState.value
        assertThat(state.isSignedIn).isFalse()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
