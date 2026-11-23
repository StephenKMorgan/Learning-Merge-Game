package com.surfswirl.bakery.music

/** What a part does in the band. */
enum class Role { PAD, COMP, BASS, DRUMS, ARP, MELODY, COUNTER, EXTRA }

/**
 * One instrument in a style. It joins in once the music's intensity reaches [from] (0..1), so a quiet
 * board is just piano, and a busy one gets the whole band.
 */
data class Part(
    val role: Role,
    val inst: Inst,
    val pattern: String,
    val from: Float,
    val gain: Float,
    val pan: Float = 0f,
    /** Semitones to shift (melody/counter/arp register). */
    val shift: Int = 0,
    /** Also play the melody an octave lower, quietly (marimba-style). */
    val double: Boolean = false,
)

/**
 * A style of music. Chords are fixed per section (so each style keeps its identity); melodies, fills
 * and voicings are made up fresh every time. To add music for a new event, add a Style here and map
 * its event id in [Styles.forEvent].
 */
data class Style(
    val id: String,
    val bpm: Double,
    /** Where the off-beat eighth lands: 0.5 = straight, ~0.62 = swing. */
    val swing: Double,
    val sections: Map<Char, List<String>>,
    val form: String,
    /** Melody key: the major scale this root (0 = C) or natural minor if [minor]. */
    val key: Int,
    val minor: Boolean = false,
    val parts: List<Part>,
    val reverb: Float = 0.22f,
    val melodyLow: Int = 67,
    val melodyHigh: Int = 84,
    /** Highest intensity this style goes to (night music stays soft). */
    val maxIntensity: Float = 1f,
)

object Styles {
    val cafe = Style("cafe", 82.0, 0.62, mapOf(
        'A' to listOf("Fmaj7", "Em7", "Dm7", "C7", "Bbmaj7", "Am7", "Gm7", "C7"),
        'B' to listOf("Bbmaj7", "Bbmaj7", "Am7", "Dm7", "Gm7", "C7", "Fmaj7", "C7sus"),
    ), "AABA", key = 5, parts = listOf(
        Part(Role.COMP, Inst.RHODES, "swing", 0f, 0.34f, -0.25f),
        Part(Role.BASS, Inst.BASS, "walk", 0.2f, 0.55f, 0.05f),
        Part(Role.MELODY, Inst.CELESTA, "melody", 0.3f, 0.32f, 0.3f),
        Part(Role.DRUMS, Inst.BRUSH, "brush", 0.45f, 0.9f),
        Part(Role.COUNTER, Inst.VIBES, "long", 0.75f, 0.22f, -0.4f, shift = -12),
    ))

    val patio = Style("patio", 104.0, 0.5, mapOf(
        'A' to listOf("Cmaj7", "Am7", "Dm7", "G7", "Em7", "A7", "Dm7", "G7"),
        'B' to listOf("Cmaj7", "C7", "Fmaj7", "Fm6", "Em7", "A7", "Dm7", "G7"),
    ), "AABA", key = 0, parts = listOf(
        Part(Role.COMP, Inst.GUITAR, "bossa", 0f, 0.26f, -0.3f),
        Part(Role.BASS, Inst.BASS, "bossa", 0.2f, 0.5f),
        Part(Role.MELODY, Inst.MARIMBA, "melody", 0.3f, 0.34f, 0.25f, shift = 5, double = true),
        Part(Role.DRUMS, Inst.SHAKER, "bossa", 0.45f, 0.9f),
        Part(Role.COUNTER, Inst.FLUTE, "long", 0.75f, 0.2f, -0.35f, shift = -5),
    ), melodyLow = 72, melodyHigh = 88)

