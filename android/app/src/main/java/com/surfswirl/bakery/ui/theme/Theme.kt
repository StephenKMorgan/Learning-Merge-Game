package com.surfswirl.bakery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.surfswirl.bakery.R
import com.surfswirl.bakery.game.Theme

val Fredoka = FontFamily(Font(R.font.fredoka_semibold, FontWeight.SemiBold), Font(R.font.fredoka_bold, FontWeight.Bold))
val Nunito = FontFamily(Font(R.font.nunito_semibold, FontWeight.Normal), Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_extrabold, FontWeight.Bold), Font(R.font.nunito_extrabold, FontWeight.ExtraBold))

object Palette {
    val Cocoa = Color(0xFF5B3220)
    val CocoaSoft = Color(0xFF8A5A44)
    val Cream = Color(0xFFFFF8EE)
    val Coin = Color(0xFFFFC21A)
    val Ruby = Color(0xFFFF3D7F)
    val Energy = Color(0xFF3CCB5A)
    val Green = Color(0xFF34C759)
    val GreenDark = Color(0xFF1E9E43)
    val Pink = Color(0xFFFF6FA8)
    val PinkDark = Color(0xFFD94A86)
    val Orange = Color(0xFFFF9F2E)
    val OrangeDark = Color(0xFFE07A10)
    val Blue = Color(0xFF3FA9F5)
    val BlueDark = Color(0xFF1E7FCC)
    val Purple = Color(0xFFA86BFF)
    val PurpleDark = Color(0xFF7D45D9)
    val Gold = Color(0xFFFFCF3F)
    val GoldDark = Color(0xFFE0A800)
    val Gray = Color(0xFFB9AFA8)
    val GrayDark = Color(0xFF8F8580)
}

data class GameColors(
    val bgTop: Color, val bgBottom: Color, val accent: Color, val tileA: Color, val tileB: Color, val frame: Color,
    val theme: Theme,
)

val LocalGameColors = staticCompositionLocalOf<GameColors> { error("no theme") }

/** The baker's chosen look, for scenes and portraits. */
val LocalAvatar = androidx.compose.runtime.compositionLocalOf { com.surfswirl.bakery.game.Avatar() }

fun Theme.colors() = GameColors(Color(bgTop), Color(bgBottom), Color(accent), Color(tileA), Color(tileB), Color(boardFrame), this)

@Composable
fun BakeryTheme(theme: Theme, content: @Composable () -> Unit) {
    val c = theme.colors()
    val type = Typography(
        displaySmall = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Palette.Cocoa),
        headlineSmall = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Palette.Cocoa),
        titleLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Palette.Cocoa),
        titleMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Palette.Cocoa),
        titleSmall = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Palette.Cocoa),
        bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Palette.Cocoa),
        bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Palette.Cocoa),
        bodySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Palette.CocoaSoft),
        labelLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        labelMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
        labelSmall = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
    )
    val scheme = lightColorScheme(primary = c.accent, secondary = Palette.Pink, background = c.bgTop, surface = Palette.Cream,
        onSurface = Palette.Cocoa, onBackground = Palette.Cocoa)
    androidx.compose.runtime.CompositionLocalProvider(LocalGameColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}
