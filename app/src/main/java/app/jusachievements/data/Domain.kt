package app.jusachievements.data

import app.jusachievements.data.api.ApiFactory
import app.jusachievements.data.api.CompletionEntry
import app.jusachievements.data.api.GameProgress
import app.jusachievements.data.api.RecentGame
import app.jusachievements.data.api.UserProfile
import app.jusachievements.data.api.UserSummary
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class AwardKind(val label: String) {
    MASTERED("Mastered"),
    COMPLETED("Completed"),
    BEATEN_HARDCORE("Beaten"),
    BEATEN_SOFTCORE("Beaten (softcore)");

    companion object {
        fun from(raw: String?): AwardKind? = when (raw?.lowercase()) {
            "mastered" -> MASTERED
            "completed" -> COMPLETED
            "beaten-hardcore" -> BEATEN_HARDCORE
            "beaten-softcore" -> BEATEN_SOFTCORE
            else -> null
        }
    }
}

/** One game the user has played, merged from the library and recently-played endpoints. */
data class GameSummary(
    val id: Int,
    val title: String,
    val icon: String?,
    val heroImage: String?,
    val consoleId: Int,
    val consoleName: String,
    val earned: Int,
    val earnedHardcore: Int,
    val total: Int,
    val lastPlayed: Instant?,
    val lastUnlock: Instant?,
    val award: AwardKind?,
    /** Title-screen art (16:9-ish), used for big cards. */
    val titleImage: String? = null,
) {
    val progress: Float get() = if (total > 0) earned.toFloat() / total else 0f
    val mastered: Boolean
        get() = award == AwardKind.MASTERED || award == AwardKind.COMPLETED || (total > 0 && earned >= total)
    val lastActivity: Instant? get() = listOfNotNull(lastPlayed, lastUnlock).maxOrNull()
}

data class AchievementUi(
    val id: Int,
    val title: String,
    val description: String,
    val points: Int,
    val trueRatio: Int,
    val badgeUrl: String,
    val lockedBadgeUrl: String,
    val earnedAt: Instant?,
    val hardcore: Boolean,
    val type: String?,
    /** Share of the game's players who have this unlocked, 0..1. */
    val unlockRate: Float?,
) {
    val unlocked: Boolean get() = earnedAt != null
    /** Fewer than 3% of the game's players have it. */
    val rare: Boolean get() = unlockRate != null && unlockRate < 0.03f
}

data class GameDetail(
    val id: Int,
    val title: String,
    val consoleName: String,
    val icon: String?,
    val imageTitle: String?,
    val imageIngame: String?,
    val boxArt: String?,
    val publisher: String?,
    val developer: String?,
    val genre: String?,
    val released: String?,
    val players: Int,
    val achievements: List<AchievementUi>,
    val earned: Int,
    val earnedHardcore: Int,
    val award: AwardKind?,
) {
    val total: Int get() = achievements.size
    val progress: Float get() = if (total > 0) earned.toFloat() / total else 0f
    /** Every achievement in the set is unlocked. */
    val complete: Boolean get() = total > 0 && earned >= total
    val pointsEarned: Int get() = achievements.filter { it.unlocked }.sumOf { it.points }
    val pointsTotal: Int get() = achievements.sumOf { it.points }
}

data class Profile(
    val user: String,
    val avatarUrl: String,
    val motto: String?,
    val richPresence: String?,
    val memberSince: String?,
    val points: Int,
    val softcorePoints: Int,
    val truePoints: Int,
    val lastGameId: Int?,
    /** Site rank; null when unranked (RetroAchievements only ranks accounts above a points threshold). */
    val rank: Int? = null,
    val totalRanked: Int? = null,
    /** When the account was last seen playing. */
    val lastActivity: Instant? = null,
) {
    /** Most of this player's points are casual (softcore), like the website's "Casual Points". */
    val casual: Boolean get() = softcorePoints > points
    val headlinePoints: Int get() = maxOf(points, softcorePoints)
}

object RaDates {
    private val sqlFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun parse(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(raw, sqlFormat).toInstant(ZoneOffset.UTC) }.getOrNull()
            ?: runCatching { Instant.parse(raw) }.getOrNull()
    }
}

object Mappers {
    fun profile(p: UserProfile, fallbackUser: String, summary: UserSummary? = null): Profile {
        val user = p.user.ifBlank { fallbackUser }
        return Profile(
            user = user,
            avatarUrl = ApiFactory.media(p.userPic) ?: ApiFactory.userPic(user),
            motto = p.motto?.takeIf { it.isNotBlank() },
            richPresence = p.richPresence?.takeIf { it.isNotBlank() },
            memberSince = p.memberSince,
            points = p.totalPoints,
            softcorePoints = p.totalSoftcorePoints,
            truePoints = p.totalTruePoints,
            lastGameId = p.lastGameId,
            rank = summary?.rank?.takeIf { it > 0 },
            totalRanked = summary?.totalRanked?.takeIf { it > 0 },
            lastActivity = RaDates.parse(summary?.richPresenceDate),
        )
    }

    /** A profile built from the summary endpoint alone (used for friends). */
    fun profile(s: UserSummary, fallbackUser: String): Profile {
        val user = s.user.ifBlank { fallbackUser }
        return Profile(
            user = user,
            avatarUrl = ApiFactory.media(s.userPic) ?: ApiFactory.userPic(user),
            motto = s.motto?.takeIf { it.isNotBlank() },
            richPresence = s.richPresence?.takeIf { it.isNotBlank() },
            memberSince = s.memberSince,
            points = s.totalPoints,
            softcorePoints = s.totalSoftcorePoints,
            truePoints = s.totalTruePoints,
            lastGameId = s.lastGameId,
            rank = s.rank?.takeIf { it > 0 },
            totalRanked = s.totalRanked?.takeIf { it > 0 },
            lastActivity = RaDates.parse(s.richPresenceDate),
        )
    }

