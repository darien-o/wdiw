package io.github.darieno.wdiw.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import androidx.tv.material3.Text
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import io.github.darieno.wdiw.R
import io.github.darieno.wdiw.data.OfferKind
import io.github.darieno.wdiw.data.TitleDetails
import io.github.darieno.wdiw.data.TitleType
import io.github.darieno.wdiw.data.posterUrl
import io.github.darieno.wdiw.domain.OfferPlan
import io.github.darieno.wdiw.domain.OpenStep
import io.github.darieno.wdiw.ui.components.GlassButton
import io.github.darieno.wdiw.ui.components.POSTER_ASPECT
import io.github.darieno.wdiw.ui.components.PosterPlaceholder
import io.github.darieno.wdiw.ui.components.PosterShape
import io.github.darieno.wdiw.ui.components.PrimaryButton
import io.github.darieno.wdiw.ui.components.QrCode
import io.github.darieno.wdiw.ui.components.glass
import io.github.darieno.wdiw.ui.theme.Ink
import io.github.darieno.wdiw.ui.theme.Motion
import io.github.darieno.wdiw.ui.theme.Paper
import io.github.darieno.wdiw.ui.theme.PaperSecondary
import io.github.darieno.wdiw.ui.theme.PaperTertiary
import io.github.darieno.wdiw.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun TitleScreen(state: TitleState) {
    when (val content = state.content) {
        Load.Loading -> TitlePlaceholder()
        is Load.Failed -> Message(errorText(content.cause), onRetry = state::load)
        is Load.Ready -> TitleLayout(content.value, state::open)
    }
}

/** The layout's outline, shown only if the answer takes long enough for an empty screen to feel broken. */
@Composable
private fun TitlePlaceholder() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(PLACEHOLDER_DELAY_MS)
        visible = true
    }
    if (!visible) return
    Row(Modifier.fillMaxSize().padding(horizontal = ScreenEdge, vertical = 28.dp)) {
        PosterPlaceholder(Modifier.width(236.dp).offset(y = 20.dp))
        Spacer(Modifier.width(40.dp))
        Column(Modifier.weight(1f)) {
            Spacer(Modifier.height(134.dp))
            Box(Modifier.fillMaxWidth().height(170.dp).glass())
        }
    }
}

private const val PLACEHOLDER_DELAY_MS = 300L

