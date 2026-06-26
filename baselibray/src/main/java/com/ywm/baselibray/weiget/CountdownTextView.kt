package com.ywm.baselibray.weiget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.animation.addListener
import com.ywm.baselibray.R
import kotlin.math.max

open class CountdownTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var progress: Float = 0f
    private var progressColor: Int = Color.GREEN
    private var progressWidth: Float = 4f
    private var cornerRadius: Float = 0f

    private var progressAnimator: ValueAnimator? = null
    private var onCountdownFinish: (() -> Unit)? = null

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val roundRectPath = Path()
    private val pathMeasure = PathMeasure()
    private val progressPath = Path()

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.CountdownTextView, 0, 0).apply {
            try {
                progressColor = getColor(R.styleable.CountdownTextView_progressColor, progressColor)
                progressWidth = getDimension(R.styleable.CountdownTextView_progressWidth, progressWidth)
                cornerRadius = getDimension(R.styleable.CountdownTextView_cornerRadius, cornerRadius)
            } finally {
                recycle()
            }
        }
        progressPaint.strokeWidth = progressWidth
        progressPaint.color = progressColor
    }

    fun setOnCountdownFinish(listener: () -> Unit) {
        onCountdownFinish = listener
    }

    fun startCountdown(durationMillis: Long) {
        stopCountdown()
        progress = 0f
        progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMillis
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = animatedValue as Float
                invalidate()
            }
            addListener(onEnd = {
                onCountdownFinish?.invoke()
            })
            start()
        }
    }

    fun resetCountdown() {
        stopCountdown()
        progress = 0f
        invalidate()
    }

    private fun stopCountdown() {
        progressAnimator?.cancel()
        progressAnimator = null
    }

    fun isCountdownRunning(): Boolean = progressAnimator?.isRunning == true
    fun getProgress(): Float = progress
    fun setProgressColor(color: Int) {
        progressColor = color
        progressPaint.color = color
        invalidate()
    }

    fun setProgressWidth(widthPx: Float) {
        progressWidth = widthPx
        progressPaint.strokeWidth = widthPx
        invalidate()
    }

    fun setCornerRadius(radiusPx: Float) {
        cornerRadius = radiusPx
        invalidate()
    }

    fun setCornerRadiusDp(radiusDp: Float) {
        cornerRadius = radiusDp * resources.displayMetrics.density
        invalidate()
    }

    fun getCornerRadius(): Float = cornerRadius

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progress <= 0f || progress >= 1f) return
        drawProgressBorder(canvas)
    }

    private fun drawProgressBorder(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val halfStroke = progressWidth / 2f
        val adjustedRadius = max(0f, cornerRadius - halfStroke)

        roundRectPath.reset()
        roundRectPath.addRoundRect(
            halfStroke, halfStroke, w - halfStroke, h - halfStroke,
            adjustedRadius, adjustedRadius, Path.Direction.CW
        )

        pathMeasure.setPath(roundRectPath, false)
        val perimeter = pathMeasure.length

        progressPath.reset()
        pathMeasure.getSegment(0f, progress * perimeter, progressPath, true)

        canvas.drawPath(progressPath, progressPaint)
    }

    override fun onDetachedFromWindow() {
        stopCountdown()
        super.onDetachedFromWindow()
    }
}
