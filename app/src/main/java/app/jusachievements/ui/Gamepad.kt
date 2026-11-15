package app.jusachievements.ui

import android.view.KeyEvent

/**
 * Gamepad shortcuts shared by both screens' activities, so they work whichever display currently
 * has input focus. Returns true when the event was handled.
 */
fun handleGamepad(state: AppState, event: KeyEvent): Boolean {
    if (event.action != KeyEvent.ACTION_DOWN || state.session.value == null) return false
    when (event.keyCode) {
        KeyEvent.KEYCODE_BUTTON_L1 -> state.cycleTab(-1)
        KeyEvent.KEYCODE_BUTTON_R1 -> state.cycleTab(1)
        KeyEvent.KEYCODE_BUTTON_Y -> state.refresh()
        // B never quits JA: close achievement → close game → Home tab → nothing.
        KeyEvent.KEYCODE_BUTTON_B -> state.back()
        // L2/R2 step through the game list, even when it's on the other screen.
        KeyEvent.KEYCODE_BUTTON_L2 -> state.moveSelection(-1)
        KeyEvent.KEYCODE_BUTTON_R2 -> state.moveSelection(1)
        KeyEvent.KEYCODE_BUTTON_SELECT -> if (state.companionActive.value) state.settings.toggleSwapScreens()
        else -> return false
    }
    return true
}
