package app.jusachievements.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.jusachievements.data.AwardKind
import app.jusachievements.data.GameSummary
import app.jusachievements.data.api.ApiFactory
import app.jusachievements.ui.motion.glow
import app.jusachievements.ui.theme.JaColors
import coil.compose.AsyncImage
import java.time.Duration
import java.time.Instant

/**
 * Gold ring + slight lift on focus, so everything is readable when driven by a gamepad/D-pad.
 * Put this *before* `clickable` so it observes that element's focus.
 */
fun Modifier.focusGlow(
    shape: Shape = RoundedCornerShape(16.dp),
    focusedScale: Float = 1.03f,
    onFocus: (() -> Unit)? = null,
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) focusedScale else 1f, label = "focusScale")
    this
        .onFocusChanged {
            focused = it.isFocused
            if (it.isFocused) onFocus?.invoke()
        }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .then(if (focused) Modifier.border(3.dp, JaColors.Gold, shape) else Modifier)
}

@Composable
fun RaImage(
    path: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    grayscale: Boolean = false,
    alpha: Float = 1f,
) {
    AsyncImage(
        model = ApiFactory.media(path),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = if (grayscale) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
    )
}

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 6.dp, mastered: Boolean = false) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(JaColors.Outline)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .then(
                    if (mastered) Modifier.background(JaColors.Mint) else Modifier.background(JaColors.GoldGradient)
                )
        )
    }
}

@Composable
fun ProgressRing(progress: Float, size: Dp, stroke: Dp = 8.dp, modifier: Modifier = Modifier, label: String? = null) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arcSize = Size(this.size.width - s, this.size.height - s)
            val topLeft = Offset(s / 2, s / 2)
            drawArc(JaColors.Outline, 0f, 360f, false, topLeft, arcSize, style = Stroke(s))
            drawArc(
                brush = JaColors.GoldGradient,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(s, cap = StrokeCap.Round),
            )
        }
        Text(
            label ?: "${(animated * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
fun AwardChip(kind: AwardKind, modifier: Modifier = Modifier) {
    val color = when (kind) {
        AwardKind.MASTERED -> JaColors.Gold
        AwardKind.COMPLETED -> JaColors.Mint
        else -> JaColors.Violet
    }
    Text(
        kind.label.uppercase(),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = color,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** List row for a game: icon, title, system, progress. */
@Composable
fun GameRow(game: GameSummary, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .then(if (game.mastered) Modifier.glow(JaColors.MasteredBlue, radius = 10.dp) else Modifier)
            .focusGlow(shape)
            .clip(shape)
            .background(if (selected) JaColors.SurfaceHigh else JaColors.Surface)
            .then(if (selected) Modifier.border(1.dp, JaColors.Gold.copy(alpha = 0.6f), shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RaImage(game.icon, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), game.title)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    game.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                game.award?.let {
                    Spacer(Modifier.width(6.dp))
                    AwardChip(it)
                }
            }
            Text(
                listOfNotNull(game.consoleName, relativeTime(game.lastActivity)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(
                    game.progress,
                    Modifier.weight(1f),
                    mastered = game.award == AwardKind.MASTERED || game.award == AwardKind.COMPLETED,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (game.total > 0) "${game.earned}/${game.total}" else "No set",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Compact poster-style tile for horizontal carousels. */
@Composable
fun GameTile(
    title: String,
    icon: String?,
    subtitle: String,
    progress: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 132.dp,
    selected: Boolean = false,
    mastered: Boolean = false,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .width(width)
            .then(if (mastered) Modifier.glow(JaColors.MasteredBlue, radius = 10.dp, corner = 18.dp) else Modifier)
            .focusGlow(shape)
            .clip(shape)
            .background(if (selected) JaColors.SurfaceHigh else JaColors.Surface)
            .then(if (selected) Modifier.border(1.dp, JaColors.Gold.copy(alpha = 0.6f), shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        RaImage(icon, Modifier.fillMaxWidth().height(width - 16.dp).clip(RoundedCornerShape(12.dp)), title)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (progress != null) {
            Spacer(Modifier.height(6.dp))
            ProgressBar(progress, Modifier.fillMaxWidth(), height = 4.dp)
        }
    }
}

@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier, accent: Color = JaColors.Text) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(JaColors.Surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Black)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LoadingBlock(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = JaColors.Gold)
    }
}

@Composable
fun ErrorBlock(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.CloudOff, null, tint = JaColors.TextDim, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Try again") }
    }
}

fun relativeTime(instant: Instant?, now: Instant = Instant.now()): String? {
    instant ?: return null
    val d = Duration.between(instant, now)
    return when {
        d.isNegative || d.toMinutes() < 1 -> "just now"
        d.toHours() < 1 -> "${d.toMinutes()}m ago"
        d.toDays() < 1 -> "${d.toHours()}h ago"
        d.toDays() < 30 -> "${d.toDays()}d ago"
        d.toDays() < 365 -> "${d.toDays() / 30}mo ago"
        else -> "${d.toDays() / 365}y ago"
    }
}

fun Int.grouped(): String = "%,d".format(this)
