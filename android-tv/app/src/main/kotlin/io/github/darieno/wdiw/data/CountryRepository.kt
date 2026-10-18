package io.github.darieno.wdiw.data

import io.github.darieno.wdiw.domain.resolveCountry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/** Decides which country's catalogue to show. See [resolveCountry] for the order of preference. */
class CountryRepository(private val settings: Settings, api: Api, scope: CoroutineScope) {

    private sealed interface Detection {
        data object Pending : Detection
        data class Done(val country: String?) : Detection
    }

    private val detection = MutableStateFlow<Detection>(Detection.Pending)

    val saved = settings.country

    /** Null only during the first moments of a first run, while the network is asked where the TV is. */
    val country: StateFlow<String?> =
        combine(settings.country, detection) { saved, detection ->
            val detected = (detection as? Detection.Done)?.country
            // With nothing better to go on yet, wait for detection instead of showing one country and then another.
            if (saved.chosen == null && saved.lastDetected == null && detection is Detection.Pending) {
                null
            } else {
                resolveCountry(saved.chosen, detected, saved.lastDetected, Locale.getDefault().country)
            }
        }.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        scope.launch {
            val detected = withTimeoutOrNull(DETECTION_TIMEOUT_MS) { runCatching { api.geo() }.getOrNull() }
            if (detected != null) settings.rememberDetectedCountry(detected)
            detection.value = Detection.Done(detected)
        }
    }

    suspend fun choose(country: String?) = settings.chooseCountry(country)

    private companion object {
        const val DETECTION_TIMEOUT_MS = 4_000L
    }
}
