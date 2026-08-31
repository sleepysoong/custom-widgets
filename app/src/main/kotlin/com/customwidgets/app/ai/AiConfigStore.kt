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
 * Manages secure persistence of OpenAI API configuration using EncryptedSharedPreferences.
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
            context.getSharedPreferences("${PREFS_NAME}_plain", Context.MODE_PRIVATE)
        }
    }

    fun loadConfig(): AiConfig {
        return AiConfig(
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            model = prefs.getString(KEY_MODEL, AiConfig.OPENAI_DEFAULT_MODEL) ?: AiConfig.OPENAI_DEFAULT_MODEL,
            temperature = prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE).toDouble(),
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS)
        )
    }

    fun saveConfig(newConfig: AiConfig) {
        prefs.edit()
            .putString(KEY_API_KEY, newConfig.apiKey)
            .putString(KEY_MODEL, newConfig.model)
            .putFloat(KEY_TEMPERATURE, newConfig.temperature.toFloat())
            .putInt(KEY_MAX_TOKENS, newConfig.maxTokens)
            .apply()

        _config.value = newConfig
    }

    companion object {
        private const val PREFS_NAME = "openai_secure_prefs"
        private const val KEY_API_KEY = "openai_api_key"
        private const val KEY_MODEL = "openai_model"
        private const val KEY_TEMPERATURE = "openai_temperature"
        private const val KEY_MAX_TOKENS = "openai_max_tokens"

        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_MAX_TOKENS = 2048
    }
}
