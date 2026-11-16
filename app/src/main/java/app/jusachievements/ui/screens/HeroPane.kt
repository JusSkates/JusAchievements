package app.jusachievements.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import app.jusachievements.ui.motion.enterStagger
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.res.painterResource
import app.jusachievements.R
import app.jusachievements.data.CommentUi
import app.jusachievements.data.FriendProfile
import app.jusachievements.ui.components.GameTile
import androidx.compose.foundation.lazy.LazyRow
import app.jusachievements.ui.motion.glow
import app.jusachievements.ui.motion.rainbowGlow
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jusachievements.data.AchievementUi
import app.jusachievements.data.GameDetail
import app.jusachievements.data.GameSummary
import app.jusachievements.data.Profile
import app.jusachievements.ui.AppState
import app.jusachievements.ui.Dashboard
import app.jusachievements.ui.Load
import app.jusachievements.ui.components.AwardChip
import app.jusachievements.ui.components.ErrorBlock
import app.jusachievements.ui.components.LoadingBlock
import app.jusachievements.ui.components.ProgressBar
import app.jusachievements.ui.components.ProgressRing
import app.jusachievements.ui.components.RaImage
import app.jusachievements.ui.components.focusGlow
import app.jusachievements.ui.components.grouped
import app.jusachievements.ui.components.relativeTime
import app.jusachievements.ui.theme.JaColors
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The showcase surface: a console-style home with your latest games, or the selected game with
 * its achievements. On the Thor this owns the big top screen (unless screens are swapped).
 *
 * @param ownsInput true when this pane is on the display that receives gamepad input, so it
 * takes initial focus and everything is reachable with the D-pad / stick.
 */
