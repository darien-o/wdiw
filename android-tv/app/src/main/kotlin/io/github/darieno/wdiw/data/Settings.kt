package io.github.darieno.wdiw.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class CountrySettings(val chosen: String?, val lastDetected: String?)

class Settings(private val context: Context) {
    val country: Flow<CountrySettings> =
        context.dataStore.data.map { CountrySettings(it[CHOSEN_COUNTRY], it[LAST_DETECTED_COUNTRY]) }

    /** Null goes back to automatic detection. */
    suspend fun chooseCountry(country: String?) {
        context.dataStore.edit {
            if (country == null) it.remove(CHOSEN_COUNTRY) else it[CHOSEN_COUNTRY] = country
        }
    }

    suspend fun rememberDetectedCountry(country: String) {
        context.dataStore.edit { it[LAST_DETECTED_COUNTRY] = country }
    }

    private companion object {
        val CHOSEN_COUNTRY = stringPreferencesKey("chosen_country")
        val LAST_DETECTED_COUNTRY = stringPreferencesKey("last_detected_country")
    }
}
