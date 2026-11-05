package com.BloodSouls.Souls

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.utils.viewport.ScreenViewport

class MainMenuScreen(private val game: BloodSoulsGame) : Screen {
    private val stage = Stage(ScreenViewport())
    private val rootTable = Table()

    init {
        Gdx.input.inputProcessor = stage
        rootTable.setFillParent(true)
        stage.addActor(rootTable)
        showMainMenu()
    }

    private fun showMainMenu() {
        rootTable.clearChildren()

        val title = Label("BLOOD SOULS", SkinFactory.skin)
        title.setFontScale(2.5f)

        val btnNewGame = TextButton("NEW GAME", SkinFactory.skin)
        val btnContinue = TextButton("CONTINUE", SkinFactory.skin)
        val btnOptions = TextButton("OPTIONS", SkinFactory.skin)
        val btnAbout = TextButton("ABOUT", SkinFactory.skin)
        val btnExit = TextButton("EXIT", SkinFactory.skin)

        btnNewGame.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showNewGameScreen()
            }
        })
        btnContinue.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showContinueScreen()
            }
        })
        btnOptions.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showOptionsScreen()
            }
        })
        btnAbout.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showAboutScreen()
            }
        })
        btnExit.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                Gdx.app.exit()
            }
        })

        rootTable.add(title).padBottom(30f).row()
        rootTable.add(btnNewGame).width(220f).height(50f).padBottom(10f).row()
        rootTable.add(btnContinue).width(220f).height(50f).padBottom(10f).row()
        rootTable.add(btnOptions).width(220f).height(50f).padBottom(10f).row()
        rootTable.add(btnAbout).width(220f).height(50f).padBottom(10f).row()
        rootTable.add(btnExit).width(220f).height(50f).row()
    }

    private fun showNewGameScreen() {
        rootTable.clearChildren()

        val title = Label("SELECT DIFFICULTY", SkinFactory.skin)
        title.setFontScale(1.8f)

        val btnPenitent = TextButton("PENITENT", SkinFactory.skin)
        val btnCursed = TextButton("CURSED", SkinFactory.skin)
        val btnDamned = TextButton("DAMNED", SkinFactory.skin)
        val btnBegin = TextButton("BEGIN JOURNEY", SkinFactory.skin)
        val btnBack = TextButton("[ BACK ]", SkinFactory.skin)

        var selectedDiff = "CURSED"

        btnPenitent.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                selectedDiff = "PENITENT"
            }
        })
        btnCursed.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                selectedDiff = "CURSED"
            }
        })
        btnDamned.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                selectedDiff = "DAMNED"
            }
        })

        val diffTable = Table()
        diffTable.add(btnPenitent).width(120f).height(45f).padRight(10f)
        diffTable.add(btnCursed).width(120f).height(45f).padRight(10f)
        diffTable.add(btnDamned).width(120f).height(45f)

        btnBegin.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = GameplayScreen(game, selectedDiff)
            }
        })

        btnBack.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showMainMenu()
            }
        })

        rootTable.add(title).padBottom(20f).row()
        rootTable.add(diffTable).padBottom(25f).row()
        rootTable.add(btnBegin).width(220f).height(50f).padBottom(15f).row()
        rootTable.add(btnBack).padTop(10f).row()
    }

    private fun showContinueScreen() {
        rootTable.clearChildren()

        val title = Label("CHOOSE SAVE SLOT", SkinFactory.skin)
        title.setFontScale(1.8f)

        val slot1 = TextButton("SLOT I - SEWER DEPTHS", SkinFactory.skin)
        val slot2 = TextButton("SLOT II - EMPTY", SkinFactory.skin)
        val btnBack = TextButton("[ BACK ]", SkinFactory.skin)

        slot1.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = GameplayScreen(game, "CURSED")
            }
        })

        btnBack.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showMainMenu()
            }
        })

        rootTable.add(title).padBottom(20f).row()
        rootTable.add(slot1).width(280f).height(50f).padBottom(10f).row()
        rootTable.add(slot2).width(280f).height(50f).padBottom(20f).row()
        rootTable.add(btnBack).row()
    }

    private fun showOptionsScreen() {
        rootTable.clearChildren()

        val title = Label("OPTIONS", SkinFactory.skin)
        title.setFontScale(1.8f)

        val bloodLabel = Label("GORE / BLOOD INTENSITY", SkinFactory.skin)
        val bloodTable = Table()
        bloodTable.add(TextButton("LOW", SkinFactory.skin)).width(80f).height(40f).padRight(5f)
        bloodTable.add(TextButton("MED", SkinFactory.skin)).width(80f).height(40f).padRight(5f)
        bloodTable.add(TextButton("HIGH", SkinFactory.skin)).width(80f).height(40f)

        val langLabel = Label("LANGUAGE", SkinFactory.skin)
        val langTable = Table()
        langTable.add(TextButton("ENGLISH", SkinFactory.skin)).width(90f).height(40f).padRight(5f)
        langTable.add(TextButton("AFRIKAANS", SkinFactory.skin)).width(90f).height(40f).padRight(5f)
        langTable.add(TextButton("XHOSA", SkinFactory.skin)).width(90f).height(40f).padRight(5f)
        langTable.add(TextButton("TSWANA", SkinFactory.skin)).width(90f).height(40f)

        val btnBack = TextButton("[ BACK ]", SkinFactory.skin)
        btnBack.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showMainMenu()
            }
        })

        rootTable.add(title).padBottom(15f).row()
        rootTable.add(bloodLabel).padBottom(5f).row()
        rootTable.add(bloodTable).padBottom(15f).row()
        rootTable.add(langLabel).padBottom(5f).row()
        rootTable.add(langTable).padBottom(20f).row()
        rootTable.add(btnBack).row()
    }

    private fun showAboutScreen() {
        rootTable.clearChildren()

        val title = Label("ABOUT BLOOD SOULS", SkinFactory.skin)
        title.setFontScale(1.8f)

        val info = Label("Created by DarkForge Studio.\n\nA dark souls action adventure game built with LibGDX and Kotlin.\n\nVersion 1.0.0", SkinFactory.skin)

        val btnBack = TextButton("[ BACK ]", SkinFactory.skin)
        btnBack.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showMainMenu()
            }
        })

        rootTable.add(title).padBottom(15f).row()
        rootTable.add(info).padBottom(20f).row()
        rootTable.add(btnBack).row()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.06f, 0.02f, 0.02f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun show() {}
    override fun hide() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() {
        stage.dispose()
    }
}
