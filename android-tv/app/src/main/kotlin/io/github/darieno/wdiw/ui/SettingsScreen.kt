package io.github.darieno.wdiw.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.darieno.wdiw.AppContainer
import io.github.darieno.wdiw.BuildConfig
import io.github.darieno.wdiw.R
import io.github.darieno.wdiw.domain.resolveCountry
import io.github.darieno.wdiw.ui.components.GlassButton
import io.github.darieno.wdiw.ui.components.glass
import io.github.darieno.wdiw.ui.theme.PaperSecondary
import io.github.darieno.wdiw.ui.theme.PaperTertiary
import io.github.darieno.wdiw.ui.theme.Type
import io.github.darieno.wdiw.update.Release
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

/** null stands for "automatic". */
private data class CountryChoice(val code: String?, val label: String)

@Composable
fun SettingsScreen(container: AppContainer, updates: UpdateState) {
    val saved by container.country.saved.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    Row(Modifier.fillMaxSize().padding(start = ScreenEdge, top = 32.dp, bottom = 32.dp)) {
        About(updates, Modifier.width(300.dp).fillMaxHeight())
        Spacer(Modifier.width(36.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.country_title), style = Type.Display)
            Text(stringResource(R.string.country_help), style = Type.Body, color = PaperSecondary)
            Spacer(Modifier.height(10.dp))

            val settings = saved ?: return@Column
            val automatic = resolveCountry(null, null, settings.lastDetected, Locale.getDefault().country)
            val automaticLabel = stringResource(R.string.country_auto, countryName(automatic))
            val choices = remember(automaticLabel) {
                val collator = Collator.getInstance()
                listOf(CountryChoice(null, automaticLabel)) +
                    Locale.getISOCountries()
                        .map { CountryChoice(it, countryName(it)) }
                        .sortedWith(compareBy(collator) { it.label })
            }
            val gridState = rememberLazyGridState()
            val selectedFocus = remember { FocusRequester() }
            // Start on the current choice, wherever it is in the list.
            LaunchedEffect(Unit) {
                gridState.scrollToItem(choices.indexOfFirst { it.code == settings.chosen }.coerceAtLeast(0))
                runCatching { selectedFocus.requestFocus() }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(start = 8.dp, end = ScreenEdge, top = 10.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(
                    choices,
                    key = { it.code ?: "auto" },
                    // "Automatic" gets a row of its own, above the alphabetical list.
                    span = { GridItemSpan(if (it.code == null) maxLineSpan else 1) },
                ) { choice ->
                    val isSelected = choice.code == settings.chosen
                    GlassButton(
                        onClick = { scope.launch { container.country.choose(choice.code) } },
                        selected = isSelected,
                        focusedScale = 1.03f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isSelected) Modifier.focusRequester(selectedFocus) else Modifier),
                    ) {
                        Text(choice.label, style = Type.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallButton(release: Release, updates: UpdateState) {
    GlassButton(onClick = { updates.install(release) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.update_install, release.version), style = Type.Label, maxLines = 1)
    }
}

/** Credits. TMDB and JustWatch require these to be shown. */
@Composable
private fun About(updates: UpdateState, modifier: Modifier = Modifier) {
    Column(modifier.glass().padding(26.dp)) {
        Wordmark()
        Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = Type.Caption, color = PaperTertiary)
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.license), style = Type.Caption, color = PaperSecondary)
        Spacer(Modifier.height(18.dp))
        when (val status = updates.status) {
            UpdateStatus.None -> Unit
            is UpdateStatus.Available -> InstallButton(status.release, updates)
            is UpdateStatus.Downloading -> Text(
                stringResource(R.string.update_downloading, ((status.progress ?: 0f) * 100).toInt()),
                style = Type.Label,
            )
            is UpdateStatus.Failed -> {
                Text(stringResource(R.string.update_failed), style = Type.Caption, color = PaperSecondary)
                Spacer(Modifier.height(10.dp))
                InstallButton(status.release, updates)
            }
        }
        Spacer(Modifier.weight(1f))
        Image(
            painterResource(R.drawable.tmdb_logo),
            contentDescription = "TMDB",
            modifier = Modifier.width(120.dp).align(Alignment.Start),
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.attribution_tmdb), style = Type.Caption, color = PaperSecondary)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.attribution_justwatch), style = Type.Caption, color = PaperSecondary)
    }
}
