package com.surfswirl.bakery.game

import kotlinx.serialization.Serializable

/** How the baker looks (Settings → Your baker). Colours are ARGB. The defaults are the original Mandy. */
@Serializable
data class Avatar(
    val hairStyle: String = "bob",
    val hair: Long = 0xFF7A4630,
    val skin: Long = 0xFFFFD9BD,
    val outfit: Long = 0xFFFF8FB5,
    val glasses: Boolean = false,
) {
    fun tint(kind: String): Long? = when (kind) { "hair" -> hair; "skin" -> skin; "outfit" -> outfit; else -> null }

    /** Whether a part drawn for [variant] ("" = always, a hair style, or "glasses") is shown. */
    fun shows(variant: String) = when (variant) {
        "" -> true
        "glasses" -> glasses
        else -> variant == hairStyle
    }
}

object Looks {
    val hairStyles = listOf("bob" to "Bob", "long" to "Long", "ponytail" to "Ponytail", "curly" to "Curly")
    val hairColors = listOf(0xFF7A4630, 0xFF4A2C20, 0xFF2B2420, 0xFFA5482A, 0xFFD8732E, 0xFFE8C26A, 0xFFB8B0A8, 0xFFFF8FB5)
    val skinTones = listOf(0xFFFFE7D6, 0xFFFFD9BD, 0xFFF1C09A, 0xFFD9A079, 0xFFB07650, 0xFF7D5036)
    val outfits = listOf(0xFFFF8FB5, 0xFF7AA0FF, 0xFF6FCFB4, 0xFFFFD34D, 0xFFB77DFF, 0xFFFF6B6B)
}
