package app.jusachievements.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import app.jusachievements.ui.motion.ambientGlow
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.grid.itemsIndexed
import app.jusachievements.data.EventUi
import app.jusachievements.data.Events
import app.jusachievements.data.RareUnlock
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Group
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import app.jusachievements.data.FeedItem
import app.jusachievements.data.Friend
import app.jusachievements.data.Profile
import app.jusachievements.data.Social
import app.jusachievements.ui.theme.Palettes
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jusachievements.data.AwardKind
import app.jusachievements.data.GameSummary
import app.jusachievements.ui.AppState
import app.jusachievements.ui.Dashboard
import app.jusachievements.ui.LibraryFilter
import app.jusachievements.ui.Load
import app.jusachievements.ui.SortOrder
import app.jusachievements.ui.StatusFilter
import app.jusachievements.ui.Tab
import app.jusachievements.ui.components.ErrorBlock
import app.jusachievements.ui.components.GameRow
import app.jusachievements.ui.components.GameTile
import app.jusachievements.ui.components.LoadingBlock
import app.jusachievements.ui.components.RaImage
import app.jusachievements.ui.components.SectionHeader
import app.jusachievements.ui.components.StatBlock
import app.jusachievements.ui.components.focusGlow
import app.jusachievements.ui.components.grouped
import app.jusachievements.ui.components.relativeTime
import app.jusachievements.ui.theme.JaColors

/**
 * Browsing surface: tabs + lists. Lives on the bottom screen on the Thor, in the left pane on a
 * wide single screen, or full-screen on a phone.
 */
@Composable
fun NavigatorPane(
    state: AppState,
    modifier: Modifier = Modifier,
    displayCount: Int = 1,
    dualActive: Boolean = false,
) {
    val dashboard by state.dashboard.collectAsStateWithLifecycle()
    val tab by state.tab.collectAsStateWithLifecycle()
    val selected by state.selectedGameId.collectAsStateWithLifecycle()
    val filter by state.libraryFilter.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val social by state.social.collectAsStateWithLifecycle()
    val top by state.top.collectAsStateWithLifecycle()
    val events by state.events.collectAsStateWithLifecycle()
    val hidden by state.settings.hiddenTabs.collectAsStateWithLifecycle()
    val tabs = remember(hidden) { AppState.visibleTabs(hidden) }
    // If the current tab was removed in Settings, fall back to Home.
    LaunchedEffect(tabs, tab) { if (tab !in tabs) state.selectTab(Tab.HOME) }

    Column(modifier.fillMaxSize().background(JaColors.Background).ambientGlow()) {
        NavTopBar(
            tabs = tabs,
            tab = tab,
            refreshing = (dashboard as? Load.Ready)?.refreshing == true,
            onTab = state::selectTab,
            onRefresh = state::refresh,
            onSettings = { showSettings = true },
            onSwap = if (dualActive) state.settings::toggleSwapScreens else null,
        )
        when (val d = dashboard) {
            Load.Loading -> LoadingBlock()
            is Load.Failed -> ErrorBlock(d.message, state::refresh)
            is Load.Ready -> AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    // Slide in the direction of travel through the tab bar.
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(tween(280)) { it / 5 * dir } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(tween(200)) { -it / 5 * dir } + fadeOut(tween(160)))
                },
                label = "tab",
            ) { current ->
                val onSelect: (Int) -> Unit = state::selectGame
                when (current) {
                    Tab.HOME -> HomeTab(d.data, selected, onSelect, onSeeAll = state::selectTab)
                    Tab.RECENT -> GameList(d.data.recent, selected, onSelect, empty = "Nothing played yet.")
                    Tab.LIBRARY -> LibraryTab(d.data.library, filter, selected, onSelect, state::setLibraryFilter)
                    Tab.FOR_YOU -> ForYouTab(d.data, selected, onSelect)
                    Tab.TOP -> TopTab(top, state::openRare)
                    Tab.EVENTS -> EventsTab(events, selected, onSelect)
                    Tab.FEED -> FeedTab(social, selected, onSelect)
                    Tab.FRIENDS -> FriendsTab(social, state::openFriend)
                }
            }
        }
    }

    if (showSettings) SettingsDialog(state, displayCount) { showSettings = false }
}

