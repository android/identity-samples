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

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.example.android.authentication.myvault.data.PasskeyItem
import com.example.android.authentication.myvault.data.PasswordItem
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        SiteMetaData::class,
        PasswordItem::class,
        PasskeyItem::class,
    ],
    version = 8,
)
abstract class MyVaultDatabase : RoomDatabase() {
    abstract fun myVaultDao(): MyVaultDao
}

@Dao
interface MyVaultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(entity: SiteMetaData): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(entity: PasswordItem): Long

    @Update
    suspend fun updatePassword(entity: PasswordItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPasskey(entity: PasskeyItem): Long

    @Update
    suspend fun updatePasskey(entity: PasskeyItem)

    @Delete
    suspend fun deletePassword(entity: PasswordItem)

    @Delete
    suspend fun deletePasskey(entity: PasskeyItem)

    @Delete
    suspend fun deleteSite(entity: SiteMetaData)

    @Transaction
    @Query("SELECT * FROM sites ORDER BY url")
    fun siteListWithCredentials(): Flow<List<SiteWithCredentials>>

    @Query("SELECT * FROM sites WHERE url = :url")
    suspend fun getSite(url: String): SiteMetaData?

    @Query("SELECT COUNT(*) FROM sites WHERE url = :url")
    fun getSiteCount(url: String): Int?

    @Query("SELECT COUNT(*) FROM passwords WHERE siteId = :site")
    suspend fun count(site: Long): Int

    @Query("SELECT COUNT(*) FROM sites WHERE url = :site")
    fun countPasskeys(site: Long): Int

    @Transaction
    @Query("SELECT * FROM sites WHERE url = :url")
    fun getCredentialsFromSite(url: String): SiteWithCredentials?

    @Query("SELECT * from passkeys WHERE credId = :credId")
    fun getPasskey(credId: String): PasskeyItem?

    @Query("SELECT * from passkeys WHERE uid = :userId")
    suspend fun getAllPasskeysForUser(userId: String): List<PasskeyItem>?
}
