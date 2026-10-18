package io.github.darieno.wdiw.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import io.github.darieno.wdiw.AppContainer
import io.github.darieno.wdiw.data.TitleType
import io.github.darieno.wdiw.ui.theme.Ink
import io.github.darieno.wdiw.ui.theme.Motion

sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data class Title(val type: TitleType, val id: Int) : Screen
}

/** A plain back stack. Home is always at the bottom. */
class Navigator {
    private val stack = mutableStateListOf<Screen>(Screen.Home)

    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun open(screen: Screen) {
        if (screen != current) stack.add(screen)
    }

    fun back() {
        if (canGoBack) stack.removeAt(stack.lastIndex)
    }

    fun home() {
        stack.removeRange(1, stack.size)
    }
}

@Composable
fun App(container: AppContainer, navigator: Navigator, home: HomeState, updates: UpdateState) {
    BackHandler(enabled = navigator.canGoBack) { navigator.back() }

    // The layout is drawn for a screen 960dp wide. TVs and projectors report very different
    // densities for the same 1080p panel, so the density is set from the real width instead.
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val density = Density(windowWidth / DESIGN_WIDTH_DP, LocalDensity.current.fontScale)

    CompositionLocalProvider(LocalDensity provides density) {
    Box(Modifier.fillMaxSize().background(Ink)) {
        AnimatedContent(
            targetState = navigator.current,
            transitionSpec = {
                fadeIn(tween(Motion.SCREEN_MS)) togetherWith fadeOut(tween(Motion.SCREEN_MS * 2 / 3))
            },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.Home -> HomeScreen(
                    state = home,
                    updateAvailable = updates.status !is UpdateStatus.None,
                    onOpen = { navigator.open(Screen.Title(it.type, it.id)) },
                    onSettings = { navigator.open(Screen.Settings) },
                )

                Screen.Settings -> SettingsScreen(container, updates)

                is Screen.Title -> {
                    val scope = rememberCoroutineScope()
                    TitleScreen(remember(screen) { TitleState(container, scope, screen.type, screen.id) })
                }
            }
        }
    }
    }
}

private const val DESIGN_WIDTH_DP = 960f
