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
    /** 基础笔画对比字模式：当前笔画高亮色 */
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF7043.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    /** 基础笔画对比字模式：字中其余笔画灰色轮廓 */
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFCFD8DC.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val animator = ValueAnimator.ofFloat(0f, 1f)
    private var currentChar: String = "一"
    private var strokeProgress = 0f
    private var activeStrokeIndex = 0
    /** 基础笔画对比字模式下高亮的笔画下标；-1 表示普通模式 */
    private var compareStrokeIndex = -1

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
        if (char == currentChar && compareStrokeIndex < 0) return
        compareStrokeIndex = -1
        currentChar = char
        buildChar(char)
        startAnimation()
    }

    fun getCharacter(): String = currentChar

    /**
     * 基础笔画教学：演示笔画写法的同时显示一个包含该笔画的字。
     * 字中当前笔画用高亮色动画绘制，其余笔画灰色轮廓（不高亮），便于理解笔画在字中的位置。
     */
    fun showStrokeInChar(strokeName: String) {
        val target = StrokeData.exampleChars[strokeName]
        currentChar = if (target != null) target.first else strokeName
        compareStrokeIndex = target?.second ?: -1
        buildChar(currentChar)
        startAnimation()
    }

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

        if (compareStrokeIndex >= 0) {
            // 对比字模式：其余笔画灰色轮廓，当前笔画橙色高亮动画
            for (i in fullPaths.indices) {
                if (i == compareStrokeIndex) continue
                canvas.drawPath(fullPaths[i], outlinePaint)
            }
            val pm = pathMeasures[compareStrokeIndex]
            val draw = drawPaths[compareStrokeIndex]
            draw.reset()
            if (strokeProgress >= 1f) {
                draw.addPath(fullPaths[compareStrokeIndex])
            } else {
                pm.getSegment(0f, pm.length * strokeProgress, draw, true)
            }
            canvas.drawPath(draw, highlightPaint)
        } else {
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
        }
        canvas.restore()

        if (compareStrokeIndex >= 0) {
            // 对比字模式只动画高亮的那一笔，画完即停
            if (strokeProgress >= 1f) {
                animator.cancel()
            }
        } else {
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
            ),
            // —— 基础笔画（基础笔画教学页用）——
            "横" to listOf(stroke(arrayOf(15f to 50f, 85f to 50f))),
            "竖" to listOf(stroke(arrayOf(50f to 15f, 50f to 85f))),
            "撇" to listOf(stroke(arrayOf(60f to 15f, 30f to 85f))),
            "捺" to listOf(stroke(arrayOf(40f to 15f, 70f to 85f))),
            "点" to listOf(stroke(arrayOf(50f to 25f, 45f to 60f))),
            "提" to listOf(stroke(arrayOf(20f to 70f, 80f to 30f))),
            "横折" to listOf(stroke(arrayOf(15f to 50f, 70f to 50f, 70f to 75f))),
            "竖钩" to listOf(stroke(arrayOf(50f to 15f, 50f to 70f, 62f to 65f))),
            "横钩" to listOf(stroke(arrayOf(15f to 50f, 70f to 50f, 76f to 38f))),
            "横折钩" to listOf(stroke(arrayOf(15f to 50f, 70f to 50f, 70f to 72f, 78f to 66f))),
            "横撇" to listOf(stroke(arrayOf(15f to 50f, 70f to 50f, 48f to 88f))),
            "横折弯钩" to listOf(stroke(arrayOf(15f to 40f, 70f to 40f, 70f to 62f, 85f to 70f))),
            "横折提" to listOf(stroke(arrayOf(15f to 65f, 62f to 65f, 62f to 50f, 76f to 42f))),
            "竖提" to listOf(stroke(arrayOf(50f to 15f, 50f to 72f, 66f to 62f))),
            "竖弯" to listOf(stroke(arrayOf(50f to 15f, 50f to 62f, 80f to 62f))),
            "竖弯钩" to listOf(stroke(arrayOf(50f to 15f, 50f to 62f, 80f to 62f, 86f to 50f))),
            "竖折" to listOf(stroke(arrayOf(50f to 15f, 50f to 60f, 85f to 60f))),
            "撇折" to listOf(stroke(arrayOf(60f to 15f, 32f to 55f, 68f to 72f))),
            "撇点" to listOf(stroke(arrayOf(62f to 12f, 34f to 55f, 58f to 75f))),
            "斜钩" to listOf(stroke(arrayOf(42f to 12f, 58f to 48f, 50f to 85f))),
            "卧钩" to listOf(stroke(arrayOf(28f to 72f, 52f to 74f, 64f to 62f))),
            "弯钩" to listOf(stroke(arrayOf(52f to 15f, 42f to 50f, 52f to 82f))),
            // —— 基础笔画对比字（习/买/计/长/云/女/戈/心/子）——
            "习" to listOf(
                stroke(arrayOf(15f to 55f, 70f to 55f, 70f to 78f)),
                stroke(arrayOf(72f to 28f, 64f to 42f)),
                stroke(arrayOf(20f to 86f, 70f to 72f))
            ),
            "买" to listOf(
                stroke(arrayOf(15f to 28f, 72f to 28f, 78f to 18f)),
                stroke(arrayOf(45f to 38f, 40f to 52f)),
                stroke(arrayOf(62f to 38f, 66f to 52f)),
                stroke(arrayOf(45f to 38f, 78f to 86f))
            ),
            "计" to listOf(
                stroke(arrayOf(24f to 18f, 18f to 34f)),
                stroke(arrayOf(32f to 46f, 66f to 46f, 66f to 62f, 80f to 54f)),
                stroke(arrayOf(72f to 46f, 72f to 86f))
            ),
            "长" to listOf(
                stroke(arrayOf(36f to 10f, 26f to 38f)),
                stroke(arrayOf(26f to 42f, 78f to 42f)),
                stroke(arrayOf(46f to 42f, 46f to 80f, 60f to 70f)),
                stroke(arrayOf(62f to 55f, 82f to 84f))
            ),
            "云" to listOf(
                stroke(arrayOf(15f to 24f, 85f to 24f)),
                stroke(arrayOf(20f to 50f, 80f to 50f)),
                stroke(arrayOf(40f to 50f, 30f to 80f, 72f to 80f))
            ),
            "女" to listOf(
                stroke(arrayOf(50f to 10f, 25f to 55f, 48f to 70f)),
                stroke(arrayOf(52f to 10f, 78f to 62f)),
                stroke(arrayOf(20f to 80f, 82f to 80f))
            ),
            "戈" to listOf(
                stroke(arrayOf(15f to 34f, 70f to 34f)),
                stroke(arrayOf(28f to 34f, 48f to 55f, 58f to 84f, 68f to 76f)),
                stroke(arrayOf(42f to 34f, 22f to 84f)),
                stroke(arrayOf(72f to 28f, 84f to 40f))
            ),
            "心" to listOf(
                stroke(arrayOf(26f to 26f, 20f to 40f)),
                stroke(arrayOf(28f to 62f, 52f to 68f, 70f to 56f)),
                stroke(arrayOf(56f to 46f, 64f to 58f)),
                stroke(arrayOf(72f to 50f, 80f to 60f))
            ),
            "子" to listOf(
                stroke(arrayOf(28f to 20f, 74f to 20f, 52f to 48f)),
                stroke(arrayOf(52f to 48f, 42f to 74f, 56f to 86f)),
                stroke(arrayOf(14f to 86f, 86f to 86f))
            )
        )

        /**
         * 基础笔画 → 对比字 + 该字中对应笔画下标（基础笔画教学页动画演示并高亮该笔画）
         * 对比字必须存在于 charStrokes 中
         */
        val exampleChars: Map<String, Pair<String, Int>> = mapOf(
            "横" to ("二" to 0),
            "竖" to ("十" to 1),
            "撇" to ("人" to 0),
            "捺" to ("人" to 1),
            "点" to ("六" to 0),
            "提" to ("习" to 2),
            "横折" to ("口" to 0),
            "竖钩" to ("小" to 0),
            "横钩" to ("买" to 0),
            "横折钩" to ("月" to 1),
            "横撇" to ("水" to 1),
            "横折弯钩" to ("九" to 1),
            "横折提" to ("计" to 1),
            "竖提" to ("长" to 2),
            "竖弯" to ("四" to 2),
            "竖弯钩" to ("七" to 1),
            "竖折" to ("山" to 1),
            "撇折" to ("云" to 2),
            "撇点" to ("女" to 0),
            "斜钩" to ("戈" to 1),
            "卧钩" to ("心" to 1),
            "弯钩" to ("子" to 1)
        )
    }
}
