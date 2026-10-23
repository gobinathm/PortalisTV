package io.github.gobinathm.portalistv.core.stalker

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

// Portal JSON is loosely typed: the same field may be a number, a numeric string, "", or null
// depending on the middleware version. These readers accept all of them.

internal fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content?.takeIf { it.isNotEmpty() }

internal fun JsonObject.int(key: String): Int? =
    (this[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.content.trim().toIntOrNull() }

/** Accepts true/false, 1/0 and "1"/"0". */
internal fun JsonObject.flag(key: String): Boolean {
    val value = this[key] as? JsonPrimitive ?: return false
    return value.booleanOrNull ?: (value.content.trim().toIntOrNull()?.let { it != 0 } ?: false)
}

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

/** A list that some portals send as an array and others as an object keyed by index. */
internal fun JsonElement?.objects(): List<JsonObject> = when (this) {
    is JsonArray -> filterIsInstance<JsonObject>()
    is JsonObject -> values.filterIsInstance<JsonObject>()
    else -> emptyList()
}
