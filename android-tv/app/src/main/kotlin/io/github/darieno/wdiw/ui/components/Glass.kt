package io.github.darieno.wdiw.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import io.github.darieno.wdiw.ui.theme.Amber
import io.github.darieno.wdiw.ui.theme.OnAmber
import io.github.darieno.wdiw.ui.theme.Paper

val PanelShape = RoundedCornerShape(28.dp)
val ControlShape = RoundedCornerShape(18.dp)

/**
 * The glass surface: translucent fill, a brighter top edge and a hairline border. It reads as
 * frosted because the screens put it over an already-soft backdrop, so it costs nothing to draw
 * and looks the same on a low-end box as on a new TV.
 */
fun Modifier.glass(shape: Shape = PanelShape): Modifier = this
    .background(Brush.verticalGradient(listOf(Paper.copy(alpha = 0.11f), Paper.copy(alpha = 0.04f))), shape)
    .border(1.dp, Brush.verticalGradient(listOf(Paper.copy(alpha = 0.30f), Paper.copy(alpha = 0.06f))), shape)

private val FocusBorder = Border(BorderStroke(2.5.dp, Amber))

/** A focusable glass control: brightens, grows slightly and gains the amber ring when focused. */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    shape: Shape = ControlShape,
    /** Wide controls should grow less, or they spill over their neighbours. */
    focusedScale: Float = 1.05f,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Paper.copy(alpha = if (selected) 0.16f else 0.07f),
            contentColor = Paper,
            focusedContainerColor = Paper.copy(alpha = 0.20f),
            focusedContentColor = Paper,
            pressedContainerColor = Paper.copy(alpha = 0.26f),
            pressedContentColor = Paper,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Paper.copy(alpha = if (selected) 0.45f else 0.14f))),
            focusedBorder = FocusBorder,
            pressedBorder = FocusBorder,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale),
        glow = ClickableSurfaceDefaults.glow(),
    ) {
        Box(Modifier.padding(contentPadding), contentAlignment = Alignment.Center, content = content)
    }
}

/** The screen's single primary action: amber outline at rest, solid amber when focused. */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(ControlShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Amber.copy(alpha = 0.12f),
            contentColor = Amber,
            focusedContainerColor = Amber,
            focusedContentColor = OnAmber,
            pressedContainerColor = Amber,
            pressedContentColor = OnAmber,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.5.dp, Amber.copy(alpha = 0.7f))),
            focusedBorder = Border(BorderStroke(2.5.dp, Paper)),
            pressedBorder = Border(BorderStroke(2.5.dp, Paper)),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        glow = ClickableSurfaceDefaults.glow(),
    ) {
        Box(
            Modifier.padding(horizontal = 30.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
