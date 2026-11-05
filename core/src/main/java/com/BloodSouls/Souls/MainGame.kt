package com.BloodSouls.Souls

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport

class MainGame : ApplicationAdapter() {
    private lateinit var batch: SpriteBatch
    private lateinit var stage: Stage
    private lateinit var joystick: Joystick

    private lateinit var knightAnims: KnightAnimations
    private var currentAnim: Animation<TextureRegion>? = null
    private var stateTimer = 0f

    private var currentIdle = KnightIdle.NORMAL
    private var currentDirection = Direction.S
    private lateinit var playerPosition: Vector2

    override fun create() {
        batch = SpriteBatch()
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        // Joystick setup
        val joystickX = Gdx.graphics.width - 200 - 60f
        joystick = Joystick(stage, joystickX, 50f, 200f)

        // Load knight animations from assets root
        knightAnims = KnightAnimations()
        knightAnims.loadIdleAnimations("")
        knightAnims.loadRunAnimations("")   // <-- load running animations too

        // Start with idle animation
        setIdleAnimation(currentIdle, currentDirection)

        // Center knight on screen
        val startX = Gdx.graphics.width / 2f
        val startY = Gdx.graphics.height / 2f
        playerPosition = Vector2(startX, startY)
    }

    private fun setIdleAnimation(idle: KnightIdle, direction: Direction) {
        currentAnim = knightAnims.getIdleAnimation(idle, direction)
        stateTimer = 0f
    }

    private fun setRunAnimation(direction: Direction) {
        currentAnim = knightAnims.getRunAnimation(direction)
        stateTimer = 0f
    }

    override fun render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stateTimer += Gdx.graphics.deltaTime

        // --- Joystick input handling ---
        val joyVec = joystick.getDirectionVector()
        if (joyVec.len() > 0.1f) { // deadzone
            currentDirection = when {
                joyVec.x > 0.5f && joyVec.y > 0.5f -> Direction.NE
                joyVec.x > 0.5f && joyVec.y < -0.5f -> Direction.SE
                joyVec.x < -0.5f && joyVec.y > 0.5f -> Direction.NW
                joyVec.x < -0.5f && joyVec.y < -0.5f -> Direction.SW
                joyVec.x > 0.5f -> Direction.E
                joyVec.x < -0.5f -> Direction.W
                joyVec.y > 0.5f -> Direction.N
                else -> Direction.S
            }

            // Moving → run animation
            setRunAnimation(currentDirection)

            // Move knight
            playerPosition.add(joyVec.scl(1000f * Gdx.graphics.deltaTime))
        } else {
            // Not moving → idle animation
            setIdleAnimation(currentIdle, currentDirection)
        }

        batch.begin()
        currentAnim?.let {
            val frame = it.getKeyFrame(stateTimer)
            batch.draw(frame, playerPosition.x, playerPosition.y)
        }
        batch.end()

        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height)
    }

    override fun dispose() {
        batch.dispose()
        stage.dispose()
    }
}
