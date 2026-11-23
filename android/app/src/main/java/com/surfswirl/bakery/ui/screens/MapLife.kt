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
)

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
fun MapBirds(w: Float, h: Float, k: Float, modifier: Modifier) {
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
                drawPath(path, Color(0xCC3B3F4A), style = Stroke(2.2f * k, cap = StrokeCap.Round))
            }
        }
    }
}

private fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
    drawLine(color, c - Offset(r, 0f), c + Offset(r, 0f), r * 0.28f, StrokeCap.Round)
    drawLine(color, c - Offset(0f, r), c + Offset(0f, r), r * 0.28f, StrokeCap.Round)
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
