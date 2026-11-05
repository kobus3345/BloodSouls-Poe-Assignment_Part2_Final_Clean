package com.BloodSouls.Souls

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Disposable

class Buttons : Disposable {

    private val size = 105f
    private val spacing = 15f

    private val texturesToDispose = mutableListOf<Texture>()
    private val fontsToDispose = mutableListOf<BitmapFont>()

    val buttonY: TextButton
    val buttonX: TextButton
    val buttonB: TextButton
    val buttonA: TextButton

    var onAttack: (() -> Unit)? = null
    var onBlock: (() -> Unit)? = null
    var onJump: (() -> Unit)? = null
    var onLookAround: (() -> Unit)? = null

    init {
        val styleY = createCircularButtonStyle(
            ringColor = Color(0.9f, 0.2f, 0.2f, 1f),   // Crimson Red for Attack
            textColor = Color(1f, 0.85f, 0.85f, 1f)
        )
        val styleB = createCircularButtonStyle(
            ringColor = Color(0.2f, 0.6f, 0.95f, 1f),  // Azure Blue for Block
            textColor = Color(0.85f, 0.92f, 1f, 1f)
        )
        val styleA = createCircularButtonStyle(
            ringColor = Color(0.2f, 0.85f, 0.3f, 1f),  // Emerald Green for Jump
            textColor = Color(0.85f, 1f, 0.88f, 1f)
        )
        val styleX = createCircularButtonStyle(
            ringColor = Color(0.95f, 0.75f, 0.15f, 1f), // Golden Amber for Look
            textColor = Color(1f, 0.95f, 0.8f, 1f)
        )

        buttonY = TextButton("ATTACK", styleY)
        buttonB = TextButton("BLOCK", styleB)
        buttonA = TextButton("JUMP", styleA)
        buttonX = TextButton("LOOK", styleX)
    }

    private fun createCircularButtonStyle(
        ringColor: Color,
        textColor: Color
    ): TextButton.TextButtonStyle {
        val sizeInt = size.toInt()
        val pmUp = Pixmap(sizeInt, sizeInt, Pixmap.Format.RGBA8888).apply {
            setColor(0.12f, 0.12f, 0.16f, 0.8f)
            fillCircle(sizeInt / 2, sizeInt / 2, sizeInt / 2 - 2)

            setColor(0.05f, 0.05f, 0.08f, 0.9f)
            fillCircle(sizeInt / 2, sizeInt / 2, sizeInt / 2 - 8)

            setColor(ringColor)
            drawCircle(sizeInt / 2, sizeInt / 2, sizeInt / 2 - 2)
            drawCircle(sizeInt / 2, sizeInt / 2, sizeInt / 2 - 3)

            setColor(0.85f, 0.7f, 0.25f, 0.9f)
            drawCircle(sizeInt / 2, sizeInt / 2, sizeInt / 2 - 1)
        }
        val texUp = Texture(pmUp)
        pmUp.dispose()
        texturesToDispose.add(texUp)

        val pmDown = Pixmap(sizeInt, sizeInt, Pixmap.Format.RGBA8888).apply {
            setColor(ringColor.r * 0.5f, ringColor.g * 0.5f, ringColor.b * 0.5f, 0.9f)
            fillCircle(sizeInt / 2, sizeInt / 2, sizeInt / 2 - 2)

            setColor(Color.WHITE)
            drawCircle(sizeInt / 2, sizeInt / 2, sizeInt / 2 - 2)
        }
        val texDown = Texture(pmDown)
        pmDown.dispose()
        texturesToDispose.add(texDown)

        val font = BitmapFont()
        fontsToDispose.add(font)

        val style = TextButton.TextButtonStyle()
        style.font = font
        style.fontColor = textColor
        style.up = TextureRegionDrawable(TextureRegion(texUp))
        style.down = TextureRegionDrawable(TextureRegion(texDown))
        return style
    }

    fun addToStage(stage: Stage) {
        buttonY.setSize(size, size)
        buttonX.setSize(size, size)
        buttonB.setSize(size, size)
        buttonA.setSize(size, size)

        updatePositions(60f, 50f)

        stage.addActor(buttonY)
        stage.addActor(buttonX)
        stage.addActor(buttonB)
        stage.addActor(buttonA)

        buttonY.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                onAttack?.invoke()
            }
        })
        buttonB.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                onBlock?.invoke()
            }
        })
        buttonA.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                onJump?.invoke()
            }
        })
        buttonX.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                onLookAround?.invoke()
            }
        })
    }

    fun updatePositions(marginX: Float = 60f, marginY: Float = 50f) {
        buttonA.setPosition(marginX + size, marginY)
        buttonY.setPosition(marginX + size, marginY + (size + spacing) * 2)
        buttonX.setPosition(marginX, marginY + size + spacing)
        buttonB.setPosition(marginX + (size + spacing) * 2, marginY + size + spacing)
    }

    override fun dispose() {
        for (tex in texturesToDispose) {
            tex.dispose()
        }
        texturesToDispose.clear()

        for (font in fontsToDispose) {
            font.dispose()
        }
        fontsToDispose.clear()
    }
}
