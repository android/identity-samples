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
package com.example.android.authentication.myvault.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a passkey item stored in the database.
 *
 * @property id The unique identifier
 * @property uid The user ID associated
 * @property username The username associated
 * @property displayName The display name
 * @property credId The credential ID
 * @property credPrivateKey The private key
 * @property siteId The ID of the site
 * @property lastUsedTimeMs The last time the passkey item was used
 */
@Entity(
    tableName = "passkeys",
    indices = [
        Index("credId", unique = false),
    ],
)
data class PasskeyItem(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "displayName") val displayName: String,
    @ColumnInfo(name = "credId") val credId: String,
    @ColumnInfo(name = "credPrivateKey") val credPrivateKey: String,
    @ColumnInfo(name = "siteId") val siteId: Long,
    @ColumnInfo(name = "lastUsedTimeMs") val lastUsedTimeMs: Long,
)
