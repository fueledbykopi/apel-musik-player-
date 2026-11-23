package com.pulseplayer.music

import android.app.Application
import com.pulseplayer.music.data.repository.MediaStoreRepository
import com.pulseplayer.music.data.repository.UserPreferencesRepository
import com.pulseplayer.music.player.AudioEffectsManager
import com.pulseplayer.music.player.MusicControllerManager

class PulsePlayerApplication : Application() {

    lateinit var mediaStoreRepository: MediaStoreRepository
        private set

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    lateinit var audioEffectsManager: AudioEffectsManager
        private set

    lateinit var musicControllerManager: MusicControllerManager
        private set

    override fun onCreate() {
        super.onCreate()
        mediaStoreRepository = MediaStoreRepository(this)
        userPreferencesRepository = UserPreferencesRepository(this)
        audioEffectsManager = AudioEffectsManager()
        musicControllerManager = MusicControllerManager(this)
    }
}
