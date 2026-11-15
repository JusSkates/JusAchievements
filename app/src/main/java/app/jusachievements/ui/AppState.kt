package app.jusachievements.ui

import app.jusachievements.data.AchievementUi
import app.jusachievements.data.CommentUi
import app.jusachievements.data.Credentials
import app.jusachievements.data.Events
import app.jusachievements.data.FriendProfile
import app.jusachievements.data.RareUnlock
import app.jusachievements.data.FeedItem
import app.jusachievements.data.Social
import app.jusachievements.data.CredentialStore
import app.jusachievements.data.GameDetail
import app.jusachievements.data.GameSummary
import app.jusachievements.data.Mappers
import app.jusachievements.data.Profile
import app.jusachievements.data.RaRepository
import app.jusachievements.data.SettingsStore
import app.jusachievements.domain.RecSection
import app.jusachievements.domain.Recommender
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

enum class Tab(val label: String) {
    HOME("Home"), RECENT("Recent"), LIBRARY("Library"), FOR_YOU("For You"), TOP("Top"),
    FEED("Feed"), FRIENDS("Friends"), EVENTS("Events")
}

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val data: T, val refreshing: Boolean = false) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}

data class Dashboard(
    val profile: Profile,
    val recent: List<GameSummary>,
    val library: List<GameSummary>,
    val recommendations: List<RecSection>,
)

enum class StatusFilter(val label: String) { ALL("All"), IN_PROGRESS("In progress"), BEATEN("Beaten"), MASTERED("Mastered") }
enum class SortOrder(val label: String) { RECENT("Recent"), PROGRESS("Progress"), TITLE("A–Z") }

data class LibraryFilter(
    val status: StatusFilter = StatusFilter.ALL,
    val sort: SortOrder = SortOrder.RECENT,
    val consoleId: Int? = null,
)

/**
 * Process-wide UI state. Both the main-screen activity and the second-screen companion observe
 * this same object, which is what keeps the two screens in sync (select on one, show on the other).
 */
