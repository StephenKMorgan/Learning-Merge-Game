package com.surfswirl.bakery.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import com.surfswirl.bakery.ui.components.OutlinedText
import com.surfswirl.bakery.ui.components.bob
import com.surfswirl.bakery.ui.components.pulse
import com.surfswirl.bakery.ui.theme.Palette
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One picture in a scene, drawn at (x, y) in scene pixels. See tools/scenes/make_scenes.py. */
@Serializable
data class SceneLayer(
    val id: String, val file: String, val x: Int, val y: Int, val w: Int, val h: Int, val z: Int, val show: String,
    /** "hair" / "skin" / "outfit": a grey part of Mandy that is multiplied by her chosen colour. */
    val tint: String = "",
    /** A hair style id or "glasses": only shown when the baker's look uses it. */
    val variant: String = "",
) {
    fun visible(done: Set<String>, avatar: com.surfswirl.bakery.game.Avatar = com.surfswirl.bakery.game.Avatar()) = avatar.shows(variant) && when {
        show.startsWith("done:") -> show.removePrefix("done:") in done
        show.startsWith("undone:") -> show.removePrefix("undone:") !in done
        else -> true
    }
}

@Serializable
data class SceneDef(val w: Int, val h: Int, val layers: List<SceneLayer>)

object Scenes {
    private val json = Json { ignoreUnknownKeys = true }
    private val defs = HashMap<String, SceneDef?>()
    private val images = HashMap<String, ImageBitmap?>()

    fun def(ctx: Context, scene: String): SceneDef? = defs.getOrPut(scene) {
        runCatching { json.decodeFromString(SceneDef.serializer(), ctx.assets.open("scenes/$scene.json").bufferedReader().readText()) }.getOrNull()
    }

    fun image(ctx: Context, file: String): ImageBitmap? = images.getOrPut(file) {
        runCatching { ctx.assets.open("scenes/$file").use { BitmapFactory.decodeStream(it) }.asImageBitmap() }.getOrNull()
    }
}

/**
 * A renovation scene. Decorations pop in when their task is done, "before" things (dust, the boarded
 * door) fade out, and the next task's spot is marked with a pulsing "?".
 */
@Composable
fun SceneView(scene: String, done: Set<String>, nextTask: String?, onTapLayer: ((SceneLayer) -> Unit)? = null,
              onExpand: (() -> Unit)? = null, modifier: Modifier = Modifier, rounded: Boolean = true) {
    val ctx = LocalContext.current
    val def = remember(scene) { Scenes.def(ctx, scene) } ?: return
    val avatar = com.surfswirl.bakery.ui.theme.LocalAvatar.current
    val avatarNow by rememberUpdatedState(avatar)
    val doneNow by rememberUpdatedState(done)
    // One shared idle bob, so all of Mandy's parts (and Surf's hat) move together
    val idle = androidx.compose.animation.core.rememberInfiniteTransition(label = "idle")
    val mandyBob by idle.animateFloat(-2.5f, 2.5f, androidx.compose.animation.core.infiniteRepeatable(
        tween(2200, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "mandy")
    val surfBob by idle.animateFloat(-1.5f, 1.5f, androidx.compose.animation.core.infiniteRepeatable(
        tween(900, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "surf")
    val onTap by rememberUpdatedState(onTapLayer)
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(def.w / def.h.toFloat())
        .then(if (rounded) Modifier.clip(RoundedCornerShape(18.dp())) else Modifier)
        .semantics { contentDescription = "Scene $scene" }) {
        val k = constraints.maxWidth / def.w.toFloat()
        // Tap a decoration (or Mandy, or Surf) to hear its story: pick the topmost visible pixel under the finger.
        Box(Modifier.fillMaxSize().pointerInput(def) {
            detectTapGestures { pos ->
                val handler = onTap ?: return@detectTapGestures
                val kk = size.width / def.w.toFloat()
                val sx = (pos.x / kk).toInt(); val sy = (pos.y / kk).toInt()
                val hit = def.layers.sortedByDescending { it.z }.firstOrNull { l ->
                    if (l.id == "base" || !l.visible(doneNow, avatarNow)) return@firstOrNull false
                    if (sx < l.x || sy < l.y || sx >= l.x + l.w || sy >= l.y + l.h) return@firstOrNull false
                    val bmp = Scenes.image(ctx, l.file)?.asAndroidBitmap() ?: return@firstOrNull false
                    val px = bmp.getPixel((sx - l.x).coerceIn(0, bmp.width - 1), (sy - l.y).coerceIn(0, bmp.height - 1))
                    (px ushr 24) > 60
                }
                if (hit != null) handler(hit)
            }
        })
        val dens = LocalDensity.current
        for (layer in def.layers.sortedBy { it.z }) key(layer.id) {
            val visible = layer.visible(done, avatar)
            val a by animateFloatAsState(if (visible) 1f else 0f,
                if (layer.show.startsWith("done:")) spring(dampingRatio = 0.45f, stiffness = 220f) else tween(700, easing = FastOutSlowInEasing),
                label = layer.id)
            if (a > 0.001f) {
                val img = remember(layer.file) { Scenes.image(ctx, layer.file) }
                if (img != null) {
                    val character = layer.id.startsWith("mandy") || layer.id.startsWith("surf")
                    val isSurf = layer.id.startsWith("surf")
                    val tintColor = avatar.tint(layer.tint)?.let {
                        androidx.compose.ui.graphics.ColorFilter.tint(Color(it), androidx.compose.ui.graphics.BlendMode.Modulate)
                    }
                    Image(img, contentDescription = null, colorFilter = tintColor,
                        modifier = Modifier
                            .offset { IntOffset((layer.x * k).toInt(), (layer.y * k).toInt()) }
                            .size(with(dens) { (layer.w * k).toDp() }, with(dens) { (layer.h * k).toDp() })
                            .graphicsLayer {
                                alpha = a.coerceIn(0f, 1f)
                                if (layer.show.startsWith("done:") && !character) { val s = 0.6f + 0.4f * a; scaleX = s; scaleY = s }
                                if (character) translationY = (if (isSurf) surfBob else mandyBob) * this.density
                            }
                            )
                }
            }
        }
        // Where the next decoration will go
        val target = nextTask?.let { t -> def.layers.firstOrNull { it.show == "done:$t" } }
        if (target != null) {
            val cx = (target.x + target.w / 2f) * k
            val cy = (target.y + target.h / 2f) * k
            val d = (minOf(maxOf(target.w, target.h) * k * 0.6f, constraints.maxWidth * 0.22f)).coerceAtLeast(constraints.maxWidth * 0.1f)
            Box(Modifier.offset { IntOffset((cx - d / 2).toInt(), (cy - d / 2).toInt()) }
                .size(with(dens) { d.toDp() }).pulse(amount = 0.08f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Color(0x88FFFFFF), size.minDimension / 2)
                    drawCircle(Color.White, size.minDimension / 2, style = Stroke(5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
                }
                OutlinedText("?", 22, outline = Palette.PinkDark)
            }
        }
        if (onExpand != null) {
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp()).size(40.dp()).clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0xCCFFFFFF)).semantics { contentDescription = "Explore" }.clickable { onExpand() },
                contentAlignment = Alignment.Center) { OutlinedText("⤢", 20, color = Palette.PinkDark, outline = Color.White) }
        }
    }
}

private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())
