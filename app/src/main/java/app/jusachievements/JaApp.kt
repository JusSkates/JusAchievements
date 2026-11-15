package app.jusachievements

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import app.jusachievements.audio.MusicController
import app.jusachievements.data.CredentialStore
import app.jusachievements.data.RaRepository
import app.jusachievements.data.SettingsStore
import app.jusachievements.data.api.ApiFactory
import app.jusachievements.display.DualScreenManager
import app.jusachievements.ui.AppState
import app.jusachievements.ui.theme.JaColors
import app.jusachievements.ui.theme.Palettes
import coil.ImageLoader
import coil.ImageLoaderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class JaApp : Application(), ImageLoaderFactory {
    lateinit var graph: AppGraph
        private set

    private var visibleActivities = 0

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)

        // Music plays only while JA is on screen and the setting is on.
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) { visibleActivities++; updateMusic() }
            override fun onActivityStopped(activity: Activity) { visibleActivities--; updateMusic() }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        graph.scope.launch {
            combine(graph.settings.musicEnabled, graph.settings.customMusicUri) { _, _ -> }.collect { updateMusic() }
        }
        graph.scope.launch {
            graph.settings.themeId.collect { JaColors.palette = Palettes.byId(it) }
        }
    }

    private fun updateMusic() {
        graph.music.update(
            play = visibleActivities > 0 && graph.settings.musicEnabled.value,
            customUri = graph.settings.customMusicUri.value,
        )
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(graph.httpClient)
        .crossfade(true)
        .build()
}

/** Manual DI: one instance of everything, shared by both screens' activities. */
class AppGraph(app: Application) {
    val credentials = CredentialStore(app)
    val settings = SettingsStore(app)
    val httpClient = ApiFactory.baseClient()
    private val api = ApiFactory.api(httpClient) { credentials.cached?.apiKey }
    val repository = RaRepository(api)
    val dualScreen = DualScreenManager(app)
    val music = MusicController(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val appState = AppState(
        repository = repository,
        credentials = credentials,
        settings = settings,
        scope = scope,
    )

    init {
        JaColors.palette = Palettes.byId(settings.themeId.value)
    }
}

val Context.graph: AppGraph get() = (applicationContext as JaApp).graph
