package com.accu.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DbConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter fun listToString(list: List<String>): String = json.encodeToString(list)
    @TypeConverter fun stringToList(s: String): List<String> = runCatching { json.decodeFromString<List<String>>(s) }.getOrDefault(emptyList())

    @TypeConverter fun mapToString(map: Map<String, String>): String = json.encodeToString(map)
    @TypeConverter fun stringToMap(s: String): Map<String, String> = runCatching { json.decodeFromString<Map<String, String>>(s) }.getOrDefault(emptyMap())
}
