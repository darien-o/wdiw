package io.github.darieno.wdiw.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import io.github.darieno.wdiw.data.Summary
import io.github.darieno.wdiw.data.posterUrl
import io.github.darieno.wdiw.ui.theme.Amber
import io.github.darieno.wdiw.ui.theme.Paper
import io.github.darieno.wdiw.ui.theme.PaperSecondary
import io.github.darieno.wdiw.ui.theme.Type

val PosterShape = RoundedCornerShape(16.dp)
const val POSTER_ASPECT = 2f / 3f

@Composable
fun PosterCard(summary: Summary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().aspectRatio(POSTER_ASPECT),
        shape = ClickableSurfaceDefaults.shape(PosterShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Paper.copy(alpha = 0.06f),
            focusedContainerColor = Paper.copy(alpha = 0.06f),
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Amber)),
            pressedBorder = Border(BorderStroke(3.dp, Amber)),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.07f),
        glow = ClickableSurfaceDefaults.glow(),
    ) {
        if (summary.poster != null) {
            AsyncImage(
                model = posterUrl(summary.poster),
                contentDescription = label(summary),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // No artwork: the title itself stands in for the poster.
            Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = label(summary),
                    style = Type.Label,
                    color = PaperSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A placeholder with the card's footprint, shown while a list loads so nothing jumps afterwards. */
@Composable
fun PosterPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().aspectRatio(POSTER_ASPECT).background(Paper.copy(alpha = 0.05f), PosterShape))
}

private fun label(summary: Summary): String =
    if (summary.year != null) "${summary.title} (${summary.year})" else summary.title
