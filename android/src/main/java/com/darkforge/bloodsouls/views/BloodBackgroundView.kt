package com.darkforge.bloodsouls.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.random.Random

class BloodBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Ember(
        var x: Float,
        var y: Float,
        var speed: Float,
        var radius: Float,
        var alpha: Int,
        var color: Int
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val embers = mutableListOf<Ember>()

    private var lastTime = System.currentTimeMillis()
    private var emberTimer = 0f

    private var bgGradient: RadialGradient? = null
    private var vignetteGradient: RadialGradient? = null

    private val drips = floatArrayOf(
        45f, 120f, 240f, 380f, 520f, 640f, 760f, 850f
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w <= 0 || h <= 0) return

        val radius = max(w.toFloat(), h.toFloat()) * 1.2f
        bgGradient = RadialGradient(
            0f, 0f, radius,
            intArrayOf(
                Color.rgb(26, 0, 10),
                Color.rgb(10, 0, 5),
                Color.BLACK
            ),
            null,
            Shader.TileMode.CLAMP
        )

        vignetteGradient = RadialGradient(
            w / 2f, h / 2f, w * 0.8f,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb(190, 0, 0, 0)
            ),
            null,
            Shader.TileMode.CLAMP
        )

        if (embers.isEmpty()) {
            repeat(20) {
                createEmber(randomY = true)
            }
        }
    }

    private fun createEmber(randomY: Boolean = false) {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)

        val ember = Ember(
            x = Random.nextFloat() * w,
            y = if (randomY) Random.nextFloat() * h else h.toFloat() + 10f,
            speed = 20f + Random.nextFloat() * 50f,
            radius = 2f + Random.nextFloat() * 3f,
            alpha = 80 + Random.nextInt(120),
            color = if (Random.nextBoolean()) Color.rgb(255, 102, 0) else Color.rgb(204, 34, 0)
        )

        embers.add(ember)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0f || h <= 0f) return

        drawBackground(canvas, w, h)
        drawEmbers(canvas, w, h)
        drawRunicBorder(canvas, w, h)
        drawBloodDrips(canvas, w, h)
        drawVignette(canvas, w, h)

        postInvalidateOnAnimation()
    }

    private fun drawBackground(canvas: Canvas, w: Float, h: Float) {
        bgGradient?.let {
            paint.shader = it
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
        } ?: run {
            canvas.drawColor(Color.rgb(10, 0, 5))
        }
    }

    private fun drawEmbers(canvas: Canvas, w: Float, h: Float) {
        val now = System.currentTimeMillis()
        val delta = (now - lastTime) / 1000f
        lastTime = now

        emberTimer += delta

        if (emberTimer > 0.35f) {
            emberTimer = 0f
            if (embers.size < 20) {
                createEmber()
            }
        }

        val iterator = embers.iterator()
        while (iterator.hasNext()) {
            val ember = iterator.next()
            ember.y -= ember.speed * delta

            val fade = (ember.y / h).coerceIn(0f, 1f)
            val alpha = (ember.alpha * fade).toInt().coerceIn(0, 255)

            paint.color = Color.argb(
                alpha,
                Color.red(ember.color),
                Color.green(ember.color),
                Color.blue(ember.color)
            )

            paint.style = Paint.Style.FILL
            canvas.drawCircle(ember.x, ember.y, ember.radius, paint)

            if (ember.y < -20f) {
                iterator.remove()
            }
        }
    }

    private fun drawRunicBorder(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE

        paint.strokeWidth = 2f
        paint.color = Color.argb(50, 139, 0, 0)
        canvas.drawRect(20f, 20f, w - 20f, h - 20f, paint)

        paint.strokeWidth = 1f
        paint.color = Color.argb(38, 74, 0, 0)
        canvas.drawRect(30f, 30f, w - 30f, h - 30f, paint)

        paint.strokeWidth = 2.5f
        paint.color = Color.argb(128, 139, 0, 0)

        drawCorner(canvas, 20f, 20f, w, h)
        drawCorner(canvas, w - 20f, 20f, w, h)
        drawCorner(canvas, 20f, h - 20f, w, h)
        drawCorner(canvas, w - 20f, h - 20f, w, h)

        paint.style = Paint.Style.FILL
    }

    private fun drawCorner(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        if (x < w / 2) {
            canvas.drawLine(x, y, x + 40f, y, paint)
        } else {
            canvas.drawLine(x, y, x - 40f, y, paint)
        }

        if (y < h / 2) {
            canvas.drawLine(x, y, x, y + 35f, paint)
        } else {
            canvas.drawLine(x, y, x, y - 35f, paint)
        }
    }

    private fun drawBloodDrips(canvas: Canvas, w: Float, h: Float) {
        drips.forEachIndexed { index, x ->
            if (x > w) return@forEachIndexed

            val length = 20f + (index % 3) * 12f

            paint.color = Color.argb(70, 139, 0, 0)
            paint.strokeWidth = 2.5f

            canvas.drawLine(x, 0f, x + 2f, length, paint)
            canvas.drawCircle(x + 2f, length + 4f, 4f, paint)
        }
    }

    private fun drawVignette(canvas: Canvas, w: Float, h: Float) {
        vignetteGradient?.let {
            paint.shader = it
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
        }
    }
}
