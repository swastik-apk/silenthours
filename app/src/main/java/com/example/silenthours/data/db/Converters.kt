package com.example.silenthours.data.db

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromStringList(contactIds: List<String>?): String? {
        if (contactIds == null) {
            return null
        }
        return JSONArray(contactIds).toString()
    }

    @TypeConverter
    fun toStringList(data: String?): List<String>? {
        if (data == null) {
            return null
        }
        val jsonArray = JSONArray(data)
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }
}
