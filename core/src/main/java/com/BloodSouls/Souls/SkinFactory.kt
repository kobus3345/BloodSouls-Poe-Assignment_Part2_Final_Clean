package com.BloodSouls.Souls

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton

object SkinFactory {
    val skin: Skin by lazy {
        val s = Skin()
        val font = BitmapFont()
        s.add("default", font)

        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color(0.2f, 0.2f, 0.2f, 0.7f))
        pixmap.fill()
        val texture = Texture(pixmap)
        pixmap.dispose()
        s.add("button_bg", texture)

        val pixmapDown = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmapDown.setColor(Color(0.5f, 0.1f, 0.1f, 0.8f))
        pixmapDown.fill()
        val textureDown = Texture(pixmapDown)
        pixmapDown.dispose()
        s.add("button_bg_down", textureDown)

        val btnStyle = TextButton.TextButtonStyle()
        btnStyle.font = font
        btnStyle.fontColor = Color.WHITE
        btnStyle.up = s.newDrawable("button_bg")
        btnStyle.down = s.newDrawable("button_bg_down")
        s.add("default", btnStyle)

        val labelStyle = Label.LabelStyle()
        labelStyle.font = font
        labelStyle.fontColor = Color.GREEN
        s.add("default", labelStyle)

        s
    }
}