    val night = Style("night", 64.0, 0.58, mapOf(
        'A' to listOf("Dbmaj7", "Bbm7", "Ebm7", "Ab7", "Gbmaj7", "Fm7", "Ebm7", "Ab7"),
        'B' to listOf("Gbmaj7", "Fm7", "Bbm7", "Ebm7", "Gbmaj7", "Ab7", "Dbmaj7", "Ab7sus"),
    ), "AABA", key = 1, parts = listOf(
        Part(Role.PAD, Inst.PAD, "pad", 0f, 0.16f),
        Part(Role.COMP, Inst.RHODES, "sparse", 0f, 0.3f, -0.2f),
        Part(Role.BASS, Inst.BASS, "roots", 0.25f, 0.45f),
        Part(Role.MELODY, Inst.MUSIC_BOX, "melody", 0.3f, 0.26f, 0.3f, shift = 12),
        Part(Role.ARP, Inst.HARP, "arp", 0.55f, 0.18f, -0.4f),
        Part(Role.DRUMS, Inst.BRUSH, "brushLight", 0.7f, 0.7f),
    ), reverb = 0.32f, maxIntensity = 0.8f)

    /** Calm and steady for the brain puzzles: no drums, a gentle repeating arpeggio, a sparse tune. */
    val puzzle = Style("puzzle", 70.0, 0.5, mapOf(
        'A' to listOf("Gmaj7", "Em7", "Cmaj7", "D7sus", "Gmaj7", "Bm7", "Cmaj7", "D7sus"),
        'B' to listOf("Em7", "Cmaj7", "Am7", "D7sus", "Em7", "Cmaj7", "Am7", "D7"),
    ), "AABA", key = 7, parts = listOf(
        Part(Role.PAD, Inst.PAD, "pad", 0f, 0.15f),
        Part(Role.ARP, Inst.MUSIC_BOX, "arp", 0f, 0.15f, 0.2f),
        Part(Role.BASS, Inst.BASS, "pedal", 0f, 0.35f),
        Part(Role.MELODY, Inst.VIBES, "melodySparse", 0f, 0.24f, -0.2f, shift = -12),
    ), reverb = 0.3f)

    val frosty = cafe.copy(id = "frosty", parts = listOf(
        Part(Role.COMP, Inst.RHODES, "swing", 0f, 0.32f, -0.25f),
        Part(Role.BASS, Inst.BASS, "walk", 0.2f, 0.55f, 0.05f),
        Part(Role.MELODY, Inst.GLOCK, "melody", 0.3f, 0.22f, 0.3f, shift = 12),
        Part(Role.EXTRA, Inst.SLEIGH, "offbeats", 0.35f, 0.7f, -0.4f),
        Part(Role.DRUMS, Inst.BRUSH, "brush", 0.5f, 0.8f),
        Part(Role.COUNTER, Inst.CELESTA, "long", 0.75f, 0.24f, -0.3f),
    ))

    val harvest = Style("harvest", 92.0, 0.5, mapOf(
        'A' to listOf("Dmaj", "Bm7", "Gmaj", "A7", "Dmaj", "Gmaj", "Em7", "A7"),
        'B' to listOf("Gmaj", "Amaj", "Bm7", "Gmaj", "Em7", "A7", "Dmaj", "A7sus"),
    ), "AABA", key = 2, parts = listOf(
        Part(Role.COMP, Inst.GUITAR, "fingerpick", 0f, 0.28f, -0.25f),
        Part(Role.BASS, Inst.BASS, "folk", 0.2f, 0.48f),
        Part(Role.MELODY, Inst.FLUTE, "melody", 0.3f, 0.26f, 0.25f),
        Part(Role.DRUMS, Inst.KICK, "folk", 0.45f, 0.85f),
        Part(Role.COUNTER, Inst.HARP, "long", 0.75f, 0.22f, -0.35f),
    ))

    val summer = Style("summer", 108.0, 0.56, mapOf(
        'A' to listOf("Gmaj", "Cmaj", "D7", "Gmaj", "Em7", "Cmaj", "D7", "Gmaj"),
        'B' to listOf("Cmaj", "D7", "Bm7", "Em7", "Am7", "D7", "Gmaj", "D7"),
    ), "AABA", key = 7, parts = listOf(
        Part(Role.COMP, Inst.UKULELE, "strum", 0f, 0.22f, -0.3f),
        Part(Role.BASS, Inst.BASS, "bossa", 0.2f, 0.5f),
        Part(Role.MELODY, Inst.STEEL_DRUM, "melody", 0.3f, 0.3f, 0.25f),
        Part(Role.DRUMS, Inst.SHAKER, "island", 0.45f, 0.9f),
        Part(Role.COUNTER, Inst.MARIMBA, "long", 0.75f, 0.24f, -0.35f, shift = -12),
    ), melodyLow = 69, melodyHigh = 86)

