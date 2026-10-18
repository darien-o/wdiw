package io.github.darieno.wdiw.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import io.github.darieno.wdiw.R

/** Warm near-black the whole app sits on. Each title tints it with its poster's colour. */
val Ink = Color(0xFF0E0B09)
/** Warm white for text. Use the alpha variants below for hierarchy instead of new greys. */
val Paper = Color(0xFFF4EFE8)
val PaperSecondary = Paper.copy(alpha = 0.72f)
val PaperTertiary = Paper.copy(alpha = 0.52f)
/** The one accent. Reserved for focus and the primary action. */
val Amber = Color(0xFFFFB648)
val OnAmber = Color(0xFF1A1205)

private val Serif = FontFamily(
    Font(R.font.instrument_serif_regular),
    Font(R.font.instrument_serif_italic, style = FontStyle.Italic),
)

private fun manrope(weight: Int) = Font(
    R.font.manrope,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Sans = FontFamily(manrope(400), manrope(600), manrope(700))

/** Sizes are for a TV seen from the sofa: nothing below 16sp, body at 20sp. */
object Type {
    val Hero = TextStyle(fontFamily = Serif, fontSize = 68.sp, lineHeight = 70.sp, letterSpacing = (-0.01).em)
    val Display = TextStyle(fontFamily = Serif, fontSize = 46.sp, lineHeight = 50.sp)
    val Wordmark = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 34.sp)
    val Headline = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp)
    val Body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 30.sp)
    val Label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp)
    val Caption = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp)
    val Eyebrow = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.16.em)
}

/** Shared motion timings so every focus change and transition has the same rhythm. */
object Motion {
    const val FOCUS_MS = 180
    const val SCREEN_MS = 260
    const val AMBIENT_MS = 700
}

@Composable
fun WdiwTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Amber,
            onPrimary = OnAmber,
            background = Ink,
            onBackground = Paper,
            surface = Ink,
            onSurface = Paper,
            border = Amber,
        ),
    ) {
        // Text outside a Surface would otherwise default to black.
        CompositionLocalProvider(LocalContentColor provides Paper, content = content)
    }
}
