package io.github.darieno.wdiw

import android.content.Context
import io.github.darieno.wdiw.data.Api
import io.github.darieno.wdiw.data.CountryRepository
import io.github.darieno.wdiw.data.RegistryStore
import io.github.darieno.wdiw.data.Settings
import io.github.darieno.wdiw.domain.languageTag
import io.github.darieno.wdiw.launch.Launcher
import io.github.darieno.wdiw.update.Updater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

/** The app's long-lived objects, wired by hand. Created once by [WdiwApp]. */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http"), 10L * 1024 * 1024))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api = Api(httpClient, BuildConfig.PROXY_URL.toHttpUrl())
    val registry = RegistryStore(context, api, scope)
    val country = CountryRepository(Settings(context), api, scope)
    val launcher = Launcher(context)
    val updater = Updater(context, httpClient)

    /** Language for titles and descriptions, following the device. */
    val language: String
        get() = Locale.getDefault().let { languageTag(it.language, it.country) }
}
