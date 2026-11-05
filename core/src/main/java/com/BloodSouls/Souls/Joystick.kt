package com.BloodSouls.Souls

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Disposable

class Joystick(
    stage: Stage,
    x: Float,
    y: Float,
    size: Float = 200f
) : Disposable {

    private val touchpad: Touchpad
    private val bgTexture: Texture
    private val knobTexture: Texture

    val inputX: Float get() = touchpad.knobPercentX
    val inputY: Float get() = touchpad.knobPercentY
    val isTouched: Boolean get() = touchpad.isTouched

    init {
        val sizeInt = size.toInt()
        val bgPixmap = Pixmap(sizeInt, sizeInt, Pixmap.Format.RGBA8888).apply {
            // Translucent dark compass base
            setColor(0.08f, 0.08f, 0.12f, 0.65f)
            fillCircle(sizeInt / 2, sizeInt / 2, sizeInt / 2 - 2)

            // Inner dark bevel
            setColor(0.04f, 0.04f, 0.06f, 0.85f)
            fillCircle(sizeInt / 2, sizeInt / 2, sizeInt / 2 - 12)

            // Directional crosshair ticks
            setColor(0.75f, 0.65f, 0.3f, 0.4f)
            drawLine(sizeInt / 2, 10, sizeInt / 2, sizeInt - 10)
            drawLine(10, sizeInt / 2, sizeInt - 10, sizeInt / 2)

            // Metallic gold outer ring
            setColor(0.85f, 0.7f, 0.25f, 0.85f)
            drawCircle(sizeInt / 2, sizeInt / 2, sizeInt / 2 - 2)
            drawCircle(sizeInt / 2, sizeInt / 2, sizeInt / 2 - 3)
        }
        bgTexture = Texture(bgPixmap)
        bgPixmap.dispose()

        val knobSize = (size * 0.35f).toInt()
        val knobPixmap = Pixmap(knobSize, knobSize, Pixmap.Format.RGBA8888).apply {
            // Metallic dark bronze knob
            setColor(0.22f, 0.2f, 0.18f, 0.95f)
            fillCircle(knobSize / 2, knobSize / 2, knobSize / 2 - 1)

            // Glowing central gold gem
            setColor(0.95f, 0.75f, 0.2f, 1f)
            fillCircle(knobSize / 2, knobSize / 2, knobSize / 4)

            // Silver highlight ring
            setColor(Color.LIGHT_GRAY)
            drawCircle(knobSize / 2, knobSize / 2, knobSize / 2 - 1)
        }
        knobTexture = Texture(knobPixmap)
        knobPixmap.dispose()

        val style = Touchpad.TouchpadStyle().apply {
            background = TextureRegionDrawable(TextureRegion(bgTexture))
            knob = TextureRegionDrawable(TextureRegion(knobTexture))
        }

        touchpad = Touchpad(10f, style).apply {
            setBounds(x, y, size, size)
        }
        stage.addActor(touchpad)
    }

    fun getDirectionVector(): Vector2 {
        return Vector2(inputX, inputY)
    }

    fun setPosition(x: Float, y: Float) {
        touchpad.setPosition(x, y)
    }

    override fun dispose() {
        bgTexture.dispose()
        knobTexture.dispose()
    }
}
