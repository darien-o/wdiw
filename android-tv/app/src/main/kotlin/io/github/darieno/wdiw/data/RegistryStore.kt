package io.github.darieno.wdiw.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The provider registry: the copy last downloaded from the proxy if there is one, otherwise the copy
 * bundled in the APK. A newer copy is fetched in the background on every start.
 */
class RegistryStore(private val context: Context, private val api: Api, scope: CoroutineScope) {
    private val downloaded = File(context.filesDir, FILE_NAME)
    private val current = MutableStateFlow<Registry?>(null)

    init {
        scope.launch {
            current.value = withContext(Dispatchers.IO) { loadLocal() }
            refresh()
        }
    }

    suspend fun get(): Registry = current.filterNotNull().first()

    private fun loadLocal(): Registry {
        val bundled = parse(context.assets.open(FILE_NAME).use { it.readBytes().decodeToString() })
            ?: error("The bundled registry is unreadable")
        val saved = if (downloaded.exists()) parse(downloaded.readText()) else null
        // An app update can bundle a newer registry than the one downloaded by the previous version.
        return if (saved != null && saved.revision >= bundled.revision) saved else bundled
    }

    private suspend fun refresh() {
        val text = runCatching { api.registryJson() }.getOrNull() ?: return
        val remote = parse(text) ?: return
        if (remote.revision <= (current.value?.revision ?: 0)) return
        withContext(Dispatchers.IO) {
            val temp = File(context.filesDir, "$FILE_NAME.tmp")
            temp.writeText(text)
            temp.renameTo(downloaded)
        }
        current.value = remote
    }

    /** Null for anything this app version cannot use, including a registry with a newer schema. */
    private fun parse(text: String): Registry? =
        runCatching { AppJson.decodeFromString<Registry>(text) }.getOrNull()
            ?.takeIf { it.schemaVersion == Registry.SUPPORTED_SCHEMA }

    private companion object {
        const val FILE_NAME = "providers.json"
    }
}