class AppState(
    private val repository: RaRepository,
    private val credentials: CredentialStore,
    val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val _session = MutableStateFlow(credentials.load())
    val session: StateFlow<Credentials?> = _session.asStateFlow()

    private val _tab = MutableStateFlow(Tab.HOME)
    val tab: StateFlow<Tab> = _tab.asStateFlow()

    private val _selectedGameId = MutableStateFlow<Int?>(null)
    val selectedGameId: StateFlow<Int?> = _selectedGameId.asStateFlow()

    private val _dashboard = MutableStateFlow<Load<Dashboard>>(Load.Loading)
    val dashboard: StateFlow<Load<Dashboard>> = _dashboard.asStateFlow()

    private val _game = MutableStateFlow<Load<GameDetail>?>(null)
    val game: StateFlow<Load<GameDetail>?> = _game.asStateFlow()

    private val _libraryFilter = MutableStateFlow(LibraryFilter())
    val libraryFilter: StateFlow<LibraryFilter> = _libraryFilter.asStateFlow()

    private val _social = MutableStateFlow<Load<Social>>(Load.Loading)
    /** People you follow and their recent unlocks. Loaded after the dashboard. */
    val social: StateFlow<Load<Social>> = _social.asStateFlow()

    private val _top = MutableStateFlow<Load<List<RareUnlock>>>(Load.Loading)
    /** Your 20 rarest unlocked achievements, rarest first. */
    val top: StateFlow<Load<List<RareUnlock>>> = _top.asStateFlow()

    private val _events = MutableStateFlow<Load<Events>>(Load.Loading)
    val events: StateFlow<Load<Events>> = _events.asStateFlow()

    private val _friend = MutableStateFlow<Load<FriendProfile>?>(null)
    /** The friend whose milestones panel is open, if any. */
    val friend: StateFlow<Load<FriendProfile>?> = _friend.asStateFlow()
    private var friendJob: Job? = null

    private val _achievement = MutableStateFlow<AchievementUi?>(null)
    /** The achievement whose detail panel is open, if any. */
    val achievement: StateFlow<AchievementUi?> = _achievement.asStateFlow()

    private val _comments = MutableStateFlow<Load<List<CommentUi>>>(Load.Loading)
    val comments: StateFlow<Load<List<CommentUi>>> = _comments.asStateFlow()

    /** True while the companion activity is alive on a second display. */
    val companionActive = MutableStateFlow(false)

    /** The main screen sets this; the companion finishes itself when it turns false. */
    val companionWanted = MutableStateFlow(false)

    /** False until the boot animation has played (once per app launch). */
    val splashDone = MutableStateFlow(false)

    private var refreshJob: Job? = null
    private var gameJob: Job? = null
    private var socialJob: Job? = null
    private var topJob: Job? = null
    private var eventsJob: Job? = null
    private var commentsJob: Job? = null

    init {
        if (_session.value != null) refresh()
    }

    // ---- Session ----------------------------------------------------------------------------

    /** Returns null on success, or a user-facing error message. */
    suspend fun login(username: String, apiKey: String): String? {
        val user = username.trim()
        val key = apiKey.trim()
        if (user.isEmpty() || key.isEmpty()) return "Enter your username and Web API key."
        return try {
            val profile = repository.validate(user, key)
            val creds = Credentials(profile.user.ifBlank { user }, key)
            credentials.save(creds)
            _session.value = creds
            refresh()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is HttpException && (e.code() == 401 || e.code() == 403)) {
                "That username and API key didn't match. Check both and try again."
            } else {
                errorMessage(e)
            }
        }
    }

    fun logout() {
        refreshJob?.cancel()
        gameJob?.cancel()
        socialJob?.cancel()
        commentsJob?.cancel()
        _social.value = Load.Loading
        topJob?.cancel()
        eventsJob?.cancel()
        socialJob = null
        topJob = null
        eventsJob = null
        _top.value = Load.Loading
        _events.value = Load.Loading
        _achievement.value = null
        closeFriend()
        credentials.clear()
        repository.clear()
        _session.value = null
        _dashboard.value = Load.Loading
        _selectedGameId.value = null
        _game.value = null
        _tab.value = Tab.HOME
        _libraryFilter.value = LibraryFilter()
    }

    // ---- Data -------------------------------------------------------------------------------

    fun refresh() {
        val user = _session.value?.username ?: return
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val previous = (_dashboard.value as? Load.Ready)?.data
            _dashboard.value = if (previous != null) Load.Ready(previous, refreshing = true) else Load.Loading
            _dashboard.value = try {
                repository.invalidateGames()
                Load.Ready(loadDashboard(user))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val wait = retryAfterSeconds(e)
                if (wait != null) {
                    // Told to slow down: keep what's on screen, wait exactly that long, try again.
                    _dashboard.value = if (previous != null) Load.Ready(previous) else Load.Failed(
                        "RetroAchievements asked JA to slow down. Trying again automatically in about ${wait}s."
                    )
                    delay((wait + 1) * 1000)
                    refresh()
                    return@launch
                }
                if (previous != null) Load.Ready(previous) else Load.Failed(errorMessage(e))
            }
            _selectedGameId.value?.let { loadGame(it, force = true) }
            // Only reload the extra tabs that have actually been opened.
            if (socialJob != null) loadSocial()
            if (topJob != null) loadTop()
            if (eventsJob != null) loadEvents()
        }
    }

    /** Feed / Friends / Top / Events load the first time their tab is opened, not at startup. */
    private fun ensureLoaded(tab: Tab) {
        when (tab) {
            Tab.FEED, Tab.FRIENDS -> if (socialJob == null) loadSocial()
            Tab.TOP -> if (topJob == null) loadTop()
            Tab.EVENTS -> if (eventsJob == null) loadEvents()
            else -> Unit
        }
    }

    private fun loadTop() {
        val user = _session.value?.username ?: return
        topJob?.cancel()
        topJob = loadInto(_top) { repository.rarest(user) }
    }

    private fun loadEvents() {
        eventsJob?.cancel()
        eventsJob = loadInto(_events) { repository.events() }
    }

    private suspend fun loadDashboard(user: String): Dashboard = coroutineScope {
        val profile = async { repository.profile(user) }
        val summary = async { runCatching { repository.summary(user) }.getOrNull() }
        val recent = async { repository.recentlyPlayed(user) }
        val library = async { repository.library(user) }
        val wantToPlay = async { runCatching { repository.wantToPlay(user) }.getOrDefault(emptyList()) }

        val recentGames = recent.await()
        val games = Mappers.library(library.await(), recentGames)

        val favourites = Recommender.topConsoles(games, count = 2)
        val discovery = favourites
            .map { id -> async { id to runCatching { repository.consoleGames(id) }.getOrDefault(emptyList()) } }
            .awaitAll()
            .toMap()

        Dashboard(
            profile = Mappers.profile(profile.await(), user, summary.await()),
            recent = recentGames.map(Mappers::recent),
            library = games,
            recommendations = Recommender.build(games, wantToPlay.await(), discovery),
        )
    }

    private fun <T> loadInto(target: MutableStateFlow<Load<T>>, block: suspend () -> T): Job = scope.launch {
        val previous = (target.value as? Load.Ready)?.data
        target.value = try {
            Load.Ready(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (previous != null) Load.Ready(previous) else Load.Failed(errorMessage(e))
        }
    }

    /** Opens a game and jumps straight to one of its achievements (from the Top tab). */
    fun openRare(rare: RareUnlock) {
        selectGame(rare.gameId)
        openAchievement(rare.achievement)
    }

    private fun loadSocial() {
        socialJob?.cancel()
        socialJob = scope.launch {
            val previous = (_social.value as? Load.Ready)?.data
            _social.value = try {
                Load.Ready(repository.social())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (previous != null) Load.Ready(previous) else Load.Failed(errorMessage(e))
            }
        }
    }

    fun openAchievement(achievement: AchievementUi) {
        _achievement.value = achievement
        commentsJob?.cancel()
        _comments.value = Load.Loading
        commentsJob = scope.launch {
            _comments.value = try {
                Load.Ready(repository.achievementComments(achievement.id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Load.Failed(errorMessage(e))
            }
        }
    }

    fun openFriend(user: String) {
        friendJob?.cancel()
        _friend.value = Load.Loading
        friendJob = scope.launch {
            _friend.value = try {
                Load.Ready(repository.friendProfile(user))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Load.Failed(errorMessage(e))
            }
        }
    }

    fun closeFriend() {
        friendJob?.cancel()
        _friend.value = null
    }

    fun closeAchievement() {
        commentsJob?.cancel()
        _achievement.value = null
    }

    /** Tabs currently in the tab bar (Home can't be removed). */
    fun visibleTabs(): List<Tab> = visibleTabs(settings.hiddenTabs.value)

    // ---- Navigation -------------------------------------------------------------------------

    fun selectTab(tab: Tab) {
        _tab.value = tab
        ensureLoaded(tab)
    }

    fun cycleTab(delta: Int) {
        val tabs = visibleTabs()
        _tab.value = tabs[(tabs.indexOf(_tab.value).coerceAtLeast(0) + delta).mod(tabs.size)]
        ensureLoaded(_tab.value)
    }

    /**
     * B / Back: close the open game, then return to the Home tab. On Home it does nothing, so
     * the app never quits by accident (leave with the system Home button).
     */
    fun back() {
        when {
            _achievement.value != null -> closeAchievement()
            _friend.value != null -> closeFriend()
            _selectedGameId.value != null -> clearSelection()
            _tab.value != Tab.HOME -> _tab.value = Tab.HOME
        }
    }

    fun selectGame(gameId: Int) {
        closeAchievement()
        closeFriend()
        if (_selectedGameId.value == gameId && _game.value is Load.Ready) return
        _selectedGameId.value = gameId
        loadGame(gameId, force = false)
    }

    fun clearSelection() {
        gameJob?.cancel()
        closeAchievement()
        _selectedGameId.value = null
        _game.value = null
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        _libraryFilter.value = filter
    }

    /** Moves the selection through the list on the current tab (gamepad on the main screen). */
    fun moveSelection(delta: Int) {
        val data = (_dashboard.value as? Load.Ready)?.data ?: return
        val feed = (_social.value as? Load.Ready)?.data?.feed.orEmpty()
        val ids = when (_tab.value) {
            Tab.TOP -> (_top.value as? Load.Ready)?.data.orEmpty().map { it.gameId }.distinct()
            Tab.EVENTS -> (_events.value as? Load.Ready)?.data?.events.orEmpty().map { it.id }
            else -> idsForTab(_tab.value, data, _libraryFilter.value, feed)
        }
        if (ids.isEmpty()) return
        val current = ids.indexOf(_selectedGameId.value)
        val next = if (current < 0) (if (delta > 0) 0 else ids.lastIndex) else (current + delta).coerceIn(0, ids.lastIndex)
        selectGame(ids[next])
    }

    private fun loadGame(gameId: Int, force: Boolean) {
        val user = _session.value?.username ?: return
        gameJob?.cancel()
        gameJob = scope.launch {
            if (!force || _game.value !is Load.Ready) _game.value = Load.Loading
            _game.value = try {
                Load.Ready(repository.gameDetail(gameId, user, force))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Load.Failed(errorMessage(e))
            }
        }
    }

    companion object {
        fun filteredLibrary(games: List<GameSummary>, filter: LibraryFilter): List<GameSummary> {
            val filtered = games.filter { g ->
                (filter.consoleId == null || g.consoleId == filter.consoleId) && when (filter.status) {
                    StatusFilter.ALL -> true
                    StatusFilter.IN_PROGRESS -> g.earned > 0 && g.earned < g.total && g.award == null
                    StatusFilter.BEATEN -> g.award != null
                    StatusFilter.MASTERED -> g.award == app.jusachievements.data.AwardKind.MASTERED ||
                        g.award == app.jusachievements.data.AwardKind.COMPLETED
                }
            }
            return when (filter.sort) {
                SortOrder.RECENT -> filtered.sortedByDescending { it.lastActivity }
                SortOrder.PROGRESS -> filtered.sortedWith(compareByDescending<GameSummary> { it.progress }.thenByDescending { it.earned })
                SortOrder.TITLE -> filtered.sortedBy { it.title.lowercase() }
            }
        }

        fun visibleTabs(hidden: Set<String>): List<Tab> =
            Tab.entries.filter { it == Tab.HOME || it.name !in hidden }

        fun idsForTab(tab: Tab, data: Dashboard, filter: LibraryFilter, feed: List<FeedItem> = emptyList()): List<Int> = when (tab) {
            Tab.FEED -> feed.map { it.gameId }.distinct()
            Tab.FRIENDS, Tab.TOP, Tab.EVENTS -> emptyList()
            Tab.HOME -> data.recent.take(12).map { it.id }
            Tab.RECENT -> data.recent.map { it.id }
            Tab.LIBRARY -> filteredLibrary(data.library, filter).map { it.id }
            Tab.FOR_YOU -> data.recommendations.flatMap { s -> s.items.map { it.gameId } }.distinct()
        }

        /** Seconds the server asked us to wait, if this was a rate-limit (429) error. */
        fun retryAfterSeconds(e: Throwable): Long? {
            val http = e as? HttpException ?: return null
            if (http.code() != 429) return null
            return http.response()?.headers()?.get("Retry-After")?.toLongOrNull()?.coerceIn(1, 600) ?: 30L
        }

        fun errorMessage(e: Throwable): String = when (e) {
            is HttpException -> when (e.code()) {
                401, 403 -> "RetroAchievements rejected the API key. Sign out and back in."
                429 -> "RetroAchievements asked JA to slow down. Try again in about ${retryAfterSeconds(e) ?: 60}s."
                in 500..599 -> "RetroAchievements is having trouble right now (${e.code()})."
                else -> "Request failed (${e.code()})."
            }
            is IOException -> "Can't reach RetroAchievements. Check your connection."
            else -> e.message ?: "Something went wrong."
        }
    }
}
