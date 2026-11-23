package com.pulseplayer.music

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import com.pulseplayer.music.ui.PulsePlayerApp
import com.pulseplayer.music.ui.theme.PulsePlayerTheme
import com.pulseplayer.music.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val musicViewModel: MusicViewModel by viewModels {
        MusicViewModel.provideFactory(application as PulsePlayerApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDark = isSystemInDarkTheme()

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            PulsePlayerTheme(darkTheme = isDark) {
                PulsePlayerApp(viewModel = musicViewModel)
            }
        }
    }
}
