package com.pulseplayer.music.data.util

import com.pulseplayer.music.data.model.Playlist
import org.json.JSONArray
import org.json.JSONObject

object PlaylistJsonSerializer {

    fun serialize(playlists: List<Playlist>): String {
        val rootArray = JSONArray()
        playlists.forEach { playlist ->
            val obj = JSONObject().apply {
                put("id", playlist.id)
                put("name", playlist.name)
                put("createdAtMs", playlist.createdAtMs)
                val idsArray = JSONArray()
                playlist.songIds.forEach { idsArray.put(it) }
                put("songIds", idsArray)
            }
            rootArray.put(obj)
        }
        return rootArray.toString()
    }

    fun deserialize(rawJson: String?): List<Playlist> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return runCatching {
            val rootArray = JSONArray(rawJson)
            buildList(rootArray.length()) {
                for (i in 0 until rootArray.length()) {
                    val obj = rootArray.getJSONObject(i)
                    val idsJson = obj.optJSONArray("songIds")
                    val songIds = buildList {
                        if (idsJson != null) {
                            for (j in 0 until idsJson.length()) {
                                add(idsJson.getLong(j))
                            }
                        }
                    }
                    add(
                        Playlist(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            songIds = songIds,
                            createdAtMs = obj.optLong("createdAtMs", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
