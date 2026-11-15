package app.jusachievements.domain

import app.jusachievements.data.GameSummary
import app.jusachievements.data.api.GameListEntry
import app.jusachievements.data.api.WantToPlayEntry
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

data class Recommendation(
    val gameId: Int,
    val title: String,
    val icon: String?,
    val consoleName: String,
    val reason: String,
    /** Current completion 0..1 when the user has already started the game. */
    val progress: Float?,
)

data class RecSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val items: List<Recommendation>,
)

/**
 * On-device recommendations. Pure and deterministic for a given day, so the picks are stable
 * across refreshes and both screens, but rotate daily. Each pick carries a human reason.
 */
object Recommender {
    private const val PER_SECTION = 12

    /** How much the user leans into each console: played games, weighted by how far they got. */
    fun consoleAffinity(games: List<GameSummary>): Map<Int, Double> =
        games.groupBy { it.consoleId }.mapValues { (_, list) -> list.sumOf { 1.0 + it.progress * 2.0 } }

    fun topConsoles(games: List<GameSummary>, count: Int): List<Int> =
        consoleAffinity(games).entries.sortedByDescending { it.value }.take(count).map { it.key }

    fun build(
        games: List<GameSummary>,
        wantToPlay: List<WantToPlayEntry>,
        discovery: Map<Int, List<GameListEntry>>,
        now: Instant = Instant.now(),
    ): List<RecSection> {
        val owned = games.mapTo(HashSet()) { it.id }
        val affinity = consoleAffinity(games)
        val sections = mutableListOf<RecSection>()

        // 1. Over halfway, not finished: the most satisfying thing to pick up.
        games.asSequence()
            .filter { it.total > 0 && it.earned < it.total && it.progress >= 0.5f }
            .sortedByDescending { it.progress }
            .take(PER_SECTION)
            .map { g ->
                val left = g.total - g.earned
                g.toRec(if (left == 1) "Just 1 achievement to master" else "$left achievements to master")
            }
            .toList()
            .takeIf { it.isNotEmpty() }
            ?.let { sections += RecSection("close", "So close", "You're past halfway on these", it) }

        // 2. Started, stalled for a while.
        games.asSequence()
            .filter { it.total > 0 && it.earned in 1 until it.total && it.progress < 0.5f }
            .mapNotNull { g -> daysSince(g.lastActivity, now)?.takeIf { it >= 21 }?.let { g to it } }
            .sortedWith(compareByDescending<Pair<GameSummary, Long>> { it.first.progress }.thenBy { it.second })
            .take(PER_SECTION)
            .map { (g, days) -> g.toRec("No progress for ${humanDays(days)}") }
            .toList()
            .takeIf { it.isNotEmpty() }
            ?.let { sections += RecSection("revisit", "Pick back up", "Games you started and drifted from", it) }

        // 3. The user's own Want to Play list, favourite systems first.
        wantToPlay.asSequence()
            .filter { it.id !in owned }
            .sortedByDescending { affinity[it.consoleId] ?: 0.0 }
            .take(PER_SECTION)
            .map {
                Recommendation(it.id, it.title, it.imageIcon, it.consoleName, "On your Want to Play list", null)
            }
            .toList()
            .takeIf { it.isNotEmpty() }
            ?.let { sections += RecSection("wtp", "On your list", "From your Want to Play list", it) }

        // 4. Discovery on the user's favourite systems, rotating daily.
        val day = now.epochSecond / 86_400
        discovery.forEach { (consoleId, list) ->
            val played = games.count { it.consoleId == consoleId }
            val picks = list.asSequence()
                .filter { it.id !in owned && it.numAchievements >= 10 && !isNonStandard(it.title) }
                .toList()
                .shuffled(Random(day * 31 + consoleId))
                .take(PER_SECTION)
            if (picks.isEmpty()) return@forEach
            val consoleName = picks.first().consoleName
            sections += RecSection(
                id = "discover-$consoleId",
                title = "New to you on $consoleName",
                subtitle = "Because you've played $played $consoleName ${if (played == 1) "game" else "games"}",
                items = picks.map {
                    Recommendation(
                        it.id, it.title, it.imageIcon, it.consoleName,
                        "${it.numAchievements} achievements · ${it.points} pts", null,
                    )
                },
            )
        }

        return sections
    }

    /** Hacks, homebrew, prototypes and subsets are tagged with `~...~` or `[Subset ...]`. */
    private fun isNonStandard(title: String) = title.contains('~') || title.contains("[Subset")

    private fun GameSummary.toRec(reason: String) =
        Recommendation(id, title, icon, consoleName, reason, progress)

    private fun daysSince(instant: Instant?, now: Instant): Long? =
        instant?.let { Duration.between(it, now).toDays() }

    private fun humanDays(days: Long): String = when {
        days >= 365 -> "${days / 365} yr"
        days >= 60 -> "${days / 30} months"
        days >= 14 -> "${days / 7} weeks"
        else -> "$days days"
    }
}
