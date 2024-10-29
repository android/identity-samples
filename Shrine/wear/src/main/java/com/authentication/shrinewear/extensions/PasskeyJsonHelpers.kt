package com.authentication.shrinewear.extensions

import android.util.Base64
import android.util.JsonReader
import android.util.JsonWriter
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType.PUBLIC_KEY
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringWriter

/**
 * Extension function for [JsonWriter] to write an object value.
 *
 * @param body A lambda expression that writes to the [JsonWriter].
 */
internal fun JsonWriter.writeObject(body: JsonWriter.() -> Unit) {
    beginObject()
    body()
    endObject()
}

/**
 * Creates a JSON request body from the given lambda expression.
 *
 * @param body A lambda expression that writes to the [JsonWriter].
 * @return A [RequestBody] containing the JSON data.
 */
internal fun createJSONRequestBody(body: JsonWriter.() -> Unit): RequestBody {
    val output = StringWriter()
    JsonWriter(output).use { writer ->
        writer.writeObject(body)
    }
    return output.toString().toRequestBody("application/json".toMediaTypeOrNull())
}

/**
 * Parses a public key credential request options object from a JSON response.
 *
 * @param responseBody The JSON response body.
 * @return A [JSONObject] containing the parsed public key credential request options.
 */
internal fun parsePublicKeyCredentialRequestOptions(
    responseBody: ResponseBody,
): JSONObject {
    val credentialRequestOptions = JSONObject()
    JsonReader(responseBody.byteStream().bufferedReader()).use { jsonReader ->
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "challenge" -> credentialRequestOptions.put(
                    "challenge",
                    jsonReader.nextString(),
                )

                "userVerification" -> jsonReader.skipValue()
                "allowCredentials" -> credentialRequestOptions.put(
                    "allowCredentials",
                    parseCredentialDescriptors(jsonReader),
                )

                "rpId" -> credentialRequestOptions.put("rpId", jsonReader.nextString())
                "timeout" -> credentialRequestOptions.put("timeout", jsonReader.nextDouble())
            }
        }
        jsonReader.endObject()
    }
    return credentialRequestOptions
}

/**
 * Creates an OkHttp [RequestBody] for a passkey sign-in validation request.
 *
 * This function parses the raw JSON response from the Android Credential Manager
 * and formats it into the specific JSON structure required by the server's
 * WebAuthn sign-in endpoint.
 *
 * @param passkeyResponseJSON The raw passkey data as a JSON string from the
 * Credential Manager API.
 * @return A [RequestBody] containing the formatted JSON data for the server.
 */
internal fun createPasskeyValidationRequest(passkeyResponseJSON: String): RequestBody {
    val signedPasskeyData = JSONObject(passkeyResponseJSON)
    val response = signedPasskeyData.getJSONObject("response")
    val credentialId = signedPasskeyData.getString("rawId")

    return createJSONRequestBody {
        name("id").value(credentialId)
        name("type").value(PUBLIC_KEY.toString())
        name("rawId").value(credentialId)
        name("response").writeObject {
            name("clientDataJSON").value(response.getString("clientDataJSON"))
            name("authenticatorData").value(response.getString("authenticatorData"))
            name("signature").value(response.getString("signature"))
            name("userHandle").value(response.getString("userHandle"))
        }
    }
}

/**
 * Parses the `credentialDescriptors` array from the JSON response.
 *
 * @param jsonReader The JSON reader.
 * @return A [JSONArray] containing the parsed `credentialDescriptors`.
 */
private fun parseCredentialDescriptors(
    jsonReader: JsonReader,
): JSONArray {
    val jsonArray = JSONArray()
    jsonReader.beginArray()
    while (jsonReader.hasNext()) {
        val jsonObject = JSONObject()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "id" -> jsonObject.put("id", b64Decode(jsonReader.nextString()))
                "type" -> jsonReader.skipValue()
                "transports" -> jsonReader.skipValue()
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        if (jsonObject.length() != 0) {
            jsonArray.put(jsonObject)
        }
    }
    jsonReader.endArray()
    return jsonArray
}

private fun b64Decode(str: String): ByteArray {
    return Base64.decode(str, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE)
}