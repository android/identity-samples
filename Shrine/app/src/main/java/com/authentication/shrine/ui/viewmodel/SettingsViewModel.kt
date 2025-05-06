package com.authentication.shrine.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.authentication.shrine.model.PasskeyCredential
import com.authentication.shrine.model.PasskeysList
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

    init {
        viewModelScope.launch {
            getPasskeysList()
        }
    }

    fun getPasskeysList() {
        _uiState.update {
            SettingsUiState(isLoading = true)
        }

        viewModelScope.launch {
            val data = authRepository.getListOfPasskeys()
            if (data != null) {
                _uiState.update {
                    SettingsUiState(
                        userHasPasskeys = data.credentials.isNotEmpty(),
                        username = "", // Set the username from Datastore
                        passkeysList = data.credentials,
                        // passwordChanged = Fetch from datastore
                    )
                }
            } else {
                _uiState.update {
                    SettingsUiState(
                        errorMessage = "Some error occurred while getting the list of passkeys"
                    )
                }
            }
        }
    }
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val userHasPasskeys: Boolean = true,
    val username: String = "",
    val passkeysList: List<PasskeyCredential> = listOf(),
    val passwordChanged: String = "Jan 1, 2025",
    val errorMessage: String = ""
)