@Composable
fun HeroPane(
    state: AppState,
    modifier: Modifier = Modifier,
    showBack: Boolean,
    gamepadHints: Boolean,
    ownsInput: Boolean = true,
) {
    val selected by state.selectedGameId.collectAsStateWithLifecycle()
    val game by state.game.collectAsStateWithLifecycle()
    val dashboard by state.dashboard.collectAsStateWithLifecycle()
    val splashDone by state.splashDone.collectAsStateWithLifecycle()
    val openAchievement by state.achievement.collectAsStateWithLifecycle()
    val comments by state.comments.collectAsStateWithLifecycle()
    val friend by state.friend.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize().background(JaColors.Background)) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                (fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.95f)) togetherWith fadeOut(tween(160))
            },
            label = "hero",
        ) { id ->
            if (id == null) {
                when (val d = dashboard) {
                    Load.Loading -> LoadingBlock()
                    is Load.Failed -> ErrorBlock(d.message, state::refresh)
                    is Load.Ready -> HomeHero(d.data, ownsInput, splashDone, onSelect = state::selectGame)
                }
            } else {
                when (val g = game) {
                    null, Load.Loading -> LoadingBlock()
                    is Load.Failed -> ErrorBlock(g.message, { state.selectGame(id) })
                    is Load.Ready -> GameHero(
                        g.data, showBack, ownsInput && openAchievement == null,
                        onBack = state::clearSelection,
                        onAchievement = state::openAchievement,
                    )
                }
            }
        }
        friend?.let { f ->
            FriendDetail(f, ownsInput, onClose = state::closeFriend, onSelectGame = state::selectGame)
        }
        openAchievement?.let { a ->
            AchievementDetail(a, comments, ownsInput, onClose = state::closeAchievement)
        }
        if (gamepadHints && openAchievement == null && friend == null) GamepadHints(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

// ---- Home ---------------------------------------------------------------------------------------

@Composable
private fun HomeHero(data: Dashboard, ownsInput: Boolean, animateIn: Boolean, onSelect: (Int) -> Unit) {
    val recent = data.recent.take(3)
    var focusedIndex by rememberSaveable { mutableIntStateOf(0) }
    val focused = recent.getOrNull(focusedIndex) ?: recent.firstOrNull()
    val firstCard = remember { FocusRequester() }

    if (ownsInput && recent.isNotEmpty()) {
        LaunchedEffect(Unit) { runCatching { firstCard.requestFocus() } }
    }

    Box(Modifier.fillMaxSize()) {
        Crossfade(focused?.id, animationSpec = tween(350), label = "backdrop") { _ ->
            Backdrop(focused?.heroImage ?: focused?.titleImage ?: focused?.icon)
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
            TopStrip(data.profile)
            Spacer(Modifier.weight(1f))

            if (recent.isEmpty()) {
                Text(
                    "Play a game with RetroAchievements enabled and it will show up here.",
                    color = JaColors.TextDim,
                )
            } else {
                Text("RECENTLY PLAYED", style = MaterialTheme.typography.labelSmall, color = JaColors.Gold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                    recent.forEachIndexed { i, g ->
                        RecentCard(
                            game = g,
                            onFocus = { focusedIndex = i },
                            onClick = { onSelect(g.id) },
                            modifier = Modifier
                                .weight(1f)
                                .enterStagger(i, start = animateIn)
                                .then(if (i == 0) Modifier.focusRequester(firstCard) else Modifier),
                        )
                    }
                    repeat(3 - recent.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(14.dp))
                focused?.let { FocusedGameInfo(it, data.profile) }
            }
            Spacer(Modifier.height(44.dp)) // room for the hint bar
        }
    }
}

@Composable
private fun TopStrip(profile: Profile) {
    val time by produceState(LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(15_000)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Image(painterResource(R.drawable.ja_logo), null, Modifier.size(30.dp))
        Spacer(Modifier.width(6.dp))
        Text("JusAchievements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(14.dp))
        Text(
            time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
            style = MaterialTheme.typography.titleSmall,
            color = JaColors.TextDim,
        )
        Spacer(Modifier.weight(1f))
        // Compact "welcome back" badge, top right.
        Row(
            Modifier
                .clip(CircleShape)
                .background(JaColors.Surface.copy(alpha = 0.75f))
                .padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RaImage(profile.avatarUrl, Modifier.size(34.dp).clip(CircleShape), profile.user)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Welcome back", style = MaterialTheme.typography.labelSmall, color = JaColors.TextDim)
                Text(profile.user, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "${profile.headlinePoints.grouped()} ${if (profile.casual) "casual pts" else "pts"}",
                style = MaterialTheme.typography.labelLarge,
                color = JaColors.Gold,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RecentCard(game: GameSummary, onFocus: () -> Unit, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .then(if (game.mastered) Modifier.glow(JaColors.MasteredBlue, corner = 20.dp) else Modifier)
            .focusGlow(shape, focusedScale = 1.06f, onFocus = onFocus)
            .clip(shape)
            .background(JaColors.Surface.copy(alpha = 0.9f))
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            RaImage(game.titleImage ?: game.heroImage ?: game.icon, Modifier.fillMaxSize(), game.title)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, JaColors.Surface.copy(alpha = 0.9f)), startY = 120f)
                )
            )
            RaImage(
                game.icon,
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, JaColors.Text.copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
            )
            game.award?.let { AwardChip(it, Modifier.align(Alignment.TopEnd).padding(8.dp)) }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(game.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(game.progress, Modifier.weight(1f), height = 5.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (game.total > 0) "${game.earned}/${game.total}" else "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = JaColors.TextDim,
                )
            }
        }
    }
}

@Composable
private fun FocusedGameInfo(game: GameSummary, profile: Profile) {
    val presence = profile.richPresence.takeIf { profile.lastGameId == game.id }
    Column {
        Text(game.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            listOfNotNull(
                game.consoleName,
                relativeTime(game.lastPlayed)?.let { "played $it" },
                if (game.total > 0) "${(game.progress * 100).toInt()}% complete" else null,
            ).joinToString("  ·  "),
            style = MaterialTheme.typography.bodyMedium,
            color = JaColors.TextDim,
        )
        presence?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = JaColors.Gold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---- Game ---------------------------------------------------------------------------------------

private enum class AchFilter(val label: String) { ALL("All"), LOCKED("Locked"), UNLOCKED("Unlocked") }

@Composable
private fun GameHero(
    game: GameDetail,
    showBack: Boolean,
    ownsInput: Boolean,
    onBack: () -> Unit,
    onAchievement: (AchievementUi) -> Unit,
) {
    var filter by rememberSaveable(game.id) { mutableStateOf(AchFilter.ALL) }
    val achievements = remember(game, filter) {
        when (filter) {
            AchFilter.ALL -> game.achievements.sortedBy { !it.unlocked }
            AchFilter.LOCKED -> game.achievements.filterNot { it.unlocked }
            AchFilter.UNLOCKED -> game.achievements.filter { it.unlocked }.sortedByDescending { it.earnedAt }
        }
    }
    val firstFocus = remember { FocusRequester() }
    if (ownsInput) {
        LaunchedEffect(game.id) { runCatching { firstFocus.requestFocus() } }
    }

    Box(Modifier.fillMaxSize()) {
        Backdrop(game.imageIngame ?: game.imageTitle)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 72.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") { GameHeader(game, showBack, onBack) }
            item(span = { GridItemSpan(maxLineSpan) }, key = "filter") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    AchFilter.entries.forEachIndexed { i, f ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = { Text(f.label) },
                            modifier = Modifier
                                .then(if (i == 0) Modifier.focusRequester(firstFocus) else Modifier)
                                .focusGlow(RoundedCornerShape(8.dp)),
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = JaColors.Text,
                                selectedContainerColor = JaColors.Gold.copy(alpha = 0.18f),
                                selectedLabelColor = JaColors.Gold,
                            ),
                        )
                    }
                }
            }
            if (game.achievements.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                    Text(
                        "This game doesn't have an achievement set yet.",
                        color = JaColors.TextDim,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            itemsIndexed(achievements, key = { _, a -> a.id }) { _, a -> AchievementCard(a) { onAchievement(a) } }
        }
    }
}

