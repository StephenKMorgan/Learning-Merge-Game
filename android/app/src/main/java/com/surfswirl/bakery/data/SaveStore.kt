package com.surfswirl.bakery.data

import android.content.Context
import android.util.AtomicFile
import com.surfswirl.bakery.game.CURRENT_SAVE_VERSION
import com.surfswirl.bakery.game.GameState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Upgrades old save files. Each step turns version N into N+1. Add a step whenever
 * CURRENT_SAVE_VERSION is bumped; never edit an existing step.
 */
object Migrations {
    private val steps: Map<Int, (JsonObject) -> JsonObject> = mapOf(
        // 1 to { old -> JsonObject(old + ("newField" to JsonPrimitive(...))) },
    )

    fun upgrade(obj: JsonObject): JsonObject {
        var o = obj
        var v = o["saveVersion"]?.jsonPrimitive?.int ?: 1
        while (v < CURRENT_SAVE_VERSION) {
            o = steps[v]?.invoke(o) ?: o
            v++
            o = JsonObject(o + ("saveVersion" to JsonPrimitive(v)))
        }
        return o
    }
}

object SaveCodec {
    val json = Json {
        ignoreUnknownKeys = true   // a newer save opened by an older build still loads
        encodeDefaults = true
        coerceInputValues = true   // bad/null values fall back to defaults instead of crashing
        explicitNulls = false
    }

    fun encode(s: GameState): String = json.encodeToString(GameState.serializer(), s)

    fun decode(text: String): GameState {
        val obj = json.parseToJsonElement(text).jsonObject
        return json.decodeFromJsonElement(GameState.serializer(), Migrations.upgrade(obj))
    }
}

/**
 * Keeps the game in files/save.json, written atomically (a crash mid-write can't corrupt it), plus
 * save.prev.json with the previous good save as a fallback. Android Auto Backup also copies these
 * to the user's Google account, so a new phone gets the progress back.
 */
class SaveStore(dir: File) {
    constructor(context: Context) : this(context.filesDir)

    private val main = AtomicFile(File(dir, "save.json"))
    private val prev = AtomicFile(File(dir, "save.prev.json"))
    private var lastWritten: String? = null

    fun load(): GameState? {
        for (f in listOf(main, prev)) {
            val s = runCatching { SaveCodec.decode(String(f.readFully(), Charsets.UTF_8)) }.getOrNull()
            if (s != null) return s
        }
        return null
    }

    @Synchronized
    fun save(s: GameState) {
        val text = SaveCodec.encode(s)
        if (text == lastWritten) return
        lastWritten?.let { write(prev, it) }
        write(main, text)
        lastWritten = text
    }

    fun exportText(s: GameState) = SaveCodec.encode(s)

    private fun write(f: AtomicFile, text: String) {
        val out = f.startWrite()
        try {
            out.write(text.toByteArray(Charsets.UTF_8))
            f.finishWrite(out)
        } catch (e: Exception) {
            f.failWrite(out)
        }
    }
}
