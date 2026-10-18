package io.github.darieno.wdiw.ui

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import io.github.darieno.wdiw.AppContainer
import io.github.darieno.wdiw.data.Summary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val cause: IOException) : Load<Nothing>
}

/** What the home screen is showing: the popular titles of the country, or the results of a search. */
data class HomeItems(val query: String, val country: String, val items: Load<List<Summary>>)

/** Lives as long as the activity so the query, the results and the scroll position survive a visit to a title. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeState(private val container: AppContainer, scope: CoroutineScope) {
    var query by mutableStateOf("")
        private set

    val gridState = LazyGridState()

    /** The title last opened from the grid, so focus can return to it. */
    var lastOpened: Summary? = null

    private val retries = MutableStateFlow(0)

    val content: StateFlow<HomeItems?> =
        combine(
            snapshotFlow { query.trim() }
                .debounce { if (it.isEmpty()) 0L else TYPING_PAUSE_MS }
                .distinctUntilChanged(),
            container.country.country.filterNotNull(),
            retries,
        ) { query, country, _ -> query to country }
            .flatMapLatest { (query, country) ->
                flow {
                    emit(HomeItems(query, country, Load.Loading))
                    val items = try {
                        val lang = container.language
                        val results =
                            if (query.isEmpty()) container.api.popular(country, lang)
                            else container.api.search(query, lang)
                        Load.Ready(results)
                    } catch (e: IOException) {
                        Load.Failed(e)
                    }
                    emit(HomeItems(query, country, items))
                }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    fun search(text: String) {
        if (text == query) return
        query = text
        lastOpened = null
        gridState.requestScrollToItem(0)
    }

    fun retry() {
        retries.value++
    }

    private companion object {
        const val TYPING_PAUSE_MS = 450L
    }
}
