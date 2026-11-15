package app.jusachievements.data

import app.jusachievements.data.api.Achievement
import app.jusachievements.data.api.ApiFactory
import app.jusachievements.data.api.CompletionEntry
import app.jusachievements.data.api.GameListEntry
import app.jusachievements.data.api.RaApi
import app.jusachievements.data.api.RecentGame
import app.jusachievements.data.api.UserProfile
import app.jusachievements.data.api.WantToPlayEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.ConcurrentHashMap

class RaRepository(private val api: RaApi) {
    private val gameCache = ConcurrentHashMap<Int, GameDetail>()
    private val consoleCache = ConcurrentHashMap<Int, List<GameListEntry>>()

    suspend fun validate(user: String, apiKey: String): UserProfile = api.validate(user, apiKey)

    suspend fun profile(user: String): UserProfile = api.profile(user)

    suspend fun recentlyPlayed(user: String): List<RecentGame> = api.recentlyPlayed(user, count = 50)

    /** Every game the user has progress in, paging through the endpoint (max 500 per call). */
    suspend fun library(user: String): List<CompletionEntry> {
        val all = ArrayList<CompletionEntry>()
        var offset = 0
        repeat(MAX_PAGES) {
            val page = api.completionProgress(user, count = PAGE, offset = offset)
            all += page.results
            offset += page.results.size
            if (page.results.isEmpty() || offset >= page.total) return all
        }
        return all
    }

    suspend fun wantToPlay(user: String): List<WantToPlayEntry> = api.wantToPlay(user).results

    suspend fun gameDetail(gameId: Int, user: String, force: Boolean = false): GameDetail {
        if (!force) gameCache[gameId]?.let { return it }
        return Mappers.detail(api.gameProgress(gameId, user)).also { gameCache[gameId] = it }
    }

    suspend fun consoleGames(consoleId: Int): List<GameListEntry> =
        consoleCache[consoleId] ?: api.gameList(consoleId).also { consoleCache[consoleId] = it }

    suspend fun achievementComments(achievementId: Int): List<CommentUi> =
        api.achievementComments(achievementId).results
            .filter { it.user.isNotBlank() && !it.user.equals("Server", ignoreCase = true) }
            .map { CommentUi(it.user, ApiFactory.userPic(it.user), it.text.trim(), RaDates.parse(it.submitted)) }

    /** People you follow plus a merged feed of their unlocks over the last [days] days. */
    suspend fun social(days: Int = 7): Social = coroutineScope {
        val follows = api.usersIFollow().results.filter { it.user.isNotBlank() }.take(MAX_FRIENDS)
        val gate = Semaphore(4) // be gentle with the API
        val perUser = follows.map { f ->
            async {
                gate.withPermit {
                    runCatching { api.recentUnlocks(f.user, days * 24 * 60) }.getOrDefault(emptyList())
                }.map { u ->
                    FeedItem(
                        user = f.user,
                        avatarUrl = ApiFactory.userPic(f.user),
                        achievementId = u.achievementId,
                        title = u.title,
                        description = u.description,
                        badgeUrl = ApiFactory.badge(u.badgeName),
                        points = u.points,
                        hardcore = u.hardcoreMode == 1,
                        gameId = u.gameId,
                        gameTitle = u.gameTitle,
                        gameIcon = u.gameIcon,
                        consoleName = u.consoleName,
                        at = RaDates.parse(u.date),
                    )
                }
            }
        }.awaitAll()
        val friends = follows.mapIndexed { i, f ->
            Friend(
                user = f.user,
                avatarUrl = ApiFactory.userPic(f.user),
                points = f.points,
                softcorePoints = f.pointsSoftcore,
                followsMe = f.isFollowingMe,
                latest = perUser[i].maxByOrNull { it.at ?: java.time.Instant.EPOCH },
                unlocksThisWeek = perUser[i].size,
            )
        }.sortedByDescending { it.latest?.at ?: java.time.Instant.EPOCH }
        Social(friends, perUser.flatten().sortedByDescending { it.at ?: java.time.Instant.EPOCH }.take(MAX_FEED))
    }

    suspend fun summary(user: String) = api.userSummary(user)

    /** A friend's milestones: one summary call (with their recent games) plus their library. */
    suspend fun friendProfile(user: String): FriendProfile = coroutineScope {
        val summary = async { api.userSummary(user, recentGames = 5) }
        val library = async { library(user) }
        val s = summary.await()
        val games = Mappers.library(library.await(), s.recentlyPlayed)
        FriendProfile(
            profile = Mappers.profile(s, user),
            games = games.size,
            unlocked = games.sumOf { it.earned },
            beaten = games.count { it.award != null },
            mastered = games.count { it.mastered },
            recent = s.recentlyPlayed.map(Mappers::recent),
        )
    }

    /**
     * Your rarest unlocks, rarest first. One call lists every unlock; the RetroPoints-to-points
     * ratio picks likely-rare candidates; then only those games are loaded for exact unlock rates.
     */
    suspend fun rarest(user: String, limit: Int = 20): List<RareUnlock> = coroutineScope {
        val all = api.earnedBetween(user, 0, System.currentTimeMillis() / 1000)
            .filter { it.points > 0 }
            .distinctBy { it.achievementId }
        val gameIds = all.sortedByDescending { it.trueRatio.toDouble() / it.points }
            .take(120).map { it.gameId }.distinct().take(MAX_RARE_GAMES)
        val gate = Semaphore(3)
        gameIds.map { id -> async { gate.withPermit { runCatching { gameDetail(id, user) }.getOrNull() } } }
            .awaitAll()
            .filterNotNull()
            .flatMap { g ->
                g.achievements.filter { it.unlocked && it.unlockRate != null }
                    .map { RareUnlock(it, g.id, g.title, g.consoleName) }
            }
            .sortedBy { it.achievement.unlockRate }
            .take(limit)
    }

    /** Achievement of the Week plus community event sets, most recently updated first. */
    suspend fun events(): Events {
        val aotw = runCatching { api.achievementOfTheWeek() }.getOrNull()?.let { r ->
            val a = r.achievement ?: return@let null
            AotwUi(
                title = a.title,
                description = a.description,
                points = a.points,
                badgeUrl = a.badgeName?.takeIf { it.isNotBlank() }?.let { ApiFactory.badge(it) },
                gameId = r.game?.id ?: 0,
                gameTitle = r.game?.title.orEmpty(),
                consoleName = r.console?.title.orEmpty(),
                unlocks = r.unlocksCount,
            )
        }
        val events = api.gameList(EVENTS_CONSOLE_ID)
            .map { EventUi(it.id, it.title, it.imageIcon, it.numAchievements, it.points, RaDates.parse(it.dateModified)) }
            .sortedByDescending { it.updated ?: java.time.Instant.EPOCH }
            .take(40)
        return Events(aotw, events)
    }

    fun invalidateGames() = gameCache.clear()

    fun clear() {
        gameCache.clear()
        consoleCache.clear()
    }

    private companion object {
        const val PAGE = 500
        const val MAX_PAGES = 20
        const val MAX_FRIENDS = 25
        const val MAX_RARE_GAMES = 15
        const val EVENTS_CONSOLE_ID = 101 // RetroAchievements files community events under this system
        const val MAX_FEED = 150
    }
}

internal object RaJson {
    fun achievements(element: JsonElement?): List<Achievement> {
        val obj = element as? JsonObject ?: return emptyList() // `[]` when a game has none
        return obj.values.mapNotNull {
            runCatching { ApiFactory.json.decodeFromJsonElement(Achievement.serializer(), it) }.getOrNull()
        }
    }
}
