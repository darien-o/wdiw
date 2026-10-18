package io.github.darieno.wdiw.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Shared parser. Unknown keys are ignored so older apps keep working with newer proxy and registry data. */
val AppJson = Json { ignoreUnknownKeys = true }

@Serializable
enum class TitleType {
    @SerialName("movie") MOVIE,
    @SerialName("tv") TV;

    val path: String get() = if (this == MOVIE) "movie" else "tv"

    companion object {
        fun fromPath(path: String?): TitleType? = entries.firstOrNull { it.path == path }
    }
}

@Serializable
data class Summary(
    val id: Int,
    val type: TitleType,
    val title: String,
    val year: Int? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val rating: Double? = null,
)

@Serializable
data class SummaryList(val results: List<Summary>, val country: String? = null)

@Serializable
enum class OfferKind {
    @SerialName("flatrate") SUBSCRIPTION,
    @SerialName("free") FREE,
    @SerialName("ads") ADS,
    @SerialName("rent") RENT,
    @SerialName("buy") BUY;

    /** True when watching costs nothing beyond a subscription the viewer may already have. */
    val isStreaming: Boolean get() = this == SUBSCRIPTION || this == FREE || this == ADS
}

@Serializable
data class Offer(
    val providerId: Int,
    val name: String,
    val logo: String? = null,
    val kinds: List<OfferKind> = emptyList(),
)

@Serializable
data class TitleDetails(
    val id: Int,
    val type: TitleType,
    val title: String,
    val originalTitle: String = "",
    val year: Int? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val rating: Double? = null,
    val overview: String = "",
    val runtime: Int? = null,
    val genres: List<String> = emptyList(),
    val country: String,
    val watchLink: String? = null,
    val offers: List<Offer> = emptyList(),
    /** Platform IDs keyed by the registry's idSource names. */
    val ids: Map<String, String> = emptyMap(),
)

@Serializable
data class GeoResponse(val country: String? = null)

@Serializable
data class Registry(
    val schemaVersion: Int,
    val revision: Int,
    val providers: List<Provider> = emptyList(),
) {
    fun providerFor(tmdbProviderId: Int): Provider? = providers.firstOrNull { tmdbProviderId in it.tmdbIds }

    companion object {
        const val SUPPORTED_SCHEMA = 1
    }
}

@Serializable
data class Provider(
    val id: String,
    val name: String,
    val tmdbIds: List<Int>,
    val packages: List<String>,
    val titleLinks: List<Link> = emptyList(),
    val searchLink: Link? = null,
)

@Serializable
data class Link(
    val uri: String,
    val idSource: String? = null,
    /** Title types the link applies to; null means both. */
    val types: List<TitleType>? = null,
    val extras: Map<String, String> = emptyMap(),
)

fun posterUrl(path: String, width: Int = 342): String = "https://image.tmdb.org/t/p/w$width$path"
