package com.surfswirl.bakery.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class Shape { STAR, CIRCLE, CONFETTI, ART }

class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val color: Color, val size: Float, val life: Float, val shape: Shape,
    val gravity: Float = 900f, var rot: Float = 0f, val spin: Float = 0f,
    val art: String? = null,
    /** If set, the particle flies to this point (coins to the counter). */
    val targetX: Float? = null, val targetY: Float? = null, val delay: Float = 0f,
) {
    var age = 0f
}

/** A screen-wide layer of sparkles, confetti and flying coins. Positions are in window pixels. */
class Particles {
    val list = mutableStateListOf<Particle>()
    var frame by mutableLongStateOf(0L)

    fun burst(x: Float, y: Float, color: Color, count: Int = 18, power: Float = 520f, shape: Shape = Shape.STAR, scale: Float = 1f) {
        repeat(count) {
            val a = Random.nextFloat() * 6.283f
            val sp = power * (0.35f + Random.nextFloat() * 0.75f)
            list += Particle(x, y, cos(a) * sp, sin(a) * sp - 180f, if (Random.nextInt(3) == 0) Color.White else color,
                (10f + Random.nextFloat() * 14f) * scale, 0.55f + Random.nextFloat() * 0.45f, shape, 700f,
                Random.nextFloat() * 360f, Random.nextFloat() * 600f - 300f)
        }
    }

    fun confetti(width: Float, count: Int = 90) {
        val colors = listOf(Color(0xFFFF6FA8), Color(0xFFFFC21A), Color(0xFF3FA9F5), Color(0xFF34C759), Color(0xFFA86BFF), Color(0xFFFF7A45))
        repeat(count) {
            list += Particle(Random.nextFloat() * width, -40f - Random.nextFloat() * 300f, Random.nextFloat() * 160f - 80f,
                200f + Random.nextFloat() * 260f, colors.random(), 16f + Random.nextFloat() * 14f, 2.6f + Random.nextFloat(),
                Shape.CONFETTI, 160f, Random.nextFloat() * 360f, Random.nextFloat() * 720f - 360f)
        }
    }

    /** Little icons (coins, rubies...) that pop out and then fly to a counter. */
    fun flyTo(art: String, fromX: Float, fromY: Float, toX: Float, toY: Float, count: Int = 6, size: Float = 70f) {
        repeat(count) { i ->
            val a = Random.nextFloat() * 6.283f
            list += Particle(fromX, fromY, cos(a) * 260f, sin(a) * 260f, Color.White, size, 1.1f, Shape.ART, 0f, art = art,
                targetX = toX, targetY = toY, delay = i * 0.05f)
        }
    }

    fun step(dt: Float) {
        val it = list.listIterator()
        while (it.hasNext()) {
            val p = it.next()
            p.age += dt
            if (p.age > p.life + p.delay) { it.remove(); continue }
            if (p.age < p.delay) continue
            if (p.targetX != null && p.targetY != null) {
                val t = ((p.age - p.delay) / p.life)
                if (t < 0.3f) { p.x += p.vx * dt; p.y += p.vy * dt; p.vx *= 0.9f; p.vy *= 0.9f }
                else { val k = ((t - 0.3f) / 0.7f).coerceIn(0f, 1f); val e = k * k * 0.35f + 0.08f
                    p.x += (p.targetX - p.x) * e; p.y += (p.targetY - p.y) * e }
            } else {
                p.vy += p.gravity * dt
                p.vx *= 0.985f
                p.x += p.vx * dt; p.y += p.vy * dt
                p.rot += p.spin * dt
            }
        }
        frame++
    }
}

