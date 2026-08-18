package com.michael.routinelock

import org.json.JSONArray
import org.json.JSONObject

data class Block(
    val id: Long,
    val start: String,
    val end: String,
    val label: String,
    val locked: Boolean,
    val apps: MutableList<String>
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("start", start)
        o.put("end", end)
        o.put("label", label)
        o.put("locked", locked)
        val arr = JSONArray()
        apps.forEach { arr.put(it) }
        o.put("apps", arr)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): Block {
            val arr = o.getJSONArray("apps")
            val apps = mutableListOf<String>()
            for (i in 0 until arr.length()) apps.add(arr.getString(i))
            return Block(
                o.getLong("id"),
                o.getString("start"),
                o.getString("end"),
                o.getString("label"),
                o.getBoolean("locked"),
                apps
            )
        }
    }
}