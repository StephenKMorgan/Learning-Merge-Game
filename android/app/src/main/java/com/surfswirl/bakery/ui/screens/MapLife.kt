package com.surfswirl.bakery.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/** Where the map's moving things are (map.json "anims", in map pixels). Drawn by [MapLife] and [MapBirds]. */
@kotlinx.serialization.Serializable
data class MapAnims(
    val smoke: List<List<Int>> = emptyList(),
    val glints: List<List<Int>> = emptyList(),
    /** x, y, colour index. */
    val boats: List<List<Int>> = emptyList(),
    /** x, y, radius. */
    val wheel: List<Int> = emptyList(),
    val beam: List<Int> = emptyList(),
    val fountain: List<Int> = emptyList(),
    val mill: List<Int> = emptyList(),
    /** Window centres and street lamps, lit at night. */
    val lights: List<List<Int>> = emptyList(),
    val lamps: List<List<Int>> = emptyList(),
    /** Where the sky ends (map pixels), for the stars. */
    val sky: Int = 0,
)

/** Which season's map to show and how light it is, from the real date and clock. */
object MapTime {
    /** For tests and previews. */
    var hourOverride: Float? = null
    var seasonOverride: String? = null
    fun hour(): Float = hourOverride ?: java.time.LocalTime.now().let { it.hour + it.minute / 60f }
    fun season(d: java.time.LocalDate): String = seasonOverride ?: when (d.monthValue) {
        12, 1, 2 -> "winter"; 3, 4, 5 -> "spring"; 6, 7, 8 -> "summer"; else -> "autumn"
    }

    /** tint over the town, how strong it is, how bright windows and lamps glow, how many stars show. */
    data class Light(val tint: Color, val amount: Float, val glow: Float, val stars: Float)
    private val night = Color(0xFF15214A)
    private val keys = listOf(
        0f to Light(night, 0.58f, 1f, 1f), 5f to Light(night, 0.58f, 1f, 1f),
        6.5f to Light(Color(0xFFFF8FA3), 0.2f, 0.3f, 0.15f), 8f to Light(Color.White, 0f, 0f, 0f),
        16.5f to Light(Color.White, 0f, 0f, 0f), 18f to Light(Color(0xFFFF9A3C), 0.2f, 0.2f, 0f),
        19.5f to Light(Color(0xFF4B3C7A), 0.38f, 0.8f, 0.4f), 21f to Light(night, 0.58f, 1f, 1f), 24f to Light(night, 0.58f, 1f, 1f),
    )
    fun light(h: Float): Light {
        val i = keys.indexOfLast { it.first <= h }.coerceIn(0, keys.size - 2)
        val (h0, a) = keys[i]; val (h1, b) = keys[i + 1]
        val t = ((h - h0) / (h1 - h0)).coerceIn(0f, 1f)
        fun f(x: Float, y: Float) = x + (y - x) * t
        return Light(androidx.compose.ui.graphics.lerp(a.tint, b.tint, t), f(a.amount, b.amount), f(a.glow, b.glow), f(a.stars, b.stars))
    }
}

/** Seconds on a loop, for all the map's little movements. */
@Composable
private fun mapClock(): Float {
    val t = rememberInfiniteTransition(label = "map")
    val s by t.animateFloat(0f, 600f, infiniteRepeatable(tween(600_000, easing = LinearEasing), RepeatMode.Restart), label = "s")
    return s
}

private val boatColors = listOf(Color(0xFFD9572B), Color(0xFF3F7FB5), Color(0xFF5B9A4A))
private val cabinColors = listOf(Color(0xFFD9572B), Color(0xFFE0A42F), Color(0xFF3F7FB5), Color(0xFF5B9A4A))

