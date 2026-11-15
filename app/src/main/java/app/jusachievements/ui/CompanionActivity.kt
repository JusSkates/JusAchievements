package app.jusachievements.ui

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import app.jusachievements.ui.motion.SplashStandby
import app.jusachievements.ui.motion.SwapTransition
import app.jusachievements.ui.theme.JaColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jusachievements.graph
import app.jusachievements.ui.screens.HeroPane
import app.jusachievements.ui.screens.NavigatorPane
import app.jusachievements.ui.theme.JaTheme

/**
 * Runs on the secondary display (the Thor's bottom screen). Normally shows the navigator; with
 * screens swapped it shows the hero instead. Both screens share the same [AppState].
 */
class CompanionActivity : ComponentActivity() {
    private val state by lazy { graph.appState }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        state.companionActive.value = true

        setContent {
            JaTheme {
                val session by state.session.collectAsStateWithLifecycle()
                val wanted by state.companionWanted.collectAsStateWithLifecycle()
                val swapped by state.settings.swapScreens.collectAsStateWithLifecycle()
                val selected by state.selectedGameId.collectAsStateWithLifecycle()
                LaunchedEffect(session, wanted) {
                    if (session == null || !wanted) finish()
                }
                BackHandler { state.back() }
                val splashDone by state.splashDone.collectAsStateWithLifecycle()
                Box(Modifier.fillMaxSize().background(JaColors.Background).safeDrawingPadding()) {
                    // Wait for the boot animation on the main screen, then drop in from above.
                    AnimatedVisibility(
                        visible = splashDone,
                        enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { -it / 5 },
                        exit = fadeOut(),
                    ) {
                        SwapTransition(swapped, isMainScreen = false) { sw ->
                            if (sw) {
                                HeroPane(state, showBack = selected != null, gamepadHints = false, ownsInput = false)
                            } else {
                                NavigatorPane(state, displayCount = 2, dualActive = true)
                            }
                        }
                    }
                    if (!splashDone) SplashStandby()
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        handleGamepad(state, event) || super.dispatchKeyEvent(event)

    override fun onDestroy() {
        state.companionActive.value = false
        super.onDestroy()
    }
}
