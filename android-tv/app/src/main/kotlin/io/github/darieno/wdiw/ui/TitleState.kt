package io.github.darieno.wdiw.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.darieno.wdiw.AppContainer
import io.github.darieno.wdiw.data.TitleDetails
import io.github.darieno.wdiw.data.TitleType
import io.github.darieno.wdiw.domain.OfferPlan
import io.github.darieno.wdiw.domain.OpenStep
import io.github.darieno.wdiw.domain.planOffers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

data class TitleContent(val details: TitleDetails, val plans: List<OfferPlan>)

class TitleState(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val type: TitleType,
    private val id: Int,
) {
    var content by mutableStateOf<Load<TitleContent>>(Load.Loading)
        private set

    init {
        load()
    }

    fun load() {
        content = Load.Loading
        scope.launch {
            content = try {
                val country = container.country.country.filterNotNull().first()
                val details = container.api.title(type, id, country, container.language)
                val registry = container.registry.get()
                val installed = withContext(Dispatchers.IO) { container.launcher.installedPackages(registry) }
                Load.Ready(TitleContent(details, planOffers(details, registry, installed)))
            } catch (e: IOException) {
                Load.Failed(e)
            }
        }
    }

    /** The step that started, or null when none could be. */
    fun open(plan: OfferPlan): OpenStep? = container.launcher.open(plan)
}
