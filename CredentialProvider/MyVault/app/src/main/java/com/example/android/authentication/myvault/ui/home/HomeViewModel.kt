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
package com.example.android.authentication.myvault.ui.home

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.authentication.myvault.data.CredentialsDataSource
import com.example.android.authentication.myvault.data.PasskeyItem
import com.example.android.authentication.myvault.data.PasswordItem
import com.example.android.authentication.myvault.data.RPIconDataSource
import com.example.android.authentication.myvault.data.room.SiteWithCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * This class is a ViewModel that holds the business logic to operate on a list of credentials.
 * @param credentialsDataSource The data source for credentials.
 * @param RPIconDataSource The data source for rpicons.
 */
class HomeViewModel(
    private val credentialsDataSource: CredentialsDataSource,
    private val RPIconDataSource: RPIconDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Removes the associated password from the database.
     *
     * @param password The password to remove.
     */
    fun onPasswordDelete(password: PasswordItem) {
        viewModelScope.launch {
            credentialsDataSource.removePassword(password)
        }
    }

    /**
     * Removes the associated passkey credential from the database.
     *
     * @param passkey The passkey credential to remove.
     */
    fun onPasskeyDelete(passkey: PasskeyItem) {
        viewModelScope.launch {
            credentialsDataSource.removePasskey(passkey)
        }
    }

    // Initialize the home screen with list of saved credentials from calling apps.
    init {
        viewModelScope.launch {
            credentialsDataSource.siteListWithCredentials().collect { siteList ->
                // Get the icons
                val icons: MutableMap<String, Bitmap> = mutableMapOf()
                siteList.forEach {
                    val icon = RPIconDataSource.getIcon(it.site.url)
                    if (icon != null) {
                        icons[it.site.url] = icon
                    }
                }
                _uiState.value = HomeUiState(siteList = siteList, iconMap = icons)
            }
        }
    }
}

data class HomeUiState(
    val siteList: List<SiteWithCredentials> = emptyList(),
    val iconMap: Map<String, Bitmap> = emptyMap(),
)
