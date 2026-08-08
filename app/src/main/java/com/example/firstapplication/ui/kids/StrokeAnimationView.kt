package com.example.firstapplication.ui.kids

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.PathParser

/**
 * 汉字笔画动画 View
 * 在田字格中按顺序动画演示每个汉字的笔画书写过程
 *
 * 笔画数据来源：hanzi-writer-data（Make Me a Hanzi 数据集，Arphic Public License）
 * assets/hanzi_strokes.json 中每个字保存：
 *  - strokes：每笔画的 SVG 轮廓（绘制灰底"字体"，保证笔画与真实字形一致）
 *  - medians：每笔画的坐标集合（按集合逐步 draw，实现书写动画）
 * 坐标为 0-1024 归一化，绘制时需 Y 翻转
 */
class StrokeAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 田字格边框 */
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE0E0E0.toInt()
        strokeWidth = 2f
    }
    /** 底字（全字灰色轮廓，与真实字形一致） */
    private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFEAECEE.toInt()
        style = Paint.Style.FILL
    }
    /** 已完成笔画：淡蓝填充 */
    private val donePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x4D4FC3F7.toInt()
        style = Paint.Style.FILL
    }
    /** 当前笔画动画色（普通模式，蓝色中线） */
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4FC3F7.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 100f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    /** 基础笔画对比字模式：当前笔画高亮色（橙色中线） */
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF7043.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 100f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    /** 基础笔画对比字模式：当前笔画写完后橙色填充 */
    private val highlightFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF7043.toInt()
        style = Paint.Style.FILL
    }
    /** 基础笔画对比字模式：字中其余笔画灰色轮廓（略深，突出当前高亮笔画） */
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB0B6BC.toInt()
        style = Paint.Style.FILL
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f)
    private var currentChar: String = "一"
    private var strokeProgress = 0f
    private var activeStrokeIndex = 0
    /** 基础笔画对比字模式下高亮的笔画下标；-1 表示普通模式 */
    private var compareStrokeIndex = -1

    /** 每个字解析后的轮廓 + 中线 + 中线测量 */
    private data class CharPaths(
        val outlines: List<Path>,
        val medians: List<Path>,
        val measures: List<PathMeasure>
    )

    private var paths: CharPaths? = null
    private val charCache = HashMap<String, CharPaths>()

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
        if (!buildChar(char)) {
            currentChar = "一"
            buildChar(currentChar)
        }
        startAnimation()
    }

    fun getCharacter(): String = currentChar

    /**
     * 基础笔画教学：演示笔画写法的同时显示一个包含该笔画的字。
     * 字中当前笔画用高亮色动画绘制，其余笔画灰色轮廓（不高亮），便于理解笔画在字中的位置。
     */
    fun showStrokeInChar(strokeName: String) {
        val target = StrokeData.exampleChars[strokeName]
        currentChar = target?.first ?: strokeName
        compareStrokeIndex = target?.second ?: -1
        if (!buildChar(currentChar)) {
            currentChar = "一"
            compareStrokeIndex = -1
            buildChar(currentChar)
        }
        startAnimation()
    }

    fun startAnimation() {
        animator.cancel()
        activeStrokeIndex = 0
        strokeProgress = 0f
        animator.start()
        invalidate()
    }

    /** 解析该字的真实笔画数据；失败返回 false */
    private fun buildChar(char: String): Boolean {
        charCache[char]?.let {
            paths = it
            return true
        }
        HanziStrokeData.load(context)
        val svgs = HanziStrokeData.strokesOf(char) ?: return false
        val medians = HanziStrokeData.mediansOf(char) ?: return false

        val outlines = svgs.map { PathParser.createPathFromPathData(it) }
        val medianPaths = medians.map { pts ->
            Path().apply {
                moveTo(pts[0][0], pts[0][1])
                // 中点二次贝塞尔平滑，起笔落笔更圆润
                for (i in 1 until pts.size - 1) {
                    val mx = (pts[i][0] + pts[i + 1][0]) / 2f
                    val my = (pts[i][1] + pts[i + 1][1]) / 2f
                    quadTo(pts[i][0], pts[i][1], mx, my)
                }
                lineTo(pts.last()[0], pts.last()[1])
            }
        }
        val cp = CharPaths(outlines, medianPaths, medianPaths.map { PathMeasure(it, false) })
        charCache[char] = cp
        paths = cp
        return true
    }

    /** 沿笔画坐标集合（中线）绘制到 progress 处 */
    private fun drawMedianSegment(
        canvas: Canvas,
        cp: CharPaths,
        index: Int,
        progress: Float,
        paint: Paint
    ) {
        if (progress <= 0f) return
        val pm = cp.measures[index]
        val seg = Path()
        if (progress >= 1f || pm.length <= 0f) {
            canvas.drawPath(cp.medians[index], paint)
        } else {
            pm.getSegment(0f, pm.length * progress, seg, true)
            canvas.drawPath(seg, paint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val size = minOf(w, h) * 0.9f
        val left = (w - size) / 2f
        val top = (h - size) / 2f
        val cp = paths ?: return

        canvas.save()
        canvas.translate(left, top)

        // 田字格（像素单位）
        canvas.drawRect(0f, 0f, size, size, gridPaint)
        gridPaint.strokeWidth = 1.5f
        gridPaint.color = 0xFFEEEEEE.toInt()
        canvas.drawLine(size / 2, 0f, size / 2, size, gridPaint)
        canvas.drawLine(0f, size / 2, size, size / 2, gridPaint)
        gridPaint.strokeWidth = 2f
        gridPaint.color = 0xFFE0E0E0.toInt()

        // 笔画数据空间：0-1024 归一化 + Y 翻转（SVG 数据绘制时需上下翻转）
        canvas.translate(0f, size)
        canvas.scale(size / 1024f, -size / 1024f)

        if (compareStrokeIndex >= 0) {
            // 对比字模式：其余笔画灰色轮廓，当前笔画橙色高亮动画
            val target = compareStrokeIndex
            for (i in cp.outlines.indices) {
                if (i == target) continue
                canvas.drawPath(cp.outlines[i], outlinePaint)
            }
            if (strokeProgress >= 1f) {
                canvas.drawPath(cp.outlines[target], highlightFillPaint)
            } else {
                drawMedianSegment(canvas, cp, target, strokeProgress, highlightPaint)
            }
        } else {
            // 全部笔画灰色底字（保证与真实字形对得上）
            for (o in cp.outlines) canvas.drawPath(o, ghostPaint)
            // 已完成的笔画淡蓝填充
            for (i in 0 until activeStrokeIndex) {
                canvas.drawPath(cp.outlines[i], donePaint)
            }
            // 当前笔画按坐标集合逐步 draw
            if (activeStrokeIndex < cp.medians.size) {
                drawMedianSegment(canvas, cp, activeStrokeIndex, strokeProgress, strokePaint)
            }
        }
        canvas.restore()

        // 动画推进：当前笔画完成后自动进入下一笔画
        if (compareStrokeIndex >= 0) {
            // 对比字模式只动画高亮的那一笔，画完即停
            if (strokeProgress >= 1f) {
                animator.cancel()
            }
        } else {
            if (strokeProgress >= 1f && activeStrokeIndex < cp.medians.size - 1) {
                activeStrokeIndex++
                strokeProgress = 0f
                animator.start()
            }
            if (strokeProgress >= 1f && activeStrokeIndex == cp.medians.size - 1) {
                // 全部完成，停止
                animator.cancel()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    /**
     * 基础笔画 → 对比字 + 该字中对应笔画下标（基础笔画教学页动画演示并高亮该笔画）
     * 对比字必须收录于 assets/hanzi_strokes.json（笔画下标与 hanzi-writer-data 笔顺一致）
     */
    object StrokeData {
        val exampleChars: Map<String, Pair<String, Int>> = mapOf(
            "横" to ("二" to 0),
            "竖" to ("十" to 1),
            "撇" to ("人" to 0),
            "捺" to ("人" to 1),
            "点" to ("六" to 0),
            "提" to ("习" to 2),
            "横折" to ("口" to 1),
            "竖钩" to ("小" to 0),
            "横钩" to ("买" to 0),
            "横折钩" to ("月" to 1),
            "横撇" to ("水" to 1),
            "横折弯钩" to ("九" to 1),
            "横折提" to ("计" to 1),
            "竖提" to ("长" to 2),
            "竖弯" to ("四" to 3),
            "竖弯钩" to ("七" to 1),
            "竖折" to ("山" to 1),
            "撇折" to ("云" to 2),
            "撇点" to ("女" to 0),
            "斜钩" to ("戈" to 1),
            "卧钩" to ("心" to 1),
            "弯钩" to ("狗" to 1)
        )
    }
}
