package com.surfswirl.bakery.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.surfswirl.bakery.game.Reward
import com.surfswirl.bakery.ui.theme.Fredoka
import com.surfswirl.bakery.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.sin

/** Loads item art from assets/items/<name>.webp, cached. */
object Art {
    private val cache = object : LruCache<String, ImageBitmap>(220) {}
    fun get(context: Context, name: String): ImageBitmap? {
        cache.get(name)?.let { return it }
        val bmp = runCatching {
            context.assets.open("items/$name.webp").use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return null
        val img = bmp.asImageBitmap()
        cache.put(name, img)
        return img
    }
}

private val grayMatrix = ColorMatrix().apply { setToSaturation(0f) }

@Composable
fun ItemArt(art: String, modifier: Modifier = Modifier, alpha: Float = 1f, gray: Boolean = false, silhouette: Boolean = false) {
    val ctx = LocalContext.current
    val img = remember(art) { Art.get(ctx, art) }
    if (img != null) {
        Image(img, contentDescription = null, modifier = modifier, alpha = alpha,
            colorFilter = when {
                silhouette -> ColorFilter.tint(Color(0x55704030))
                gray -> ColorFilter.colorMatrix(grayMatrix)
                else -> null
            })
    } else Box(modifier)
}

/** A glossy, chunky game button that squishes when pressed. */
@Composable
fun CandyButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Palette.Green,
    dark: Color = Palette.GreenDark,
    enabled: Boolean = true,
    icon: String? = null,
    small: Boolean = false,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.4f, stiffness = 900f), label = "press")
    val top = if (enabled) color else Palette.Gray
    val bottom = if (enabled) dark else Palette.GrayDark
    val h = if (small) 34.dp else 46.dp
    Box(
        modifier
            .scale(scale)
            .semantics { contentDescription = text }
            .clickable(src, null, enabled = enabled, onClick = onClick),
        propagateMinConstraints = true,
    ) {
        Box(Modifier.matchParentSize().padding(top = 4.dp).clip(RoundedCornerShape(50)).background(bottom))
        Box(
            Modifier.padding(bottom = 4.dp).height(h).clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(top.lighten(0.25f), top)))
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            // shine
            Box(Modifier.matchParentSize().padding(top = 3.dp, start = 14.dp, end = 14.dp, bottom = h * 0.62f)
                .clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.28f)))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = if (small) 10.dp else 16.dp)) {
                if (icon != null) { ItemArt(icon, Modifier.size(if (small) 20.dp else 26.dp)); Spacer(Modifier.width(6.dp)) }
                OutlinedText(text, if (small) 14 else 18, Color.White, bottom.darken(0.25f))
            }
        }
    }
}

fun Color.lighten(f: Float) = Color(red + (1 - red) * f, green + (1 - green) * f, blue + (1 - blue) * f, alpha)
fun Color.darken(f: Float) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)

/** Chunky game text with a darker outline, like in the big mobile games. */
@Composable
fun OutlinedText(text: String, size: Int, color: Color = Color.White, outline: Color = Palette.Cocoa, modifier: Modifier = Modifier,
                 align: TextAlign = TextAlign.Center) {
    Box(modifier) {
        val d = (size / 14f).coerceAtLeast(1f)
        for ((dx, dy) in listOf(-d to 0f, d to 0f, 0f to -d, 0f to d, -d to -d, d to d, -d to d, d to -d, 0f to d * 1.6f)) {
            Text(text, Modifier.offset(dx.dp / 2, dy.dp / 2), color = outline, fontFamily = Fredoka, fontWeight = FontWeight.Bold,
                fontSize = size.sp, textAlign = align, maxLines = 2)
        }
        Text(text, color = color, fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = size.sp, textAlign = align, maxLines = 2)
    }
}

/** A cream card with a soft shadow. */
@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = Palette.Cream, radius: Dp = 22.dp, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.shadow(6.dp, RoundedCornerShape(radius), ambientColor = Palette.Cocoa, spotColor = Palette.Cocoa)
            .clip(RoundedCornerShape(radius)).background(color)
            .border(2.dp, Color.White, RoundedCornerShape(radius)),
        content = content,
    )
}

