package com.BloodSouls.Souls

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer

class MapScreen : Screen {
    private val camera = OrthographicCamera()
    private val map: TiledMap = TmxMapLoader().load("sewer_map.tmx")
    private val mapRenderer = OrthogonalTiledMapRenderer(map, 6.5f)

    init{
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f,0f,1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        mapRenderer.setView(camera)
        mapRenderer.render()
    }

    override fun resize(width: Int, height: Int) {}
    override fun show() {}
    override fun hide() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() {}
}
