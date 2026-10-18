package io.github.darieno.wdiw.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** The proxy answered, but not with a result (for example 404 for an unknown title). */
class ApiException(val status: Int) : IOException("Proxy responded $status")

/** Client for the proxy in /proxy. Every call throws [IOException] when it cannot be completed. */
class Api(private val client: OkHttpClient, private val baseUrl: HttpUrl) {

    suspend fun search(query: String, lang: String): List<Summary> =
        get<SummaryList>("search", "q" to query, "lang" to lang).results

    suspend fun popular(country: String, lang: String): List<Summary> =
        get<SummaryList>("popular", "country" to country, "lang" to lang).results

    suspend fun title(type: TitleType, id: Int, country: String, lang: String): TitleDetails =
        get("title/${type.path}/$id", "country" to country, "lang" to lang)

    /** The country the proxy sees this device in, or null when it cannot tell. */
    suspend fun geo(): String? = get<GeoResponse>("geo").country

    suspend fun registryJson(): String = getText("registry")

    private suspend inline fun <reified T> get(path: String, vararg params: Pair<String, String>): T =
        AppJson.decodeFromString(getText(path, *params))

    private suspend fun getText(path: String, vararg params: Pair<String, String>): String =
        withContext(Dispatchers.IO) {
            val url = baseUrl.newBuilder().addPathSegments(path)
                .apply { params.forEach { (name, value) -> addQueryParameter(name, value) } }
                .build()
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) throw ApiException(response.code)
                response.body.string()
            }
        }
}
