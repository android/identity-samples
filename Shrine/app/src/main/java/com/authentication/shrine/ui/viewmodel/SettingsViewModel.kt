package com.authentication.shrine.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.authentication.shrine.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
): ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    // TODO: Change the logic once we are able to fetch list of passkeys for a user
    init {
        viewModelScope.launch {
            _uiState.update {
                SettingsUiState(
                    userHasPasskeys = authRepository.isSignedInThroughPasskeys(),
                    userHasPassword = authRepository.isSignedInThroughPassword(),
                    username = authRepository.getUsername()
                )
            }
        }
    }
}

data class SettingsUiState(
    val userHasPasskeys: Boolean = true,
    val userHasPassword: Boolean = false,
    val username: String = "",
    val noOfPasskeys: Int = 3,
    val aaguidList: List<String> = listOf(),
    val passwordChanged: String = "Jan 1, 2025",
)
