package com.accu.data.repositories

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.navHistoryDataStore by preferencesDataStore("nav_history")

@Singleton
class NavigationHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val HISTORY_KEY = stringPreferencesKey("route_history")
    private val MAX_ENTRIES = 5

    private val excludedRoutes = setOf(
        "dashboard", "onboarding",
    )

    val historyFlow: Flow<List<String>> = context.navHistoryDataStore.data.map { prefs ->
        prefs[HISTORY_KEY]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun record(rawRoute: String) {
        val route = rawRoute.substringBefore("/").trim()
        if (route.isBlank() || route in excludedRoutes) return
        context.navHistoryDataStore.edit { prefs ->
            val current = prefs[HISTORY_KEY]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val deduped = listOf(route) + current.filter { it != route }
            prefs[HISTORY_KEY] = deduped.take(MAX_ENTRIES).joinToString(",")
        }
    }

    suspend fun clear() {
        context.navHistoryDataStore.edit { it.remove(HISTORY_KEY) }
    }
}
