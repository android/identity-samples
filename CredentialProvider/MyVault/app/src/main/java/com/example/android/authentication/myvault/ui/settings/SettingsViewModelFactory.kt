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
package com.example.android.authentication.myvault.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android.authentication.myvault.data.room.MyVaultDatabase

/**
 * This class is a factory for creating instances of the {@link SettingsViewModel} class.
 *
 * <p>This factory is used by the {@link ViewModelProvider} to create instances of the {@link
 * SettingsViewModel} class. The factory takes one parameter, {@code database}, which is used to
 * initialize the {@link SettingsViewModel} instance.
 */
class SettingsViewModelFactory(
    private val database: MyVaultDatabase,
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(database) as T
    }
}
