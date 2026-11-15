package app.jusachievements.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import app.jusachievements.R
import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.SweepGradient
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.jusachievements.ui.theme.JaColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Boot animation: the trophy springs in while spinning, a ring sweeps round it, the name fades
 * up, then the trophy zooms towards the viewer and the splash dissolves into the app.
 */
@Composable
fun SplashOverlay(onFinished: () -> Unit) {
    val spin = remember { Animatable(0f) }
    val scale = remember { Animatable(0.15f) }
    val overlayAlpha = remember { Animatable(1f) }
    val textAlpha = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 160f)) }
            launch { spin.animateTo(1080f, tween(1600, easing = FastOutSlowInEasing)) }
            launch { ring.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
            launch { delay(550); textAlpha.animateTo(1f, tween(500)) }
        }
        delay(350)
        coroutineScope {
            launch { textAlpha.animateTo(0f, tween(180)) }
            launch { scale.animateTo(16f, tween(600, easing = FastOutLinearInEasing)) }
            launch { delay(180); overlayAlpha.animateTo(0f, tween(420)) }
        }
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(JaColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(220.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
            drawCircle(
                Brush.radialGradient(listOf(JaColors.Gold.copy(alpha = 0.35f), Color.Transparent)),
                radius = size.minDimension / 2,
            )
            val stroke = 3.dp.toPx()
            val inset = 18.dp.toPx()
            drawArc(
                brush = Brush.sweepGradient(listOf(JaColors.Cyan, JaColors.Violet, JaColors.Gold, JaColors.Cyan)),
                startAngle = -90f,
                sweepAngle = 360f * ring.value,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Image(
            painterResource(R.drawable.ja_logo),
            contentDescription = null,
            modifier = Modifier
                .size(128.dp)
                .graphicsLayer {
                    rotationY = spin.value
                    scaleX = scale.value
                    scaleY = scale.value
                    cameraDistance = 14f * density
                },
        )
        Text(
            "JUSACHIEVEMENTS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 6.sp,
            modifier = Modifier.offset(y = 140.dp).graphicsLayer { alpha = textAlpha.value },
        )
    }
}

/** Quiet stand-in for the second screen while the splash plays on the main one. */
@Composable
fun SplashStandby() {
    val pulse by rememberInfiniteTransition(label = "standby").animateFloat(
        0.25f, 0.8f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse",
    )
    Box(Modifier.fillMaxSize().background(JaColors.Background), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.ja_logo), null,
            modifier = Modifier.size(64.dp).graphicsLayer { alpha = pulse },
        )
    }
}

/**
 * Animates the two panes trading places. On the main (upper) screen the old pane drops away
 * towards the lower screen and the new one rises from it; the second screen mirrors that, so
 * across both displays it reads as one continuous exchange.
 */
@Composable
fun SwapTransition(swapped: Boolean, isMainScreen: Boolean, content: @Composable (swapped: Boolean) -> Unit) {
    val dir = if (isMainScreen) 1 else -1
    AnimatedContent(
        targetState = swapped,
        transitionSpec = {
            (slideInVertically(tween(460, easing = FastOutSlowInEasing)) { it * dir } +
                fadeIn(tween(320, delayMillis = 80)) +
                scaleIn(tween(460), initialScale = 0.9f)) togetherWith
                (slideOutVertically(tween(420, easing = FastOutSlowInEasing)) { it * dir } +
                    fadeOut(tween(260)) +
                    scaleOut(tween(420), targetScale = 0.9f))
        },
        label = "swap",
    ) { content(it) }
}

/** Slow drifting colour glows behind content: gives flat dark panes some depth and life. */
fun Modifier.ambientGlow(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "ambient")
    val a by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse), label = "a",
    )
    val b by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(19_000, easing = LinearEasing), RepeatMode.Reverse), label = "b",
    )
    drawBehind {
        val r = size.maxDimension * 0.65f
        val c1 = Offset(size.width * (0.1f + 0.5f * a), size.height * (0.05f + 0.3f * b))
        drawCircle(Brush.radialGradient(listOf(JaColors.Violet.copy(alpha = 0.20f), Color.Transparent), c1, r), r, c1)
        val c2 = Offset(size.width * (0.95f - 0.5f * b), size.height * (0.95f - 0.3f * a))
        drawCircle(Brush.radialGradient(listOf(JaColors.Cyan.copy(alpha = 0.13f), Color.Transparent), c2, r), r, c2)
    }
}

/** Fade + rise entrance, staggered by [index]. Runs once [start] becomes true. */
fun Modifier.enterStagger(index: Int, start: Boolean = true): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(start) {
        if (start) {
            delay(index * 90L)
            progress.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 200f))
        }
    }
    graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * 70f
    }
}

/**
 * Soft pulsing halo behind a card. Put it *before* `clip` in the modifier chain so the blur can
 * spill outside the card's bounds.
 */
fun Modifier.glow(color: Color, radius: Dp = 14.dp, corner: Dp = 16.dp): Modifier = composed {
    val pulse by rememberInfiniteTransition(label = "glow").animateFloat(
        0.45f, 0.95f, infiniteRepeatable(tween(1700), RepeatMode.Reverse), label = "glowPulse",
    )
    val blurPx = with(LocalDensity.current) { radius.toPx() }
    val cornerPx = with(LocalDensity.current) { corner.toPx() }
    val paint = remember(blurPx) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL) }
    }
    drawBehind {
        paint.color = color.copy(alpha = pulse).toArgb()
        drawIntoCanvas { it.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, cornerPx, cornerPx, paint) }
    }
}

private val RainbowColors = intArrayOf(
    0xFFFF4D5E.toInt(), 0xFFFFB03D.toInt(), 0xFFFFF04D.toInt(), 0xFF3DF0A0.toInt(),
    0xFF35D6FF.toInt(), 0xFF8B6CFF.toInt(), 0xFFFF4FD8.toInt(), 0xFFFF4D5E.toInt(),
)

/** Rotating rainbow halo, for the rarest achievements. [strength] 0..1 dims it (e.g. while locked). */
fun Modifier.rainbowGlow(radius: Dp = 16.dp, corner: Dp = 16.dp, strength: Float = 1f): Modifier = composed {
    val angle by rememberInfiniteTransition(label = "rainbow").animateFloat(
        0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "rainbowAngle",
    )
    val blurPx = with(LocalDensity.current) { radius.toPx() }
    val cornerPx = with(LocalDensity.current) { corner.toPx() }
    val paint = remember(blurPx) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL) }
    }
    val matrix = remember { Matrix() }
    var shader by remember { mutableStateOf<SweepGradient?>(null) }
    var shaderSize by remember { mutableStateOf(Size.Zero) }
    drawBehind {
        if (shader == null || shaderSize != size) {
            shader = SweepGradient(size.width / 2, size.height / 2, RainbowColors, null)
            shaderSize = size
        }
        matrix.setRotate(angle, size.width / 2, size.height / 2)
        shader!!.setLocalMatrix(matrix)
        paint.shader = shader
        paint.alpha = (255 * strength).toInt()
        drawIntoCanvas { it.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, cornerPx, cornerPx, paint) }
    }
}
