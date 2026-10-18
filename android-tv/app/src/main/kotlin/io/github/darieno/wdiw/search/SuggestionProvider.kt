package io.github.darieno.wdiw.search

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import io.github.darieno.wdiw.WdiwApp
import io.github.darieno.wdiw.data.posterUrl
import io.github.darieno.wdiw.domain.titleDeepLink
import kotlinx.coroutines.runBlocking
import java.io.IOException

/**
 * Answers the TV's own search (the launcher's search box and the assistant behind the remote's
 * mic button) so titles found there open this app on the right page. Read-only.
 */
class SuggestionProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(COLUMNS)
        // The system passes the text as the selection argument (see searchSuggestSelection in
        // searchable.xml); some callers append it to the path instead.
        val query = (selectionArgs?.firstOrNull() ?: uri.lastPathSegment?.takeIf { it != SearchManager.SUGGEST_URI_PATH_QUERY })
            ?.trim().orEmpty()
        if (query.length < 2) return cursor

        val container = (context?.applicationContext as? WdiwApp)?.container ?: return cursor
        val results = try {
            // query() runs on a binder thread, so waiting for the network here is expected.
            runBlocking { container.api.search(query, container.language) }
        } catch (_: IOException) {
            return cursor
        }
        results.take(MAX_RESULTS).forEachIndexed { index, summary ->
            cursor.addRow(
                arrayOf<Any?>(
                    index,
                    summary.title,
                    summary.poster?.let { posterUrl(it) },
                    summary.year ?: 0,
                    0, // The search endpoint does not know running times.
                    "video/*",
                    titleDeepLink(summary.type, summary.id),
                ),
            )
        }
        return cursor
    }

    override fun getType(uri: Uri): String = SearchManager.SUGGEST_MIME_TYPE
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private companion object {
        const val MAX_RESULTS = 10
        val COLUMNS = arrayOf(
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE,
            SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
            SearchManager.SUGGEST_COLUMN_DURATION,
            SearchManager.SUGGEST_COLUMN_CONTENT_TYPE,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA,
        )
    }
}
