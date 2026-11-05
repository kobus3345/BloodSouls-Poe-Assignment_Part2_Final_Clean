package com.darkforge.bloodsouls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.BloodSouls.Souls.BloodSoulsGame
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.darkforge.bloodsouls.util.SettingsManager

class GameFragment : AndroidFragmentApplication() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = false
            useGL30 = false
            useCompass = false
            useAccelerometer = false
            r = 8
            g = 8
            b = 8
            a = 8
            depth = 16
            stencil = 0
            numSamples = 0
        }
        val difficulty = try {
            SettingsManager(requireContext()).difficulty
        } catch (_: Exception) {
            "CURSED"
        }
        return initializeForView(BloodSoulsGame(difficulty), config)
    }
}
