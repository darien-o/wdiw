package io.github.darieno.wdiw.domain

import io.github.darieno.wdiw.data.Offer
import io.github.darieno.wdiw.data.Provider
import io.github.darieno.wdiw.data.Registry
import io.github.darieno.wdiw.data.TitleDetails
import java.net.URLEncoder

/** One way of getting the viewer to the title. Steps are tried in order until one starts. */
sealed interface OpenStep {
    val packageName: String

    /** Opens the exact title inside the provider's app. */
    data class TitleLink(
        override val packageName: String,
        val uri: String,
        val extras: Map<String, String>,
    ) : OpenStep

    /** Opens the provider's own search for the title. */
    data class SearchLink(
        override val packageName: String,
        val uri: String,
        val extras: Map<String, String>,
    ) : OpenStep

    /** Opens the provider's app on its home screen. */
    data class Launch(override val packageName: String) : OpenStep
}

/** What the app can do about one offer on this device. */
data class OfferPlan(
    val offer: Offer,
    /** Null when the registry does not know the provider. */
    val provider: Provider?,
    /** Empty when the provider's app is not installed (or not known). */
    val steps: List<OpenStep>,
) {
    val isInstalled: Boolean get() = steps.isNotEmpty()
}

fun expand(template: String, id: String? = null, query: String? = null): String {
    var uri = template
    if (id != null) uri = uri.replace("{id}", URLEncoder.encode(id, "UTF-8"))
    if (query != null) uri = uri.replace("{query}", URLEncoder.encode(query, "UTF-8"))
    return uri
}

/**
 * Registry links come from a file anyone can propose changes to, so only plain https links to a host are
 * ever opened. A file:, content: or javascript: link, or one without a host, is skipped.
 */
fun isAllowedLink(uri: String): Boolean {
    if (!uri.startsWith("https://", ignoreCase = true)) return false
    val host = uri.substring("https://".length).takeWhile { it != '/' && it != '?' && it != '#' }
    return host.isNotEmpty() && host.all { it.isLetterOrDigit() || it == '.' || it == '-' }
}

fun planOffer(offer: Offer, title: TitleDetails, registry: Registry, installed: Set<String>): OfferPlan {
    val provider = registry.providerFor(offer.providerId)
    val packageName = provider?.packages?.firstOrNull { it in installed }
        ?: return OfferPlan(offer, provider, emptyList())

    val steps = buildList {
        for (link in provider.titleLinks) {
            if (link.types != null && title.type !in link.types) continue
            val id = title.ids[link.idSource] ?: continue
            val uri = expand(link.uri, id = id)
            if (isAllowedLink(uri)) add(OpenStep.TitleLink(packageName, uri, link.extras))
        }
        provider.searchLink?.let {
            val uri = expand(it.uri, query = title.title)
            if (isAllowedLink(uri)) add(OpenStep.SearchLink(packageName, uri, it.extras))
        }
        add(OpenStep.Launch(packageName))
    }
    return OfferPlan(offer, provider, steps)
}

/**
 * Plans every offer and puts the most useful first: apps already on the device before ones that
 * are not, and within each group watching for free or with a subscription before paying.
 * The proxy's order (TMDB's display priority) breaks ties.
 */
fun planOffers(title: TitleDetails, registry: Registry, installed: Set<String>): List<OfferPlan> =
    title.offers
        .map { planOffer(it, title, registry, installed) }
        .sortedWith(
            compareByDescending<OfferPlan> { it.isInstalled }
                .thenByDescending { plan -> plan.offer.kinds.any { it.isStreaming } },
        )
