package io.github.darieno.wdiw.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import io.github.darieno.wdiw.BuildConfig
import io.github.darieno.wdiw.data.AppJson
import io.github.darieno.wdiw.domain.isNewerVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class Release(val version: String, val apkUrl: String)

/**
 * Updates the app from the project's GitHub releases. Android only accepts an APK signed with the
 * same key as the installed app, and always asks the viewer before installing.
 */
class Updater(private val context: Context, private val client: OkHttpClient) {

    @Serializable
    private class GitHubRelease(
        @SerialName("tag_name") val tag: String,
        val assets: List<Asset> = emptyList(),
    ) {
        @Serializable
        class Asset(val name: String, @SerialName("browser_download_url") val url: String)
    }

    /** The newest release if it is newer than this build, otherwise null. Null also when the check fails. */
    suspend fun check(): Release? = withContext(Dispatchers.IO) {
        // Debug builds are signed with a different key, so a release could never install over them.
        if (BuildConfig.DEBUG) return@withContext null
        // Only APKs attached to this repository's own releases are ever downloaded.
        val downloadPrefix = "https://github.com/${BuildConfig.UPDATE_REPO}/releases/download/"
        val request = Request.Builder()
            .url("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val release = AppJson.decodeFromString<GitHubRelease>(response.body.string())
                val apk = release.assets.firstOrNull { it.name.endsWith(".apk") && it.url.startsWith(downloadPrefix) }
                if (apk != null && isNewerVersion(release.tag, BuildConfig.VERSION_NAME)) {
                    Release(release.tag.removePrefix("v"), apk.url)
                } else {
                    null
                }
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null // GitHub answered with something that is not a release.
        }
    }

    /**
     * Downloads the release straight into an install session and hands it to the system, which
     * then shows its own confirmation. [onProgress] receives 0..1, or nothing when the size is unknown.
     */
    suspend fun install(release: Release, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(context.packageName)
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                client.newCall(Request.Builder().url(release.apkUrl).build()).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Download failed: ${response.code}")
                    val total = response.body.contentLength()
                    session.openWrite("update.apk", 0, total).use { out ->
                        val buffer = ByteArray(64 * 1024)
                        var written = 0L
                        response.body.byteStream().use { input ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                out.write(buffer, 0, read)
                                written += read
                                if (total > 0) onProgress(written.toFloat() / total)
                            }
                        }
                        session.fsync(out)
                    }
                }
                val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val status = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    Intent(context, InstallStatusReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or mutable,
                )
                session.commit(status.intentSender)
            }
        } catch (e: IOException) {
            installer.abandonSession(sessionId)
            throw e
        }
    }
}

/** Shows the system's "install this update?" screen when the session asks for it. */
class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status != PackageInstaller.STATUS_PENDING_USER_ACTION) return
        val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }
        confirm?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
