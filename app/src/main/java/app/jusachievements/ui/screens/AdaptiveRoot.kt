package app.jusachievements.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jusachievements.ui.AppState
import app.jusachievements.ui.motion.SwapTransition
import app.jusachievements.ui.theme.JaColors

/**
 * Picks a layout for the main screen (the one that gets gamepad input):
 *  - Dual screen: this screen shows the hero, or the navigator if the screens are swapped.
 *  - Wide single screen (landscape handheld, tablet): navigator + hero side by side.
 *  - Narrow single screen (phone portrait): navigator, and the hero opens over it on selection.
 */
@Composable
fun AdaptiveRoot(state: AppState, dualActive: Boolean, displayCount: Int) {
    val selected by state.selectedGameId.collectAsStateWithLifecycle()
    val swapped by state.settings.swapScreens.collectAsStateWithLifecycle()

    // Always consume Back: close game → go to Home tab → do nothing. JA never exits on B.
    BackHandler { state.back() }

    BoxWithConstraints(Modifier.fillMaxSize().background(JaColors.Background).safeDrawingPadding()) {
        val twoPane = maxWidth >= 600.dp && maxWidth > maxHeight * 1.1f
        val navWidth = (maxWidth * 0.42f).coerceIn(340.dp, 520.dp)
        when {
            dualActive -> SwapTransition(swapped, isMainScreen = true) { sw ->
                if (sw) {
                    NavigatorPane(state, displayCount = displayCount, dualActive = true)
                } else {
                    HeroPane(state, showBack = selected != null, gamepadHints = true)
                }
            }

            twoPane -> Row(Modifier.fillMaxSize()) {
                NavigatorPane(state, Modifier.width(navWidth).fillMaxHeight(), displayCount)
                HeroPane(state, Modifier.weight(1f), showBack = selected != null, gamepadHints = false, ownsInput = false)
            }

            selected != null -> HeroPane(state, showBack = true, gamepadHints = false)

            else -> NavigatorPane(state, displayCount = displayCount)
        }
    }
}
