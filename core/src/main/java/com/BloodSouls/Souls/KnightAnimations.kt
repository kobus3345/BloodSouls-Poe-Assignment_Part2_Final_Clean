package com.BloodSouls.Souls

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array as GdxArray
import com.badlogic.gdx.utils.Disposable

enum class KnightIdle {
    NORMAL,
    LOOK_AT_SWORD_START,
    LOOK_AT_SWORD_LOOP,
    LOOK_AROUND,
    JUMP
}

enum class Direction { N, NE, E, SE, S, SW, W, NW }

class KnightAnimations : Disposable {
    private val animations = mutableMapOf<Pair<String, Direction>, Animation<TextureRegion>>()
    private val allTextures = mutableListOf<Texture>()

    private val assetList: List<String> by lazy {
        try {
            val file = Gdx.files.internal("assets.txt")
            if (file.exists()) {
                file.readString().lines().map { it.trim().replace('\\', '/') }.filter { it.isNotEmpty() }
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadIdleAnimations(basePath: String) {
        loadVariant("IDLE_NORMAL", resolvePath(basePath, "Idle_Normal_1"), 0.15f)
        loadVariant("IDLE_LOOK_AT_SWORD_START", resolvePath(basePath, "Idle_LookAtSword_Start"), 0.15f)
        loadVariant("IDLE_LOOK_AT_SWORD_LOOP", resolvePath(basePath, "Idle_LookAtSword_2_(Requires Loop)"), 0.15f, loop = true)
        loadVariant("IDLE_LOOK_AROUND", resolvePath(basePath, "Idle_Look_Around"), 0.15f)
        loadVariant("IDLE_JUMP", resolvePath(basePath, "Idle_Jump_1"), 0.15f)
    }

    fun loadRunAnimations(basePath: String) {
        loadVariant("RUNNING", resolvePath(basePath, "Running_1"), 0.04f, loop = true)
    }

    fun loadAttackAnimations(basePath: String) {
        loadVariant("ATTACK_1", resolvePath(basePath, "Slash_Attack_1"), 0.025f, loop = false)
        loadVariant("ATTACK_2", resolvePath(basePath, "Attack_1"), 0.025f, loop = false)
        loadVariant("ATTACK_3", resolvePath(basePath, "Attack_2"), 0.025f, loop = false)
    }

    fun loadBlockAnimations(basePath: String) {
        loadVariant("BLOCK_START", resolvePath(basePath, "Block_Start"), 0.08f, loop = false)
        loadVariant("BLOCK_IDLE", resolvePath(basePath, "Block_Idle"), 0.1f, loop = true)
        loadVariant("BLOCK_END", resolvePath(basePath, "Block_End"), 0.08f, loop = false)
        loadVariant("BLOCK_IMPACT", resolvePath(basePath, "Block_Idle_Impact"), 0.08f, loop = false)
    }

    fun loadSkeletonAnimations() {
        val base = "skeleton/frames"
        loadVariant("SKELETON_IDLE", "$base/idle", 0.15f)
        loadVariant("SKELETON_RUN", "$base/walk", 0.05f, loop = true)
        loadVariant("SKELETON_ATTACK", "$base/attack", 0.035f, loop = false)
    }

    fun loadBossAnimations() {
        val baseP1 = "free-blade-assassin-bladeassassinbossfree-v1-1-3/characters/blade-assassin/frames"
        loadVariant("BOSS_P1_IDLE", "$baseP1/idle", 0.12f)
        loadVariant("BOSS_P1_RUN", "$baseP1/walk", 0.08f, loop = true)
        loadVariant("BOSS_P1_ATTACK_1", "$baseP1/attack", 0.09f, loop = false)
        loadVariant("BOSS_P1_ATTACK_2", "$baseP1/attack2", 0.09f, loop = false)
        loadVariant("BOSS_P1_ATTACK_3", "$baseP1/attack3", 0.09f, loop = false)

        val baseP2 = "free-blade-assassin-bladeassassinbossfree-v1-1-3/characters/blade-assassin-phase-two/frames"
        loadVariant("BOSS_P2_IDLE", "$baseP2/idle", 0.12f)
        loadVariant("BOSS_P2_RUN", "$baseP2/walk", 0.07f, loop = true)
        loadVariant("BOSS_P2_ATTACK_1", "$baseP2/attack", 0.08f, loop = false)
        loadVariant("BOSS_P2_ATTACK_2", "$baseP2/attack2", 0.08f, loop = false)
        loadVariant("BOSS_P2_ATTACK_3", "$baseP2/attack3", 0.08f, loop = false)
    }

    fun getSkeletonAnimation(isAttacking: Boolean, direction: Direction): Animation<TextureRegion>? {
        return if (isAttacking) {
            animations["SKELETON_ATTACK" to direction] ?: animations["SKELETON_IDLE" to direction]
        } else {
            animations["SKELETON_RUN" to direction] ?: animations["SKELETON_IDLE" to direction]
        }
    }

    fun getBossAnimation(isPhaseTwo: Boolean, isAttacking: Boolean, comboIndex: Int, direction: Direction): Animation<TextureRegion>? {
        val prefix = if (isPhaseTwo) "BOSS_P2_" else "BOSS_P1_"
        return if (isAttacking) {
            val idx = (comboIndex % 3) + 1
            animations["${prefix}ATTACK_$idx" to direction]
                ?: animations["${prefix}ATTACK_1" to direction]
                ?: animations["${prefix}RUN" to direction]
        } else {
            animations["${prefix}RUN" to direction]
                ?: animations["${prefix}IDLE" to direction]
        }
    }

    private fun resolvePath(basePath: String, folder: String): String {
        return if (basePath.isEmpty()) folder else "$basePath/$folder"
    }

    private fun loadVariant(state: String, folder: String, frameDuration: Float, loop: Boolean = true) {
        val directions = listOf("N","NE","E","SE","S","SW","W","NW")
        val cleanFolder = folder.replace('\\', '/').trimEnd('/')

        for (dir in directions) {
            val dirPath = "$cleanFolder/$dir"
            val matchingFiles = GdxArray<FileHandle>()

            try {
                if (Gdx.files.internal(dirPath).exists()) {
                    val list = Gdx.files.internal(dirPath).list()
                    for (f in list) {
                        if (f.extension().lowercase() == "png") {
                            matchingFiles.add(f)
                        }
                    }
                }
            } catch (_: Exception) {}

            if (matchingFiles.size == 0 && assetList.isNotEmpty()) {
                val matchingPaths = assetList.filter { line ->
                    line.startsWith("$dirPath/", ignoreCase = true) && line.endsWith(".png", ignoreCase = true)
                }.sorted()

                for (path in matchingPaths) {
                    val fh = Gdx.files.internal(path)
                    if (fh.exists()) {
                        matchingFiles.add(fh)
                    }
                }
            }

            if (matchingFiles.size > 0) {
                val frames = GdxArray<TextureRegion>()
                val maxFramesToLoad = 4
                val step = (matchingFiles.size / maxFramesToLoad).coerceAtLeast(1)

                for (i in 0 until matchingFiles.size step step) {
                    if (frames.size >= maxFramesToLoad) break
                    val file = matchingFiles[i]
                    try {
                        val tex = Texture(file)
                        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                        allTextures.add(tex)
                        frames.add(TextureRegion(tex))
                    } catch (e: Throwable) {
                        Gdx.app.error("KnightAnimations", "Failed to load frame texture ${file.path()}: ${e.message}")
                    }
                }

                if (frames.size > 0) {
                    val anim = Animation(frameDuration * step, frames)
                    anim.playMode = if (loop) Animation.PlayMode.LOOP else Animation.PlayMode.NORMAL
                    animations[state to Direction.valueOf(dir)] = anim
                    Gdx.app.log("KnightAnimations", "Loaded ${frames.size} frames for $state $dir")
                }
            } else {
                Gdx.app.log("KnightAnimations", "Skipped missing folder $dirPath")
            }
        }
    }

    fun getIdleAnimation(idle: KnightIdle, direction: Direction): Animation<TextureRegion>? {
        return animations["IDLE_$idle" to direction]
            ?: animations["IDLE_NORMAL" to direction]
            ?: animations.values.firstOrNull()
    }

    fun getRunAnimation(direction: Direction): Animation<TextureRegion>? {
        return animations["RUNNING" to direction]
            ?: getIdleAnimation(KnightIdle.NORMAL, direction)
            ?: animations.values.firstOrNull()
    }

    fun playAttack(index: Int = 1, direction: Direction = Direction.S): Animation<TextureRegion>? {
        return animations["ATTACK_$index" to direction]
            ?: animations["ATTACK_1" to direction]
            ?: animations.values.firstOrNull()
    }

    fun playBlockStart(direction: Direction = Direction.S): Animation<TextureRegion>? {
        return animations["BLOCK_START" to direction]
            ?: animations["BLOCK_IDLE" to direction]
            ?: animations.values.firstOrNull()
    }

    fun playBlockIdle(direction: Direction = Direction.S): Animation<TextureRegion>? {
        return animations["BLOCK_IDLE" to direction] ?: animations.values.firstOrNull()
    }

    fun playBlockEnd(direction: Direction = Direction.S): Animation<TextureRegion>? {
        return animations["BLOCK_END" to direction] ?: animations.values.firstOrNull()
    }

    override fun dispose() {
        for (tex in allTextures) {
            tex.dispose()
        }
        allTextures.clear()
        animations.clear()
    }
}
