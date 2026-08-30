package com.customwidgets.app.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.customwidgets.app.ai.model.AiConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages secure persistence of the AI API configuration using EncryptedSharedPreferences,
 * with graceful fallback to standard SharedPreferences if Keystore operations fail on certain devices.
 */
@Singleton
class AiConfigStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = createPreferences(context)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AiConfig> = _config.asStateFlow()

    private fun createPreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Fallback for emulators/devices with broken KeyStore
            context.getSharedPreferences("${PREFS_NAME}_plain", Context.MODE_PRIVATE)
        }
    }

    fun loadConfig(): AiConfig {
        return AiConfig(
            baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            model = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL,
            temperature = prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE).toDouble(),
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS)
        )
    }

    fun saveConfig(newConfig: AiConfig) {
        prefs.edit()
            .putString(KEY_BASE_URL, newConfig.baseUrl)
            .putString(KEY_API_KEY, newConfig.apiKey)
            .putString(KEY_MODEL, newConfig.model)
            .putFloat(KEY_TEMPERATURE, newConfig.temperature.toFloat())
            .putInt(KEY_MAX_TOKENS, newConfig.maxTokens)
            .apply()

        _config.value = newConfig
    }

    companion object {
        private const val PREFS_NAME = "ai_api_secure_prefs"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_MAX_TOKENS = "max_tokens"

        const val DEFAULT_BASE_URL = "https://api.openai.com"
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_MAX_TOKENS = 2048
    }
}