@Composable
private fun NavTopBar(
    tabs: List<Tab>,
    tab: Tab,
    refreshing: Boolean,
    onTab: (Tab) -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onSwap: (() -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tabRow = rememberLazyListState()
        // Keep the selected tab in view (it can change from the gamepad or the other screen).
        LaunchedEffect(tab, tabs) {
            val i = tabs.indexOf(tab)
            if (i >= 0) tabRow.animateScrollToItem(i)
        }
        LazyRow(
            Modifier.weight(1f),
            state = tabRow,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(tabs, key = { it.name }) { t -> TabPill(t, t == tab) { onTab(t) } }
        }
        Spacer(Modifier.width(6.dp))
        if (onSwap != null) {
            BarButton(Icons.Rounded.SwapVert, "Swap screens", onSwap)
        }
        if (refreshing) {
            CircularProgressIndicator(Modifier.padding(horizontal = 12.dp).size(20.dp), strokeWidth = 2.dp)
        } else {
            BarButton(Icons.Rounded.Refresh, "Refresh", onRefresh)
        }
        BarButton(Icons.Rounded.Settings, "Settings", onSettings)
    }
}

@Composable
private fun BarButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.focusGlow(CircleShape)) {
        Icon(icon, label, tint = JaColors.Text)
    }
}

private val Tab.icon: ImageVector
    get() = when (this) {
        Tab.HOME -> Icons.Rounded.Home
        Tab.RECENT -> Icons.Rounded.History
        Tab.LIBRARY -> Icons.Rounded.GridView
        Tab.FOR_YOU -> Icons.Rounded.AutoAwesome
        Tab.TOP -> Icons.Rounded.MilitaryTech
        Tab.EVENTS -> Icons.Rounded.Event
        Tab.FEED -> Icons.Rounded.DynamicFeed
        Tab.FRIENDS -> Icons.Rounded.Group
    }

