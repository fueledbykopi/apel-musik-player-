package com.pulseplayer.music.data.util

import java.util.Locale

object TimeFormatter {

    fun formatDuration(durationMs: Long): String {
        val safeMs = durationMs.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L

        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun formatTotalDurationHuman(durationMs: Long): String {
        val safeMs = durationMs.coerceAtLeast(0L)
        val totalMinutes = safeMs / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> "$hours jam $minutes mnt"
            hours > 0L -> "$hours jam"
            totalMinutes > 0L -> "$totalMinutes mnt"
            else -> "< 1 mnt"
        }
    }
}
