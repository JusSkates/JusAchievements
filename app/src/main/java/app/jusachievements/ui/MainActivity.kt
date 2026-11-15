package app.jusachievements.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.jusachievements.graph
import app.jusachievements.ui.motion.SplashOverlay
import app.jusachievements.ui.screens.AdaptiveRoot
import app.jusachievements.ui.screens.LoginScreen
import app.jusachievements.ui.theme.JaTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val appGraph by lazy { graph }
    private val state get() = appGraph.appState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            JaTheme {
                val session by state.session.collectAsStateWithLifecycle()
                val companionActive by state.companionActive.collectAsStateWithLifecycle()
                val secondary by appGraph.dualScreen.secondary.collectAsStateWithLifecycle()
                val splashDone by state.splashDone.collectAsStateWithLifecycle()
                if (session == null) {
                    LoginScreen(state)
                } else {
                    AdaptiveRoot(
                        state = state,
                        dualActive = companionActive,
                        displayCount = if (secondary != null) 2 else 1,
                    )
                }
                if (!splashDone) SplashOverlay(onFinished = { state.splashDone.value = true })
            }
        }

        // Launch / dismiss the companion on the second display as conditions change.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    state.session,
                    appGraph.dualScreen.secondary.map { it?.displayId }.distinctUntilChanged(),
                    state.settings.useSecondScreen,
                ) { session, displayId, enabled -> session != null && enabled && displayId != null }
                    .distinctUntilChanged()
                    .collect { wanted ->
                        state.companionWanted.value = wanted
                        val display = appGraph.dualScreen.secondary.value
                        if (wanted && !state.companionActive.value && display != null) {
                            if (appGraph.dualScreen.launchOn(this@MainActivity, display, CompanionActivity::class.java)) {
                                reclaimFocus()
                            }
                        }
                    }
            }
        }
    }

    /**
     * Launching on the second display moves input focus there. Pull this task back to the front
     * so the gamepad keeps driving the main screen.
     */
    private fun reclaimFocus() {
        window.decorView.postDelayed({
            if (!isFinishing) {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
            }
        }, 400)
    }

    // D-pad and analog stick are left to Compose focus navigation; the rest are shortcuts.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        handleGamepad(state, event) || super.dispatchKeyEvent(event)

    override fun onDestroy() {
        if (isFinishing) state.companionWanted.value = false
        super.onDestroy()
    }
}
