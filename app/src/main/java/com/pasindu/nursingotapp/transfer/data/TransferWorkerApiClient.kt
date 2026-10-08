package com.pasindu.nursingotapp.transfer.data

import com.pasindu.nursingotapp.transfer.data.model.Decision
import com.pasindu.nursingotapp.transfer.data.model.DecisionRequest
import com.pasindu.nursingotapp.transfer.data.model.DecisionResponse
import com.pasindu.nursingotapp.transfer.data.model.WorkerMatchResponse
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult
import com.pasindu.nursingotapp.transfer.data.model.WorkerThreeWayMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Android client contract for the Cloudflare Worker Mutual Transfer matching service.
 */
interface TransferWorkerApiClient {
    /**
     * Triggers the Cloudflare Worker matching engine for the authenticated user.
     */
    suspend fun findAndLockMatch(firebaseIdToken: String): WorkerSyncResult

    /**
     * Send a decision (ACCEPT/REJECT) for a matched transfer.
     */
    suspend fun respondToMatch(firebaseIdToken: String, payload: DecisionRequest): DecisionResponse
}

/**
 * Standard Android HTTP client communicating with Cloudflare Worker.
 *
 * Uses built-in [HttpURLConnection] to avoid adding unneeded heavy networking dependencies.
 *
 * Invariants:
 * 1. Derives caller identity strictly on the server from the cryptographically verified JWT.
 * 2. Never passes a client-supplied UID in the request body or URL.
 * 3. Never logs the Firebase ID token.
 * 4. Sanitizes error responses.
 */
@Singleton
class HttpTransferWorkerApiClient @Inject constructor(
    @Named("transfer_worker_base_url") private val baseUrl: String
) : TransferWorkerApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun findAndLockMatch(firebaseIdToken: String): WorkerSyncResult =
        withContext(Dispatchers.IO) {
            require(firebaseIdToken.isNotBlank()) {
                "Firebase ID token must not be blank"
            }

            var connection: HttpURLConnection? = null
            try {
                val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
                val endpointUrl = URL("$cleanBaseUrl/api/matching/find-and-lock")

                connection = (endpointUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    doInput = true
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer ${firebaseIdToken.trim()}")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }

                // Send empty JSON body
                connection.outputStream.use { os ->
                    os.write("{}".toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }

                val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

                when (responseCode) {
                    200, 201 -> {
                        val response = runCatching {
                            json.decodeFromString<WorkerMatchResponse>(responseBody)
                        }.getOrElse { parseError ->
                            return@withContext WorkerSyncResult.NetworkError(
                                "Failed to parse Worker response: ${parseError.message}",
                                parseError
                            )
                        }

                        if (response.matched && response.matchId != null) {
                            val is3Way = response.matchType == "THREE_WAY" || response.threeWayMatch != null
                            if (is3Way) {
                                val match3Way = response.threeWayMatch ?: runCatching {
                                    json.decodeFromString<WorkerThreeWayMatch>(responseBody.let {
                                        val element = json.parseToJsonElement(it)
                                        element.toString()
                                    })
                                }.getOrNull()

                                // If threeWayMatch wasn't explicitly in threeWayMatch field, try decoding "match" as WorkerThreeWayMatch
                                val final3Way = match3Way ?: runCatching {
                                    val element = json.parseToJsonElement(responseBody)
                                    val matchElement = element.jsonObject["match"]
                                    if (matchElement != null) {
                                        json.decodeFromJsonElement<WorkerThreeWayMatch>(matchElement)
                                    } else null
                                }.getOrNull()

                                if (final3Way != null) {
                                    WorkerSyncResult.MatchFound(
                                        matchId = response.matchId,
                                        matchType = "THREE_WAY",
                                        threeWayMatch = final3Way,
                                        createdAt = response.createdAt ?: "",
                                        expiresAt = response.expiresAt ?: ""
                                    )
                                } else {
                                    WorkerSyncResult.NetworkError("Received THREE_WAY match response with invalid 3-way payload")
                                }
                            } else if (response.match != null) {
                                WorkerSyncResult.MatchFound(
                                    matchId = response.matchId,
                                    matchType = "DIRECT_2_WAY",
                                    directMatch = response.match,
                                    createdAt = response.createdAt ?: "",
                                    expiresAt = response.expiresAt ?: ""
                                )
                            } else {
                                WorkerSyncResult.NoMatch(
                                    response.message ?: "No compatible mutual transfer candidate found"
                                )
                            }
                        } else {
                            WorkerSyncResult.NoMatch(
                                response.message ?: "No compatible mutual transfer candidate found"
                            )
                        }
                    }

                    401 -> WorkerSyncResult.AuthError("Authentication failure: Invalid or expired ID token")
                    404 -> WorkerSyncResult.NoMatch("Transfer request not found on server")
                    409 -> WorkerSyncResult.Conflict("Match conflict or transfer request already locked on server")
                    else -> {
                        val errObj = runCatching {
                            json.decodeFromString<WorkerMatchResponse>(responseBody)
                        }.getOrNull()

                        val errorMsg = errObj?.message ?: errObj?.error ?: "Server error HTTP $responseCode"
                        WorkerSyncResult.NetworkError("Worker error (HTTP $responseCode): $errorMsg")
                    }
                }
            } catch (e: Exception) {
                WorkerSyncResult.NetworkError(
                    "Network connection error while contacting matching worker: ${e.message}",
                    e
                )
            } finally {
                connection?.disconnect()
            }
        }
    override suspend fun respondToMatch(firebaseIdToken: String, payload: DecisionRequest): DecisionResponse =
        withContext(Dispatchers.IO) {
            require(firebaseIdToken.isNotBlank()) { "Firebase ID token must not be blank" }
            require(payload.matchId.isNotBlank()) { "matchId must not be blank" }

            var connection: HttpURLConnection? = null
            try {
                val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
                val endpointUrl = URL("$cleanBaseUrl/api/matching/respond")
                connection = (endpointUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    doInput = true
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer ${firebaseIdToken.trim()}")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }

                // Send JSON payload
                val jsonBody = json.encodeToString(DecisionRequest.serializer(), payload)
                connection.outputStream.use { os ->
                    os.write(jsonBody.toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }
                val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

                if (responseCode !in 200..299) {
                    throw Exception("Server returned HTTP $responseCode: $responseBody")
                }

                json.decodeFromString(DecisionResponse.serializer(), responseBody)
            } catch (e: Exception) {
                throw Exception("Network error while sending decision: ${e.message}", e)
            } finally {
                connection?.disconnect()
            }
        }
}

