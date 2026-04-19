package me.bmax.apatch.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.bmax.apatch.apApp
import me.bmax.apatch.Natives
import me.bmax.apatch.ui.model.ApiMarketplaceItem
import me.bmax.apatch.ui.screen.BannerApiService
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.FolkApiClient
import okhttp3.Request
import org.json.JSONArray
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class ApiMarketplaceViewModel : ViewModel() {
    companion object {
        private const val TAG = "ApiMarketplaceViewModel"
        private const val MARKETPLACE_URL = "https://folk.mysqil.com/api/banners"
        private const val VERIFICATION_TIMEOUT_SECONDS = 10L
        private val VALID_IMAGE_TYPES = listOf("image/jpeg", "image/png", "image/webp", "image/gif", "image/bmp")
    }

    // Marketplace items state
    var items by mutableStateOf<List<ApiMarketplaceItem>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Verification state for applying API
    sealed class VerificationState {
        data object Idle : VerificationState()
        data object Loading : VerificationState()
        data class Success(val url: String) : VerificationState()
        data class Error(val message: String) : VerificationState()
    }

    var verificationState by mutableStateOf<VerificationState>(VerificationState.Idle)
        private set

    /**
     * Fetch marketplace items from the API
     */
    fun fetchMarketplaceItems() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            errorMessage = null
            try {
                val token = Natives.getApiToken(apApp)
                val url = "$MARKETPLACE_URL?token=$token"

                val result = FolkApiClient.fetchJson(url)
                val jsonString = result.getOrNull()
                if (jsonString != null) {
                    val jsonArray = JSONArray(jsonString)
                    val list = ArrayList<ApiMarketplaceItem>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            ApiMarketplaceItem(
                                name = obj.optString("name"),
                                url = obj.optString("url"),
                                description = obj.optString("description"),
                                descriptionEn = obj.optString("description_en")
                            )
                        )
                    }
                    items = list
                    Log.d(TAG, "Fetched ${list.size} marketplace items")
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Failed to fetch marketplace: ${exception?.message}")
                    errorMessage = "Error: ${exception?.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching marketplace", e)
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Verify and apply an API source
     * First verifies the API returns a valid image, then applies it
     */
    fun verifyAndApplyApi(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            verificationState = VerificationState.Loading
            Log.d(TAG, "Verifying API: $url")

            try {
                // Create a client with shorter timeout for verification
                val client = apApp.okhttpClient.newBuilder()
                    .connectTimeout(VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FolkPatch-BannerAPI/1.0")
                    .build()

                val response = client.newCall(request).execute()

                when {
                    !response.isSuccessful -> {
                        val errorMsg = "HTTP ${response.code}: ${response.message}"
                        Log.w(TAG, "API verification failed: $errorMsg")
                        verificationState = VerificationState.Error(errorMsg)
                    }

                    isValidImageResponse(response) -> {
                        Log.d(TAG, "API verification successful: $url")
                        // Apply to FolkBannerAPI (higher priority than user-configured API)
                        BackgroundConfig.setBannerApiSourceValue(url)
                        // Enable API mode if not enabled
                        BackgroundConfig.setBannerApiModeEnabledState(true)
                        // Persist the changes to SharedPreferences
                        BackgroundConfig.save(apApp.applicationContext)
                        // No need to clear cache - each API source has its own cache namespace
                        verificationState = VerificationState.Success(url)
                    }

                    else -> {
                        val errorMsg = "Invalid response: Not an image format"
                        Log.w(TAG, errorMsg)
                        verificationState = VerificationState.Error(errorMsg)
                    }
                }
            } catch (e: SocketTimeoutException) {
                val errorMsg = "Connection timeout: API did not respond in time"
                Log.e(TAG, errorMsg, e)
                verificationState = VerificationState.Error(errorMsg)
            } catch (e: UnknownHostException) {
                val errorMsg = "Network error: Unable to reach server"
                Log.e(TAG, errorMsg, e)
                verificationState = VerificationState.Error(errorMsg)
            } catch (e: Exception) {
                val errorMsg = "Verification failed: ${e.message}"
                Log.e(TAG, errorMsg, e)
                verificationState = VerificationState.Error(errorMsg)
            }
        }
    }

    /**
     * Check if the response is a valid image
     */
    private fun isValidImageResponse(response: okhttp3.Response): Boolean {
        val contentType = response.body?.contentType()?.toString()?.lowercase() ?: ""
        return VALID_IMAGE_TYPES.any { contentType.contains(it, ignoreCase = true) }
    }

    /**
     * Reset verification state
     */
    fun resetVerificationState() {
        verificationState = VerificationState.Idle
    }

    /**
     * Retry fetching marketplace items
     */
    fun retry() {
        fetchMarketplaceItems()
    }
}
