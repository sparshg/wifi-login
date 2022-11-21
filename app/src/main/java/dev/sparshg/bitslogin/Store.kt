package dev.sparshg.bitslogin

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Store(private val context: Context) {

    // to make sure there is only one instance
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")
        val CREDSET = booleanPreferencesKey("credSet")
        val QSADDED = booleanPreferencesKey("qsAdded")
        val SERVICE = booleanPreferencesKey("service")
        val REVIEW = longPreferencesKey("review")
    }

    val credSet: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CREDSET] ?: false
        }
    val qsAdded: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[QSADDED] ?: false
        }
    val service: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SERVICE] ?: false
        }
    val review: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[REVIEW] ?: System.currentTimeMillis()
        }

    suspend fun setCredSet(isSet: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CREDSET] = isSet
        }
    }
    suspend fun setQsAdded(isSet: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[QSADDED] = isSet
        }
    }
    suspend fun setService(service: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SERVICE] = service
        }
    }
    suspend fun setReview(review: Long) {
        context.dataStore.edit { preferences ->
            preferences[REVIEW] = review
        }
    }
}