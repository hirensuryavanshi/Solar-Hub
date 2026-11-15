package com.example.solarhub.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.solarhub.model.UserRole
import com.example.solarhub.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.solarHubDataStore: DataStore<Preferences> by preferencesDataStore(name = "solar_hub_prefs")

class SessionManager(private val context: Context) {

  companion object {
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_NAME = stringPreferencesKey("user_name")
    private val KEY_MOBILE = stringPreferencesKey("user_mobile")
    private val KEY_ROLE = stringPreferencesKey("user_role")
    private val KEY_TOKEN = stringPreferencesKey("auth_token")
    private val KEY_LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    private val KEY_API_BASE_URL = stringPreferencesKey("api_base_url")
    private val KEY_IS_OFFLINE = stringPreferencesKey("is_offline_session")
  }

  val sessionFlow: Flow<UserSession?> = context.solarHubDataStore.data.map { prefs ->
    val token = prefs[KEY_TOKEN].orEmpty()
    val userId = prefs[KEY_USER_ID]
    val name = prefs[KEY_NAME]
    val isOffline = prefs[KEY_IS_OFFLINE] == "true"
    if (userId.isNullOrBlank() || name.isNullOrBlank() || (!isOffline && token.isBlank())) {
      null
    } else {
      val role = try {
        UserRole.valueOf(prefs[KEY_ROLE] ?: UserRole.EMPLOYEE.name)
      } catch (_: Exception) {
        UserRole.EMPLOYEE
      }
      UserSession(
        userId = userId,
        name = name,
        mobile = prefs[KEY_MOBILE] ?: "",
        role = role,
        token = token,
        isOfflineSession = isOffline
      )
    }
  }

  val apiBaseUrlFlow: Flow<String> = context.solarHubDataStore.data.map { prefs ->
    normalizeUrl(prefs[KEY_API_BASE_URL] ?: BuildConfig.DEFAULT_API_BASE_URL)
  }

  suspend fun getApiBaseUrl(): String = apiBaseUrlFlow.first()

  suspend fun setApiBaseUrl(url: String) {
    val clean = normalizeUrl(url)
    context.solarHubDataStore.edit { prefs ->
      prefs[KEY_API_BASE_URL] = clean
    }
  }

  suspend fun saveSession(session: UserSession) {
    context.solarHubDataStore.edit { prefs ->
      prefs[KEY_USER_ID] = session.userId
      prefs[KEY_NAME] = session.name
      prefs[KEY_MOBILE] = session.mobile
      prefs[KEY_ROLE] = session.role.name
      prefs[KEY_TOKEN] = session.token
      prefs[KEY_IS_OFFLINE] = if (session.isOfflineSession) "true" else "false"
      prefs[KEY_LAST_SYNC_AT] = 0L // Force fresh sync for newly logged-in user
    }
  }

  suspend fun clearSession() {
    context.solarHubDataStore.edit { prefs ->
      prefs.remove(KEY_USER_ID)
      prefs.remove(KEY_NAME)
      prefs.remove(KEY_MOBILE)
      prefs.remove(KEY_ROLE)
      prefs.remove(KEY_TOKEN)
      prefs.remove(KEY_IS_OFFLINE)
      prefs[KEY_LAST_SYNC_AT] = 0L
    }
  }

  suspend fun getLastSyncAt(): Long {
    return context.solarHubDataStore.data.first()[KEY_LAST_SYNC_AT] ?: 0L
  }

  suspend fun setLastSyncAt(timestamp: Long) {
    context.solarHubDataStore.edit { prefs ->
      prefs[KEY_LAST_SYNC_AT] = timestamp
    }
  }

  private fun normalizeUrl(url: String): String {
    val trimmed = url.trim().trimEnd('/')
    return if (trimmed.isBlank()) BuildConfig.DEFAULT_API_BASE_URL.trimEnd('/') else trimmed
  }
}
