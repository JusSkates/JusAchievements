package app.jusachievements.display

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether a usable secondary display exists. On the AYN Thor that's the built-in bottom
 * screen; elsewhere it could be an external monitor, a dock, or a dual-screen phone. If there is
 * none, the app simply runs its single-screen adaptive layout.
 */
class DualScreenManager(context: Context) {
    private val displayManager = context.getSystemService(DisplayManager::class.java)

    private val _secondary = MutableStateFlow(findSecondary())
    val secondary: StateFlow<Display?> = _secondary.asStateFlow()

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = update()
        override fun onDisplayRemoved(displayId: Int) = update()
        override fun onDisplayChanged(displayId: Int) = update()
    }

    init {
        displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
    }

    private fun update() {
        _secondary.value = findSecondary()
    }

    private fun findSecondary(): Display? {
        val candidates = displayManager.displays.filter { d ->
            d.displayId != Display.DEFAULT_DISPLAY &&
                d.isValid &&
                // Skip private virtual displays (screen recorders, mirroring tools, etc.).
                (d.flags and Display.FLAG_PRIVATE) == 0
        }
        val presentation = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .map { it.displayId }.toSet()
        // Prefer built-in/presentation displays, then anything else that qualifies.
        return candidates.firstOrNull { it.displayId in presentation } ?: candidates.firstOrNull()
    }

    fun displayCount(): Int = displayManager.displays.size

    /** Launches [activityClass] on [display]. Returns false if the system refused. */
    fun launchOn(activity: Activity, display: Display, activityClass: Class<out Activity>): Boolean {
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(display.displayId)
        val intent = Intent(activity, activityClass).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { activity.startActivity(intent, options.toBundle()) }.isSuccess
    }
}
