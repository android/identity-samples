package com.google.android.gms.identity.sample.blockstore

import android.content.ContentValues
import android.util.Log
import com.google.android.gms.auth.blockstore.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * The Repository handles data operations and provides a clean API so that the rest of the app can
 * retrieve the Block Store data easily.
 */
class BlockStoreRepository @Inject constructor(
    private val blockStoreClient: BlockstoreClient
) {
    private val key = BlockstoreClient.DEFAULT_BYTES_DATA_KEY

    // This function saves the authentication token to Block Store.
    suspend fun storeBytes(inputString: String) {
        val storeRequest: StoreBytesData = StoreBytesData.Builder()
            .setBytes(inputString.toByteArray())
            .setKey(key)
            .build()

        blockStoreClient.storeBytes(storeRequest)
            .addOnSuccessListener { result: Int ->
                Log.d(ContentValues.TAG, "Stored $result bytes")
            }.addOnFailureListener { e ->
                Log.e(ContentValues.TAG, "Failed to store bytes", e)
            }.await()
    }

    // This function retrieves your Block Store data.
    suspend fun retrieveBytes(): String {
        var bytes = ""
        val requestedKeys = listOf(key)
        val retrieveRequest = RetrieveBytesRequest
            .Builder()
            .setKeys(requestedKeys)
            .build()

        blockStoreClient.retrieveBytes(retrieveRequest)
            .addOnSuccessListener { result: RetrieveBytesResponse ->
                val blockstoreDataMap = result.blockstoreDataMap
                for ((key, value) in blockstoreDataMap) {
                    Log.d(ContentValues.TAG, String.format("Retrieved bytes %s associated with key %s.", String(value.bytes), key))
                    bytes = String(value.bytes)
                }
            }
            .addOnFailureListener { e: Exception? ->
                Log.e(ContentValues.TAG, "Failed to store bytes", e)
            }.await()

        return bytes
    }

    // This function clears your Block Store data.
    suspend fun clearBytes() {
        val requestedKeys = listOf(key)
        val retrieveRequest = DeleteBytesRequest.Builder().setKeys(requestedKeys).build()

        blockStoreClient.deleteBytes(retrieveRequest)
            .addOnSuccessListener { result: Boolean ->
                Log.d(ContentValues.TAG, "Any data found and deleted? $result")
            }.await()
    }
}