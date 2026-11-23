package com.pulseplayer.music.data.model

enum class SortOrder(val label: String) {
    TITLE_ASC("Judul (A-Z)"),
    ARTIST_ASC("Artis (A-Z)"),
    DATE_ADDED_DESC("Terbaru Ditambahkan"),
    DURATION_DESC("Durasi Terpanjang")
}
