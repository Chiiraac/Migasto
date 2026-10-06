package com.chiiraac.migasto.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chiiraac.migasto.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Preferencias del dispositivo guardadas con DataStore. */
class UserPreferences(private val dataStore: DataStore<Preferences>) {

    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { ThemeMode.fromKey(it[THEME_MODE]) }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    /** Recibir los avisos generales de MiGasto (novedades, felicitaciones…). Activado por defecto. */
    val newsEnabled: Flow<Boolean> = dataStore.data
        .map { it[NEWS_ENABLED] ?: true }
        .distinctUntilChanged()

    suspend fun setNewsEnabled(enabled: Boolean) {
        dataStore.edit { it[NEWS_ENABLED] = enabled }
    }

    fun selectedGroupId(uid: String): Flow<String?> = dataStore.data
        .map { it[selectedGroupKey(uid)] }
        .distinctUntilChanged()

    suspend fun setSelectedGroupId(uid: String, groupId: String) {
        dataStore.edit { it[selectedGroupKey(uid)] = groupId }
    }

    data class LocalProfile(val uid: String, val name: String)

    val localProfile: Flow<LocalProfile?> = dataStore.data
        .map { prefs ->
            val uid = prefs[LOCAL_USER_ID]
            val name = prefs[LOCAL_USER_NAME]
            if (uid != null && name != null) LocalProfile(uid, name) else null
        }
        .distinctUntilChanged()

    suspend fun currentLocalProfile(): LocalProfile? = localProfile.first()

    suspend fun saveLocalProfile(profile: LocalProfile) {
        dataStore.edit {
            it[LOCAL_USER_ID] = profile.uid
            it[LOCAL_USER_NAME] = profile.name
        }
    }

    /** Borra todo excepto el tema elegido. */
    suspend fun clearAccountData() {
        dataStore.edit { prefs ->
            val theme = prefs[THEME_MODE]
            prefs.clear()
            if (theme != null) prefs[THEME_MODE] = theme
        }
    }

    private fun selectedGroupKey(uid: String) = stringPreferencesKey("selected_group_$uid")

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NEWS_ENABLED = booleanPreferencesKey("news_enabled")
        val LOCAL_USER_ID = stringPreferencesKey("local_user_id")
        val LOCAL_USER_NAME = stringPreferencesKey("local_user_name")
    }
}
