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
package com.example.android.authentication.myvault.data.room

import androidx.room.Embedded
import androidx.room.Relation
import com.example.android.authentication.myvault.data.PasskeyItem
import com.example.android.authentication.myvault.data.PasswordItem

/**
 * This class represents a site with all its associated credentials, including passwords and passkeys.
 */
data class SiteWithCredentials(
    @Embedded val site: SiteMetaData,
    @Relation(
        parentColumn = "id",
        entityColumn = "siteId",
    )
    val passwords: List<PasswordItem>,

    @Relation(
        parentColumn = "id",
        entityColumn = "siteId",
    )
    val passkeys: List<PasskeyItem>,
)
