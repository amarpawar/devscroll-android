package dev.devscroll.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()
    private val type = object : TypeToken<List<String>>() {}.type

    @TypeConverter
    fun fromStringList(value: String): List<String> =
        runCatching { gson.fromJson<List<String>?>(value, type) }.getOrNull() ?: emptyList()

    @TypeConverter
    fun toStringList(list: List<String>): String = gson.toJson(list)
}
