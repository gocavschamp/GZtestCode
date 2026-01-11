package com.ywm.baselibray.weiget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.core.graphics.toColorInt

class ShineDrawable : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 0f, 0f,
            intArrayOf(
                Color.TRANSPARENT,
                "#80ffffff".toColorInt(),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.3f, 0.5f, 0.7f),
            Shader.TileMode.CLAMP
        )
    }

    private var offsetX = 0f
    private var offsetY = 0f
    private var gradientWidth = 50f

    fun setOffset(x: Float, y: Float) {
        offsetX = x
        offsetY = y
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        paint.shader = LinearGradient(
            offsetX - gradientWidth, offsetY,
            offsetX + gradientWidth, offsetY,
            intArrayOf(
                Color.TRANSPARENT,
                "#aeffffff".toColorInt(),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.1f, 0.5f, 0.9f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(bounds, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}