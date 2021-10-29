package com.google.android.gms.identity.sample.blockstore

import com.google.android.gms.auth.blockstore.BlockstoreClient
import com.google.android.gms.auth.blockstore.StoreBytesData
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * The Repository handles data operations and provides a clean API so that the rest of the app can
 * retrieve the Block Store data easily.
 */
class BlockStoreRepository @Inject constructor(
    private val blockStoreClient: BlockstoreClient
) {

    // This function saves the authentication token to Block Store.
    suspend fun storeBytes(inputString: String) {
        val data: StoreBytesData =
            StoreBytesData.Builder().setBytes(inputString.toByteArray()).build()
        blockStoreClient.storeBytes(data).await()
    }

    // This function retrieves your Block Store data.
    suspend fun retrieveBytes(): String {
        return String(blockStoreClient.retrieveBytes().await())
    }

    // This function clears your Block Store data.
    suspend fun clearBytes() {
        storeBytes("")
    }
}