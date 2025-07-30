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

import com.example.android.authentication.myvault.data.room.MyVaultDao
import com.example.android.authentication.myvault.data.room.SiteMetaData
import com.example.android.authentication.myvault.data.room.SiteWithCredentials
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * This class is responsible for providing utility methods for updating credentials in database
 */
class CredentialsDataSource(
    private val myVaultDao: MyVaultDao,
) {

    fun siteListWithCredentials(): Flow<List<SiteWithCredentials>> {
        return myVaultDao.siteListWithCredentials()
    }

    fun credentialsForSite(url: String?): SiteWithCredentials? {
        if (url == null) {
            return null
        }
        return myVaultDao.getCredentialsFromSite(url)
    }

    fun getPasskeysCount(siteId: String?): Int {
        if (siteId == null) {
            return 0
        }

        val credentialsFromSite = myVaultDao.getCredentialsFromSite(siteId)

        if (credentialsFromSite != null) {
            return credentialsFromSite.passkeys.size
        }

        return 0
    }

    fun getPasswordCount(url: String?): Int {
        if (url == null) {
            return 0
        }

        val credentialsFromSite = myVaultDao.getCredentialsFromSite(url)

        if (credentialsFromSite != null) {
            return credentialsFromSite.passwords.size
        }
        return 0
    }

    private suspend fun addSite(siteMetaData: SiteMetaData): Long {
        return myVaultDao.insertSite(siteMetaData)
    }

    suspend fun updatePassword(password: PasswordItem) {
        myVaultDao.updatePassword(password)
    }

    suspend fun updatePasskey(passkey: PasskeyItem) {
        myVaultDao.updatePasskey(passkey)
    }

    suspend fun removePassword(password: PasswordItem) {
        val siteId = password.siteId
        myVaultDao.deletePassword(password)
        if (myVaultDao.count(siteId) == 0) {
            myVaultDao.deleteSite(SiteMetaData(id = siteId))
        }
    }

    suspend fun removePasskey(passkey: PasskeyItem) {
        val siteId = passkey.siteId
        myVaultDao.deletePasskey(passkey)
        if (myVaultDao.countPasskeys(siteId) == 0) {
            myVaultDao.deleteSite(SiteMetaData(id = siteId))
        }
    }

    suspend fun addNewPassword(passwordMetaData: PasswordMetaData) {
        val site = myVaultDao.getSite(passwordMetaData.url)
        val siteId = site?.id ?: addSite(SiteMetaData(url = passwordMetaData.url, name = ""))
        myVaultDao.insertPassword(
            PasswordItem(
                username = passwordMetaData.username,
                password = passwordMetaData.password,
                siteId = siteId,
                lastUsedTimeMs = Instant.now().toEpochMilli(),
            ),
        )
    }

    suspend fun addNewPasskey(passkeyMetadata: PasskeyMetadata) {
        val site = myVaultDao.getSite(passkeyMetadata.rpid)
        val siteId = site?.id ?: addSite(SiteMetaData(url = passkeyMetadata.rpid, name = ""))
        myVaultDao.insertPasskey(
            PasskeyItem(
                uid = passkeyMetadata.uid,
                username = passkeyMetadata.username,
                displayName = passkeyMetadata.displayName,
                credId = passkeyMetadata.credId,
                credPrivateKey = passkeyMetadata.credPrivateKey,
                siteId = siteId,
                lastUsedTimeMs = Instant.now().toEpochMilli(),
            ),
        )
    }

    fun getPasskey(credId: String): PasskeyItem? {
        return myVaultDao.getPasskey(credId)
    }

    fun getPasskeyForUser(userId: String): List<PasskeyItem>? {
        return myVaultDao.getPasskeysForUser(userId)
    }

    suspend fun hidePasskey(passkey: PasskeyItem) {
        myVaultDao.updatePasskey(passkey.copy(hidden = true))
    }

    suspend fun unhidePasskey(passkey: PasskeyItem) {
        myVaultDao.updatePasskey(passkey.copy(hidden = false))
    }
}

data class PasswordMetaData(
    val username: String,
    val password: String,
    val url: String,
    val name: String = "",
    val lastUsedTimeMs: Long,
)

data class PasskeyMetadata(
    val uid: String,
    val rpid: String,
    val username: String,
    val displayName: String,
    val credId: String,
    val credPrivateKey: String,
    val lastUsedTimeMs: Long,
)