@Composable
fun ParticleLayer(particles: Particles, modifier: Modifier = Modifier.fillMaxSize()) {
    val ctx = LocalContext.current
    LaunchedEffect(particles) {
        var last = 0L
        while (true) {
            withInfiniteAnimationFrameNanos { t ->
                val dt = if (last == 0L) 0.016f else ((t - last) / 1e9f).coerceAtMost(0.05f)
                last = t
                if (particles.list.isNotEmpty()) particles.step(dt)
            }
        }
    }
    Canvas(modifier) {
        @Suppress("UNUSED_VARIABLE") val f = particles.frame
        for (p in particles.list) {
            if (p.age < p.delay) continue
            val lifeT = ((p.age - p.delay) / p.life).coerceIn(0f, 1f)
            val alpha = if (p.shape == Shape.ART) (if (lifeT > 0.92f) (1 - lifeT) / 0.08f else 1f) else (1f - lifeT * lifeT)
            when (p.shape) {
                Shape.STAR -> rotate(p.rot, Offset(p.x, p.y)) { drawStar(p.x, p.y, p.size, p.color.copy(alpha = alpha)) }
                Shape.CIRCLE -> drawCircle(p.color.copy(alpha = alpha), p.size / 2, Offset(p.x, p.y))
                Shape.CONFETTI -> rotate(p.rot, Offset(p.x, p.y)) {
                    drawRect(p.color.copy(alpha = alpha), Offset(p.x - p.size / 2, p.y - p.size / 4), Size(p.size, p.size / 2))
                }
                Shape.ART -> {
                    val img = p.art?.let { Art.get(ctx, it) } ?: continue
                    val s = p.size.toInt()
                    drawImage(img, srcSize = IntSize(img.width, img.height), dstOffset = IntOffset((p.x - s / 2).toInt(), (p.y - s / 2).toInt()),
                        dstSize = IntSize(s, s), alpha = alpha)
                }
            }
        }
    }
}

fun DrawScope.drawStar(x: Float, y: Float, s: Float, color: Color) {
    val p = Path()
    for (i in 0 until 8) {
        val r = if (i % 2 == 0) s else s * 0.38f
        val a = i * Math.PI / 4 - Math.PI / 2
        val px = x + (r * cos(a)).toFloat(); val py = y + (r * sin(a)).toFloat()
        if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
    }
    p.close()
    drawPath(p, color)
}

/** Slowly drifting theme decorations (leaves, snowflakes, sparkles) behind everything. */
@Composable
fun FloatingBackground(arts: List<String>, modifier: Modifier = Modifier.fillMaxSize()) {
    val ctx = LocalContext.current
    data class Floater(var x: Float, var y: Float, val speed: Float, val sway: Float, val phase: Float, val size: Float, val art: String, var rot: Float, val spin: Float)
    val items = remember(arts) { mutableListOf<Floater>() }
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(arts) {
        var last = 0L
        while (true) withInfiniteAnimationFrameNanos { t ->
            val dt = if (last == 0L) 0.016f else ((t - last) / 1e9f).coerceAtMost(0.05f)
            last = t
            for (f in items) { f.y += f.speed * dt; f.rot += f.spin * dt }
            tick++
        }
    }
    Canvas(modifier) {
        @Suppress("UNUSED_VARIABLE") val tk = tick
        if (items.isEmpty() && size.width > 0) {
            repeat(14) {
                items += Floater(Random.nextFloat() * size.width, Random.nextFloat() * size.height, 18f + Random.nextFloat() * 30f,
                    20f + Random.nextFloat() * 30f, Random.nextFloat() * 6f, 40f + Random.nextFloat() * 50f, arts.random(),
                    Random.nextFloat() * 360f, Random.nextFloat() * 40f - 20f)
            }
        }
        for (f in items) {
            if (f.y > size.height + 80) { f.y = -80f; f.x = Random.nextFloat() * size.width }
            val img = Art.get(ctx, f.art) ?: continue
            val x = f.x + sin(f.y / 90f + f.phase) * f.sway
            translate(x, f.y) {
                rotate(f.rot, Offset.Zero) {
                    val s = f.size.toInt()
                    drawImage(img, srcSize = IntSize(img.width, img.height), dstOffset = IntOffset(-s / 2, -s / 2), dstSize = IntSize(s, s), alpha = 0.35f)
                }
            }
        }
    }
}