    val spooky = Style("spooky", 96.0, 0.62, mapOf(
        'A' to listOf("Dm", "Dm6", "Gm", "A7", "Dm", "Bbmaj7", "Gm6", "A7"),
        'B' to listOf("Gm", "Dm", "Gm6", "A7", "Bbmaj7", "Gm6", "A7", "A7"),
    ), "AABA", key = 2, minor = true, parts = listOf(
        Part(Role.COMP, Inst.PIZZ, "staccato", 0f, 0.3f, -0.25f),
        Part(Role.BASS, Inst.PIZZ, "walk", 0.2f, 0.55f, shift = 0),
        Part(Role.MELODY, Inst.CELESTA, "melody", 0.3f, 0.3f, 0.3f),
        Part(Role.DRUMS, Inst.BRUSH, "brush", 0.45f, 0.8f),
        Part(Role.COUNTER, Inst.VIBES, "long", 0.75f, 0.22f, -0.3f, shift = -12),
    ))

    val sweetheart = Style("sweetheart", 76.0, 0.6, mapOf(
        'A' to listOf("Ebmaj7", "Cm7", "Fm7", "Bb7", "Ebmaj7", "Abmaj7", "Fm7", "Bb7"),
        'B' to listOf("Abmaj7", "Gm7", "Cm7", "Fm7", "Abmaj7", "Bb7", "Ebmaj7", "Bb7sus"),
    ), "AABA", key = 3, parts = listOf(
        Part(Role.PAD, Inst.PAD, "pad", 0f, 0.12f),
        Part(Role.COMP, Inst.RHODES, "swing", 0f, 0.3f, -0.25f),
        Part(Role.BASS, Inst.BASS, "roots", 0.2f, 0.5f),
        Part(Role.MELODY, Inst.VIBES, "melody", 0.3f, 0.3f, 0.3f),
        Part(Role.DRUMS, Inst.BRUSH, "brushLight", 0.5f, 0.8f),
        Part(Role.COUNTER, Inst.FLUTE, "long", 0.75f, 0.2f, -0.35f),
    ))

    val spring = Style("spring", 100.0, 0.5, mapOf(
        'A' to listOf("Fmaj7", "Gm7", "Am7", "Bbmaj7", "Fmaj7", "Dm7", "Gm7", "C7sus"),
        'B' to listOf("Bbmaj7", "C7", "Am7", "Dm7", "Gm7", "C7", "Fmaj7", "C7"),
    ), "AABA", key = 5, parts = listOf(
        Part(Role.COMP, Inst.HARP, "fingerpick", 0f, 0.26f, -0.25f),
        Part(Role.BASS, Inst.BASS, "folk", 0.2f, 0.45f),
        Part(Role.MELODY, Inst.FLUTE, "melody", 0.3f, 0.26f, 0.25f),
        Part(Role.DRUMS, Inst.SHAKER, "light", 0.45f, 0.8f),
        Part(Role.COUNTER, Inst.MARIMBA, "long", 0.75f, 0.24f, -0.3f, shift = -12),
    ))

    /** Volume per style, so they're all about equally loud (measured by MusicEngineTest). */
    val level = mapOf("cafe" to 0.8f, "frosty" to 0.8f, "patio" to 1.1f, "night" to 1.1f, "puzzle" to 1.25f, "harvest" to 1.1f,
        "summer" to 1.05f, "spooky" to 1.8f, "sweetheart" to 0.9f, "spring" to 1.15f)

    fun forEvent(id: String): Style? = when (id) {
        "harvest" -> harvest; "frosty" -> frosty; "summer" -> summer; "spooky" -> spooky
        "sweetheart" -> sweetheart; "spring" -> spring
        else -> null
    }
}

/** What's going on in the game, which decides the music. */
enum class Mood { DAY, NIGHT, PUZZLE }
