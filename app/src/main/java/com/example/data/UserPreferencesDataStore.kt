package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.noticeBoardDataStore by preferencesDataStore(name = "notice_board_prefs")

class UserPreferencesDataStore(context: Context) {
    private val dataStore = context.applicationContext.noticeBoardDataStore

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACTIVE_INSTITUTION_ID = stringPreferencesKey("active_institution_id")
        val DISMISSED_EMERGENCY_KEYS = stringSetPreferencesKey("dismissed_emergency_keys")
        val MUTED_CATEGORIES = stringSetPreferencesKey("muted_categories")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.fromName(prefs[Keys.THEME_MODE])
    }

    val activeInstitutionId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_INSTITUTION_ID]
    }

    val dismissedEmergencyKeys: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[Keys.DISMISSED_EMERGENCY_KEYS] ?: emptySet()
    }

    val mutedCategories: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[Keys.MUTED_CATEGORIES] ?: emptySet()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setActiveInstitutionId(institutionId: String?) {
        dataStore.edit { prefs ->
            if (institutionId.isNullOrBlank()) {
                prefs.remove(Keys.ACTIVE_INSTITUTION_ID)
            } else {
                prefs[Keys.ACTIVE_INSTITUTION_ID] = institutionId
            }
        }
    }

    /**
     * Stores "noticeId:revisionNumber" so dismissed popups do not reappear
     * unless a renewed critical revision is published (Section 18).
     */
    suspend fun dismissEmergencyBanner(noticeId: String, revisionNumber: Long) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.DISMISSED_EMERGENCY_KEYS]?.toMutableSet() ?: mutableSetOf()
            current.add("$noticeId:$revisionNumber")
            prefs[Keys.DISMISSED_EMERGENCY_KEYS] = current
        }
    }

    suspend fun toggleCategoryNotification(category: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.MUTED_CATEGORIES]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(category)) current.remove(category) else current.add(category)
            prefs[Keys.MUTED_CATEGORIES] = current
        }
    }
}
