package io.github.darieno.wdiw.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.darieno.wdiw.update.Release
import io.github.darieno.wdiw.update.Updater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface UpdateStatus {
    data object None : UpdateStatus
    data class Available(val release: Release) : UpdateStatus
    /** [progress] is 0..1, or null while the size is unknown. */
    data class Downloading(val release: Release, val progress: Float?) : UpdateStatus
    data class Failed(val release: Release) : UpdateStatus
}

/** Checks for a newer release once per launch and drives the download when the viewer asks for it. */
class UpdateState(private val updater: Updater, private val scope: CoroutineScope) {
    var status by mutableStateOf<UpdateStatus>(UpdateStatus.None)
        private set

    init {
        scope.launch { updater.check()?.let { status = UpdateStatus.Available(it) } }
    }

    fun install(release: Release) {
        if (status is UpdateStatus.Downloading) return
        status = UpdateStatus.Downloading(release, null)
        scope.launch {
            status = try {
                updater.install(release) { progress ->
                    // Called from the download thread; Compose state may be written from any thread.
                    status = UpdateStatus.Downloading(release, progress)
                }
                // From here the system's own install screen takes over.
                UpdateStatus.Available(release)
            } catch (_: IOException) {
                UpdateStatus.Failed(release)
            }
        }
    }
}