@Composable
private fun TitleLayout(content: TitleContent, open: (OfferPlan) -> OpenStep?) {
    val details = content.details
    var selected by remember(content) { mutableStateOf(content.plans.firstOrNull()) }
    var outcome by remember(selected) { mutableStateOf<OpenOutcome?>(null) }
    val primaryFocus = remember { FocusRequester() }
    val firstChipFocus = remember { FocusRequester() }

    LaunchedEffect(content) {
        runCatching {
            if (selected?.isInstalled == true) primaryFocus.requestFocus() else firstChipFocus.requestFocus()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Backdrop(details)
        Row(Modifier.fillMaxSize().padding(horizontal = ScreenEdge, vertical = 28.dp)) {
            if (details.poster != null) {
                AsyncImage(
                    model = posterUrl(details.poster, width = 500),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(236.dp)
                        .offset(y = 20.dp)
                        .aspectRatio(POSTER_ASPECT)
                        .clip(PosterShape),
                )
                Spacer(Modifier.width(40.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(facts(details).uppercase(), style = Type.Eyebrow, color = PaperTertiary)
                Spacer(Modifier.height(8.dp))
                Text(details.title, style = Type.Display, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (details.overview.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        details.overview,
                        style = Type.Caption,
                        color = PaperSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Answer(
                    details = details,
                    plan = selected,
                    outcome = outcome,
                    onOpen = { plan -> outcome = OpenOutcome(open(plan)) },
                    primaryFocus = primaryFocus,
                )
                if (content.plans.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        // Room for the focused chip to grow without being clipped by the row.
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.offset(x = (-8).dp),
                    ) {
                        itemsIndexed(content.plans, key = { _, plan -> plan.offer.providerId }) { index, plan ->
                            OfferChip(
                                plan = plan,
                                selected = plan == selected,
                                onClick = { selected = plan },
                                modifier = if (index == 0) Modifier.focusRequester(firstChipFocus) else Modifier,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** What happened when the button was pressed. A null step means nothing could be started. */
private data class OpenOutcome(val step: OpenStep?)

/** The one thing the viewer came for: where the title is, and the button that takes them there. */
@Composable
private fun Answer(
    details: TitleDetails,
    plan: OfferPlan?,
    outcome: OpenOutcome?,
    onOpen: (OfferPlan) -> Unit,
    primaryFocus: FocusRequester,
) {
    val name = plan?.provider?.name ?: plan?.offer?.name.orEmpty()
    val firstStep = plan?.steps?.firstOrNull()
    Row(
        Modifier.fillMaxWidth().glass().padding(horizontal = 28.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.where_to_watch, countryName(details.country)).uppercase(),
                    style = Type.Eyebrow,
                    color = PaperTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(16.dp))
                // Required credit, kept next to the data it is about.
                Text(stringResource(R.string.data_by_justwatch), style = Type.Caption, color = PaperTertiary, maxLines = 1)
            }
            Spacer(Modifier.height(2.dp))
            if (plan == null) {
                Text(
                    stringResource(R.string.not_streaming, countryName(details.country)),
                    style = Type.Display,
                    maxLines = 2,
                )
                return@Column
            }
            Text(
                stringResource(R.string.on_provider, name),
                style = Type.Hero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (firstStep == null) {
                Text(kinds(plan.offer.kinds), style = Type.Label, color = PaperSecondary)
                Spacer(Modifier.height(6.dp))
                Text(
                    listOfNotNull(
                        stringResource(R.string.not_installed, name),
                        details.watchLink?.let { stringResource(R.string.scan_phone) },
                    ).joinToString(" "),
                    style = Type.Caption,
                    color = PaperSecondary,
                )
                return@Column
            }
            Spacer(Modifier.height(10.dp))
            // The button sits under the answer, not beside it, so long provider names are never cut short.
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(onClick = { onOpen(plan) }, modifier = Modifier.focusRequester(primaryFocus)) {
                    Text(
                        stringResource(
                            when (firstStep) {
                                is OpenStep.TitleLink -> R.string.open_title
                                is OpenStep.SearchLink -> R.string.open_search
                                is OpenStep.Launch -> R.string.open_app
                            },
                            name,
                        ),
                        style = Type.Headline,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(24.dp))
                Column {
                    Text(kinds(plan.offer.kinds), style = Type.Label, color = PaperSecondary)
                    // Say up front what pressing the button will and will not do.
                    val note = when {
                        outcome != null && outcome.step == null -> stringResource(R.string.open_failed, name)
                        // Also when a better link was promised but only the plain launch worked.
                        firstStep is OpenStep.Launch || outcome?.step is OpenStep.Launch ->
                            stringResource(R.string.open_app_hint, details.title)
                        else -> null
                    }
                    if (note != null) {
                        Text(note, style = Type.Caption, color = PaperSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (plan != null && firstStep == null && details.watchLink != null) {
            Spacer(Modifier.width(24.dp))
            QrCode(details.watchLink, stringResource(R.string.scan_phone), size = 104.dp)
        }
    }
}

@Composable
private fun OfferChip(plan: OfferPlan, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassButton(
        onClick = onClick,
        modifier = modifier,
        selected = selected,
        contentPadding = PaddingValues(start = 10.dp, end = 18.dp, top = 9.dp, bottom = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (plan.offer.logo != null) {
                AsyncImage(
                    model = posterUrl(plan.offer.logo, width = 92),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(plan.provider?.name ?: plan.offer.name, style = Type.Label, maxLines = 1)
                Text(
                    if (plan.isInstalled) kinds(plan.offer.kinds) else stringResource(R.string.not_on_this_tv),
                    style = Type.Caption,
                    color = PaperTertiary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The title's own artwork, softened, with its dominant colour glowing behind the glass. */
@Composable
private fun Backdrop(details: TitleDetails) {
    val ambient by animateColorAsState(
        targetValue = ambientColor(details.poster),
        animationSpec = tween(Motion.AMBIENT_MS),
        label = "ambient",
    )
    Box(Modifier.fillMaxSize().background(Ink)) {
        val image = details.backdrop ?: details.poster
        if (image != null) {
            AsyncImage(
                // Before Android 12 there is no blur, so a small image is stretched instead: soft for free.
                model = posterUrl(image, width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 780 else 300),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.42f).blur(36.dp),
            )
        }
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(ambient.copy(alpha = 0.50f), Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 0.35f),
                        radius = size.width * 0.75f,
                    ),
                )
                drawRect(Brush.verticalGradient(listOf(Ink.copy(alpha = 0.30f), Ink.copy(alpha = 0.88f))))
            },
        )
    }
}

@Composable
private fun ambientColor(posterPath: String?): Color {
    val context = LocalContext.current
    var color by remember(posterPath) { mutableStateOf(Color.Transparent) }
    LaunchedEffect(posterPath) {
        if (posterPath == null) return@LaunchedEffect
        val request = ImageRequest.Builder(context).data(posterUrl(posterPath, width = 92)).allowHardware(false).build()
        val image = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image ?: return@LaunchedEffect
        val swatch = withContext(Dispatchers.Default) {
            Palette.from(image.toBitmap()).generate().run { vibrantSwatch ?: dominantSwatch }
        }
        if (swatch != null) color = Color(swatch.rgb)
    }
    return color
}

@Composable
private fun facts(details: TitleDetails): String = listOfNotNull(
    stringResource(if (details.type == TitleType.MOVIE) R.string.type_movie else R.string.type_tv),
    details.year?.toString(),
    details.runtime?.takeIf { it > 0 }?.let { stringResource(R.string.runtime_minutes, it) },
    details.rating?.let { stringResource(R.string.rating, it) },
).joinToString("  ·  ")

@Composable
private fun kinds(kinds: List<OfferKind>): String = kinds.map {
    stringResource(
        when (it) {
            OfferKind.SUBSCRIPTION -> R.string.kind_subscription
            OfferKind.FREE -> R.string.kind_free
            OfferKind.ADS -> R.string.kind_ads
            OfferKind.RENT -> R.string.kind_rent
            OfferKind.BUY -> R.string.kind_buy
        },
    )
}.joinToString(" · ")