/** A top-bar counter: icon, number, optional plus button. */
@Composable
fun CurrencyPill(art: String, value: String, color: Color, modifier: Modifier = Modifier, desc: String = "", onPlus: (() -> Unit)? = null) {
    Box(modifier.height(36.dp).semantics { if (desc.isNotEmpty()) contentDescription = desc }) {
        Row(
            Modifier.padding(start = 14.dp).height(30.dp).align(Alignment.CenterStart)
                .clip(RoundedCornerShape(50)).background(Color(0xCC5B3220))
                .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(50))
                .padding(start = 22.dp, end = if (onPlus != null) 4.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, color = Color.White, fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (onPlus != null) {
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(22.dp).clip(CircleShape).background(Palette.Green).clickable { onPlus() }, contentAlignment = Alignment.Center) {
                    Text("+", color = Color.White, fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
        }
        ItemArt(art, Modifier.size(36.dp).align(Alignment.CenterStart))
    }
}

/** A rounded progress bar with a shine. */
@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = Palette.Green, height: Dp = 16.dp, label: String? = null) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600, easing = FastOutSlowInEasing), label = "bar")
    Box(modifier.height(height).clip(RoundedCornerShape(50)).background(Color(0x33000000)), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().padding(2.dp)) {
            if (p > 0f) Box(Modifier.fillMaxWidth(p).fillMaxSize().clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(color.lighten(0.35f), color))))
        }
        if (label != null) OutlinedText(label, 11, outline = Color(0xAA3A2010))
    }
}

/** Rotating light rays behind a reward. */
@Composable
fun Sunburst(modifier: Modifier = Modifier, color: Color = Color(0x66FFF3B0)) {
    val t = rememberInfiniteTransition(label = "rays")
    val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "rot")
    Canvas(modifier.rotate(a)) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension * 0.75f
        val rays = 14
        for (i in 0 until rays) {
            val a0 = (i * 2 * Math.PI / rays).toFloat()
            val a1 = a0 + (Math.PI / rays).toFloat()
            val p = Path().apply {
                moveTo(c.x, c.y)
                lineTo(c.x + r * cos(a0), c.y + r * sin(a0))
                lineTo(c.x + r * cos(a1), c.y + r * sin(a1))
                close()
            }
            drawPath(p, Brush.radialGradient(listOf(color, Color.Transparent), c, r))
        }
    }
}

/** Gentle up-down float for idle things. */
@Composable
fun Modifier.bob(amount: Float = 4f, period: Int = 1600, delayMs: Int = 0): Modifier {
    val t = rememberInfiniteTransition(label = "bob")
    val y by t.animateFloat(-amount, amount, infiniteRepeatable(tween(period, delayMs, FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    return this.graphicsLayer { translationY = y * density }
}

@Composable
fun Modifier.pulse(on: Boolean = true, amount: Float = 0.06f, period: Int = 900): Modifier {
    if (!on) return this
    val t = rememberInfiniteTransition(label = "pulse")
    val s by t.animateFloat(1f, 1f + amount, infiniteRepeatable(tween(period, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

@Composable
fun Modifier.wiggle(on: Boolean, degrees: Float = 7f): Modifier {
    if (!on) return this
    val t = rememberInfiniteTransition(label = "wiggle")
    val r by t.animateFloat(-degrees, degrees, infiniteRepeatable(tween(160, easing = LinearEasing), RepeatMode.Reverse), label = "r")
    return this.graphicsLayer { rotationZ = r }
}

/** A row of reward chips: coins, rubies, energy, items. */
@Composable
fun RewardRow(r: Reward, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        if (r.coins > 0) RewardChip("coin", "${r.coins}", size)
        if (r.rubies > 0) RewardChip("gem_stone", "${r.rubies}", size)
        if (r.energy > 0) RewardChip("high_voltage", "${r.energy}", size)
        r.items.groupingBy { it }.eachCount().forEach { (item, n) ->
            RewardChip(com.surfswirl.bakery.game.Content.art(item), if (n > 1) "×$n" else "", size)
        }
        if (r.vip) RewardChip("ticket", "VIP", size)
    }
}

@Composable
fun RewardChip(art: String, label: String, size: Dp = 44.dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ItemArt(art, Modifier.size(size))
        if (label.isNotEmpty()) OutlinedText(label, (size.value / 3f).toInt().coerceIn(11, 18))
    }
}

@Composable
fun Title(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)

@Composable
fun Badge(text: String, modifier: Modifier = Modifier, color: Color = Palette.Ruby) {
    Box(modifier.clip(RoundedCornerShape(50)).background(color).border(1.5.dp, Color.White, RoundedCornerShape(50))
        .padding(horizontal = 6.dp, vertical = 1.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

/** A round icon button with a label underneath, for the bottom bar. */
@Composable
fun NavButton(art: String, label: String, modifier: Modifier = Modifier, dot: Boolean = false, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.4f), label = "nav")
    Column(modifier.scale(scale).semantics { contentDescription = label }.clickable(src, null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(Modifier.size(50.dp).shadow(4.dp, CircleShape).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFFE9D6))))
                .border(2.5.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                ItemArt(art, Modifier.size(34.dp))
            }
            if (dot) Box(Modifier.size(14.dp).align(Alignment.TopEnd).clip(CircleShape).background(Palette.Ruby).border(2.dp, Color.White, CircleShape))
        }
        OutlinedText(label, 12, outline = Color(0xFF6B3A22))
    }
}
