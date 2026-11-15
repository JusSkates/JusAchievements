package app.jusachievements.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _useSecondScreen = MutableStateFlow(prefs.getBoolean(KEY_DUAL, true))
    /** Put the navigator on a second display when one is present (bottom screen on the Thor). */
    val useSecondScreen: StateFlow<Boolean> = _useSecondScreen.asStateFlow()

    fun setUseSecondScreen(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DUAL, enabled).apply()
        _useSecondScreen.value = enabled
    }

    private val _swapScreens = MutableStateFlow(prefs.getBoolean(KEY_SWAP, false))
    /** When true, the lists go on the main screen and the showcase on the second screen. */
    val swapScreens: StateFlow<Boolean> = _swapScreens.asStateFlow()

    fun setSwapScreens(swapped: Boolean) {
        prefs.edit().putBoolean(KEY_SWAP, swapped).apply()
        _swapScreens.value = swapped
    }

    fun toggleSwapScreens() = setSwapScreens(!_swapScreens.value)

    private val _musicEnabled = MutableStateFlow(prefs.getBoolean(KEY_MUSIC, true))
    val musicEnabled: StateFlow<Boolean> = _musicEnabled.asStateFlow()

    fun setMusicEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUSIC, enabled).apply()
        _musicEnabled.value = enabled
    }

    private val _customMusicUri = MutableStateFlow(prefs.getString(KEY_MUSIC_URI, null))
    /** A user-picked audio file to loop instead of the built-in music; null = built-in. */
    val customMusicUri: StateFlow<String?> = _customMusicUri.asStateFlow()

    fun setCustomMusicUri(uri: String?) {
        prefs.edit().putString(KEY_MUSIC_URI, uri).apply()
        _customMusicUri.value = uri
    }

    private val _themeId = MutableStateFlow(prefs.getString(KEY_THEME, null))
    val themeId: StateFlow<String?> = _themeId.asStateFlow()

    fun setThemeId(id: String) {
        prefs.edit().putString(KEY_THEME, id).apply()
        _themeId.value = id
    }

    private val _hiddenTabs = MutableStateFlow(prefs.getStringSet(KEY_HIDDEN_TABS, emptySet())!!.toSet())
    /** Names of tabs the user has removed from the tab bar. */
    val hiddenTabs: StateFlow<Set<String>> = _hiddenTabs.asStateFlow()

    fun setTabHidden(tab: String, hidden: Boolean) {
        val next = if (hidden) _hiddenTabs.value + tab else _hiddenTabs.value - tab
        prefs.edit().putStringSet(KEY_HIDDEN_TABS, next).apply()
        _hiddenTabs.value = next
    }

    private companion object {
        const val KEY_MUSIC = "music"
        const val KEY_MUSIC_URI = "music_uri"
        const val KEY_THEME = "theme"
        const val KEY_HIDDEN_TABS = "hidden_tabs"
        const val KEY_DUAL = "use_second_screen"
        const val KEY_SWAP = "swap_screens"
    }
}
