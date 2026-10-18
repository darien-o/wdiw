package io.github.darieno.wdiw

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import io.github.darieno.wdiw.domain.Destination
import io.github.darieno.wdiw.domain.parseDeepLink
import io.github.darieno.wdiw.ui.App
import io.github.darieno.wdiw.ui.HomeState
import io.github.darieno.wdiw.ui.Navigator
import io.github.darieno.wdiw.ui.Screen
import io.github.darieno.wdiw.ui.UpdateState
import io.github.darieno.wdiw.ui.theme.WdiwTheme

class MainActivity : ComponentActivity() {
    private val navigator = Navigator()
    private lateinit var home: HomeState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as WdiwApp).container
        home = HomeState(container, lifecycleScope)
        val updates = UpdateState(container.updater, lifecycleScope)
        if (savedInstanceState == null) follow(intent)
        setContent {
            WdiwTheme { App(container, navigator, home, updates) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        follow(intent)
    }

    /** Handles a search sent by the system or an assistant, and `wdiw://` links. */
    private fun follow(intent: Intent) {
        val destination = when (intent.action) {
            Intent.ACTION_SEARCH ->
                intent.getStringExtra(SearchManager.QUERY)?.trim()?.takeIf { it.isNotEmpty() }?.let(Destination::Search)

            Intent.ACTION_VIEW -> intent.dataString?.let(::parseDeepLink)
            else -> null
        }
        when (destination) {
            is Destination.Search -> {
                navigator.home()
                home.search(destination.query)
            }

            is Destination.Title -> navigator.open(Screen.Title(destination.type, destination.id))
            null -> Unit
        }
    }
}