@Composable
private fun TabPill(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val shape = CircleShape
    val container by animateColorAsState(if (selected) JaColors.Gold else JaColors.Surface, tween(250), label = "pill")
    val color by animateColorAsState(if (selected) JaColors.Background else JaColors.TextDim, tween(250), label = "pillText")
    Row(
        Modifier
            .focusGlow(shape)
            .clip(shape)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tab.icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(tab.label, color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

// ---- Home ---------------------------------------------------------------------------------------

@Composable
private fun HomeTab(data: Dashboard, selected: Int?, onSelect: (Int) -> Unit, onSeeAll: (Tab) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ProfileCard(data) }
        if (data.recent.isNotEmpty()) {
            item { HeaderWithAction("Continue playing", "See all") { onSeeAll(Tab.RECENT) } }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(data.recent.take(12), key = { it.id }) { g ->
                        GameTile(
                            g.title, g.icon,
                            subtitle = listOfNotNull(g.consoleName, relativeTime(g.lastPlayed)).joinToString(" · "),
                            progress = g.progress.takeIf { g.total > 0 },
                            onClick = { onSelect(g.id) },
                            selected = g.id == selected,
                            mastered = g.mastered,
                        )
                    }
                }
            }
        }
        data.recommendations.firstOrNull()?.let { section ->
            item { HeaderWithAction(section.title, "More picks") { onSeeAll(Tab.FOR_YOU) } }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(section.items, key = { it.gameId }) { r ->
                        GameTile(r.title, r.icon, r.reason, r.progress, { onSelect(r.gameId) }, selected = r.gameId == selected)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(data: Dashboard) {
    val p = data.profile
    MilestoneCard(
        profile = p,
        games = data.library.size,
        unlocked = data.library.sumOf { it.earned },
        beaten = data.library.count { it.award != null },
        mastered = data.library.count { it.mastered },
        lastGameTitle = data.recent.firstOrNull { it.id == p.lastGameId }?.title ?: data.recent.firstOrNull()?.title,
        lastPlayed = data.recent.firstOrNull()?.lastPlayed,
        own = true,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

/** The milestones card: used for your own profile on Home and for a friend's profile. */
@Composable
fun MilestoneCard(
    profile: Profile,
    games: Int,
    unlocked: Int,
    beaten: Int,
    mastered: Int,
    lastGameTitle: String?,
    lastPlayed: java.time.Instant?,
    own: Boolean,
    modifier: Modifier = Modifier,
) {
    val p = profile
    Column(
        modifier
            .fillMaxWidth()
            .focusGlow(RoundedCornerShape(22.dp), focusedScale = 1f)
            .focusable() // so the D-pad / stick can move (and scroll) back up to it
            .clip(RoundedCornerShape(22.dp))
            .background(JaColors.Surface)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RaImage(p.avatarUrl, Modifier.size(56.dp).clip(CircleShape), p.user)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.user, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (p.casual) {
                        "${p.softcorePoints.grouped()} casual pts · ${p.points.grouped()} hardcore pts"
                    } else {
                        "${p.points.grouped()} pts · ${p.truePoints.grouped()} RetroPoints"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = JaColors.Gold,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            StatBlock(
                p.rank?.let { "#${it.grouped()}" } ?: "Unranked",
                p.totalRanked?.let { "Hardcore rank of ${it.grouped()}" } ?: "Hardcore rank",
                accent = JaColors.Cyan,
            )
            StatBlock(games.grouped(), "Games")
            StatBlock(unlocked.grouped(), "Unlocked")
            StatBlock(beaten.grouped(), "Beaten", accent = JaColors.Violet)
            StatBlock(mastered.grouped(), "Mastered", accent = JaColors.Gold)
        }
        val seen = p.lastActivity ?: lastPlayed
        if (seen != null || lastGameTitle != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Last activity: " + listOfNotNull(
                    relativeTime(seen),
                    lastGameTitle?.let { t -> p.richPresence?.let { "$t: $it" } ?: t } ?: p.richPresence,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = JaColors.TextDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (own && p.casual && p.rank == null) {
            Text(
                "RetroAchievements only shares hardcore rank with apps. Your casual rank is on the website.",
                style = MaterialTheme.typography.labelSmall,
                color = JaColors.TextDim,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun HeaderWithAction(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        SectionHeader(title, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) { Text(action) }
    }
}

// ---- Lists --------------------------------------------------------------------------------------

@Composable
private fun GameList(games: List<GameSummary>, selected: Int?, onSelect: (Int) -> Unit, empty: String) {
    if (games.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(empty, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val listState = rememberLazyGridState()
    ScrollToSelected(games, selected) { listState.animateScrollToItem(it) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(games, key = { it.id }) { g -> GameRow(g, g.id == selected, { onSelect(g.id) }) }
    }
}

/** Keeps the selected item visible when selection changes from the other screen / gamepad. */
@Composable
private fun ScrollToSelected(games: List<GameSummary>, selected: Int?, offset: Int = 0, scroll: suspend (Int) -> Unit) {
    LaunchedEffect(selected) {
        val index = games.indexOfFirst { it.id == selected }
        if (index >= 0) scroll(index + offset)
    }
}

@Composable
private fun LibraryTab(
    library: List<GameSummary>,
    filter: LibraryFilter,
    selected: Int?,
    onSelect: (Int) -> Unit,
    onFilter: (LibraryFilter) -> Unit,
) {
    val consoles = remember(library) {
        library.groupBy { it.consoleId }
            .map { (id, games) -> Triple(id, games.first().consoleName, games.size) }
            .sortedByDescending { it.third }
    }
    val games = remember(library, filter) { AppState.filteredLibrary(library, filter) }
    val gridState = rememberLazyGridState()
    ScrollToSelected(games, selected, offset = 1) { gridState.animateScrollToItem(it) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        state = gridState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "filters") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ChipRow {
                    StatusFilter.entries.forEach { s ->
                        Chip(s.label, filter.status == s) { onFilter(filter.copy(status = s)) }
                    }
                    Spacer(Modifier.width(8.dp))
                    SortOrder.entries.forEach { s ->
                        Chip("↕ ${s.label}", filter.sort == s) { onFilter(filter.copy(sort = s)) }
                    }
                }
                ChipRow {
                    Chip("All systems", filter.consoleId == null) { onFilter(filter.copy(consoleId = null)) }
                    consoles.forEach { (id, name, count) ->
                        Chip("$name · $count", filter.consoleId == id) { onFilter(filter.copy(consoleId = id)) }
                    }
                }
                Text(
                    "${games.size} games",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        items(games, key = { it.id }) { g -> GameRow(g, g.id == selected, { onSelect(g.id) }) }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.focusGlow(RoundedCornerShape(8.dp)),
        colors = FilterChipDefaults.filterChipColors(
            labelColor = JaColors.Text,
            selectedContainerColor = JaColors.Gold.copy(alpha = 0.18f),
            selectedLabelColor = JaColors.Gold,
        ),
    )
}

// ---- For you ------------------------------------------------------------------------------------

@Composable
private fun ForYouTab(data: Dashboard, selected: Int?, onSelect: (Int) -> Unit) {
    if (data.recommendations.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Play a few games with achievements and recommendations will show up here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val listState = rememberLazyListState()
    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.recommendations.forEach { section ->
            item(key = "h-${section.id}") { SectionHeader(section.title, section.subtitle) }
            item(key = "r-${section.id}") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(section.items, key = { it.gameId }) { r ->
                        GameTile(
                            r.title, r.icon, "${r.consoleName}\n${r.reason}", r.progress,
                            onClick = { onSelect(r.gameId) },
                            width = 156.dp,
                            selected = r.gameId == selected,
                        )
                    }
                }
            }
        }
    }
}

// ---- Feed & friends -----------------------------------------------------------------------------

@Composable
private fun CenterNote(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, color = JaColors.TextDim, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAction, modifier = Modifier.focusGlow(CircleShape)) { Text(action) }
        }
    }
}

private const val RA_SITE = "https://retroachievements.org"

@Composable
private fun FeedTab(social: Load<Social>, selected: Int?, onSelect: (Int) -> Unit) {
    val context = LocalContext.current
    val openSite = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RA_SITE))) }; Unit }
    when (social) {
        Load.Loading -> LoadingBlock()
        is Load.Failed -> CenterNote(social.message)
        is Load.Ready -> when {
            social.data.friends.isEmpty() -> CenterNote(
                "You're not following anyone yet. Follow players on retroachievements.org and their unlocks show up here.",
                "Open RetroAchievements", openSite,
            )
            social.data.feed.isEmpty() -> CenterNote("Nobody you follow has unlocked anything in the last 7 days.")
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(social.data.feed, key = { "${it.user}-${it.achievementId}-${it.hardcore}" }) { item ->
                    FeedRow(item, item.gameId == selected) { onSelect(item.gameId) }
                }
            }
        }
    }
}

