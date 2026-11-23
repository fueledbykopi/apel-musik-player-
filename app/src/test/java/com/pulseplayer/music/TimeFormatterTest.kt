package com.pulseplayer.music

import com.pulseplayer.music.data.util.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun formatDuration_formatsMinutesAndSeconds() {
        assertEquals("0:00", TimeFormatter.formatDuration(0L))
        assertEquals("0:00", TimeFormatter.formatDuration(-500L))
        assertEquals("0:09", TimeFormatter.formatDuration(9_000L))
        assertEquals("3:45", TimeFormatter.formatDuration(225_000L))
    }

    @Test
    fun formatDuration_formatsHoursWhenOverAnHour() {
        assertEquals("1:00:00", TimeFormatter.formatDuration(3_600_000L))
        assertEquals("2:05:09", TimeFormatter.formatDuration(7_509_000L))
    }

    @Test
    fun formatTotalDurationHuman_formatsReadableIndonesianLabels() {
        assertEquals("< 1 mnt", TimeFormatter.formatTotalDurationHuman(25_000L))
        assertEquals("12 mnt", TimeFormatter.formatTotalDurationHuman(12 * 60_000L))
        assertEquals("1 jam", TimeFormatter.formatTotalDurationHuman(60 * 60_000L))
        assertEquals("1 jam 30 mnt", TimeFormatter.formatTotalDurationHuman(90 * 60_000L))
    }
}
