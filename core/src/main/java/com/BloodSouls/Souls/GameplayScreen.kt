package com.BloodSouls.Souls

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import kotlin.math.sin

enum class ActionState { NONE, ATTACK, BLOCK_START, BLOCK_IDLE, JUMP, LOOK_AROUND }
enum class GameState { RUNNING, PAUSED, GAME_OVER, VICTORY }

class FloatingText(
    val text: String,
    val position: Vector2,
    val color: Color,
    var lifetime: Float = 1.0f
)

class Chest(
    val position: Vector2,
    var isOpened: Boolean = false,
    val hasKey: Boolean = false
)

class GameplayScreen(
    private val game: BloodSoulsGame,
    val difficulty: String = "CURSED"
) : Screen {

    companion object {
        private const val TAG = "GameplayScreen"
    }

    val damageMultiplier: Float = when (difficulty.uppercase()) {
        "PENITENT" -> 0.7f
        "DAMNED" -> 2.2f
        else -> 1.35f
    }

    private val camera = OrthographicCamera()
    private var map: TiledMap? = null
    private var mapRenderer: OrthogonalTiledMapRenderer? = null

    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()
    private val stage = Stage(ScreenViewport())
    private val joystick: Joystick
    private val buttons = Buttons()

    private val knightAnims = KnightAnimations()
    private var currentAnim: Animation<TextureRegion>? = null
    private var stateTimer = 0f

    private val bonfirePos = Vector2(800f, 800f)
    private var playerPosition = Vector2(768f, 768f)

    private var playerHp = 100f
    private var maxHp = 100f
    private var playerStamina = 100f
    private var maxStamina = 100f
    private var playerAttackDamage = 35f

    private var vitalityLevel = 1
    private var enduranceLevel = 1
    private var strengthLevel = 1
    private val levelUpCost = 100

    private var soulsCollected = 0
    private var currentGameState = GameState.RUNNING

    private val hudFont: BitmapFont by lazy {
        BitmapFont().apply { data.setScale(1.0f) }
    }
    private val notifyFont: BitmapFont by lazy {
        BitmapFont().apply { data.setScale(1.25f) }
    }
    private val dialogFont: BitmapFont by lazy {
        BitmapFont().apply { data.setScale(1.2f) }
    }
    private val dialogTitleFont: BitmapFont by lazy {
        BitmapFont().apply { data.setScale(1.8f) }
    }

    private val dialogButtonStyle: TextButton.TextButtonStyle by lazy {
        val pmUp = Pixmap(220, 50, Pixmap.Format.RGBA8888).apply {
            setColor(0.12f, 0.12f, 0.16f, 0.9f)
            fillRectangle(0, 0, 220, 50)
            setColor(0.85f, 0.7f, 0.25f, 0.9f)
            drawRectangle(0, 0, 220, 50)
            drawRectangle(1, 1, 218, 48)
        }
        val texUp = Texture(pmUp)
        pmUp.dispose()

        TextButton.TextButtonStyle().apply {
            font = dialogFont
            fontColor = Color(0.95f, 0.85f, 0.3f, 1f)
            up = TextureRegionDrawable(TextureRegion(texUp))
        }
    }

    private var hpLabel: Label? = null
    private var staminaLabel: Label? = null
    private var soulsLabel: Label? = null
    private var difficultyLabel: Label? = null
    private var notificationLabel: Label? = null
    private var notificationTimer = 0f
    private var bonfireCooldownTimer = 0f
    private var hasLeftBonfire = true
    private var bossSpawned = false
    private var bossTransformed = false
    private var isBossIntroActive = false
    private var bossIntroTimer = 0f
    private val bossIntroDuration = 3.8f
    private var bossHpFillAnim = 0f
    private val glyphLayout = GlyphLayout()
    private var btnPause: TextButton? = null

    private var facingDirection = Direction.S
    private var actionState = ActionState.NONE

    private val enemies = mutableListOf<Enemy>()
    private val chests = mutableListOf<Chest>()
    private val floatingTexts = mutableListOf<FloatingText>()

    private var hasSewerKey = false
    private val exitPos = Vector2(3400f, 3400f)

    private var pauseTable: Table? = null
    private var gameOverTable: Table? = null
    private var victoryTable: Table? = null
    private var bonfireTable: Table? = null
    private var tutorialTable: Table? = null

    private var slashTimer = 0f
    private var slashPos = Vector2()
    private var slashAngle = 0f

    private var shakeTimer = 0f
    private var shakeIntensity = 0f

    private val fallbackTexture: Texture by lazy {
        val pm = Pixmap(64, 64, Pixmap.Format.RGBA8888)
        pm.setColor(Color.FIREBRICK)
        pm.fillCircle(32, 32, 28)
        pm.setColor(Color.GOLD)
        pm.drawCircle(32, 32, 28)
        val tex = Texture(pm)
        pm.dispose()
        tex
    }

    private val bonfireTexture: Texture by lazy {
        val pm = Pixmap(128, 128, Pixmap.Format.RGBA8888)
        pm.setColor(Color.DARK_GRAY)
        pm.fillCircle(64, 96, 40)
        pm.setColor(Color.GRAY)
        pm.drawCircle(64, 96, 40)
        pm.drawCircle(64, 96, 39)

        pm.setColor(Color(0.25f, 0.12f, 0.05f, 1f))
        pm.fillCircle(64, 96, 30)

        pm.setColor(Color.LIGHT_GRAY)
        pm.fillRectangle(60, 16, 8, 80)
        pm.setColor(Color.GOLD)
        pm.fillRectangle(48, 40, 32, 8)
        pm.fillCircle(64, 16, 6)

        pm.setColor(Color.FIREBRICK)
        pm.fillCircle(64, 92, 20)
        pm.setColor(Color.ORANGE)
        pm.fillCircle(64, 92, 12)
        pm.setColor(Color.YELLOW)
        pm.fillCircle(64, 92, 6)

        val tex = Texture(pm)
        pm.dispose()
        tex
    }

    private val flameTexture: Texture by lazy {
        val pm = Pixmap(128, 128, Pixmap.Format.RGBA8888)
        pm.setColor(Color.RED)
        pm.fillCircle(64, 64, 52)
        pm.setColor(Color.ORANGE)
        pm.fillCircle(64, 64, 38)
        pm.setColor(Color.YELLOW)
        pm.fillCircle(64, 64, 22)
        pm.setColor(Color.WHITE)
        pm.fillCircle(64, 64, 10)
        val tex = Texture(pm)
        pm.dispose()
        tex
    }

    private val chestTextureClosed: Texture by lazy {
        val pm = Pixmap(64, 64, Pixmap.Format.RGBA8888)
        pm.setColor(Color(0.35f, 0.2f, 0.1f, 1f))
        pm.fillRectangle(8, 16, 48, 36)
        pm.setColor(Color.GOLD)
        pm.drawRectangle(8, 16, 48, 36)
        pm.fillRectangle(28, 28, 8, 12)
        val tex = Texture(pm)
        pm.dispose()
        tex
    }

    private val chestTextureOpen: Texture by lazy {
        val pm = Pixmap(64, 64, Pixmap.Format.RGBA8888)
        pm.setColor(Color(0.25f, 0.15f, 0.08f, 1f))
        pm.fillRectangle(8, 24, 48, 28)
        pm.setColor(Color.GOLD)
        pm.fillCircle(32, 32, 14)
        pm.setColor(Color.YELLOW)
        pm.fillCircle(32, 32, 8)
        val tex = Texture(pm)
        pm.dispose()
        tex
    }

    private val slashTexture: Texture by lazy {
        val pm = Pixmap(64, 64, Pixmap.Format.RGBA8888)
        pm.setColor(Color.GOLD)
        pm.fillCircle(32, 32, 28)
        pm.setColor(Color.CLEAR)
        pm.fillCircle(24, 32, 26)
        pm.setColor(Color.FIREBRICK)
        pm.drawCircle(32, 32, 28)
        val tex = Texture(pm)
        pm.dispose()
        tex
    }

    init {
        Gdx.app.log(TAG, "Initializing GameplayScreen (Difficulty: $difficulty, Scale: $damageMultiplier)...")
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        Gdx.input.inputProcessor = stage

        val joystickX = Gdx.graphics.width - 250f
        joystick = Joystick(stage, joystickX, 50f, 200f)
        buttons.addToStage(stage)

        setupHUD()
        setupPauseButton()
        initEnemies()
        initChests()
        loadSavedProgress()
        showTutorialDialog()

        try {
            val loadedMap = TmxMapLoader().load("sewer_map.tmx")
            map = loadedMap
            mapRenderer = OrthogonalTiledMapRenderer(loadedMap, 6.5f)
            Gdx.app.log(TAG, "Loaded map 'sewer_map.tmx' successfully.")
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Failed to load sewer_map.tmx: ${e.message}", e)
            createFallbackMap()
        }

        try {
            knightAnims.loadIdleAnimations("")
            knightAnims.loadRunAnimations("")
            knightAnims.loadAttackAnimations("")
            knightAnims.loadBlockAnimations("")
            knightAnims.loadSkeletonAnimations()
            knightAnims.loadBossAnimations()
            currentAnim = knightAnims.getIdleAnimation(KnightIdle.NORMAL, facingDirection)
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Failed to load knight animations: ${e.message}", e)
        }

        buttons.onAttack = {
            if (currentGameState == GameState.RUNNING && !isBossIntroActive) {
                if (playerStamina >= 15f) {
                    playerStamina -= 15f
                    val anim = knightAnims.playAttack(1, facingDirection)
                    if (anim != null) {
                        currentAnim = anim
                        stateTimer = 0f
                        actionState = ActionState.ATTACK
                    }
                    SoundEffects.playAttack()
                    performPlayerAttack()
                } else {
                    showNotification("NOT ENOUGH STAMINA!")
                }
            }
        }

        buttons.onBlock = {
            if (currentGameState == GameState.RUNNING && !isBossIntroActive) {
                if (playerStamina >= 10f) {
                    val anim = knightAnims.playBlockStart(facingDirection)
                    if (anim != null) {
                        currentAnim = anim
                        stateTimer = 0f
                        actionState = ActionState.BLOCK_START
                    }
                } else {
                    showNotification("NOT ENOUGH STAMINA!")
                }
            }
        }

        buttons.onJump = {
            if (currentGameState == GameState.RUNNING && !isBossIntroActive) {
                if (playerStamina >= 15f) {
                    playerStamina -= 15f
                    val anim = knightAnims.getIdleAnimation(KnightIdle.JUMP, facingDirection)
                    if (anim != null) {
                        currentAnim = anim
                        stateTimer = 0f
                        actionState = ActionState.JUMP
                    }
                } else {
                    showNotification("NOT ENOUGH STAMINA!")
                }
            }
        }

        buttons.onLookAround = {
            if (currentGameState == GameState.RUNNING) {
                val anim = knightAnims.getIdleAnimation(KnightIdle.LOOK_AROUND, facingDirection)
                if (anim != null) {
                    currentAnim = anim
                    stateTimer = 0f
                    actionState = ActionState.LOOK_AROUND
                }
            }
        }
    }

    private fun setupHUD() {
        try {
            val startY = Gdx.graphics.height.toFloat() - 60f

            val whiteStyle = Label.LabelStyle(hudFont, Color.WHITE)
            val yellowStyle = Label.LabelStyle(hudFont, Color.YELLOW)
            val notifyStyle = Label.LabelStyle(notifyFont, Color.GOLD)

            hpLabel = Label("HP: 100 / 100", whiteStyle).apply {
                setPosition(75f, startY - 20f)
            }
            stage.addActor(hpLabel)

            staminaLabel = Label("STAMINA: 100 / 100", Label.LabelStyle(hudFont, Color(1f, 0.95f, 0.8f, 1f))).apply {
                setPosition(75f, startY - 52f)
            }
            stage.addActor(staminaLabel)

            soulsLabel = Label("SOULS: 0", yellowStyle).apply {
                setPosition(75f, startY - 78f)
            }
            stage.addActor(soulsLabel)

            difficultyLabel = Label("MODE: ${difficulty.uppercase()}", Label.LabelStyle(hudFont, if (difficulty.equals("DAMNED", ignoreCase = true)) Color.SCARLET else Color.LIGHT_GRAY)).apply {
                setPosition(75f, startY - 98f)
            }
            stage.addActor(difficultyLabel)

            notificationLabel = Label("", notifyStyle).apply {
                setPosition(Gdx.graphics.width / 2f - 160f, startY - 55f)
                isVisible = false
            }
            stage.addActor(notificationLabel)
        } catch (e: Exception) {
            Gdx.app.error(TAG, "HUD setup error: ${e.message}")
        }
    }

    private fun setupPauseButton() {
        try {
            val pmUp = Pixmap(120, 50, Pixmap.Format.RGBA8888).apply {
                setColor(0.12f, 0.12f, 0.16f, 0.85f)
                fillRectangle(0, 0, 120, 50)
                setColor(0.85f, 0.7f, 0.25f, 0.9f)
                drawRectangle(0, 0, 120, 50)
                drawRectangle(1, 1, 118, 48)
            }
            val texUp = Texture(pmUp)
            pmUp.dispose()

            val font = BitmapFont()
            val style = TextButton.TextButtonStyle().apply {
                this.font = font
                fontColor = Color(0.95f, 0.85f, 0.3f, 1f)
                up = TextureRegionDrawable(TextureRegion(texUp))
            }

            val btn = TextButton("II PAUSE", style).apply {
                setSize(120f, 50f)
                setPosition(Gdx.graphics.width - 170f, Gdx.graphics.height - 75f)
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        if (currentGameState == GameState.RUNNING) {
                            togglePauseMenu(true)
                        } else if (currentGameState == GameState.PAUSED) {
                            togglePauseMenu(false)
                        }
                    }
                })
            }
            btnPause = btn
            stage.addActor(btn)
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Pause button error: ${e.message}")
        }
    }

    private fun triggerScreenShake(duration: Float = 0.2f, intensity: Float = 8f) {
        shakeTimer = duration
        shakeIntensity = intensity
    }

    private fun showNotification(msg: String) {
        notificationLabel?.setText(msg)
        notificationLabel?.isVisible = true
        notificationTimer = 2.2f
    }

    private fun showTutorialDialog() {
        currentGameState = GameState.PAUSED
        if (tutorialTable == null) {
            tutorialTable = Table().apply {
                setFillParent(true)
                background = TextureRegionDrawable(TextureRegion(getDimTexture()))

                val titleStyle = Label.LabelStyle(dialogTitleFont, Color.GOLD)
                val bodyStyle = Label.LabelStyle(dialogFont, Color.LIGHT_GRAY)

                val title = Label("❖ BLOOD SOULS TUTORIAL ❖", titleStyle)

                val bodyText = Label(
                    "• MOVEMENT: Touch & drag Joystick (bottom right) to move.\n" +
                    "• ATTACK (Y): Swing sword at monsters in front of you.\n" +
                    "• BLOCK (B): Raise shield to absorb 100% HP damage (drains Stamina).\n" +
                    "• JUMP (A) & LOOK (X): Dodge jump and survey room.\n" +
                    "• BONFIRE: Touch Bonfire to restore HP/Stamina & Level Up stats!\n" +
                    "• CHESTS: Find Treasure Chests for bonus Souls & the SEWER MASTER KEY!\n" +
                    "• BOSS: Slay the ABYSSAL CORRUPTED KING to unlock the exit!\n" +
                    "• SOULS: Slay monsters to earn Souls. DYING LOSES ALL UNSPENT SOULS!",
                    bodyStyle
                )

                val btnBegin = TextButton("BEGIN JOURNEY", dialogButtonStyle).apply {
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            tutorialTable?.isVisible = false
                            currentGameState = GameState.RUNNING
                        }
                    })
                }

                add(title).padBottom(20f).row()
                add(bodyText).padBottom(25f).padLeft(30f).padRight(30f).row()
                add(btnBegin).width(240f).height(55f).row()
            }
            stage.addActor(tutorialTable)
        }
        tutorialTable?.isVisible = true
    }

    private fun saveBonfireProgress() {
        try {
            val prefs = Gdx.app.getPreferences("BloodSoulsSaveData")
            prefs.putFloat("hp", maxHp)
            prefs.putFloat("stamina", maxStamina)
            prefs.putFloat("damage", playerAttackDamage)
            prefs.putInteger("souls", soulsCollected)
            prefs.putInteger("vitalityLevel", vitalityLevel)
            prefs.putInteger("enduranceLevel", enduranceLevel)
            prefs.putInteger("strengthLevel", strengthLevel)
            prefs.putBoolean("hasSewerKey", hasSewerKey)
            prefs.flush()
            showNotification("❖ GAME PROGRESS SAVED AT BONFIRE! ❖")
            SoundEffects.playVictory()
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Save error: ${e.message}")
        }
    }

    private fun loadSavedProgress() {
        try {
            val prefs = Gdx.app.getPreferences("BloodSoulsSaveData")
            if (prefs.contains("hp")) {
                maxHp = prefs.getFloat("hp", 100f)
                playerHp = maxHp
                maxStamina = prefs.getFloat("stamina", 100f)
                playerStamina = maxStamina
                playerAttackDamage = prefs.getFloat("damage", 30f)
                soulsCollected = prefs.getInteger("souls", 0)
                vitalityLevel = prefs.getInteger("vitalityLevel", 1)
                enduranceLevel = prefs.getInteger("enduranceLevel", 1)
                strengthLevel = prefs.getInteger("strengthLevel", 1)
                hasSewerKey = prefs.getBoolean("hasSewerKey", false)

                hpLabel?.setText("HP: ${playerHp.toInt()} / ${maxHp.toInt()}")
                staminaLabel?.setText("STAMINA: ${playerStamina.toInt()} / ${maxStamina.toInt()}")
                soulsLabel?.setText("SOULS: $soulsCollected")
            }
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Load error: ${e.message}")
        }
    }

    private var cachedDimTexture: Texture? = null
    private var cachedModalTexture: Texture? = null
    private var cachedBadgeTexture: Texture? = null
    private val tempTextCol = Color()

    private fun getDimTexture(): Texture {
        if (cachedDimTexture == null) {
            val pm = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
                setColor(0.02f, 0.02f, 0.04f, 0.88f)
                fill()
            }
            cachedDimTexture = Texture(pm)
            pm.dispose()
        }
        return cachedDimTexture!!
    }

    private fun getModalTexture(width: Int, height: Int): Texture {
        if (cachedModalTexture == null || cachedModalTexture!!.width != width || cachedModalTexture!!.height != height) {
            cachedModalTexture?.dispose()
            val pm = Pixmap(width, height, Pixmap.Format.RGBA8888).apply {
                setColor(0.08f, 0.08f, 0.12f, 0.96f)
                fillRectangle(0, 0, width, height)
                setColor(0.85f, 0.70f, 0.25f, 0.95f)
                drawRectangle(0, 0, width, height)
                drawRectangle(2, 2, width - 4, height - 4)
                setColor(0.20f, 0.18f, 0.12f, 0.8f)
                drawRectangle(4, 4, width - 8, height - 8)
            }
            cachedModalTexture = Texture(pm)
            pm.dispose()
        }
        return cachedModalTexture!!
    }

    private fun getBadgeTexture(): Texture {
        if (cachedBadgeTexture == null) {
            val pm = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
                setColor(0.14f, 0.12f, 0.08f, 0.9f)
                fill()
            }
            cachedBadgeTexture = Texture(pm)
            pm.dispose()
        }
        return cachedBadgeTexture!!
    }

    private fun openBonfireMenu() {
        currentGameState = GameState.PAUSED
        if (bonfireTable == null) {
            bonfireTable = Table().apply {
                setFillParent(true)
            }
            stage.addActor(bonfireTable)
        }
        updateBonfireMenuContents(bonfireTable!!)
        bonfireTable?.isVisible = true
    }

    private fun updateBonfireMenuContents(table: Table) {
        table.clearChildren()

        // Dim full screen background
        table.background = TextureRegionDrawable(TextureRegion(getDimTexture()))

        // Modal Frame Card (Dark metallic box with double gold border)
        val cardWidth = (stage.viewport.worldWidth * 0.82f).coerceIn(580f, 760f).toInt()
        val cardHeight = 430

        val container = Table().apply {
            background = TextureRegionDrawable(TextureRegion(getModalTexture(cardWidth, cardHeight)))
            pad(20f, 25f, 20f, 25f)
        }

        val titleStyle = Label.LabelStyle(dialogTitleFont, Color.GOLD)
        val subtitleStyle = Label.LabelStyle(hudFont, Color(0.8f, 0.9f, 1.0f, 1f))
        val headerStyle = Label.LabelStyle(hudFont, Color.GOLD)
        val textStyle = Label.LabelStyle(dialogFont, Color.WHITE)
        val costStyle = Label.LabelStyle(dialogFont, Color.YELLOW)

        // 1. Header Section
        val title = Label("❖ BONFIRE SHRINE ❖", titleStyle)
        val subtitle = Label("[ RESTED • CHECKPOINT SAVED • HP & STAMINA RESTORED ]", subtitleStyle)

        // 2. Souls & Level Info Badge Panel
        val soulsBadge = Table().apply {
            background = TextureRegionDrawable(TextureRegion(getBadgeTexture()))
            pad(8f, 20f, 8f, 20f)

            val soulsLabel = Label("AVAILABLE SOULS: $soulsCollected", Label.LabelStyle(dialogFont, Color.GOLD))
            val costLabel = Label("LEVEL UP COST: $levelUpCost SOULS", costStyle)

            add(soulsLabel).expandX().left()
            add(costLabel).right()
        }

        // 3. Attribute Rows (Vitality, Endurance, Strength)
        val attrTable = Table().apply {
            defaults().padBottom(10f)
        }

        // VITALITY
        val vitTitle = Label("VITALITY (Lvl $vitalityLevel)", headerStyle)
        val vitDesc = Label("Max HP: ${maxHp.toInt()} ➔ ${(maxHp + 20f).toInt()}", textStyle)
        val btnVit = TextButton("+ UPGRADE (+20 HP)", dialogButtonStyle).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (soulsCollected >= levelUpCost) {
                        soulsCollected -= levelUpCost
                        vitalityLevel++
                        maxHp += 20f
                        playerHp = maxHp
                        hpLabel?.setText("HP: ${playerHp.toInt()} / ${maxHp.toInt()}")
                        soulsLabel?.setText("SOULS: $soulsCollected")
                        showNotification("❖ VITALITY INCREASED! (+20 MAX HP) ❖")
                        saveBonfireProgress()
                        updateBonfireMenuContents(table)
                    } else {
                        showNotification("NOT ENOUGH SOULS!")
                    }
                }
            })
        }
        attrTable.add(vitTitle).width(190f).left()
        attrTable.add(vitDesc).expandX().left()
        attrTable.add(btnVit).width(210f).height(42f).right().row()

        // ENDURANCE
        val endTitle = Label("ENDURANCE (Lvl $enduranceLevel)", headerStyle)
        val endDesc = Label("Max Stamina: ${maxStamina.toInt()} ➔ ${(maxStamina + 20f).toInt()}", textStyle)
        val btnEnd = TextButton("+ UPGRADE (+20 STAMINA)", dialogButtonStyle).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (soulsCollected >= levelUpCost) {
                        soulsCollected -= levelUpCost
                        enduranceLevel++
                        maxStamina += 20f
                        playerStamina = maxStamina
                        staminaLabel?.setText("STAMINA: ${playerStamina.toInt()} / ${maxStamina.toInt()}")
                        soulsLabel?.setText("SOULS: $soulsCollected")
                        showNotification("❖ ENDURANCE INCREASED! (+20 MAX STAMINA) ❖")
                        saveBonfireProgress()
                        updateBonfireMenuContents(table)
                    } else {
                        showNotification("NOT ENOUGH SOULS!")
                    }
                }
            })
        }
        attrTable.add(endTitle).width(190f).left()
        attrTable.add(endDesc).expandX().left()
        attrTable.add(btnEnd).width(210f).height(42f).right().row()

        // STRENGTH
        val strTitle = Label("STRENGTH (Lvl $strengthLevel)", headerStyle)
        val strDesc = Label("Attack Damage: ${playerAttackDamage.toInt()} ➔ ${(playerAttackDamage + 8f).toInt()}", textStyle)
        val btnStr = TextButton("+ UPGRADE (+8 DAMAGE)", dialogButtonStyle).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (soulsCollected >= levelUpCost) {
                        soulsCollected -= levelUpCost
                        strengthLevel++
                        playerAttackDamage += 8f
                        soulsLabel?.setText("SOULS: $soulsCollected")
                        showNotification("❖ STRENGTH INCREASED! (+8 DAMAGE) ❖")
                        saveBonfireProgress()
                        updateBonfireMenuContents(table)
                    } else {
                        showNotification("NOT ENOUGH SOULS!")
                    }
                }
            })
        }
        attrTable.add(strTitle).width(190f).left()
        attrTable.add(strDesc).expandX().left()
        attrTable.add(btnStr).width(210f).height(42f).right().row()

        // 4. Action Footer Buttons
        val btnSave = TextButton("SAVE PROGRESS", dialogButtonStyle).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    saveBonfireProgress()
                }
            })
        }

        val btnLeave = TextButton("LEAVE BONFIRE", dialogButtonStyle).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    bonfireTable?.isVisible = false
                    currentGameState = GameState.RUNNING
                    bonfireCooldownTimer = 4.0f
                }
            })
        }

        val actionTable = Table().apply {
            add(btnSave).width(200f).height(44f).padRight(15f)
            add(btnLeave).width(200f).height(44f)
        }

        // Assemble Modal
        container.add(title).padBottom(4f).row()
        container.add(subtitle).padBottom(12f).row()
        container.add(soulsBadge).fillX().padBottom(14f).row()
        container.add(attrTable).fillX().padBottom(12f).row()
        container.add(actionTable).padTop(4f).row()

        table.add(container)
    }

    private fun initEnemies() {
        enemies.clear()
        bossSpawned = false
        bossTransformed = false
        isBossIntroActive = false
        bossIntroTimer = 0f
        bossHpFillAnim = 0f
        enemies.add(Enemy(1, Vector2(1300f, 1000f), EnemyType.UNDEAD_SKELETON, difficulty = difficulty).apply { attackCooldown = 0.0f })
        enemies.add(Enemy(2, Vector2(1650f, 1200f), EnemyType.CORRUPTED_KNIGHT, difficulty = difficulty).apply { attackCooldown = 0.4f })
        enemies.add(Enemy(3, Vector2(2100f, 1450f), EnemyType.UNDEAD_SKELETON, difficulty = difficulty).apply { attackCooldown = 0.8f })
        enemies.add(Enemy(4, Vector2(2500f, 1800f), EnemyType.CORRUPTED_KNIGHT, difficulty = difficulty).apply { attackCooldown = 1.2f })
        enemies.add(Enemy(5, Vector2(2850f, 2200f), EnemyType.UNDEAD_SKELETON, difficulty = difficulty).apply { attackCooldown = 0.3f })
        enemies.add(Enemy(6, Vector2(3200f, 2500f), EnemyType.CORRUPTED_KNIGHT, difficulty = difficulty).apply { attackCooldown = 0.7f })
        enemies.add(Enemy(7, Vector2(3450f, 2800f), EnemyType.UNDEAD_SKELETON, difficulty = difficulty).apply { attackCooldown = 1.1f })
    }

    private fun checkBossSpawn() {
        if (!bossSpawned && enemies.isNotEmpty() && enemies.none { it.id != 99 && it.state != EnemyState.DEAD }) {
            bossSpawned = true
            val boss = Enemy(99, Vector2(3600f, 3000f), EnemyType.ABYSSAL_BOSS, difficulty = difficulty)
            enemies.add(boss)
            triggerBossEntrance(boss)
        }

        val activeBoss = enemies.firstOrNull { it.type == EnemyType.ABYSSAL_BOSS && it.state != EnemyState.DEAD }
        if (activeBoss != null && !bossTransformed && activeBoss.hp <= activeBoss.maxHp * 0.5f) {
            bossTransformed = true
            triggerScreenShake(0.6f, 20f)
            showNotification("❖ PHASE II: ENRAGED BLADE ASSASSIN ❖")
            SoundEffects.playVictory()
            floatingTexts.add(FloatingText("PHASE II: ENRAGED!", Vector2(activeBoss.position.x, activeBoss.position.y + 75f), Color.RED))
        }
    }

    private fun triggerBossEntrance(boss: Enemy) {
        isBossIntroActive = true
        bossIntroTimer = bossIntroDuration
        bossHpFillAnim = 0f
        triggerScreenShake(0.6f, 18f)
        SoundEffects.playVictory()
        showNotification("❖ WARNING: THE BLADE ASSASSIN HAS AWAKENED! ❖")
        floatingTexts.add(FloatingText("❖ FOE AWAKENED! ❖", Vector2(boss.position.x, boss.position.y + 80f), Color.GOLD))
    }

    private fun initChests() {
        chests.clear()
        chests.add(Chest(Vector2(1800f, 1800f), isOpened = false, hasKey = false)) // +150 Souls
        chests.add(Chest(Vector2(3100f, 2400f), isOpened = false, hasKey = true))  // Sewer Master Key
    }

    private fun checkChests() {
        val playerCenter = Vector2(playerPosition.x + 32f, playerPosition.y + 32f)
        for (chest in chests) {
            if (!chest.isOpened && playerCenter.dst(chest.position) < 80f) {
                chest.isOpened = true
                SoundEffects.playVictory()
                triggerScreenShake(0.2f, 6f)
                if (chest.hasKey) {
                    hasSewerKey = true
                    showNotification("OBTAINED SEWER MASTER KEY!")
                    floatingTexts.add(FloatingText("SEWER MASTER KEY!", Vector2(chest.position.x, chest.position.y + 40f), Color.GOLD))
                } else {
                    soulsCollected += 150
                    soulsLabel?.setText("SOULS: $soulsCollected")
                    showNotification("TREASURE OPENED! (+150 SOULS)")
                    floatingTexts.add(FloatingText("+150 SOULS!", Vector2(chest.position.x, chest.position.y + 40f), Color.YELLOW))
                }
            }
        }
    }

    private fun performPlayerAttack() {
        val attackRange = 110f
        val attackDir = when (facingDirection) {
            Direction.N -> Vector2(0f, attackRange)
            Direction.NE -> Vector2(attackRange * 0.707f, attackRange * 0.707f)
            Direction.E -> Vector2(attackRange, 0f)
            Direction.SE -> Vector2(attackRange * 0.707f, -attackRange * 0.707f)
            Direction.S -> Vector2(0f, -attackRange)
            Direction.SW -> Vector2(-attackRange * 0.707f, -attackRange * 0.707f)
            Direction.W -> Vector2(-attackRange, 0f)
            Direction.NW -> Vector2(-attackRange * 0.707f, attackRange * 0.707f)
        }

        val facingVec = Vector2(attackDir).nor()

        slashAngle = when (facingDirection) {
            Direction.E -> 0f
            Direction.NE -> 45f
            Direction.N -> 90f
            Direction.NW -> 135f
            Direction.W -> 180f
            Direction.SW -> 225f
            Direction.S -> 270f
            Direction.SE -> 315f
        }

        val frameW = currentAnim?.getKeyFrame(stateTimer)?.regionWidth?.toFloat() ?: 64f
        val frameH = currentAnim?.getKeyFrame(stateTimer)?.regionHeight?.toFloat() ?: 64f
        val playerCenter = Vector2(playerPosition.x + frameW / 2f, playerPosition.y + frameH / 2f)

        val slashImpactPoint = Vector2(playerCenter).add(Vector2(facingVec).scl(60f))
        slashPos.set(slashImpactPoint)
        slashTimer = 0.25f

        for (enemy in enemies) {
            if (enemy.state != EnemyState.DEAD) {
                val toEnemy = Vector2(enemy.position.x - playerCenter.x, enemy.position.y - playerCenter.y)
                val distToEnemyCenter = toEnemy.len()

                val isBoss = (enemy.type == EnemyType.ABYSSAL_BOSS)
                val maxHitDist = if (isBoss) 210f else 145f
                val splashHitDist = if (isBoss) 130f else 80f

                if (distToEnemyCenter <= maxHitDist && distToEnemyCenter > 0.01f) {
                    val dirToEnemy = Vector2(toEnemy).nor()
                    val dot = facingVec.dot(dirToEnemy)

                    if (dot > -0.2f || enemy.position.dst(slashImpactPoint) <= splashHitDist) {
                        val killed = enemy.takeDamage(playerAttackDamage, attackDir)
                        SoundEffects.playHit()
                        triggerScreenShake(0.15f, if (isBoss) 12f else 7f)

                        val textCol = if (isBoss) Color.GOLD else Color.WHITE
                        val labelTxt = if (isBoss) "CRIT -${playerAttackDamage.toInt()}" else "-${playerAttackDamage.toInt()}"
                        floatingTexts.add(FloatingText(labelTxt, Vector2(enemy.position.x, enemy.position.y + 45f), textCol))

                        if (killed) {
                            val awardedSouls = if (isBoss) 500 else 50
                            soulsCollected += awardedSouls
                            playerHp = (playerHp + 30f).coerceAtLeast(0f).coerceAtMost(maxHp)
                            hpLabel?.setText("HP: ${playerHp.toInt()} / ${maxHp.toInt()}")
                            soulsLabel?.setText("SOULS: $soulsCollected")
                            if (isBoss) {
                                showNotification("❖ LEGENDARY VICTORY! ABYSSAL BLADE ASSASSIN SLAIN! ❖")
                                floatingTexts.add(FloatingText("BOSS SLAIN! +$awardedSouls SOULS", Vector2(enemy.position.x, enemy.position.y + 70f), Color.GOLD))
                                triggerVictory()
                            } else {
                                showNotification("+25 HP RECOVERED! (+$awardedSouls SOULS)")
                                floatingTexts.add(FloatingText("+$awardedSouls SOULS", Vector2(enemy.position.x, enemy.position.y + 55f), Color.GOLD))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createFallbackMap() {
        try {
            val fallbackMap = TiledMap()
            val layer = TiledMapTileLayer(20, 20, 32, 32)

            val pixmap = Pixmap(32, 32, Pixmap.Format.RGBA8888)
            pixmap.setColor(Color.DARK_GRAY)
            pixmap.fill()
            pixmap.setColor(Color.GRAY)
            pixmap.drawRectangle(0, 0, 32, 32)
            val floorTexture = Texture(pixmap)
            pixmap.dispose()

            val tile = StaticTiledMapTile(TextureRegion(floorTexture))
            tile.id = 1

            for (x in 0 until 20) {
                for (y in 0 until 20) {
                    val cell = TiledMapTileLayer.Cell()
                    cell.tile = tile
                    layer.setCell(x, y, cell)
                }
            }

            fallbackMap.layers.add(layer)
            map = fallbackMap
            mapRenderer = OrthogonalTiledMapRenderer(fallbackMap, 6.5f)
            Gdx.app.log(TAG, "Created 20x20 fallback procedural map successfully.")
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Fallback map creation error: ${e.message}")
        }
    }

    private fun getTileIdAtWorldPos(x: Float, y: Float): Long {
        val activeMap = map ?: return 0L
        val layer = activeMap.layers.get(0) as? TiledMapTileLayer ?: return 0L
        val tileSizeWorld = 32f * 6.5f
        val tileX = (x / tileSizeWorld).toInt()
        val tileY = (y / tileSizeWorld).toInt()

        if (tileX !in 0 until layer.width || tileY !in 0 until layer.height) {
            return 17L
        }

        val cell = layer.getCell(tileX, tileY) ?: return 0L
        return cell.tile?.id?.toLong() ?: 0L
    }

    private fun isLavaTile(tileId: Long): Boolean {
        if (tileId == 0L) return false
        val masked = tileId and 0x1FFFFFFF
        return tileId == 86L || masked == 86L
    }

    private fun isWalkableTile(tileId: Long): Boolean {
        if (tileId == 0L) return true
        val collisionIds = setOf(
            17L, 28L, 29L, 1610612754L, 2684354577L, 3221225489L, 2684354598L, 2684354599L
        )
        val masked = tileId and 0x1FFFFFFF
        return !collisionIds.contains(tileId) && !collisionIds.contains(masked)
    }

    private fun isPositionWalkable(x: Float, y: Float, width: Float, height: Float): Boolean {
        val footCenterX = x + width / 2f
        val footCenterY = y + height * 0.35f
        val tileId = getTileIdAtWorldPos(footCenterX, footCenterY)
        return isWalkableTile(tileId)
    }

    private fun checkLavaDamage(x: Float, y: Float, width: Float, height: Float, delta: Float) {
        val footCenterX = x + width / 2f
        val footCenterY = y + height * 0.35f
        val tileId = getTileIdAtWorldPos(footCenterX, footCenterY)

        if (isLavaTile(tileId)) {
            val baseDamage = 7f
            val damage = baseDamage * damageMultiplier * delta
            playerHp = (playerHp - damage).coerceAtLeast(0f)
            hpLabel?.color = Color.RED
            if (playerHp <= 0f && currentGameState == GameState.RUNNING) {
                triggerGameOver()
            }
        } else {
            hpLabel?.color = Color.GREEN
        }

        hpLabel?.setText("HP: ${playerHp.toInt()} / ${maxHp.toInt()}")
    }

    private fun checkBonfireAndExit() {
        val playerCenter = Vector2(playerPosition.x + 32f, playerPosition.y + 32f)

        val distToBonfire = playerCenter.dst(bonfirePos)
        if (distToBonfire > 125f) {
            hasLeftBonfire = true
        }

        if (distToBonfire < 80f && currentGameState == GameState.RUNNING) {
            if (bonfireCooldownTimer <= 0f && (hasLeftBonfire || bonfireTable?.isVisible != true)) {
                hasLeftBonfire = false
                playerHp = maxHp
                playerStamina = maxStamina
                hpLabel?.setText("HP: ${playerHp.toInt()} / ${maxHp.toInt()}")
                staminaLabel?.setText("STAMINA: ${playerStamina.toInt()} / ${maxStamina.toInt()}")
                openBonfireMenu()
                showNotification("BONFIRE RESTORED - HP & STAMINA FULL!")
                SoundEffects.playVictory()
            }
        }

        if (playerCenter.dst(exitPos) < 120f && currentGameState == GameState.RUNNING) {
            if (hasSewerKey) {
                triggerVictory()
            } else {
                showNotification("EXIT LOCKED - FIND SEWER MASTER KEY IN CHEST!")
            }
        }
    }

    private fun togglePauseMenu(show: Boolean) {
        if (show) {
            currentGameState = GameState.PAUSED
            if (pauseTable == null) {
                pauseTable = Table().apply {
                    setFillParent(true)
                    background = TextureRegionDrawable(TextureRegion(getDimTexture()))

                    val titleStyle = Label.LabelStyle(dialogTitleFont, Color.GOLD)
                    val title = Label("❖ GAME PAUSED ❖", titleStyle)

                    val btnResume = TextButton("RESUME GAME", dialogButtonStyle)
                    val btnMenu = TextButton("RETURN TO MAIN MENU", dialogButtonStyle)

                    btnResume.addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            togglePauseMenu(false)
                        }
                    })

                    btnMenu.addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            Gdx.app.exit()
                        }
                    })

                    add(title).padBottom(30f).row()
                    add(btnResume).width(240f).height(50f).padBottom(15f).row()
                    add(btnMenu).width(240f).height(50f).row()
                }
                stage.addActor(pauseTable)
            }
            pauseTable?.isVisible = true
        } else {
            currentGameState = GameState.RUNNING
            pauseTable?.isVisible = false
        }
    }

    private fun triggerGameOver() {
        currentGameState = GameState.GAME_OVER
        SoundEffects.playGameOver()

        val lostSouls = soulsCollected
        soulsCollected = 0
        soulsLabel?.setText("SOULS: 0")

        if (gameOverTable == null) {
            gameOverTable = Table().apply {
                setFillParent(true)
                background = TextureRegionDrawable(TextureRegion(getDimTexture()))

                val titleStyle = Label.LabelStyle(dialogTitleFont, Color.RED)
                val subtitleStyle = Label.LabelStyle(dialogFont, Color.GOLD)

                val title = Label("YOU DIED", titleStyle)
                val subtitle = Label("SOULS LOST IN THE DEPTHS: $lostSouls", subtitleStyle)
                val btnRespawn = TextButton("RESPAWN AT BONFIRE", dialogButtonStyle)
                val btnMenu = TextButton("MAIN MENU", dialogButtonStyle)

                btnRespawn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        respawnPlayer()
                    }
                })

                btnMenu.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        Gdx.app.exit()
                    }
                })

                add(title).padBottom(20f).row()
                add(subtitle).padBottom(30f).row()
                add(btnRespawn).width(260f).height(55f).padBottom(15f).row()
                add(btnMenu).width(260f).height(55f).row()
            }
            stage.addActor(gameOverTable)
        } else {
            val subtitle = gameOverTable?.children?.get(1) as? Label
            subtitle?.setText("SOULS LOST IN THE DEPTHS: $lostSouls")
        }
        gameOverTable?.isVisible = true
    }

    private fun triggerVictory() {
        currentGameState = GameState.VICTORY
        SoundEffects.playVictory()

        if (victoryTable == null) {
            victoryTable = Table().apply {
                setFillParent(true)
                background = TextureRegionDrawable(TextureRegion(getDimTexture()))

                val cardWidth = (stage.viewport.worldWidth * 0.80f).coerceIn(580f, 750f).toInt()
                val cardHeight = 420

                val container = Table().apply {
                    background = TextureRegionDrawable(TextureRegion(getModalTexture(cardWidth, cardHeight)))
                    pad(25f, 30f, 25f, 30f)
                }

                val titleStyle = Label.LabelStyle(dialogTitleFont, Color.GOLD)
                val bossSlainStyle = Label.LabelStyle(hudFont, Color(1.0f, 0.35f, 0.2f, 1f))
                val mainMsgStyle = Label.LabelStyle(dialogFont, Color.WHITE)
                val statsStyle = Label.LabelStyle(dialogFont, Color.YELLOW)

                val title = Label("❖ VICTORY ACHIEVED ❖", titleStyle)
                val bossSlain = Label("❖ THE ABYSSAL BLADE ASSASSIN HAS BEEN SLAIN! ❖", bossSlainStyle)

                val thankYouLabel = Label(
                    "THANK YOU FOR PLAYING BLOOD SOULS!\n" +
                    "MORE LEVELS & CHAPTERS COMING SOON!",
                    mainMsgStyle
                ).apply {
                    setAlignment(com.badlogic.gdx.utils.Align.center)
                }

                val statsInfo = Label(
                    "TOTAL SOULS COLLECTED: $soulsCollected   |   MODE: ${difficulty.uppercase()}",
                    statsStyle
                )

                val btnRestart = TextButton("PLAY AGAIN", dialogButtonStyle).apply {
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            victoryTable?.isVisible = false
                            respawnPlayer()
                        }
                    })
                }

                val btnMenu = TextButton("MAIN MENU", dialogButtonStyle).apply {
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            Gdx.app.exit()
                        }
                    })
                }

                val btnTable = Table().apply {
                    add(btnRestart).width(210f).height(48f).padRight(15f)
                    add(btnMenu).width(210f).height(48f)
                }

                container.add(title).padBottom(6f).row()
                container.add(bossSlain).padBottom(18f).row()
                container.add(thankYouLabel).padBottom(20f).row()
                container.add(statsInfo).padBottom(25f).row()
                container.add(btnTable).row()

                add(container)
            }
            stage.addActor(victoryTable)
        }
        victoryTable?.isVisible = true
    }

    private fun respawnPlayer() {
        playerHp = maxHp
        playerStamina = maxStamina
        playerPosition.set(bonfirePos.x - 32f, bonfirePos.y - 32f)
        currentGameState = GameState.RUNNING
        gameOverTable?.isVisible = false
        victoryTable?.isVisible = false
        initEnemies()
        initChests()
        showNotification("RESPAWNED AT BONFIRE!")
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (bonfireCooldownTimer > 0f) {
            bonfireCooldownTimer -= delta
        }

        if (notificationTimer > 0f) {
            notificationTimer -= delta
            if (notificationTimer <= 0f) {
                notificationLabel?.isVisible = false
            }
        }

        if (currentGameState == GameState.RUNNING) {
            stateTimer += delta

            if (actionState != ActionState.BLOCK_START && actionState != ActionState.BLOCK_IDLE) {
                playerStamina = (playerStamina + 25f * delta).coerceAtMost(maxStamina)
            } else {
                playerStamina = (playerStamina - 12f * delta).coerceAtLeast(0f)
                if (playerStamina <= 0f) {
                    actionState = ActionState.NONE
                    showNotification("STAMINA EXHAUSTED!")
                }
            }
            staminaLabel?.setText("STAMINA: ${playerStamina.toInt()} / ${maxStamina.toInt()}")

            val frame = currentAnim?.getKeyFrame(stateTimer)
            val frameWidth = frame?.regionWidth?.toFloat() ?: 64f
            val frameHeight = frame?.regionHeight?.toFloat() ?: 64f

            val mapTileWidth = map?.properties?.get("width", Int::class.java) ?: 20
            val mapTileHeight = map?.properties?.get("height", Int::class.java) ?: 20
            val tilePixelWidth = map?.properties?.get("tilewidth", Int::class.java) ?: 32
            val tilePixelHeight = map?.properties?.get("tileheight", Int::class.java) ?: 32

            val mapWorldWidth = mapTileWidth * tilePixelWidth * 6.5f
            val mapWorldHeight = mapTileHeight * tilePixelHeight * 6.5f

            val joyVec = joystick.getDirectionVector()
            if (joyVec.len() > 0.1f) {
                facingDirection = when {
                    joyVec.x > 0.5f && joyVec.y > 0.5f -> Direction.NE
                    joyVec.x > 0.5f && joyVec.y < -0.5f -> Direction.SE
                    joyVec.x < -0.5f && joyVec.y > 0.5f -> Direction.NW
                    joyVec.x < -0.5f && joyVec.y < -0.5f -> Direction.SW
                    joyVec.x > 0.5f -> Direction.E
                    joyVec.x < -0.5f -> Direction.W
                    joyVec.y > 0.5f -> Direction.N
                    else -> Direction.S
                }

                val animFinished = currentAnim?.isAnimationFinished(stateTimer) ?: true
                if (actionState == ActionState.NONE || currentAnim == null || animFinished) {
                    actionState = ActionState.NONE
                    currentAnim = knightAnims.getRunAnimation(facingDirection)
                }

                val speed = 250f * delta
                val dir = Vector2(joyVec).nor()
                val deltaX = dir.x * speed
                val deltaY = dir.y * speed

                val nextX = playerPosition.x + deltaX
                if (isPositionWalkable(nextX, playerPosition.y, frameWidth, frameHeight)) {
                    playerPosition.x = nextX
                }

                val nextY = playerPosition.y + deltaY
                if (isPositionWalkable(playerPosition.x, nextY, frameWidth, frameHeight)) {
                    playerPosition.y = nextY
                }
            } else {
                if (actionState != ActionState.NONE && currentAnim != null) {
                    if (currentAnim!!.isAnimationFinished(stateTimer)) {
                        when (actionState) {
                            ActionState.BLOCK_START -> {
                                val blockIdleAnim = knightAnims.playBlockIdle(facingDirection)
                                if (blockIdleAnim != null) {
                                    currentAnim = blockIdleAnim
                                    stateTimer = 0f
                                    actionState = ActionState.BLOCK_IDLE
                                } else {
                                    actionState = ActionState.NONE
                                }
                            }
                            ActionState.ATTACK, ActionState.JUMP, ActionState.LOOK_AROUND -> {
                                actionState = ActionState.NONE
                            }
                            ActionState.BLOCK_IDLE, ActionState.NONE -> {}
                        }
                    }
                }

                if (actionState == ActionState.NONE) {
                    currentAnim = knightAnims.getIdleAnimation(KnightIdle.NORMAL, facingDirection)
                }
            }
            if (isBossIntroActive) {
                bossIntroTimer -= delta
                bossHpFillAnim = ((1.0f - (bossIntroTimer / bossIntroDuration)) * 450f).coerceIn(0f, 450f)
                if (bossIntroTimer <= 0f) {
                    isBossIntroActive = false
                    showNotification("⚔ BATTLE COMMENCED! DEFEAT THE ASSASSIN! ⚔")
                }
            }

            checkLavaDamage(playerPosition.x, playerPosition.y, frameWidth, frameHeight, delta)
            checkBonfireAndExit()
            checkChests()
            checkBossSpawn()

            val isBlocking = (actionState == ActionState.BLOCK_START || actionState == ActionState.BLOCK_IDLE)
            val playerCenter = Vector2(playerPosition.x + frameWidth / 2f, playerPosition.y + frameHeight / 2f)
            for (enemy in enemies) {
                enemy.update(delta, playerCenter, isBlocking, enemies, damageMultiplier) { dmg, knockback ->
                    if (dmg < 0f) {
                        val staminaCost = -dmg
                        if (playerStamina >= staminaCost) {
                            playerStamina -= staminaCost
                            showNotification("BLOCKED! (-${staminaCost.toInt()} STAMINA)")
                            floatingTexts.add(FloatingText("BLOCKED!", Vector2(playerPosition.x + 32f, playerPosition.y + 60f), Color.CYAN))
                        } else {
                            val leftoverDamage = staminaCost - playerStamina
                            playerStamina = 0f
                            playerHp = (playerHp - leftoverDamage * 0.5f).coerceAtLeast(0f)
                            showNotification("GUARD BROKEN!")
                            floatingTexts.add(FloatingText("GUARD BROKEN!", Vector2(playerPosition.x + 32f, playerPosition.y + 60f), Color.RED))
                        }
                    } else {
                        playerHp = (playerHp - dmg).coerceAtLeast(0f)
                        floatingTexts.add(FloatingText("-${dmg.toInt()} HP", Vector2(playerPosition.x + 32f, playerPosition.y + 60f), Color.RED))
                        triggerScreenShake(0.2f, 9f)
                    }

                    playerPosition.add(knockback)
                    if (playerHp <= 0f) {
                        triggerGameOver()
                    }
                }
            }

            playerPosition.x = playerPosition.x.coerceIn(0f, mapWorldWidth - frameWidth)
            playerPosition.y = playerPosition.y.coerceIn(0f, mapWorldHeight - frameHeight)

            val activeBoss = enemies.firstOrNull { it.type == EnemyType.ABYSSAL_BOSS && it.state != EnemyState.DEAD }
            val playerCenterX = playerPosition.x + frameWidth / 2f
            val playerCenterY = playerPosition.y + frameHeight / 2f

            val targetCamX = if (isBossIntroActive && activeBoss != null) activeBoss.position.x else playerCenterX
            val targetCamY = if (isBossIntroActive && activeBoss != null) activeBoss.position.y else playerCenterY

            if (mapWorldWidth > camera.viewportWidth) {
                camera.position.x = targetCamX.coerceIn(
                    camera.viewportWidth / 2f,
                    mapWorldWidth - camera.viewportWidth / 2f
                )
            } else {
                camera.position.x = mapWorldWidth / 2f
            }

            if (mapWorldHeight > camera.viewportHeight) {
                camera.position.y = targetCamY.coerceIn(
                    camera.viewportHeight / 2f,
                    mapWorldHeight - camera.viewportHeight / 2f
                )
            } else {
                camera.position.y = mapWorldHeight / 2f
            }
        }

        if (shakeTimer > 0f) {
            shakeTimer -= delta
            val offsetX = (Math.random().toFloat() * 2f - 1f) * shakeIntensity
            val offsetY = (Math.random().toFloat() * 2f - 1f) * shakeIntensity
            camera.position.add(offsetX, offsetY, 0f)
        }

        camera.update()

        try {
            mapRenderer?.setView(camera)
            mapRenderer?.render()
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Map render error: ${e.message}")
        }

        // Render Large Radial Ambient Light Aura around Bonfire
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.95f, 0.55f, 0.1f, 0.28f)
        shapeRenderer.circle(bonfirePos.x, bonfirePos.y, 160f)
        shapeRenderer.color = Color(1.0f, 0.75f, 0.2f, 0.15f)
        shapeRenderer.circle(bonfirePos.x, bonfirePos.y, 220f)
        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        batch.projectionMatrix = camera.combined
        batch.begin()

        // Draw Chests
        for (chest in chests) {
            val tex = if (chest.isOpened) chestTextureOpen else chestTextureClosed
            batch.draw(tex, chest.position.x - 32f, chest.position.y - 32f)
            if (!chest.isOpened && Vector2(playerPosition.x + 32f, playerPosition.y + 32f).dst(chest.position) < 100f) {
                hudFont.color = Color.GOLD
                hudFont.draw(batch, "[OPEN CHEST]", chest.position.x - 40f, chest.position.y + 50f)
            }
        }

        // Draw Large Bonfire Base & Coiled Sword (160x160)
        batch.draw(bonfireTexture, bonfirePos.x - 80f, bonfirePos.y - 80f, 160f, 160f)

        // Draw Towering Flickering Bonfire Flame (140x140)
        val flamePulse = (sin(stateTimer * 10.0) * 8f).toFloat()
        batch.setColor(1f, 0.75f + flamePulse * 0.02f, 0.2f, 0.95f)
        batch.draw(
            flameTexture,
            bonfirePos.x - 70f - flamePulse / 2f,
            bonfirePos.y - 30f,
            140f + flamePulse,
            140f + flamePulse
        )
        batch.setColor(Color.WHITE)

        // Floating Gold Map Label above Bonfire
        notifyFont.color = Color.GOLD
        notifyFont.draw(batch, "BONFIRE", bonfirePos.x - 45f, bonfirePos.y + 110f)
        hudFont.color = Color.WHITE
        hudFont.draw(batch, "[REST & CHECKPOINT]", bonfirePos.x - 70f, bonfirePos.y + 85f)

        val frame = currentAnim?.getKeyFrame(stateTimer)
        if (frame != null) {
            batch.draw(frame, playerPosition.x, playerPosition.y)
        } else {
            batch.draw(fallbackTexture, playerPosition.x, playerPosition.y)
        }

        if (slashTimer > 0f) {
            slashTimer -= delta
            batch.draw(
                slashTexture,
                slashPos.x - 32f,
                slashPos.y - 32f,
                32f, 32f,
                64f, 64f,
                1.3f, 1.3f,
                slashAngle,
                0, 0, 64, 64, false, false
            )
        }

        // Render Floating Texts
        val ftIter = floatingTexts.iterator()
        while (ftIter.hasNext()) {
            val ft = ftIter.next()
            ft.position.y += 35f * delta
            ft.lifetime -= delta
            if (ft.lifetime <= 0f) {
                ftIter.remove()
            } else {
                tempTextCol.set(ft.color).a = ft.lifetime.coerceIn(0f, 1f)
                notifyFont.color = tempTextCol
                notifyFont.draw(batch, ft.text, ft.position.x, ft.position.y)
            }
        }

        batch.end()

        for (enemy in enemies) {
            enemy.render(batch, shapeRenderer, knightAnims)
        }

        // 1. Render Cinematic Boss Entrance Overlay (Letterbox Bars & Title Card)
        if (isBossIntroActive) {
            val sw = stage.viewport.worldWidth
            val sh = stage.viewport.worldHeight
            val barHeight = 85f

            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            shapeRenderer.projectionMatrix = stage.camera.combined
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

            // Letterbox black bars
            shapeRenderer.color = Color(0f, 0f, 0f, 0.92f)
            shapeRenderer.rect(0f, sh - barHeight, sw, barHeight)
            shapeRenderer.rect(0f, 0f, sw, barHeight)

            // Red Vignette overlay
            shapeRenderer.color = Color(0.35f, 0.05f, 0.05f, 0.22f)
            shapeRenderer.rect(0f, barHeight, sw, sh - barHeight * 2f)

            shapeRenderer.end()
            Gdx.gl.glDisable(GL20.GL_BLEND)

            // Cinematic Intro Title Card
            batch.projectionMatrix = stage.camera.combined
            batch.begin()

            notifyFont.color = Color.GOLD
            val topSubtitle = "❖ FOE AWAKENED ❖"
            glyphLayout.setText(notifyFont, topSubtitle)
            notifyFont.draw(batch, topSubtitle, sw / 2f - glyphLayout.width / 2f, sh - 30f)

            dialogTitleFont.color = Color(1.0f, 0.2f, 0.2f, 1.0f)
            val bossTitleStr = "ABYSSAL BLADE ASSASSIN"
            glyphLayout.setText(dialogTitleFont, bossTitleStr)
            dialogTitleFont.draw(batch, bossTitleStr, sw / 2f - glyphLayout.width / 2f, sh / 2f + 50f)

            hudFont.color = Color(0.95f, 0.9f, 0.75f, 1.0f)
            val bossSubStr = "- GUARDIAN OF THE SEWER CRYPT -"
            glyphLayout.setText(hudFont, bossSubStr)
            hudFont.draw(batch, bossSubStr, sw / 2f - glyphLayout.width / 2f, sh / 2f + 10f)

            batch.end()
        }

        // 2. Render Prominent Boss HP Bar across screen bottom with Name AND Numbers
        val boss = enemies.firstOrNull { it.type == EnemyType.ABYSSAL_BOSS && it.state != EnemyState.DEAD }
        if (boss != null) {
            val distToBoss = Vector2(playerPosition.x + 32f, playerPosition.y + 32f).dst(boss.position)
            if (distToBoss <= 1000f || boss.hp < boss.maxHp || isBossIntroActive) {
                val barW = (stage.viewport.worldWidth * 0.65f).coerceIn(450f, 680f)
                val barH = 26f
                val barX = stage.viewport.worldWidth / 2f - barW / 2f
                val barY = 45f

                val currentHp = if (isBossIntroActive) bossHpFillAnim else boss.hp
                val maxHp = boss.maxHp
                val hpRatio = (currentHp / maxHp).coerceIn(0f, 1f)
                val isPhaseTwo = boss.hp <= maxHp * 0.5f

                Gdx.gl.glEnable(GL20.GL_BLEND)
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

                shapeRenderer.projectionMatrix = stage.camera.combined
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

                // Outer Background Frame Panel
                shapeRenderer.color = Color(0.08f, 0.02f, 0.02f, 0.92f)
                shapeRenderer.rect(barX - 4f, barY - 4f, barW + 8f, barH + 8f)

                // Dark Bar Slot
                shapeRenderer.color = Color(0.2f, 0.05f, 0.05f, 0.85f)
                shapeRenderer.rect(barX, barY, barW, barH)

                // Main Crimson / Fiery Fill Bar
                shapeRenderer.color = if (isPhaseTwo) Color(1.0f, 0.22f, 0.05f, 1.0f) else Color(0.85f, 0.15f, 0.15f, 1.0f)
                shapeRenderer.rect(barX, barY, barW * hpRatio, barH)

                shapeRenderer.end()

                // Ornate Gold Double Border Frame
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                shapeRenderer.color = Color.GOLD
                shapeRenderer.rect(barX, barY, barW, barH)
                shapeRenderer.rect(barX - 3f, barY - 3f, barW + 6f, barH + 6f)
                shapeRenderer.end()

                Gdx.gl.glDisable(GL20.GL_BLEND)

                // Boss Name Title & Exact HP Numbers Overlay
                batch.projectionMatrix = stage.camera.combined
                batch.begin()

                // Boss Name Title above HP Bar
                notifyFont.color = if (isPhaseTwo) Color(1.0f, 0.35f, 0.2f, 1.0f) else Color.GOLD
                val titleStr = if (isPhaseTwo) "❖ ABYSSAL BLADE ASSASSIN (ENRAGED) ❖" else "❖ ABYSSAL BLADE ASSASSIN ❖"
                glyphLayout.setText(notifyFont, titleStr)
                notifyFont.draw(batch, titleStr, stage.viewport.worldWidth / 2f - glyphLayout.width / 2f, barY + barH + 24f)

                // Prominent HP Numbers directly on HP Bar: "HP: 350 / 450 (77%)"
                hudFont.color = Color.WHITE
                val hpNumStr = "HP: ${currentHp.toInt()} / ${maxHp.toInt()}  (${(hpRatio * 100).toInt()}%)"
                glyphLayout.setText(hudFont, hpNumStr)
                hudFont.draw(batch, hpNumStr, stage.viewport.worldWidth / 2f - glyphLayout.width / 2f, barY + barH / 2f + 6f)

                batch.end()
            }
        }

        shapeRenderer.projectionMatrix = stage.camera.combined
        val worldH = stage.viewport.worldHeight
        val hudY = worldH - 60f

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Dark HUD Container Background Panel
        shapeRenderer.color = Color(0.06f, 0.06f, 0.09f, 0.88f)
        shapeRenderer.rect(55f, hudY - 98f, 275f, 120f)

        // HP Bar Background Slot
        shapeRenderer.color = Color(0.2f, 0.05f, 0.05f, 1f)
        shapeRenderer.rect(70f, hudY - 20f, 240f, 18f)

        // HP Bar Fill
        val healthRatio = (playerHp / maxHp).coerceIn(0f, 1f)
        shapeRenderer.color = if (healthRatio < 0.3f) Color.RED else Color(0.85f, 0.15f, 0.15f, 1f)
        shapeRenderer.rect(70f, hudY - 20f, 240f * healthRatio, 18f)

        // Stamina Bar Background Slot
        shapeRenderer.color = Color(0.2f, 0.16f, 0.04f, 1f)
        shapeRenderer.rect(70f, hudY - 52f, 240f, 16f)

        // Stamina Bar Fill
        val staminaRatio = (playerStamina / maxStamina).coerceIn(0f, 1f)
        shapeRenderer.color = Color.GOLD
        shapeRenderer.rect(70f, hudY - 52f, 240f * staminaRatio, 16f)

        shapeRenderer.end()

        // Metallic Gold Borders around Panel and Bars
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = Color(0.85f, 0.7f, 0.25f, 1f)
        shapeRenderer.rect(55f, hudY - 98f, 275f, 120f)
        shapeRenderer.rect(70f, hudY - 20f, 240f, 18f)
        shapeRenderer.rect(70f, hudY - 52f, 240f, 16f)
        shapeRenderer.end()

        try {
            stage.act(delta)
            stage.draw()
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Stage render error: ${e.message}")
        }
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()

        stage.viewport.update(width, height, true)
        val worldW = stage.viewport.worldWidth
        val worldH = stage.viewport.worldHeight

        val joystickX = worldW - 250f
        joystick.setPosition(joystickX, 50f)
        buttons.updatePositions(60f, 50f)

        val startY = worldH - 60f
        hpLabel?.setPosition(75f, startY - 20f)
        staminaLabel?.setPosition(75f, startY - 52f)
        soulsLabel?.setPosition(75f, startY - 78f)
        difficultyLabel?.setPosition(75f, startY - 98f)
        notificationLabel?.setPosition(worldW / 2f - 160f, startY - 55f)

        btnPause?.setPosition(worldW - 170f, worldH - 75f)
    }

    override fun show() {
        Gdx.app.log(TAG, "show called: GameplayScreen active")
    }

    override fun hide() {
        Gdx.app.log(TAG, "hide called")
    }

    override fun pause() {
        Gdx.app.log(TAG, "pause called")
    }

    override fun resume() {
        Gdx.app.log(TAG, "resume called")
    }

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        stage.dispose()
        map?.dispose()
        mapRenderer?.dispose()
        joystick.dispose()
        buttons.dispose()
        knightAnims.dispose()
        hudFont.dispose()
        notifyFont.dispose()
        dialogFont.dispose()
        dialogTitleFont.dispose()
        cachedDimTexture?.dispose()
        cachedModalTexture?.dispose()
        cachedBadgeTexture?.dispose()
        Enemy.disposeTexture()
        SoundEffects.dispose()
    }
}