    fun recent(r: RecentGame): GameSummary = GameSummary(
        id = r.gameId,
        title = r.title,
        icon = r.imageIcon,
        heroImage = r.imageIngame ?: r.imageTitle,
        consoleId = r.consoleId,
        consoleName = r.consoleName,
        earned = r.numAchieved,
        earnedHardcore = r.numAchievedHardcore,
        total = r.numPossible,
        lastPlayed = RaDates.parse(r.lastPlayed),
        lastUnlock = null,
        award = null,
        titleImage = r.imageTitle ?: r.imageIngame,
    )

    /** Library = completion progress, enriched with recently-played data where we have it. */
    fun library(entries: List<CompletionEntry>, recent: List<RecentGame>): List<GameSummary> {
        val recentById = recent.associateBy { it.gameId }
        val fromLibrary = entries.map { e ->
            val r = recentById[e.gameId]
            GameSummary(
                id = e.gameId,
                title = e.title,
                icon = e.imageIcon,
                heroImage = r?.imageIngame ?: r?.imageTitle,
                consoleId = e.consoleId,
                consoleName = e.consoleName,
                earned = e.numAwarded,
                earnedHardcore = e.numAwardedHardcore,
                total = e.maxPossible,
                lastPlayed = RaDates.parse(r?.lastPlayed),
                lastUnlock = RaDates.parse(e.mostRecentAwardedDate),
                award = AwardKind.from(e.highestAwardKind),
                titleImage = r?.imageTitle ?: r?.imageIngame,
            )
        }
        val known = fromLibrary.mapTo(HashSet()) { it.id }
        val onlyRecent = recent.filter { it.gameId !in known }.map(::recent)
        return fromLibrary + onlyRecent
    }

    fun detail(g: GameProgress): GameDetail {
        val players = g.numDistinctPlayers
        val achievements = RaJson.achievements(g.achievements)
            .sortedWith(compareBy({ it.displayOrder }, { it.id }))
            .map { a ->
                val hc = RaDates.parse(a.dateEarnedHardcore)
                val sc = RaDates.parse(a.dateEarned)
                AchievementUi(
                    id = a.id,
                    title = a.title,
                    description = a.description,
                    points = a.points,
                    trueRatio = a.trueRatio,
                    badgeUrl = ApiFactory.badge(a.badgeName),
                    lockedBadgeUrl = ApiFactory.badge(a.badgeName, locked = true),
                    earnedAt = hc ?: sc,
                    hardcore = hc != null,
                    type = a.type,
                    unlockRate = if (players > 0) (a.numAwarded.toFloat() / players).coerceIn(0f, 1f) else null,
                )
            }
        return GameDetail(
            id = g.id,
            title = g.title,
            consoleName = g.consoleName,
            icon = g.imageIcon,
            imageTitle = g.imageTitle,
            imageIngame = g.imageIngame,
            boxArt = g.imageBoxArt,
            publisher = g.publisher?.takeIf { it.isNotBlank() },
            developer = g.developer?.takeIf { it.isNotBlank() },
            genre = g.genre?.takeIf { it.isNotBlank() },
            released = g.released?.takeIf { it.isNotBlank() }?.take(4),
            players = players,
            achievements = achievements,
            earned = achievements.count { it.unlocked },
            earnedHardcore = achievements.count { it.hardcore },
            award = AwardKind.from(g.highestAwardKind),
        )
    }
}

data class CommentUi(val user: String, val avatarUrl: String, val text: String, val at: Instant?)

/** One unlock by someone you follow. */
data class FeedItem(
    val user: String,
    val avatarUrl: String,
    val achievementId: Int,
    val title: String,
    val description: String,
    val badgeUrl: String,
    val points: Int,
    val hardcore: Boolean,
    val gameId: Int,
    val gameTitle: String,
    val gameIcon: String?,
    val consoleName: String,
    val at: Instant?,
)

data class Friend(
    val user: String,
    val avatarUrl: String,
    val points: Int,
    val softcorePoints: Int,
    val followsMe: Boolean,
    val latest: FeedItem?,
    val unlocksThisWeek: Int,
)

data class Social(val friends: List<Friend>, val feed: List<FeedItem>)

/** One of your unlocked achievements, with the game it belongs to (for the "Top" tab). */
data class RareUnlock(val achievement: AchievementUi, val gameId: Int, val gameTitle: String, val consoleName: String)

data class EventUi(
    val id: Int,
    val title: String,
    val icon: String?,
    val achievements: Int,
    val points: Int,
    val updated: Instant?,
)

data class AotwUi(
    val title: String,
    val description: String,
    val points: Int,
    val badgeUrl: String?,
    val gameId: Int,
    val gameTitle: String,
    val consoleName: String,
    val unlocks: Int,
)

data class Events(val aotw: AotwUi?, val events: List<EventUi>)

/** Everything shown on a friend's milestones card. */
data class FriendProfile(
    val profile: Profile,
    val games: Int,
    val unlocked: Int,
    val beaten: Int,
    val mastered: Int,
    val recent: List<GameSummary>,
)
