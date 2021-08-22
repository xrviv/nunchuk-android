package com.nunchuk.android.messages.util

import org.json.JSONArray
import org.json.JSONObject
import org.matrix.android.sdk.api.session.events.model.toContent

fun String.toContent() = JSONObject(this).toMap().toContent()

fun JSONObject.toMap(): Map<String, *> = keys().asSequence().associateWith {
    when (val value = this[it]) {
        is JSONArray -> {
            val map = (0 until value.length()).associate { "$it" to value[it] }
            JSONObject(map).toMap().values.toList()
        }
        is JSONObject -> value.toMap()
        JSONObject.NULL -> null
        else -> value
    }
}
