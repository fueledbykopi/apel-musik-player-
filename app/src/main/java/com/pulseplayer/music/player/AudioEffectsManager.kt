package com.pulseplayer.music.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.compose.runtime.Immutable
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class EqualizerPreset(
    val label: String,
    val bandLevelsMb: List<Short>,
    val suggestedBassBoost: Int? = null
) {
    NORMAL("Normal", listOf(0, 0, 0, 0, 0), 0),
    POP("Pop", listOf(-100, 250, 450, 250, -100), 150),
    ROCK("Rock", listOf(500, 200, -150, 300, 550), 300),
    JAZZ("Jazz", listOf(350, 200, -100, 200, 400), 200),
    CLASSICAL("Classical", listOf(450, 250, -50, 250, 450), 100),
    BASS_BOOST("Bass Boost", listOf(750, 450, 100, 0, 0), 700),
    CUSTOM("Custom", listOf(0, 0, 0, 0, 0), null)
}

@Immutable
data class EqualizerBand(
    val index: Short,
    val centerFreqHz: Int,
    val levelMillibels: Short
) {
    val label: String
        get() = if (centerFreqHz >= 1000) {
            val khz = centerFreqHz / 1000f
            if (khz == khz.toInt().toFloat()) {
                "${khz.toInt()} kHz"
            } else {
                String.format(Locale.US, "%.1f kHz", khz)
            }
        } else {
            "$centerFreqHz Hz"
        }
}

@Immutable
data class EqualizerState(
    val isHardwareAttached: Boolean = false,
    val isEnabled: Boolean = false,
    val minLevelMb: Short = -1500,
    val maxLevelMb: Short = 1500,
    val bands: List<EqualizerBand> = defaultFiveBands(),
    val selectedPreset: EqualizerPreset = EqualizerPreset.NORMAL,
    val bassBoostSupported: Boolean = true,
    val bassBoostStrength: Int = 0 // 0..1000
) {
    companion object {
        fun defaultFiveBands(): List<EqualizerBand> = listOf(
            EqualizerBand(0, 60, 0),
            EqualizerBand(1, 230, 0),
            EqualizerBand(2, 910, 0),
            EqualizerBand(3, 3600, 0),
            EqualizerBand(4, 14000, 0)
        )
    }
}

class AudioEffectsManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var attachedSessionId: Int = 0

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    @Synchronized
    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == attachedSessionId) return
        release()
        attachedSessionId = audioSessionId

        val previous = _state.value
        var eqAttached = false
        var minMb: Short = previous.minLevelMb
        var maxMb: Short = previous.maxLevelMb
        var bandsList: List<EqualizerBand> = previous.bands

        try {
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = previous.isEnabled
            }
            equalizer = eq
            eqAttached = true
            val range = eq.bandLevelRange
            minMb = range[0]
            maxMb = range[1]

            val hwBandCount = eq.numberOfBands.toInt().coerceIn(1, 5)
            bandsList = (0 until hwBandCount).map { i ->
                val bandIdx = i.toShort()
                val desiredLevel = previous.bands.getOrNull(i)?.levelMillibels
                    ?.coerceIn(minMb, maxMb) ?: 0
                runCatching { eq.setBandLevel(bandIdx, desiredLevel) }
                EqualizerBand(
                    index = bandIdx,
                    centerFreqHz = eq.getCenterFreq(bandIdx) / 1000,
                    levelMillibels = runCatching { eq.getBandLevel(bandIdx) }.getOrDefault(desiredLevel)
                )
            }
        } catch (_: Throwable) {
            equalizer = null
            eqAttached = false
        }

        var bassSupported = false
        try {
            val bb = BassBoost(0, audioSessionId).apply {
                enabled = previous.isEnabled && previous.bassBoostStrength > 0
                if (strengthSupported) {
                    setStrength(previous.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
            }
            bassBoost = bb
            bassSupported = bb.strengthSupported
        } catch (_: Throwable) {
            bassBoost = null
            bassSupported = false
        }

        _state.value = previous.copy(
            isHardwareAttached = eqAttached,
            minLevelMb = minMb,
            maxLevelMb = maxMb,
            bands = bandsList,
            bassBoostSupported = if (eqAttached) bassSupported else true
        )
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        runCatching {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled && _state.value.bassBoostStrength > 0
        }
        _state.update { it.copy(isEnabled = enabled) }
    }

    @Synchronized
    fun setBandLevel(bandIndex: Short, levelMillibels: Short) {
        val current = _state.value
        val clamped = levelMillibels.coerceIn(current.minLevelMb, current.maxLevelMb)
        runCatching {
            equalizer?.let { eq ->
                if (!eq.enabled) eq.enabled = true
                if (bandIndex < eq.numberOfBands) {
                    eq.setBandLevel(bandIndex, clamped)
                }
            }
        }
        _state.update { state ->
            state.copy(
                isEnabled = true,
                selectedPreset = EqualizerPreset.CUSTOM,
                bands = state.bands.map { band ->
                    if (band.index == bandIndex) band.copy(levelMillibels = clamped) else band
                }
            )
        }
    }

    @Synchronized
    fun selectPreset(preset: EqualizerPreset) {
        if (preset == EqualizerPreset.CUSTOM) {
            _state.update { it.copy(selectedPreset = EqualizerPreset.CUSTOM) }
            return
        }

        val current = _state.value
        val updatedBands = current.bands.mapIndexed { i, band ->
            val targetLevel = preset.bandLevelsMb
                .getOrElse(i) { preset.bandLevelsMb.lastOrNull() ?: 0 }
                .coerceIn(current.minLevelMb, current.maxLevelMb)
            runCatching {
                equalizer?.let { eq ->
                    if (!eq.enabled) eq.enabled = true
                    if (band.index < eq.numberOfBands) {
                        eq.setBandLevel(band.index, targetLevel)
                    }
                }
            }
            band.copy(levelMillibels = targetLevel)
        }

        val nextBass = preset.suggestedBassBoost ?: current.bassBoostStrength
        runCatching {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    bb.setStrength(nextBass.coerceIn(0, 1000).toShort())
                }
                bb.enabled = nextBass > 0
            }
        }

        _state.update { state ->
            state.copy(
                isEnabled = true,
                selectedPreset = preset,
                bands = updatedBands,
                bassBoostStrength = nextBass
            )
        }
    }

    @Synchronized
    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            bassBoost?.let { bb ->
                bb.enabled = clamped > 0
                if (bb.strengthSupported) {
                    bb.setStrength(clamped.toShort())
                }
            }
            equalizer?.let { eq ->
                if (!eq.enabled) eq.enabled = true
            }
        }
        _state.update {
            it.copy(
                isEnabled = true,
                bassBoostStrength = clamped
            )
        }
    }

    @Synchronized
    fun resetBands() {
        selectPreset(EqualizerPreset.NORMAL)
    }

    @Synchronized
    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
        attachedSessionId = 0
    }
}