/** Life on the map, under the fog: chimney smoke, sparkling water, the Ferris wheel, the windmill, boats, the lighthouse. */
@Composable
fun MapLife(a: MapAnims, scale: Float, modifier: Modifier) {
    // positions are in map pixels (scale); sizes are in drawing units, 1.25 map pixels each (k)
    val k = scale * 1.25f
    val time = mapClock()
    Canvas(modifier) {
        fun p(v: List<Int>) = Offset(v[0] * scale, v[1] * scale)
        // sparkles on the river, pond and sea
        a.glints.forEachIndexed { i, g ->
            val on = max(0f, sin(time * 1.3f + i * 2.17f)).pow(6)
            if (on > 0.05f) sparkle(p(g), 7f * k * (0.6f + on), Color.White.copy(alpha = on * 0.9f))
        }
        // fountain: a little rising and falling spray
        if (a.fountain.size >= 2) {
            val c = p(a.fountain)
            for (i in 0 until 8) {
                val ph = ((time * 0.9f + i / 8f) % 1f)
                val dir = if (i % 2 == 0) 1f else -1f
                val x = c.x + dir * ph * 16f * k * (0.5f + (i % 3) * 0.25f)
                val y = c.y - (sin(ph * PI.toFloat()) * 22f * k)
                drawCircle(Color(0xFFDDF4FC).copy(alpha = 0.9f * (1f - ph * 0.5f)), 2.4f * k, Offset(x, y))
            }
        }
        // windmill sails
        if (a.mill.size >= 2) {
            val c = p(a.mill)
            rotate(time * 40f, c) {
                for (j in 0 until 4) rotate(j * 90f, c) {
                    drawLine(Color(0xFF6B4630), c, c + Offset(0f, -30f * k), 3f * k)
                    drawRect(Color(0xFFF6EFE2), Offset(c.x + 1f * k, c.y - 30f * k), Size(9f * k, 22f * k))
                }
            }
            drawCircle(Color(0xFF6B4630), 3.5f * k, c)
        }
        // Ferris wheel
        if (a.wheel.size >= 3) {
            val c = p(a.wheel); val r = a.wheel[2] * scale
            val ang = time * 0.25f
            drawCircle(Color(0xFF5D7389), r, c, style = Stroke(4f * k))
            for (j in 0 until 8) {
                val t = ang + j * PI.toFloat() / 4
                val e = c + Offset(cos(t) * r, sin(t) * r)
                drawLine(Color(0xFF5D7389), c, e, 1.6f * k)
                drawRoundRect(cabinColors[j % 4], Offset(e.x - 6f * k, e.y), Size(12f * k, 10f * k), CornerRadius(2f * k))
            }
            drawCircle(Color(0xFF5D7389), 4f * k, c)
        }
        // boats rocking on the sea
        a.boats.forEach { b ->
            val i = b.getOrElse(2) { 0 }
            if (i == 3) {
                val c = Offset(b[0] * scale, b[1] * scale + sin(time * 1.1f) * 2f * k)
                rotate(sin(time * 0.9f) * 1.5f, c) { ferry(c, k) }
                return@forEach
            }
            val c = Offset(b[0] * scale + sin(time * 0.15f + i) * 18f * k, b[1] * scale + sin(time * 1.4f + i * 2f) * 3f * k)
            rotate(sin(time * 1.1f + i) * 5f, c) { boat(c, k, boatColors[i % boatColors.size]) }
        }
        // chimney smoke
        a.smoke.forEachIndexed { i, s ->
            val c = p(s)
            for (j in 0 until 3) {
                val ph = (time * 0.22f + j / 3f + i * 0.37f) % 1f
                val pos = Offset(c.x + sin(ph * 5f + i) * 4f * k + ph * 12f * k, c.y - ph * 46f * k)
                drawCircle(Color.White.copy(alpha = 0.55f * (1f - ph)), (3.5f + ph * 9f) * k, pos)
            }
        }
        // lighthouse: a slow sweeping beam
        if (a.beam.size >= 2) {
            val c = p(a.beam)
            val sweep = sin(time * 0.6f) * 40f
            rotate(25f + sweep, c) {
                val path = Path().apply { moveTo(c.x, c.y); lineTo(c.x + 190f * k, c.y - 40f * k); lineTo(c.x + 190f * k, c.y + 40f * k); close() }
                drawPath(path, Brush.horizontalGradient(listOf(Color(0x99FFF6C8), Color(0x00FFF6C8)), c.x, c.x + 190f * k))
            }
            drawCircle(Color(0xFFFFF1A8), 6f * k, c)
        }
    }
}

/** Birds gliding over the town (drawn above the fog, so the map is alive even before much is revealed). */
@Composable
fun MapBirds(w: Float, h: Float, k: Float, modifier: Modifier, alpha: Float = 1f) {
    if (alpha <= 0.02f) return
    val time = mapClock()
    Canvas(modifier) {
        val flocks = listOf(Triple(0.12f, 0.018f, 0f), Triple(0.47f, 0.013f, 0.4f), Triple(0.78f, 0.016f, 0.7f))
        for ((fy, speed, off) in flocks) {
            val x0 = (((time * speed + off) % 1.3f) - 0.15f) * w
            val y0 = fy * h + sin(time * 0.3f + off * 9f) * 20f * k
            for (b in 0 until 3) {
                val pos = Offset(x0 - b * 26f * k, y0 + (if (b == 0) 0f else (if (b % 2 == 1) -1f else 1f) * 14f * k))
                val flap = abs(sin(time * 5f + b + off * 7f))
                val wing = 9f * k; val lift = (2f + flap * 6f) * k
                val path = Path().apply {
                    moveTo(pos.x - wing, pos.y - lift); quadraticTo(pos.x - wing / 2, pos.y - lift / 3, pos.x, pos.y)
                    quadraticTo(pos.x + wing / 2, pos.y - lift / 3, pos.x + wing, pos.y - lift)
                }
                drawPath(path, Color(0xCC3B3F4A).copy(alpha = 0.8f * alpha), style = Stroke(2.2f * k, cap = StrokeCap.Round))
            }
        }
    }
}

private fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
    drawLine(color, c - Offset(r, 0f), c + Offset(r, 0f), r * 0.28f, StrokeCap.Round)
    drawLine(color, c - Offset(0f, r), c + Offset(0f, r), r * 0.28f, StrokeCap.Round)
}

private fun DrawScope.ferry(c: Offset, k: Float) {
    translate(c.x, c.y) {
        val hull = Path().apply { moveTo(-46f * k, -4f * k); lineTo(50f * k, -4f * k); lineTo(38f * k, 16f * k); lineTo(-40f * k, 16f * k); close() }
        drawPath(hull, Color(0xFFF6F2EA))
        drawRect(Color(0xFFD9572B), Offset(-42f * k, 8f * k), Size(84f * k, 5f * k))
        drawRoundRect(Color(0xFFFBEFE0), Offset(-30f * k, -22f * k), Size(54f * k, 18f * k), CornerRadius(3f * k))
        for (j in 0 until 5) drawRect(Color(0xFF5D7389), Offset((-26f + j * 10f) * k, -17f * k), Size(6f * k, 6f * k))
        drawRect(Color(0xFF3F6F8F), Offset(4f * k, -36f * k), Size(10f * k, 14f * k))
        drawRect(Color(0xFFD9572B), Offset(4f * k, -36f * k), Size(10f * k, 4f * k))
        drawOval(Color.White.copy(alpha = 0.45f), Offset(-52f * k, 14f * k), Size(108f * k, 6f * k))
    }
}

/** Evening and night over the town: a colour wash, lit windows and lamps, stars and the moon. */
@Composable
fun MapNight(a: MapAnims, scale: Float, light: MapTime.Light, modifier: Modifier) {
    if (light.amount <= 0.01f && light.glow <= 0.01f) return
    val time = mapClock()
    Canvas(modifier) {
        val k = scale * 1.25f
        drawRect(light.tint.copy(alpha = light.amount))
        if (light.stars > 0.01f) {
            val skyH = a.sky * scale
            val rnd = kotlin.random.Random(5)
            repeat(40) { i ->
                val p = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * skyH * 0.8f)
                val tw = 0.55f + 0.45f * sin(time * 1.7f + i * 1.3f)
                drawCircle(Color(0xFFFFF8E1).copy(alpha = light.stars * tw), (1.2f + rnd.nextFloat() * 1.4f) * k, p)
            }
            val moon = Offset(size.width * 0.86f, skyH * 0.28f)
            drawCircle(Color(0x33FFF6D5).copy(alpha = 0.3f * light.stars), 26f * k, moon)
            drawCircle(Color(0xFFFFF6D5).copy(alpha = light.stars), 14f * k, moon)
            drawCircle(light.tint.copy(alpha = light.stars), 12f * k, moon + Offset(6f * k, -4f * k))
        }
        if (light.glow > 0.01f) {
            val warm = Color(0xFFFFD27A)
            a.lights.forEachIndexed { i, w ->
                val c = Offset(w[0] * scale, w[1] * scale)
                val on = if (i % 7 == 3) 0.4f else 1f   // not every window is lit
                drawRect(Color(0xFFFFE9A8).copy(alpha = light.glow * on), c - Offset(5.5f * k, 6f * k), Size(11f * k, 12f * k))
                drawCircle(Brush.radialGradient(listOf(warm.copy(alpha = 0.45f * light.glow * on), Color.Transparent), c, 18f * k), 18f * k, c)
            }
            a.lamps.forEach { l ->
                val c = Offset(l[0] * scale, l[1] * scale)
                drawCircle(Brush.radialGradient(listOf(warm.copy(alpha = 0.6f * light.glow), Color.Transparent), c, 30f * k), 30f * k, c)
                drawCircle(Color(0xFFFFF1C2).copy(alpha = light.glow), 4f * k, c)
            }
        }
    }
}

private fun DrawScope.boat(c: Offset, k: Float, color: Color) {
    translate(c.x, c.y) {
        val hull = Path().apply { moveTo(-22f * k, 0f); lineTo(22f * k, 0f); lineTo(14f * k, 12f * k); lineTo(-14f * k, 12f * k); close() }
        drawPath(hull, color)
        drawLine(Color(0xFF5B3A26), Offset(0f, 0f), Offset(0f, -30f * k), 2.5f * k)
        val sail = Path().apply { moveTo(1f * k, -30f * k); lineTo(18f * k, -6f * k); lineTo(1f * k, -6f * k); close() }
        drawPath(sail, Color(0xFFFBEFE0))
        drawOval(Color.White.copy(alpha = 0.5f), Offset(-26f * k, 10f * k), Size(52f * k, 5f * k))
    }
}
