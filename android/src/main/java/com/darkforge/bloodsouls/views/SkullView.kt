package com.darkforge.bloodsouls.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class SkullView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val skullPath = Path()
    private val jawPath = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w <= 0 || h <= 0) return

        val fw = w.toFloat()
        val fh = h.toFloat()

        skullPath.reset()
        skullPath.moveTo(fw * .5f, fh * .09f)
        skullPath.cubicTo(fw * .28f, fh * .09f, fw * .16f, fh * .25f, fw * .16f, fh * .44f)
        skullPath.cubicTo(fw * .16f, fh * .56f, fw * .22f, fh * .66f, fw * .31f, fh * .72f)
        skullPath.lineTo(fw * .31f, fh * .84f)
        skullPath.lineTo(fw * .69f, fh * .84f)
        skullPath.lineTo(fw * .69f, fh * .72f)
        skullPath.cubicTo(fw * .78f, fh * .66f, fw * .84f, fh * .56f, fw * .84f, fh * .44f)
        skullPath.cubicTo(fw * .84f, fh * .25f, fw * .72f, fh * .09f, fw * .5f, fh * .09f)
        skullPath.close()

        jawPath.reset()
        jawPath.moveTo(fw * .31f, fh * .68f)
        jawPath.quadTo(fw * .5f, fh * .63f, fw * .69f, fh * .68f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0f || h <= 0f) return

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(26, 0, 8)
        canvas.drawPath(skullPath, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = Color.rgb(139, 0, 0)
        canvas.drawPath(skullPath, paint)

        drawEye(canvas, w * .375f, h * .44f, w, h)
        drawEye(canvas, w * .625f, h * .44f, w, h)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.argb(128, 139, 0, 0)
        canvas.drawPath(jawPath, paint)
    }

    private fun drawEye(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawCircle(x, y, w * .09f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.argb(230, 139, 0, 0)
        canvas.drawCircle(x, y, w * .09f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(204, 204, 0, 0)
        canvas.drawCircle(x, y + h * .03f, w * .031f, paint)
    }
}
