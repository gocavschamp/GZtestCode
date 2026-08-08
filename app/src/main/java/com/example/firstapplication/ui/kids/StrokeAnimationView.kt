package com.example.firstapplication.ui.kids

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * 汉字笔画动画 View
 * 在田字格中按顺序动画演示每个汉字的笔画书写过程
 */
class StrokeAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE0E0E0.toInt()
        strokeWidth = 2f
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4FC3F7.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val animator = ValueAnimator.ofFloat(0f, 1f)
    private var currentChar: String = "一"
    private var strokeProgress = 0f
    private var activeStrokeIndex = 0

    private val fullPaths = mutableListOf<Path>()
    private val drawPaths = mutableListOf<Path>()
    private val pathMeasures = mutableListOf<PathMeasure>()

    init {
        animator.duration = 1200L
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener {
            strokeProgress = it.animatedValue as Float
            postInvalidateOnAnimation()
        }
        buildChar(currentChar)
    }

    fun setCharacter(char: String) {
        if (char == currentChar) return
        currentChar = char
        buildChar(char)
        startAnimation()
    }

    fun getCharacter(): String = currentChar

    fun startAnimation() {
        animator.cancel()
        activeStrokeIndex = 0
        strokeProgress = 0f
        animator.start()
        invalidate()
    }

    private fun buildChar(char: String) {
        fullPaths.clear()
        drawPaths.clear()
        pathMeasures.clear()

        val strokes = StrokeData.charStrokes[char] ?: StrokeData.charStrokes["一"]!!
        for (points in strokes) {
            val p = Path()
            p.moveTo(points[0].first, points[0].second)
            for (i in 1 until points.size) {
                p.lineTo(points[i].first, points[i].second)
            }
            fullPaths.add(p)
            drawPaths.add(Path())
            pathMeasures.add(PathMeasure(p, false))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val size = minOf(w, h) * 0.9f
        val left = (w - size) / 2f
        val top = (h - size) / 2f

        // 田字格
        canvas.save()
        canvas.translate(left, top)
        canvas.drawRect(0f, 0f, size, size, gridPaint)
        gridPaint.strokeWidth = 1.5f
        gridPaint.color = 0xFFEEEEEE.toInt()
        canvas.drawLine(size / 2, 0f, size / 2, size, gridPaint)
        canvas.drawLine(0f, size / 2, size, size / 2, gridPaint)

        // 缩放笔画到格子内（笔画坐标 0-100 归一化）
        val scale = size / 100f
        canvas.scale(scale, scale)

        // 已完成的笔画 + 当前动画笔画
        for (i in fullPaths.indices) {
            val target = if (i < activeStrokeIndex) 1f
            else if (i == activeStrokeIndex) strokeProgress
            else 0f
            if (target <= 0f) continue
            val pm = pathMeasures[i]
            val len = pm.length
            val draw = drawPaths[i]
            draw.reset()
            if (target >= 1f) {
                draw.addPath(fullPaths[i])
            } else {
                pm.getSegment(0f, len * target, draw, true)
            }
            canvas.drawPath(draw, strokePaint)
        }
        canvas.restore()

        // 当前笔画完成后自动进入下一笔画
        if (strokeProgress >= 1f && activeStrokeIndex < fullPaths.size - 1) {
            activeStrokeIndex++
            strokeProgress = 0f
            animator.start()
        }
        if (strokeProgress >= 1f && activeStrokeIndex == fullPaths.size - 1) {
            // 全部完成，停止
            animator.cancel()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    /** 预设常用汉字的笔画数据（坐标 0-100 归一化） */
    object StrokeData {
        private val stroke = { pts: Array<Pair<Float, Float>> -> pts.toList() }

        val charStrokes: Map<String, List<List<Pair<Float, Float>>>> = mapOf(
            "一" to listOf(stroke(arrayOf(10f to 50f, 90f to 50f))),
            "二" to listOf(
                stroke(arrayOf(30f to 30f, 70f to 30f)),
                stroke(arrayOf(15f to 70f, 85f to 70f))
            ),
            "三" to listOf(
                stroke(arrayOf(35f to 20f, 65f to 20f)),
                stroke(arrayOf(20f to 50f, 80f to 50f)),
                stroke(arrayOf(12f to 80f, 88f to 80f))
            ),
            "十" to listOf(
                stroke(arrayOf(12f to 50f, 88f to 50f)),
                stroke(arrayOf(50f to 12f, 50f to 88f))
            ),
            "人" to listOf(
                stroke(arrayOf(50f to 12f, 30f to 80f)),
                stroke(arrayOf(50f to 12f, 72f to 80f))
            ),
            "口" to listOf(
                stroke(arrayOf(25f to 30f, 75f to 30f, 75f to 75f, 25f to 75f, 25f to 30f))
            ),
            "月" to listOf(
                stroke(arrayOf(30f to 12f, 28f to 45f)),
                stroke(arrayOf(30f to 20f, 70f to 20f, 70f to 85f)),
                stroke(arrayOf(30f to 48f, 70f to 48f)),
                stroke(arrayOf(30f to 68f, 70f to 68f))
            ),
            "田" to listOf(
                stroke(arrayOf(25f to 20f, 25f to 80f)),
                stroke(arrayOf(25f to 20f, 75f to 20f, 75f to 80f)),
                stroke(arrayOf(25f to 80f, 75f to 80f)),
                stroke(arrayOf(50f to 20f, 50f to 80f)),
                stroke(arrayOf(25f to 50f, 75f to 50f))
            ),
            "日" to listOf(
                stroke(arrayOf(28f to 20f, 72f to 20f, 72f to 80f, 28f to 80f, 28f to 20f)),
                stroke(arrayOf(28f to 50f, 72f to 50f))
            ),
            "山" to listOf(
                stroke(arrayOf(30f to 20f, 30f to 65f)),
                stroke(arrayOf(30f to 65f, 70f to 65f)),
                stroke(arrayOf(70f to 20f, 70f to 65f))
            ),
            "大" to listOf(
                stroke(arrayOf(15f to 35f, 85f to 35f)),
                stroke(arrayOf(50f to 35f, 28f to 85f)),
                stroke(arrayOf(50f to 35f, 75f to 85f))
            ),
            "天" to listOf(
                stroke(arrayOf(20f to 20f, 80f to 20f)),
                stroke(arrayOf(50f to 20f, 50f to 60f)),
                stroke(arrayOf(15f to 60f, 85f to 60f)),
                stroke(arrayOf(50f to 60f, 30f to 90f)),
                stroke(arrayOf(50f to 60f, 72f to 90f))
            ),
            "小" to listOf(
                stroke(arrayOf(28f to 15f, 28f to 75f)),
                stroke(arrayOf(50f to 25f, 50f to 90f)),
                stroke(arrayOf(72f to 15f, 72f to 75f))
            ),
            "木" to listOf(
                stroke(arrayOf(20f to 80f, 80f to 80f)),
                stroke(arrayOf(50f to 10f, 50f to 88f)),
                stroke(arrayOf(50f to 45f, 25f to 70f)),
                stroke(arrayOf(50f to 45f, 75f to 70f))
            ),
            "上" to listOf(
                stroke(arrayOf(25f to 60f, 25f to 25f, 75f to 25f)),
                stroke(arrayOf(50f to 25f, 50f to 85f)),
                stroke(arrayOf(20f to 85f, 80f to 85f))
            ),
            "下" to listOf(
                stroke(arrayOf(25f to 30f, 75f to 30f)),
                stroke(arrayOf(50f to 30f, 50f to 85f)),
                stroke(arrayOf(35f to 55f, 20f to 70f, 15f to 65f, 25f to 55f, 35f to 55f))
            ),
            "四" to listOf(
                stroke(arrayOf(25f to 20f, 75f to 20f, 75f to 75f, 25f to 75f, 25f to 20f)),
                stroke(arrayOf(28f to 45f, 45f to 62f)),
                stroke(arrayOf(72f to 45f, 55f to 62f)),
                stroke(arrayOf(25f to 78f, 75f to 78f))
            ),
            "五" to listOf(
                stroke(arrayOf(15f to 25f, 85f to 25f)),
                stroke(arrayOf(30f to 25f, 30f to 70f)),
                stroke(arrayOf(30f to 45f, 70f to 45f, 70f to 70f)),
                stroke(arrayOf(15f to 75f, 85f to 75f))
            ),
            "六" to listOf(
                stroke(arrayOf(40f to 12f, 40f to 30f)),
                stroke(arrayOf(15f to 55f, 85f to 55f)),
                stroke(arrayOf(30f to 55f, 15f to 85f)),
                stroke(arrayOf(70f to 55f, 85f to 85f))
            ),
            "七" to listOf(
                stroke(arrayOf(15f to 30f, 85f to 30f)),
                stroke(arrayOf(45f to 30f, 45f to 75f, 75f to 75f))
            ),
            "八" to listOf(
                stroke(arrayOf(35f to 20f, 20f to 80f)),
                stroke(arrayOf(65f to 20f, 80f to 80f))
            ),
            "九" to listOf(
                stroke(arrayOf(45f to 15f, 25f to 60f)),
                stroke(arrayOf(25f to 60f, 75f to 60f, 75f to 85f))
            ),
            "中" to listOf(
                stroke(arrayOf(30f to 30f, 70f to 30f, 70f to 70f, 30f to 70f, 30f to 30f)),
                stroke(arrayOf(50f to 10f, 50f to 90f)),
                stroke(arrayOf(20f to 90f, 80f to 90f))
            ),
            "王" to listOf(
                stroke(arrayOf(15f to 20f, 85f to 20f)),
                stroke(arrayOf(50f to 20f, 50f to 80f)),
                stroke(arrayOf(15f to 50f, 85f to 50f)),
                stroke(arrayOf(15f to 80f, 85f to 80f))
            ),
            "土" to listOf(
                stroke(arrayOf(15f to 30f, 85f to 30f)),
                stroke(arrayOf(50f to 30f, 50f to 85f)),
                stroke(arrayOf(25f to 85f, 75f to 85f))
            ),
            "火" to listOf(
                stroke(arrayOf(45f to 12f, 45f to 28f)),
                stroke(arrayOf(50f to 25f, 25f to 60f)),
                stroke(arrayOf(25f to 60f, 20f to 88f)),
                stroke(arrayOf(50f to 25f, 80f to 88f))
            ),
            "水" to listOf(
                stroke(arrayOf(50f to 15f, 50f to 75f)),
                stroke(arrayOf(50f to 40f, 75f to 20f, 75f to 40f)),
                stroke(arrayOf(50f to 40f, 25f to 70f)),
                stroke(arrayOf(50f to 50f, 80f to 88f))
            ),
            "门" to listOf(
                stroke(arrayOf(30f to 12f, 30f to 28f)),
                stroke(arrayOf(35f to 20f, 35f to 80f)),
                stroke(arrayOf(35f to 80f, 75f to 80f, 75f to 20f))
            ),
            "牛" to listOf(
                stroke(arrayOf(25f to 15f, 35f to 45f)),
                stroke(arrayOf(15f to 35f, 85f to 35f)),
                stroke(arrayOf(50f to 15f, 50f to 85f)),
                stroke(arrayOf(15f to 60f, 85f to 60f))
            ),
            "马" to listOf(
                stroke(arrayOf(30f to 20f, 30f to 55f)),
                stroke(arrayOf(30f to 55f, 70f to 55f, 70f to 20f)),
                stroke(arrayOf(20f to 75f, 80f to 75f))
            ),
            "手" to listOf(
                stroke(arrayOf(25f to 15f, 35f to 40f)),
                stroke(arrayOf(15f to 45f, 85f to 45f)),
                stroke(arrayOf(50f to 40f, 50f to 85f)),
                stroke(arrayOf(15f to 70f, 85f to 70f))
            ),
            "头" to listOf(
                stroke(arrayOf(45f to 12f, 45f to 28f)),
                stroke(arrayOf(65f to 12f, 65f to 28f)),
                stroke(arrayOf(25f to 35f, 80f to 35f)),
                stroke(arrayOf(50f to 35f, 30f to 80f)),
                stroke(arrayOf(50f to 35f, 75f to 80f))
            ),
            "目" to listOf(
                stroke(arrayOf(25f to 20f, 25f to 80f)),
                stroke(arrayOf(25f to 20f, 75f to 20f, 75f to 80f)),
                stroke(arrayOf(25f to 45f, 75f to 45f)),
                stroke(arrayOf(25f to 65f, 75f to 65f)),
                stroke(arrayOf(25f to 80f, 75f to 80f))
            ),
            "耳" to listOf(
                stroke(arrayOf(20f to 15f, 80f to 15f)),
                stroke(arrayOf(25f to 15f, 25f to 80f)),
                stroke(arrayOf(75f to 15f, 75f to 80f)),
                stroke(arrayOf(20f to 40f, 80f to 40f)),
                stroke(arrayOf(20f to 65f, 80f to 65f)),
                stroke(arrayOf(20f to 80f, 80f to 80f))
            )
        )
    }
}