@Composable
private fun FeedRow(item: FeedItem, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .focusGlow(shape)
            .clip(shape)
            .background(if (selected) JaColors.SurfaceHigh else JaColors.Surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RaImage(item.avatarUrl, Modifier.size(40.dp).clip(CircleShape), item.user)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                listOfNotNull(item.user, relativeTime(item.at)).joinToString("  ·  "),
                style = MaterialTheme.typography.labelSmall,
                color = JaColors.TextDim,
            )
            Text(
                "${item.title}  (${item.points})",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${item.gameTitle} · ${item.consoleName}${if (item.hardcore) " · Hardcore" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = JaColors.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        RaImage(item.badgeUrl, Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)), item.title)
    }
}

@Composable
private fun FriendsTab(social: Load<Social>, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val openSite = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RA_SITE))) }; Unit }
    when (social) {
        Load.Loading -> LoadingBlock()
        is Load.Failed -> CenterNote(social.message)
        is Load.Ready -> if (social.data.friends.isEmpty()) {
            CenterNote(
                "You're not following anyone yet. Follow players on retroachievements.org and they appear here.",
                "Open RetroAchievements", openSite,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "note") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Following ${social.data.friends.size} · follow or unfollow on the website",
                            style = MaterialTheme.typography.labelSmall,
                            color = JaColors.TextDim,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = openSite, modifier = Modifier.focusGlow(CircleShape)) { Text("Open site") }
                    }
                }
                items(social.data.friends, key = { it.user }) { f -> FriendRow(f) { onOpen(f.user) } }
            }
        }
    }
}

