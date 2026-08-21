package com.gcap.client.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_CHAT_MODEL = stringPreferencesKey("default_chat_model")
        val DEFAULT_IMAGE_MODEL = stringPreferencesKey("default_image_model")
    }

    val apiKeyFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[API_KEY] ?: ""
    }

    fun getApiKey(): Flow<String> = apiKeyFlow

    suspend fun setApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey.trim()
        }
    }

    val themeModeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "system"
    }

    fun getThemeMode(): Flow<String> = themeModeFlow

    suspend fun setThemeMode(themeMode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode
        }
    }

    val defaultChatModelFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[DEFAULT_CHAT_MODEL] ?: "gemini-3.7-flash"
    }

    fun getDefaultChatModel(): Flow<String> = defaultChatModelFlow

    suspend fun setDefaultChatModel(model: String) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_CHAT_MODEL] = model
        }
    }

    val defaultImageModelFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[DEFAULT_IMAGE_MODEL] ?: "gemini-3.1-flash-image"
    }

    fun getDefaultImageModel(): Flow<String> = defaultImageModelFlow

    suspend fun setDefaultImageModel(model: String) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_IMAGE_MODEL] = model
        }
    }
}
