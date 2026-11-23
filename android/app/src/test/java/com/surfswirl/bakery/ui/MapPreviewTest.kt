package com.surfswirl.bakery.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.surfswirl.bakery.ui.screens.MapBirds
import com.surfswirl.bakery.ui.screens.MapData
import com.surfswirl.bakery.ui.screens.MapLife
import com.surfswirl.bakery.ui.screens.MapNight
import com.surfswirl.bakery.ui.screens.MapTime
import com.surfswirl.bakery.ui.screens.Scenes
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h732dp-xxhdpi")
class MapPreviewTest {
    @get:org.junit.Rule val rule = androidx.compose.ui.test.junit4.createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun summerDay() = render("summer", 12f, "map-life")
    @Test fun winterNight() = render("winter", 22f, "map-winter-night")
    @Test fun autumnEvening() = render("autumn", 18.5f, "map-autumn-evening")
    @Test fun springMorning() = render("spring", 7f, "map-spring-morning")

    private fun render(season: String, hour: Float, name: String) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val ctx = LocalContext.current
            val map = MapData.get(ctx)!!
            val img = Scenes.image(ctx, "map_$season.webp")!!
            Box(Modifier.fillMaxSize()) {
                Image(img, null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
                val k = 1236f / map.w
                val light = MapTime.light(hour)
                MapLife(map.anims, k, Modifier.matchParentSize())
                MapNight(map.anims, k, light, Modifier.matchParentSize())
                MapBirds(map.w * k, map.h * k, k * 1.25f, Modifier.matchParentSize(), alpha = 1f - light.stars)
            }
        }
        rule.mainClock.advanceTimeBy(7_300)
        val view = rule.activity.window.decorView
        val bmp = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(android.graphics.Canvas(bmp)) }
        File(System.getProperty("user.dir"), "build/screenshots/$name.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