@Composable
private fun FriendRow(friend: Friend, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .focusGlow(shape)
            .clip(shape)
            .background(JaColors.Surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RaImage(friend.avatarUrl, Modifier.size(52.dp).clip(CircleShape), friend.user)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(friend.user, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${friend.points.grouped()} pts${if (friend.followsMe) " · follows you" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = JaColors.Gold,
            )
            val latest = friend.latest
            Text(
                if (latest != null) {
                    "${friend.unlocksThisWeek} this week · latest: ${latest.title} (${latest.gameTitle}) ${relativeTime(latest.at)}"
                } else {
                    "No unlocks this week"
                },
                style = MaterialTheme.typography.bodySmall,
                color = JaColors.TextDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---- Settings -----------------------------------------------------------------------------------

@Composable
private fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = JaColors.TextDim)
        }
        control()
    }
}

@Composable
private fun SettingsDialog(state: AppState, displayCount: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settings = state.settings
    val dual by settings.useSecondScreen.collectAsStateWithLifecycle()
    val swapped by settings.swapScreens.collectAsStateWithLifecycle()
    val music by settings.musicEnabled.collectAsStateWithLifecycle()
    val musicUri by settings.customMusicUri.collectAsStateWithLifecycle()
    val themeId by settings.themeId.collectAsStateWithLifecycle()
    val hidden by settings.hiddenTabs.collectAsStateWithLifecycle()
    val session by state.session.collectAsStateWithLifecycle()

    val pickMusic = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // Keep access to the file across restarts.
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            settings.setCustomMusicUri(uri.toString())
            settings.setMusicEnabled(true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("SCREENS", style = MaterialTheme.typography.labelSmall, color = JaColors.Gold)
                SettingRow(
                    "Use second screen",
                    if (displayCount > 1) "$displayCount displays detected" else "Only one display detected",
                ) { Switch(checked = dual, onCheckedChange = settings::setUseSecondScreen) }
                SettingRow("Swap screens", "Lists on the main screen, game details on the second (SELECT)") {
                    Switch(checked = swapped, onCheckedChange = settings::setSwapScreens, enabled = dual)
                }

                Text("MUSIC", style = MaterialTheme.typography.labelSmall, color = JaColors.Gold)
                SettingRow(
                    "Background music",
                    if (musicUri != null) "Playing your own file on loop" else "Built-in ambient loop",
                ) { Switch(checked = music, onCheckedChange = settings::setMusicEnabled) }
                ChipRow {
                    Chip("Built-in", musicUri == null) { settings.setCustomMusicUri(null) }
                    Chip(if (musicUri != null) "Your file ✓ (change…)" else "Choose your own file…", musicUri != null) {
                        runCatching { pickMusic.launch(arrayOf("audio/*")) }
                    }
                }

                Text("THEME", style = MaterialTheme.typography.labelSmall, color = JaColors.Gold)
                ChipRow {
                    val current = Palettes.byId(themeId)
                    Palettes.all.forEach { p -> Chip(p.label, p.id == current.id) { settings.setThemeId(p.id) } }
                }

                Text("TABS", style = MaterialTheme.typography.labelSmall, color = JaColors.Gold)
                Text(
                    "Tap to add or remove a tab. Home always stays.",
                    style = MaterialTheme.typography.bodySmall,
                    color = JaColors.TextDim,
                )
                ChipRow {
                    Tab.entries.filter { it != Tab.HOME }.forEach { t ->
                        val shown = t.name !in hidden
                        Chip(if (shown) "${t.label} ✓" else t.label, shown) { settings.setTabHidden(t.name, shown) }
                    }
                }

                session?.let {
                    Text("Signed in as ${it.username}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = {
            TextButton(onClick = { onDismiss(); state.logout() }) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sign out")
            }
        },
    )
}

