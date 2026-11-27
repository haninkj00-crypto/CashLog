package com.example.cashlog.data

import androidx.room.TypeConverter
import com.example.cashlog.model.Denomination
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromCountsMap(value: Map<Denomination, Int>?): String? {
        if (value == null) return null
        val json = JSONObject()
        for (entry in value.entries) {
            json.put(entry.key.name, entry.value)
        }
        return json.toString()
    }

    @TypeConverter
    fun toCountsMap(value: String?): Map<Denomination, Int>? {
        if (value == null) return null
        val json = JSONObject(value)
        val map = mutableMapOf<Denomination, Int>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            try {
                val denom = Denomination.valueOf(key)
                val count = json.getInt(key)
                map[denom] = count
            } catch (e: Exception) {
                // Ignore
            }
        }
        return map
    }
}
