package com.google.android.gms.identity.sample.blockstore

import com.google.android.gms.auth.blockstore.BlockstoreClient
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesResponse
import com.google.android.gms.auth.blockstore.StoreBytesData
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * The Repository handles data operations and provides a clean API so that the rest of the app can
 * retrieve the Block Store data easily.
 */
class BlockStoreRepository @Inject constructor(
    private val blockStoreClient: BlockstoreClient
) {

    /** This function saves the authentication token to Block Store. */
    suspend fun storeBytes(inputString: String) {
        val data: StoreBytesData =
            StoreBytesData.Builder().setBytes(inputString.toByteArray()).build()
        blockStoreClient.storeBytes(data).await()
    }

    /** This function retrieves your Block Store data. */
    @Deprecated(message = "retrieveBytes() now requires argument RetrieveBytesRequest")
    suspend fun retrieveBytes(): String {
        return String(blockStoreClient.retrieveBytes().await())
    }

    /** This function retrieves your Block Store data with RetrieveBytesRequest. */
    suspend fun retrieveBytes(bytesRequest: RetrieveBytesRequest) {
        val response: Task<RetrieveBytesResponse?> = blockStoreClient
            .retrieveBytes(bytesRequest)
            .addOnSuccessListener {}
            .addOnFailureListener {}
    }

    /** This function clears your Block Store data. */
    suspend fun clearBytes() {
        storeBytes("")
    }
}