// ---- Top (rarest unlocks) ------------------------------------------------------------------------

@Composable
private fun TopTab(top: Load<List<RareUnlock>>, onOpen: (RareUnlock) -> Unit) {
    when (top) {
        Load.Loading -> LoadingBlock()
        is Load.Failed -> CenterNote(top.message)
        is Load.Ready -> if (top.data.isEmpty()) {
            CenterNote("Unlock some achievements and your rarest ones will be ranked here.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "head") {
                    SectionHeader("Your rarest achievements", "Top ${top.data.size}, rarest first", Modifier.padding(start = 0.dp))
                }
                itemsIndexed(top.data, key = { _, r -> r.achievement.id }) { i, r -> RareRow(i + 1, r) { onOpen(r) } }
            }
        }
    }
}

@Composable
private fun RareRow(rank: Int, rare: RareUnlock, onClick: () -> Unit) {
    val a = rare.achievement
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .achievementGlow(a)
            .focusGlow(shape)
            .clip(shape)
            .background(JaColors.SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "#$rank",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = if (rank <= 3) JaColors.UnlockGold else JaColors.TextDim,
            modifier = Modifier.width(52.dp),
        )
        RaImage(a.badgeUrl, Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)), a.title)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${rare.gameTitle} · ${rare.consoleName}",
                style = MaterialTheme.typography.bodySmall,
                color = JaColors.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${"%.2f".format((a.unlockRate ?: 0f) * 100)}% of players · ${a.points} pts",
                style = MaterialTheme.typography.labelSmall,
                color = JaColors.Gold,
            )
        }
    }
}

// ---- Community events ----------------------------------------------------------------------------

@Composable
private fun EventsTab(events: Load<Events>, selected: Int?, onSelect: (Int) -> Unit) {
    when (events) {
        Load.Loading -> LoadingBlock()
        is Load.Failed -> CenterNote(events.message)
        is Load.Ready -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            events.data.aotw?.let { w ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "aotw") {
                    val shape = RoundedCornerShape(20.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .focusGlow(shape, focusedScale = 1.01f)
                            .clip(shape)
                            .background(JaColors.SurfaceHigh)
                            .clickable { if (w.gameId > 0) onSelect(w.gameId) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (w.badgeUrl != null) {
                            RaImage(w.badgeUrl, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)), w.title)
                            Spacer(Modifier.width(14.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("ACHIEVEMENT OF THE WEEK", style = MaterialTheme.typography.labelSmall, color = JaColors.Gold)
                            Text("${w.title}  (${w.points})", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(w.description, style = MaterialTheme.typography.bodySmall, color = JaColors.TextDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${w.gameTitle} · ${w.consoleName} · ${w.unlocks.grouped()} unlocks so far",
                                style = MaterialTheme.typography.labelSmall,
                                color = JaColors.TextDim,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "head") {
                SectionHeader("Community events", "Most recently updated first", Modifier.padding(start = 0.dp))
            }
            if (events.data.events.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "none") {
                    Text("No events found right now.", color = JaColors.TextDim)
                }
            }
            items(events.data.events, key = { it.id }) { e -> EventRow(e, e.id == selected) { onSelect(e.id) } }
        }
    }
}

@Composable
private fun EventRow(event: EventUi, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .focusGlow(shape)
            .clip(shape)
            .background(if (selected) JaColors.SurfaceHigh else JaColors.Surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RaImage(event.icon, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), event.title)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    "${event.achievements} achievements",
                    "${event.points} pts",
                    relativeTime(event.updated)?.let { "updated $it" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = JaColors.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
