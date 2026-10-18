package io.github.darieno.wdiw.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import io.github.darieno.wdiw.R
import io.github.darieno.wdiw.data.ApiException
import io.github.darieno.wdiw.data.Summary
import io.github.darieno.wdiw.ui.components.GlassButton
import io.github.darieno.wdiw.ui.components.PosterCard
import io.github.darieno.wdiw.ui.components.PosterPlaceholder
import io.github.darieno.wdiw.ui.theme.Amber
import io.github.darieno.wdiw.ui.theme.Paper
import io.github.darieno.wdiw.ui.theme.PaperSecondary
import io.github.darieno.wdiw.ui.theme.PaperTertiary
import io.github.darieno.wdiw.ui.theme.Type
import java.util.Locale

/** Left and right margin that keeps content inside the area every TV shows (5% overscan). */
val ScreenEdge = 52.dp
private const val COLUMNS = 6

@Composable
fun HomeScreen(state: HomeState, updateAvailable: Boolean, onOpen: (Summary) -> Unit, onSettings: () -> Unit) {
    val content by state.content.collectAsState()
    val searchFocus = remember { FocusRequester() }
    val firstCardFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Coming back from a title, focus returns to its poster instead (see the grid below).
        if (state.lastOpened == null) runCatching { searchFocus.requestFocus() }
    }

    Column(Modifier.fillMaxSize().padding(top = 30.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ScreenEdge),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Wordmark()
            Spacer(Modifier.weight(1f))
            if (updateAvailable) {
                GlassButton(onClick = onSettings, shape = CircleShape) {
                    Text(stringResource(R.string.update_available), style = Type.Label, color = Amber)
                }
                Spacer(Modifier.width(14.dp))
            }
            content?.let {
                GlassButton(onClick = onSettings, shape = CircleShape) {
                    Text(stringResource(R.string.country_change, countryName(it.country)), style = Type.Label)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        SearchRow(
            query = state.query,
            onQueryChange = state::search,
            focusRequester = searchFocus,
            downTarget = firstCardFocus,
            modifier = Modifier.padding(horizontal = ScreenEdge),
        )
        Spacer(Modifier.height(26.dp))

        val shown = content
        Text(
            text = when {
                shown == null -> ""
                shown.query.isEmpty() -> stringResource(R.string.popular_in, countryName(shown.country))
                else -> stringResource(R.string.results_for, shown.query)
            }.uppercase(),
            style = Type.Eyebrow,
            color = PaperTertiary,
            modifier = Modifier.padding(horizontal = ScreenEdge),
        )
        Spacer(Modifier.height(10.dp))

        when (val items = shown?.items) {
            null, Load.Loading -> PosterGrid { items(COLUMNS * 2) { PosterPlaceholder() } }
            is Load.Failed -> Message(errorText(items.cause), onRetry = state::retry)
            is Load.Ready ->
                if (items.value.isEmpty()) {
                    Message(stringResource(R.string.no_results, shown.query))
                } else {
                    PosterGrid(state) {
                        itemsIndexed(items.value, key = { _, it -> "${it.type}/${it.id}" }) { index, summary ->
                            val focus = if (index == 0) firstCardFocus else remember { FocusRequester() }
                            if (summary == state.lastOpened) {
                                LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                            }
                            PosterCard(
                                summary = summary,
                                onClick = {
                                    state.lastOpened = summary
                                    onOpen(summary)
                                },
                                modifier = Modifier.focusRequester(focus),
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun PosterGrid(
    state: HomeState? = null,
    content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        state = state?.gridState ?: androidx.compose.foundation.lazy.grid.rememberLazyGridState(),
        // Room on every side for the focused card to grow without being clipped.
        contentPadding = PaddingValues(start = ScreenEdge, end = ScreenEdge, top = 12.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier.fillMaxSize(),
        content = content,
    )
}

@Composable
fun Wordmark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("where do I ", style = Type.Wordmark, color = Paper)
        Text("watch", style = Type.Wordmark, color = Amber)
    }
}

@Composable
private fun SearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    downTarget: FocusRequester,
    modifier: Modifier = Modifier,
) {
    // On a TV the keyboard should open when the viewer presses OK on the field, not whenever focus passes over it.
    var editing by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    fun stopEditing() {
        editing = false
        keyboard?.hide()
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(editing) {
        if (editing) {
            runCatching { fieldFocus.requestFocus() }
            keyboard?.show()
        }
    }
    BackHandler(enabled = editing) { stopEditing() }

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GlassButton(
            onClick = { editing = true },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                // The bar spans the screen, so "down" would otherwise land in the middle of the row.
                .focusProperties { down = downTarget },
            shape = RoundedCornerShape(26.dp),
            focusedScale = 1.012f,
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_search), contentDescription = null, Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(stringResource(R.string.search_hint), style = Type.Headline, color = PaperTertiary)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        enabled = editing,
                        singleLine = true,
                        textStyle = Type.Headline.copy(color = Paper),
                        cursorBrush = SolidColor(Amber),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { stopEditing() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(fieldFocus)
                            // Closing the keyboard with Back is handled by the keyboard itself, so the field can
                            // still be editing with no keyboard shown. The remote's own keys then end editing
                            // instead of moving the cursor, which would leave the viewer stuck in the field.
                            .onPreviewKeyEvent { event ->
                                if (!editing || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (event.key) {
                                    Key.DirectionDown -> {
                                        stopEditing()
                                        runCatching { downTarget.requestFocus() }
                                        true
                                    }
                                    Key.DirectionUp, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                        stopEditing()
                                        true
                                    }
                                    else -> false
                                }
                            }
                            .onFocusChanged { if (editing && !it.isFocused && !it.hasFocus) editing = false },
                    )
                }
            }
        }
        VoiceButton(onSpoken = onQueryChange)
    }
}

/** Shown only when the device has a speech recogniser. */
@Composable
private fun VoiceButton(onSpoken: (String) -> Unit) {
    val context = LocalContext.current
    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    }
    val available = remember { intent.resolveActivity(context.packageManager) != null }
    if (!available) return

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onSpoken)
        }
    }
    Spacer(Modifier.width(16.dp))
    GlassButton(
        onClick = { runCatching { launcher.launch(intent) } },
        shape = CircleShape,
        contentPadding = PaddingValues(18.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_mic),
            contentDescription = stringResource(R.string.voice_search),
            Modifier.size(28.dp),
        )
    }
}

/** A centred message for empty and error states, with a way out when there is one. */
@Composable
fun Message(text: String, onRetry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = ScreenEdge, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = Type.Headline, color = PaperSecondary, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(22.dp))
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            GlassButton(onClick = onRetry, modifier = Modifier.focusRequester(focus)) {
                Text(stringResource(R.string.retry), style = Type.Label)
            }
        }
    }
}

@Composable
fun errorText(cause: Exception): String =
    if (cause is ApiException && cause.status == 404) stringResource(R.string.error_title_missing)
    else stringResource(R.string.error_offline)

fun countryName(code: String): String = Locale("", code).getDisplayCountry(Locale.getDefault()).ifEmpty { code }
