package app.jusachievements.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * Wire models for the RetroAchievements Web API (https://api-docs.retroachievements.org).
 * Everything is defaulted/nullable: the API is old, and fields come and go between endpoints.
 */

@Serializable
data class UserProfile(
    @SerialName("User") val user: String = "",
    @SerialName("ULID") val ulid: String? = null,
    @SerialName("UserPic") val userPic: String? = null,
    @SerialName("MemberSince") val memberSince: String? = null,
    @SerialName("RichPresenceMsg") val richPresence: String? = null,
    @SerialName("LastGameID") val lastGameId: Int? = null,
    @SerialName("TotalPoints") val totalPoints: Int = 0,
    @SerialName("TotalSoftcorePoints") val totalSoftcorePoints: Int = 0,
    @SerialName("TotalTruePoints") val totalTruePoints: Int = 0,
    @SerialName("Motto") val motto: String? = null,
)

@Serializable
data class RecentGame(
    @SerialName("GameID") val gameId: Int,
    @SerialName("ConsoleID") val consoleId: Int = 0,
    @SerialName("ConsoleName") val consoleName: String = "",
    @SerialName("Title") val title: String = "",
    @SerialName("ImageIcon") val imageIcon: String? = null,
    @SerialName("ImageTitle") val imageTitle: String? = null,
    @SerialName("ImageIngame") val imageIngame: String? = null,
    @SerialName("ImageBoxArt") val imageBoxArt: String? = null,
    @SerialName("LastPlayed") val lastPlayed: String? = null,
    @SerialName("NumPossibleAchievements") val numPossible: Int = 0,
    @SerialName("PossibleScore") val possibleScore: Int = 0,
    @SerialName("NumAchieved") val numAchieved: Int = 0,
    @SerialName("ScoreAchieved") val scoreAchieved: Int = 0,
    @SerialName("NumAchievedHardcore") val numAchievedHardcore: Int = 0,
)

@Serializable
data class CompletionPage(
    @SerialName("Count") val count: Int = 0,
    @SerialName("Total") val total: Int = 0,
    @SerialName("Results") val results: List<CompletionEntry> = emptyList(),
)

@Serializable
data class CompletionEntry(
    @SerialName("GameID") val gameId: Int,
    @SerialName("Title") val title: String = "",
    @SerialName("ImageIcon") val imageIcon: String? = null,
    @SerialName("ConsoleID") val consoleId: Int = 0,
    @SerialName("ConsoleName") val consoleName: String = "",
    @SerialName("MaxPossible") val maxPossible: Int = 0,
    @SerialName("NumAwarded") val numAwarded: Int = 0,
    @SerialName("NumAwardedHardcore") val numAwardedHardcore: Int = 0,
    @SerialName("MostRecentAwardedDate") val mostRecentAwardedDate: String? = null,
    @SerialName("HighestAwardKind") val highestAwardKind: String? = null,
)

@Serializable
data class WantToPlayPage(
    @SerialName("Count") val count: Int = 0,
    @SerialName("Total") val total: Int = 0,
    @SerialName("Results") val results: List<WantToPlayEntry> = emptyList(),
)

@Serializable
data class WantToPlayEntry(
    @SerialName("ID") val id: Int,
    @SerialName("Title") val title: String = "",
    @SerialName("ImageIcon") val imageIcon: String? = null,
    @SerialName("ConsoleID") val consoleId: Int = 0,
    @SerialName("ConsoleName") val consoleName: String = "",
    @SerialName("PointsTotal") val pointsTotal: Int = 0,
    @SerialName("AchievementsPublished") val achievementsPublished: Int = 0,
)

@Serializable
data class GameListEntry(
    @SerialName("ID") val id: Int,
    @SerialName("Title") val title: String = "",
    @SerialName("ConsoleID") val consoleId: Int = 0,
    @SerialName("ConsoleName") val consoleName: String = "",
    @SerialName("ImageIcon") val imageIcon: String? = null,
    @SerialName("NumAchievements") val numAchievements: Int = 0,
    @SerialName("Points") val points: Int = 0,
    @SerialName("DateModified") val dateModified: String? = null,
)

@Serializable
data class GameProgress(
    @SerialName("ID") val id: Int,
    @SerialName("Title") val title: String = "",
    @SerialName("ConsoleID") val consoleId: Int = 0,
    @SerialName("ConsoleName") val consoleName: String = "",
    @SerialName("ImageIcon") val imageIcon: String? = null,
    @SerialName("ImageTitle") val imageTitle: String? = null,
    @SerialName("ImageIngame") val imageIngame: String? = null,
    @SerialName("ImageBoxArt") val imageBoxArt: String? = null,
    @SerialName("Publisher") val publisher: String? = null,
    @SerialName("Developer") val developer: String? = null,
    @SerialName("Genre") val genre: String? = null,
    @SerialName("Released") val released: String? = null,
    @SerialName("NumAchievements") val numAchievements: Int = 0,
    @SerialName("NumDistinctPlayers") val numDistinctPlayers: Int = 0,
    // An object keyed by achievement id — or `[]` when the game has none, hence JsonElement.
    @SerialName("Achievements") val achievements: JsonElement? = null,
    @SerialName("NumAwardedToUser") val numAwardedToUser: Int = 0,
    @SerialName("NumAwardedToUserHardcore") val numAwardedToUserHardcore: Int = 0,
    @SerialName("HighestAwardKind") val highestAwardKind: String? = null,
)

@Serializable
data class Achievement(
    @SerialName("ID") val id: Int,
    @SerialName("Title") val title: String = "",
    @SerialName("Description") val description: String = "",
    @SerialName("Points") val points: Int = 0,
    @SerialName("TrueRatio") val trueRatio: Int = 0,
    @SerialName("BadgeName") val badgeName: String = "",
    @SerialName("DisplayOrder") val displayOrder: Int = 0,
    @SerialName("NumAwarded") val numAwarded: Int = 0,
    @SerialName("NumAwardedHardcore") val numAwardedHardcore: Int = 0,
    @SerialName("DateEarned") val dateEarned: String? = null,
    @SerialName("DateEarnedHardcore") val dateEarnedHardcore: String? = null,
    @SerialName("type") val type: String? = null,
)

@Serializable
data class CommentsPage(
    @SerialName("Count") val count: Int = 0,
    @SerialName("Total") val total: Int = 0,
    @SerialName("Results") val results: List<CommentEntry> = emptyList(),
)

@Serializable
data class CommentEntry(
    @SerialName("User") val user: String = "",
    @SerialName("Submitted") val submitted: String? = null,
    @SerialName("CommentText") val text: String = "",
)

@Serializable
data class FollowPage(
    @SerialName("Count") val count: Int = 0,
    @SerialName("Total") val total: Int = 0,
    @SerialName("Results") val results: List<FollowEntry> = emptyList(),
)

@Serializable
data class FollowEntry(
    @SerialName("User") val user: String = "",
    @SerialName("Points") val points: Int = 0,
    @SerialName("PointsSoftcore") val pointsSoftcore: Int = 0,
    @SerialName("IsFollowingMe") val isFollowingMe: Boolean = false,
)

@Serializable
data class RecentUnlock(
    @SerialName("Date") val date: String? = null,
    @SerialName("HardcoreMode") val hardcoreMode: Int = 0,
    @SerialName("AchievementID") val achievementId: Int = 0,
    @SerialName("Title") val title: String = "",
    @SerialName("Description") val description: String = "",
    @SerialName("BadgeName") val badgeName: String = "",
    @SerialName("Points") val points: Int = 0,
    @SerialName("TrueRatio") val trueRatio: Int = 0,
    @SerialName("GameTitle") val gameTitle: String = "",
    @SerialName("GameIcon") val gameIcon: String? = null,
    @SerialName("GameID") val gameId: Int = 0,
    @SerialName("ConsoleName") val consoleName: String = "",
)

@Serializable
data class UserSummary(
    @SerialName("User") val user: String = "",
    @SerialName("UserPic") val userPic: String? = null,
    @SerialName("MemberSince") val memberSince: String? = null,
    @SerialName("Motto") val motto: String? = null,
    @SerialName("RichPresenceMsg") val richPresence: String? = null,
    @SerialName("RichPresenceMsgDate") val richPresenceDate: String? = null,
    @SerialName("LastGameID") val lastGameId: Int? = null,
    @SerialName("TotalPoints") val totalPoints: Int = 0,
    @SerialName("TotalSoftcorePoints") val totalSoftcorePoints: Int = 0,
    @SerialName("TotalTruePoints") val totalTruePoints: Int = 0,
    /** Hardcore rank only: the API has no casual (softcore) rank. Null when unranked. */
    @SerialName("Rank") val rank: Int? = null,
    @SerialName("TotalRanked") val totalRanked: Int? = null,
    @SerialName("Status") val status: String? = null,
    @SerialName("RecentlyPlayed") val recentlyPlayed: List<RecentGame> = emptyList(),
)

@Serializable
data class AotwResponse(
    @SerialName("Achievement") val achievement: AotwAchievement? = null,
    @SerialName("Console") val console: AotwNamed? = null,
    @SerialName("Game") val game: AotwNamed? = null,
    @SerialName("StartAt") val startAt: String? = null,
    @SerialName("TotalPlayers") val totalPlayers: Int = 0,
    @SerialName("UnlocksCount") val unlocksCount: Int = 0,
)

@Serializable
data class AotwAchievement(
    @SerialName("ID") val id: Int = 0,
    @SerialName("Title") val title: String = "",
    @SerialName("Description") val description: String = "",
    @SerialName("Points") val points: Int = 0,
    @SerialName("BadgeName") val badgeName: String? = null,
)

@Serializable
data class AotwNamed(
    @SerialName("ID") val id: Int = 0,
    @SerialName("Title") val title: String = "",
)
