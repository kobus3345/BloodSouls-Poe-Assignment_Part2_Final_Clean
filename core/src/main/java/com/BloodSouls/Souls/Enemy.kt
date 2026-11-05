package com.BloodSouls.Souls

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import kotlin.math.sin

enum class EnemyState { CHASE, ATTACK, DEAD }
enum class EnemyType { CORRUPTED_KNIGHT, UNDEAD_SKELETON, ABYSSAL_BOSS }

class Enemy(
    val id: Int,
    var position: Vector2,
    val type: EnemyType = if (id == 99) EnemyType.ABYSSAL_BOSS else if (id % 2 == 1) EnemyType.CORRUPTED_KNIGHT else EnemyType.UNDEAD_SKELETON,
    val difficulty: String = "CURSED",
    val maxHp: Float = when (type) {
        EnemyType.ABYSSAL_BOSS -> when (difficulty.uppercase()) {
            "PENITENT" -> 400f
            "DAMNED" -> 850f
            else -> 600f
        }
        EnemyType.CORRUPTED_KNIGHT -> when (difficulty.uppercase()) {
            "DAMNED" -> 220f
            "PENITENT" -> 110f
            else -> 160f
        }
        EnemyType.UNDEAD_SKELETON -> when (difficulty.uppercase()) {
            "DAMNED" -> 150f
            "PENITENT" -> 80f
            else -> 110f
        }
    }
) {
    var hp: Float = maxHp
    var state: EnemyState = EnemyState.CHASE
    var facingDirection: Direction = Direction.S

    var attackCooldown = 0f
    private val attackRate = if (type == EnemyType.ABYSSAL_BOSS) 1.1f else 1.3f
    private val speed = if (type == EnemyType.ABYSSAL_BOSS) 150f else 135f
    private val detectionRadius = if (type == EnemyType.ABYSSAL_BOSS) 650f else 320f
    private val attackRadius = if (type == EnemyType.ABYSSAL_BOSS) 110f else 90f

    var isHitFlashing = false
    private var hitFlashTimer = 0f
    private var animTimer = 0f

    var attackAnimTimer = 0f
    var isAttacking = false
    var bossComboIndex = 0

    private var hasDealtDamageThisAttack = false
    private var drawEnemySlashTimer = 0f
    private var enemySlashPos = Vector2()
    private var enemySlashAngle = 0f

    companion object {
        private val cachedBatchColor = Color()
        private var knightTexture: Texture? = null
        private var skeletonTexture: Texture? = null
        private var bossTexture: Texture? = null
        private var enemySlashTexture: Texture? = null

        fun getKnightTexture(): Texture {
            if (knightTexture == null) {
                val pm = Pixmap(64, 64, Pixmap.Format.RGBA8888)

                pm.setColor(0f, 0f, 0f, 0.4f)
                pm.fillCircle(32, 54, 16)

                pm.setColor(Color.DARK_GRAY)
                pm.fillRectangle(20, 24, 24, 24)

                pm.setColor(Color.SLATE)
                pm.fillTriangle(14, 24, 24, 24, 20, 14)
                pm.fillTriangle(40, 24, 50, 24, 44, 14)

                pm.setColor(Color.LIGHT_GRAY)
                pm.fillRectangle(22, 10, 20, 16)

                pm.setColor(Color.FIREBRICK)
                pm.fillTriangle(18, 10, 24, 10, 16, 2)
                pm.fillTriangle(40, 10, 46, 10, 48, 2)

                pm.setColor(Color.BLACK)
                pm.fillRectangle(24, 16, 16, 6)
                pm.setColor(Color.RED)
                pm.fillRectangle(26, 17, 4, 3)
                pm.fillRectangle(34, 17, 4, 3)

                pm.setColor(Color.MAROON)
                pm.fillRectangle(8, 26, 12, 18)
                pm.setColor(Color.GOLD)
                pm.drawRectangle(8, 26, 12, 18)

                pm.setColor(Color.LIGHT_GRAY)
                pm.fillRectangle(48, 12, 5, 28)
                pm.setColor(Color.GOLD)
                pm.fillRectangle(45, 38, 11, 4)

                knightTexture = Texture(pm)
                pm.dispose()
            }
            return knightTexture!!
        }

        fun getSkeletonTexture(): Texture {
            if (skeletonTexture == null) {
                val pm = Pixmap(64, 64, Pixmap.Format.RGBA8888)

                pm.setColor(0f, 0f, 0f, 0.4f)
                pm.fillCircle(32, 54, 14)

                pm.setColor(Color(0.95f, 0.9f, 0.8f, 1f))
                pm.fillRectangle(24, 24, 16, 22)
                pm.setColor(Color.BLACK)
                pm.drawLine(24, 28, 39, 28)
                pm.drawLine(24, 34, 39, 34)
                pm.drawLine(24, 40, 39, 40)

                pm.setColor(Color.WHITE)
                pm.fillCircle(32, 16, 10)

                pm.setColor(Color.BLACK)
                pm.fillCircle(28, 16, 3)
                pm.fillCircle(36, 16, 3)

                pm.setColor(Color.RED)
                pm.drawPixel(28, 16)
                pm.drawPixel(36, 16)

                pm.setColor(Color.BROWN)
                pm.fillRectangle(46, 16, 4, 26)
                pm.setColor(Color.ORANGE)
                pm.drawRectangle(46, 16, 4, 26)

                skeletonTexture = Texture(pm)
                pm.dispose()
            }
            return skeletonTexture!!
        }

        fun getBossTexture(): Texture {
            if (bossTexture == null) {
                val pm = Pixmap(128, 128, Pixmap.Format.RGBA8888)

                pm.setColor(0f, 0f, 0f, 0.5f)
                pm.fillCircle(64, 110, 32)

                pm.setColor(Color(0.12f, 0.12f, 0.15f, 1f))
                pm.fillRectangle(40, 48, 48, 48)

                pm.setColor(Color.GOLD)
                pm.drawRectangle(40, 48, 48, 48)

                pm.setColor(Color.DARK_GRAY)
                pm.fillTriangle(28, 48, 48, 48, 38, 28)
                pm.fillTriangle(80, 48, 100, 48, 90, 28)

                pm.setColor(Color.FIREBRICK)
                pm.fillRectangle(44, 20, 40, 32)

                pm.setColor(Color.BLACK)
                pm.fillRectangle(48, 32, 32, 10)
                pm.setColor(Color.GOLD)
                pm.fillRectangle(52, 34, 8, 6)
                pm.fillRectangle(68, 34, 8, 6)

                pm.setColor(Color.MAROON)
                pm.fillRectangle(16, 52, 24, 36)
                pm.setColor(Color.GOLD)
                pm.drawRectangle(16, 52, 24, 36)

                pm.setColor(Color.GOLD)
                pm.fillRectangle(96, 24, 10, 60)
                pm.setColor(Color.FIREBRICK)
                pm.fillRectangle(90, 76, 22, 8)

                bossTexture = Texture(pm)
                pm.dispose()
            }
            return bossTexture!!
        }

        fun getEnemySlashTexture(): Texture {
            if (enemySlashTexture == null) {
                val pm = Pixmap(64, 64, Pixmap.Format.RGBA8888)
                pm.setColor(Color.RED)
                pm.fillCircle(32, 32, 28)
                pm.setColor(Color.CLEAR)
                pm.fillCircle(24, 32, 26)
                pm.setColor(Color.MAROON)
                pm.drawCircle(32, 32, 28)
                enemySlashTexture = Texture(pm)
                pm.dispose()
            }
            return enemySlashTexture!!
        }

        fun disposeTexture() {
            knightTexture?.dispose()
            knightTexture = null
            skeletonTexture?.dispose()
            skeletonTexture = null
            bossTexture?.dispose()
            bossTexture = null
            enemySlashTexture?.dispose()
            enemySlashTexture = null
        }
    }

    fun update(
        delta: Float,
        playerCenter: Vector2,
        isPlayerBlocking: Boolean,
        allEnemies: List<Enemy>,
        damageMultiplier: Float = 1.0f,
        onDealDamage: (Float, Vector2) -> Unit
    ) {
        if (state == EnemyState.DEAD) return

        animTimer += delta
        if (drawEnemySlashTimer > 0f) drawEnemySlashTimer -= delta

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= delta
            if (hitFlashTimer <= 0f) isHitFlashing = false
        }

        if (attackCooldown > 0f) attackCooldown -= delta

        val distToPlayer = position.dst(playerCenter)
        val isEnraged = (type == EnemyType.ABYSSAL_BOSS && hp <= maxHp * 0.5f)
        val effectiveSpeed = if (isEnraged) speed * 1.35f else speed

        // Body Separation vs Player
        val bodyRadius = if (type == EnemyType.ABYSSAL_BOSS) 65f else 48f
        if (distToPlayer < bodyRadius && distToPlayer > 0.01f) {
            val pushDir = Vector2(position.x - playerCenter.x, position.y - playerCenter.y).nor()
            position.add(pushDir.scl((bodyRadius - distToPlayer) * 0.5f))
        }

        // Enemy-to-Enemy Physics Separation
        for (other in allEnemies) {
            if (other !== this && other.state != EnemyState.DEAD) {
                val distToOther = position.dst(other.position)
                if (distToOther < 45f && distToOther > 0.01f) {
                    val pushDir = Vector2(position.x - other.position.x, position.y - other.position.y).nor()
                    position.add(pushDir.scl((45f - distToOther) * 0.4f))
                }
            }
        }

        val dirToPlayer = Vector2(playerCenter.x - position.x, playerCenter.y - position.y).nor()

        if (isAttacking) {
            attackAnimTimer += delta
            updateDirection(dirToPlayer)

            val totalAnimTime = if (type == EnemyType.ABYSSAL_BOSS) (if (isEnraged) 0.48f else 0.54f) else (if (isEnraged) 0.32f else 0.40f)

            // Dynamic lunge forward toward player during swing
            if (type == EnemyType.ABYSSAL_BOSS) {
                if (attackAnimTimer >= 0.15f && attackAnimTimer <= 0.38f) {
                    position.add(dirToPlayer.x * (if (isEnraged) 160f else 110f) * delta, dirToPlayer.y * (if (isEnraged) 160f else 110f) * delta)
                }
            } else {
                if (attackAnimTimer >= 0.10f && attackAnimTimer <= 0.22f) {
                    position.add(dirToPlayer.x * 70f * delta, dirToPlayer.y * 70f * delta)
                }
            }

            // Impact frame at 0.25s for Boss, 0.18s for regular enemies
            val impactTime = if (type == EnemyType.ABYSSAL_BOSS) 0.25f else 0.18f
            if (attackAnimTimer >= impactTime && !hasDealtDamageThisAttack) {
                hasDealtDamageThisAttack = true
                drawEnemySlashTimer = 0.30f

                val attackOffset = Vector2(dirToPlayer).scl(if (type == EnemyType.ABYSSAL_BOSS) 65f else 40f)
                enemySlashPos.set(position).add(attackOffset)
                enemySlashAngle = when (facingDirection) {
                    Direction.E -> 0f
                    Direction.NE -> 45f
                    Direction.N -> 90f
                    Direction.NW -> 135f
                    Direction.W -> 180f
                    Direction.SW -> 225f
                    Direction.S -> 270f
                    Direction.SE -> 315f
                }

                // SWORD TIP HITBOX COLLISION
                val hitDistance = if (type == EnemyType.ABYSSAL_BOSS) 105f else 65f
                if (enemySlashPos.dst(playerCenter) <= hitDistance || position.dst(playerCenter) <= hitDistance) {
                    val baseDamage = when (type) {
                        EnemyType.ABYSSAL_BOSS -> if (isEnraged) 42f else 30f
                        EnemyType.CORRUPTED_KNIGHT -> 22f
                        EnemyType.UNDEAD_SKELETON -> 15f
                    }
                    val scaledDamage = baseDamage * damageMultiplier

                    if (isPlayerBlocking) {
                        val staminaCost = scaledDamage * 1.3f
                        val knockback = Vector2(dirToPlayer).scl(50f)
                        onDealDamage(-staminaCost, knockback)
                    } else {
                        val knockback = Vector2(dirToPlayer).scl(50f)
                        onDealDamage(scaledDamage, knockback)
                    }
                    SoundEffects.playHit()
                }
            }

            if (attackAnimTimer >= totalAnimTime) {
                isAttacking = false
                attackAnimTimer = 0f
                attackCooldown = if (isEnraged) 0.6f else attackRate
                bossComboIndex = (bossComboIndex + 1) % 3
                state = EnemyState.CHASE
            }
        } else {
            updateDirection(dirToPlayer)

            if (distToPlayer <= attackRadius) {
                if (attackCooldown <= 0f) {
                    state = EnemyState.ATTACK
                    isAttacking = true
                    attackAnimTimer = 0f
                    hasDealtDamageThisAttack = false
                } else {
                    state = EnemyState.CHASE
                }
            } else if (distToPlayer <= detectionRadius) {
                state = EnemyState.CHASE
                position.add(dirToPlayer.x * effectiveSpeed * delta, dirToPlayer.y * effectiveSpeed * delta)
            }
        }
    }

    fun takeDamage(amount: Float, knockbackDir: Vector2? = null): Boolean {
        if (state == EnemyState.DEAD || isHitFlashing) return false
        hp = (hp - amount).coerceAtLeast(0f)
        isHitFlashing = true
        hitFlashTimer = 0.20f
        if (knockbackDir != null && knockbackDir.len() > 0.01f) {
            val kb = Vector2(knockbackDir).nor().scl(if (type == EnemyType.ABYSSAL_BOSS) 15f else 45f)
            position.add(kb)
        }
        if (hp <= 0f) {
            state = EnemyState.DEAD
            return true
        }
        return false
    }

    private fun updateDirection(dir: Vector2) {
        val angle = (dir.angleDeg() + 360f) % 360f
        facingDirection = when {
            angle >= 337.5f || angle < 22.5f -> Direction.E
            angle >= 22.5f && angle < 67.5f -> Direction.NE
            angle >= 67.5f && angle < 112.5f -> Direction.N
            angle >= 112.5f && angle < 157.5f -> Direction.NW
            angle >= 157.5f && angle < 202.5f -> Direction.W
            angle >= 202.5f && angle < 247.5f -> Direction.SW
            angle >= 247.5f && angle < 292.5f -> Direction.S
            else -> Direction.SE
        }
    }

    fun render(
        batch: SpriteBatch,
        shapeRenderer: ShapeRenderer,
        knightAnims: KnightAnimations? = null
    ) {
        if (state == EnemyState.DEAD) return

        val bobOffset = if (state == EnemyState.CHASE) {
            sin(animTimer * 10f.toDouble()).toFloat() * 4f
        } else 0f

        val scale = if (type == EnemyType.ABYSSAL_BOSS) 1.8f else 1.0f

        var frameToDraw: TextureRegion? = null
        if (knightAnims != null) {
            val isPhaseTwo = (type == EnemyType.ABYSSAL_BOSS && hp <= maxHp * 0.5f)
            val anim: Animation<TextureRegion>? = when (type) {
                EnemyType.ABYSSAL_BOSS -> knightAnims.getBossAnimation(isPhaseTwo, isAttacking, bossComboIndex, facingDirection)
                EnemyType.UNDEAD_SKELETON -> knightAnims.getSkeletonAnimation(isAttacking, facingDirection)
                else -> if (isAttacking) knightAnims.playAttack(1, facingDirection) else knightAnims.getRunAnimation(facingDirection)
            }
            val timeToSample = if (isAttacking) attackAnimTimer else animTimer
            frameToDraw = anim?.getKeyFrame(timeToSample)
        }

        val wasDrawing = batch.isDrawing
        if (!wasDrawing) batch.begin()

        cachedBatchColor.set(batch.color)

        val isEnraged = (type == EnemyType.ABYSSAL_BOSS && hp <= maxHp * 0.5f)

        if (isHitFlashing) {
            batch.color = Color.WHITE
        } else if (isEnraged) {
            batch.color = Color(1f, 0.1f, 0.1f, 1f)
        } else if (isAttacking) {
            batch.color = Color(1f, 0.2f, 0.2f, 1f)
        } else if (type == EnemyType.ABYSSAL_BOSS) {
            batch.color = Color(0.9f, 0.3f, 0.1f, 1f)
        } else if (type == EnemyType.CORRUPTED_KNIGHT) {
            batch.color = Color(0.85f, 0.25f, 0.25f, 1f)
        } else {
            batch.color = Color(0.95f, 0.85f, 0.65f, 1f)
        }

        if (frameToDraw != null) {
            batch.draw(
                frameToDraw,
                position.x - (frameToDraw.regionWidth / 2f) * scale,
                position.y - (frameToDraw.regionHeight / 2f) * scale + bobOffset,
                frameToDraw.regionWidth * scale,
                frameToDraw.regionHeight * scale
            )
        } else {
            val fallbackTex = when (type) {
                EnemyType.ABYSSAL_BOSS -> getBossTexture()
                EnemyType.CORRUPTED_KNIGHT -> getKnightTexture()
                EnemyType.UNDEAD_SKELETON -> getSkeletonTexture()
            }
            val drawSize = 64f * scale
            batch.draw(
                fallbackTex,
                position.x - drawSize / 2f,
                position.y - drawSize / 2f + bobOffset,
                drawSize,
                drawSize
            )
        }

        if (drawEnemySlashTimer > 0f) {
            val slashScale = if (type == EnemyType.ABYSSAL_BOSS) 2.0f else 1.3f
            batch.draw(
                getEnemySlashTexture(),
                enemySlashPos.x - 32f,
                enemySlashPos.y - 32f,
                32f, 32f,
                64f, 64f,
                slashScale, slashScale,
                enemySlashAngle,
                0, 0, 64, 64, false, false
            )
        }

        batch.color = cachedBatchColor
        if (!wasDrawing) batch.end()

        if (type != EnemyType.ABYSSAL_BOSS) {
            val wasShapeDrawing = shapeRenderer.isDrawing
            if (!wasShapeDrawing) shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

            shapeRenderer.color = Color.BLACK
            shapeRenderer.rect(position.x - 28f, position.y + 38f + bobOffset, 56f, 7f)

            val healthRatio = (hp / maxHp).coerceIn(0f, 1f)
            shapeRenderer.color = if (type == EnemyType.CORRUPTED_KNIGHT) Color.FIREBRICK else Color.GOLD
            shapeRenderer.rect(position.x - 28f, position.y + 38f + bobOffset, 56f * healthRatio, 7f)

            if (!wasShapeDrawing) shapeRenderer.end()
        }
    }
}