@Composable
private fun GameHeader(game: GameDetail, showBack: Boolean, onBack: () -> Unit) {
    Column {
        if (showBack) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.focusGlow(CircleShape),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = JaColors.Surface.copy(alpha = 0.7f),
                    contentColor = JaColors.Text,
                ),
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Spacer(Modifier.height(8.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Phones in portrait are too narrow for cover + text side by side: stack them instead.
            val narrow = maxWidth < 560.dp
            val cover: @Composable () -> Unit = {
                // Box art keeps its own shape (tall for SNES, square for Game Boy...): never cropped.
                RaImage(
                    game.boxArt ?: game.icon,
                    Modifier
                        .height(if (narrow) 120.dp else 170.dp)
                        .widthIn(max = if (narrow) maxWidth * 0.32f else 240.dp)
                        .then(if (game.complete) Modifier.glow(JaColors.MasteredBlue, radius = 18.dp, corner = 10.dp) else Modifier)
                        .clip(RoundedCornerShape(10.dp)),
                    game.title,
                    contentScale = ContentScale.Fit,
                )
            }
            val titles: @Composable (Modifier) -> Unit = { mod ->
                Column(mod) {
                    Text(game.consoleName.uppercase(), style = MaterialTheme.typography.labelSmall, color = JaColors.Gold)
                    Text(
                        game.title,
                        style = if (narrow) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(game.developer ?: game.publisher, game.genre, game.released).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = JaColors.TextDim,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val stats: @Composable (Modifier) -> Unit = { mod ->
                Row(mod, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProgressRing(game.progress, size = if (narrow) 56.dp else 64.dp, stroke = 6.dp)
                    Column(Modifier.weight(1f)) {
                        Text("${game.earned} / ${game.total} achievements", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${game.pointsEarned.grouped()} / ${game.pointsTotal.grouped()} pts · ${game.earnedHardcore} hardcore",
                            style = MaterialTheme.typography.bodySmall,
                            color = JaColors.TextDim,
                        )
                        if (game.players > 0) {
                            Text("${game.players.grouped()} players", style = MaterialTheme.typography.bodySmall, color = JaColors.TextDim)
                        }
                        if (game.award != null || game.complete) {
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                game.award?.let { AwardChip(it) }
                                if (game.complete) {
                                    Text(
                                        "100% COMPLETE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JaColors.MasteredBlue,
                                        modifier = Modifier
                                            .glow(JaColors.MasteredBlue.copy(alpha = 0.5f), radius = 10.dp, corner = 6.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(JaColors.Background)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (narrow) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        cover()
                        Spacer(Modifier.width(14.dp))
                        titles(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    stats(Modifier.fillMaxWidth())
                }
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    cover()
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        titles(Modifier)
                        Spacer(Modifier.height(10.dp))
                        stats(Modifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(a: AchievementUi, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .achievementGlow(a)
            .focusGlow(shape)
            .clip(shape)
            .background(if (a.unlocked) JaColors.SurfaceHigh else JaColors.Surface.copy(alpha = 0.9f))
            .clickable(onClick = onClick) // also lets the D-pad / stick walk (and scroll) the list
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            RaImage(
                if (a.unlocked) a.badgeUrl else a.lockedBadgeUrl,
                Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                a.title,
                grayscale = !a.unlocked,
                alpha = if (a.unlocked) 1f else 0.7f,
            )
            if (!a.unlocked) {
                Icon(
                    Icons.Rounded.Lock, null,
                    tint = JaColors.Text,
                    modifier = Modifier.align(Alignment.BottomEnd).size(16.dp).background(JaColors.Background, CircleShape).padding(2.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(6.dp))
                Text("${a.points}", color = JaColors.Gold, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
            }
            Text(a.description, style = MaterialTheme.typography.bodySmall, color = JaColors.TextDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val meta = buildList {
                    if (a.unlocked) add("Unlocked ${relativeTime(a.earnedAt)}${if (a.hardcore) " · Hardcore" else ""}")
                    a.unlockRate?.let { add("${"%.1f".format(it * 100)}% of players") }
                }
                Text(meta.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = JaColors.TextDim)
                a.type?.let { TypeTag(it) }
            }
        }
    }
}

@Composable
private fun TypeTag(type: String) {
    val (label, color) = when (type) {
        "progression" -> "PROGRESSION" to JaColors.Violet
        "win_condition" -> "WIN" to JaColors.Gold
        "missable" -> "MISSABLE" to JaColors.Error
        else -> return
    }
    Spacer(Modifier.width(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (type == "win_condition") Icon(Icons.Rounded.Star, null, tint = color, modifier = Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** Gold halo when unlocked; rainbow when under 3% of players have it (dim until you unlock it). */
fun Modifier.achievementGlow(a: AchievementUi): Modifier = when {
    a.rare && a.unlocked -> rainbowGlow()
    a.rare -> rainbowGlow(radius = 8.dp, strength = 0.4f)
    a.unlocked -> glow(JaColors.UnlockGold, radius = 10.dp)
    else -> this
}

// ---- Achievement detail --------------------------------------------------------------------------

@Composable
private fun AchievementDetail(
    a: AchievementUi,
    comments: Load<List<CommentUi>>,
    ownsInput: Boolean,
    onClose: () -> Unit,
) {
    val closeFocus = remember { FocusRequester() }
    if (ownsInput) LaunchedEffect(a.id) { runCatching { closeFocus.requestFocus() } }
    val shape = RoundedCornerShape(24.dp)

    Box(
        Modifier
            .fillMaxSize()
            .background(JaColors.Background.copy(alpha = 0.82f))
            .clickable(interactionSource = null, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .enterStagger(0)
                .achievementGlow(a)
                .clip(shape)
                .background(JaColors.Surface)
                .clickable(interactionSource = null, indication = null) {} // swallow taps on the card
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RaImage(
                    if (a.unlocked) a.badgeUrl else a.lockedBadgeUrl,
                    Modifier.size(88.dp).clip(RoundedCornerShape(14.dp)),
                    a.title,
                    grayscale = !a.unlocked,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.title, style = MaterialTheme.typography.headlineSmall)
                    Text(a.description, style = MaterialTheme.typography.bodyMedium, color = JaColors.TextDim)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        buildList {
                            add("${a.points} pts")
                            a.unlockRate?.let { add("${"%.1f".format(it * 100)}% of players${if (a.rare) " · RARE" else ""}") }
                            add(if (a.unlocked) "Unlocked ${relativeTime(a.earnedAt)}${if (a.hardcore) " (hardcore)" else ""}" else "Locked")
                        }.joinToString("  ·  "),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (a.unlocked) JaColors.UnlockGold else JaColors.Text,
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.focusRequester(closeFocus).focusGlow(CircleShape),
                ) { Icon(Icons.Rounded.Close, "Close", tint = JaColors.Text) }
            }
            Spacer(Modifier.height(16.dp))
            Text("COMMENTS & TIPS", style = MaterialTheme.typography.labelSmall, color = JaColors.Gold)
            Spacer(Modifier.height(8.dp))
            when (comments) {
                Load.Loading -> Text("Loading comments…", color = JaColors.TextDim)
                is Load.Failed -> Text(comments.message, color = JaColors.TextDim)
                is Load.Ready -> if (comments.data.isEmpty()) {
                    Text("Nobody has commented on this achievement yet.", color = JaColors.TextDim)
                } else {
                    LazyColumn(
                        Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(comments.data.size) { i ->
                            val c = comments.data[i]
                            val row = RoundedCornerShape(12.dp)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .focusGlow(row, focusedScale = 1f)
                                    .focusable()
                                    .clip(row)
                                    .background(JaColors.SurfaceHigh)
                                    .padding(10.dp),
                            ) {
                                RaImage(c.avatarUrl, Modifier.size(32.dp).clip(CircleShape), c.user)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        listOfNotNull(c.user, relativeTime(c.at)).joinToString("  ·  "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JaColors.TextDim,
                                    )
                                    Text(c.text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- Friend milestones ---------------------------------------------------------------------------

@Composable
private fun FriendDetail(
    friend: Load<FriendProfile>,
    ownsInput: Boolean,
    onClose: () -> Unit,
    onSelectGame: (Int) -> Unit,
) {
    val closeFocus = remember { FocusRequester() }
    if (ownsInput) LaunchedEffect(Unit) { runCatching { closeFocus.requestFocus() } }
    val shape = RoundedCornerShape(24.dp)

    Box(
        Modifier
            .fillMaxSize()
            .background(JaColors.Background.copy(alpha = 0.86f))
            .clickable(interactionSource = null, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .enterStagger(0)
                .clip(shape)
                .background(JaColors.SurfaceHigh)
                .clickable(interactionSource = null, indication = null) {} // swallow taps on the card
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FRIEND", style = MaterialTheme.typography.labelSmall, color = JaColors.Gold, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.focusRequester(closeFocus).focusGlow(CircleShape),
                ) { Icon(Icons.Rounded.Close, "Close", tint = JaColors.Text) }
            }
            when (friend) {
                Load.Loading -> Box(Modifier.fillMaxWidth().height(200.dp)) { LoadingBlock() }
                is Load.Failed -> Text(friend.message, color = JaColors.TextDim, modifier = Modifier.padding(16.dp))
                is Load.Ready -> {
                    val f = friend.data
                    MilestoneCard(
                        profile = f.profile,
                        games = f.games,
                        unlocked = f.unlocked,
                        beaten = f.beaten,
                        mastered = f.mastered,
                        lastGameTitle = f.recent.firstOrNull { it.id == f.profile.lastGameId }?.title,
                        lastPlayed = f.recent.firstOrNull()?.lastPlayed,
                        own = false,
                    )
                    if (f.recent.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("RECENTLY PLAYED", style = MaterialTheme.typography.labelSmall, color = JaColors.Gold)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(f.recent.size) { i ->
                                val g = f.recent[i]
                                GameTile(
                                    g.title, g.icon,
                                    subtitle = listOfNotNull(g.consoleName, relativeTime(g.lastPlayed)).joinToString(" · "),
                                    progress = null,
                                    onClick = { onSelectGame(g.id) },
                                    width = 124.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- Shared -------------------------------------------------------------------------------------

@Composable
private fun Backdrop(image: String?) {
    Box(Modifier.fillMaxSize()) {
        RaImage(image, Modifier.fillMaxSize().blur(28.dp), alpha = 0.5f)
        Box(Modifier.fillMaxSize().background(JaColors.Scrim))
    }
}

@Composable
private fun GamepadHints(modifier: Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(JaColors.Surface.copy(alpha = 0.85f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Hint("A", "Open")
        Hint("B", "Back")
        Hint("L1/R1", "Tabs")
        Hint("L2/R2", "Games")
        Hint("SELECT", "Swap screens")
        Hint("Y", "Refresh")
    }
}

@Composable
private fun Hint(key: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            key,
            style = MaterialTheme.typography.labelSmall,
            color = JaColors.Background,
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(JaColors.Text).padding(horizontal = 5.dp, vertical = 1.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = JaColors.Text)
    }
}
