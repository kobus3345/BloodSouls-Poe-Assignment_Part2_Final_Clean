package com.BloodSouls.Souls

import com.badlogic.gdx.Game

class BloodSoulsGame(val difficulty: String = "CURSED") : Game() {
    override fun create() {
        setScreen(GameplayScreen(this, difficulty))
    }
}
