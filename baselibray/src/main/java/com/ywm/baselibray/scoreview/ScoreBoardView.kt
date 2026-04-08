package com.ywm.baselibray.scoreview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.ywm.baselibray.R
import kotlin.math.min

class ScoreBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 数字图片资源ID数组
    private val numberResIds = arrayOf(
        R.mipmap.ic_number_0,
        R.mipmap.ic_number_1,
        R.mipmap.ic_number_2,
        R.mipmap.ic_number_3,
        R.mipmap.ic_number_4,
        R.mipmap.ic_number_5,
        R.mipmap.ic_number_6,
        R.mipmap.ic_number_7,
        R.mipmap.ic_number_8,
        R.mipmap.ic_number_9
    )

    // 缓存的数字Bitmap
    private val numberBitmaps = mutableListOf<Bitmap>()

    // 当前显示的积分
    private var currentScore = 0
    private var targetScore = 0
    private var displayDigits = mutableListOf<Int>()

    // 每个数字的动画状态
    private data class DigitAnimation(
        var currentValue: Int,
        var targetValue: Int,
        var animator: ValueAnimator? = null,
        var animationProgress: Float = 1f
    )

    private val digitAnimations = mutableListOf<DigitAnimation>()

    // 绘制相关参数
    private var digitWidth = 0
    private var digitHeight = 0
    private var padding = 0
    private var cornerRadius = 0f

    // 卡片背景颜色
    private val cardColor = 0xFF5D4037.toInt() // 深棕色
    private val cardPadding = 4f

    // 动画时长
    private val animationDuration = 300L

    init {
        loadNumberBitmaps()
    }

    /**
     * 加载数字图片到内存
     */
    private fun loadNumberBitmaps() {
        numberResIds.forEach { resId ->
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565
            val bitmap = BitmapFactory.decodeResource(resources, resId, options)
            numberBitmaps.add(bitmap)
        }
    }

    /**
     * 设置积分
     * @param score 新的积分值
     */
    fun setScore(score: Int) {
        if (score < 0) {
            throw IllegalArgumentException("积分不能为负数")
        }

        val oldScore = currentScore
        currentScore = score
        targetScore = score

        // 将积分转换为数字列表
        val newDigits = score.toString().map { it.toString().toInt() }

        // 停止所有正在进行的动画
        stopAllAnimations()

        // 更新显示数字
        if (displayDigits.isEmpty() || oldScore == 0) {
            // 第一次设置或从0开始，直接显示
            displayDigits = newDigits.toMutableList()
            digitAnimations.clear()
            newDigits.forEach { digit ->
                digitAnimations.add(DigitAnimation(digit, digit, animationProgress = 1f))
            }
            invalidate()
        } else {
            // 开始数字动画
            startDigitAnimation(oldScore, score, newDigits)
        }
    }

    /**
     * 开始数字动画
     */
    private fun startDigitAnimation(oldScore: Int, newScore: Int, newDigits: List<Int>) {
        val oldDigits = oldScore.toString().map { it.toString().toInt() }
        val maxLength = maxOf(oldDigits.size, newDigits.size)

        // 对齐数字位数（前面补0）
        val alignedOldDigits = oldDigits.padStart(maxLength, 0)
        val alignedNewDigits = newDigits.padStart(maxLength, 0)

        // 初始化显示数字
        displayDigits = alignedOldDigits.toMutableList()

        // 清理旧的动画状态
        digitAnimations.clear()

        // 为每个数字创建动画
        alignedOldDigits.forEachIndexed { index, oldDigit ->
            val newDigit = alignedNewDigits[index]
            val animation = DigitAnimation(oldDigit, newDigit, animationProgress = 0f)

            if (oldDigit != newDigit) {
                // 创建数字滚动动画
                val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = animationDuration
                    addUpdateListener {
                        animation.animationProgress = it.animatedValue as Float
                        invalidate()
                    }
                }
                animation.animator = animator
                animator.start()
            } else {
                animation.animationProgress = 1f
            }

            digitAnimations.add(animation)
        }
    }

    /**
     * 停止所有动画
     */
    private fun stopAllAnimations() {
        digitAnimations.forEach { it.animator?.cancel() }
        digitAnimations.clear()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (digitWidth + padding) * 8 // 7个数字+一些padding
        val desiredHeight = digitHeight + padding * 2

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (numberBitmaps.isNotEmpty()) {
            // 根据View大小计算数字尺寸
            digitHeight = h - padding * 2
            digitWidth = (digitHeight * numberBitmaps[0].width / numberBitmaps[0].height.toFloat()).toInt()
            cornerRadius = h * 0.1f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (numberBitmaps.isEmpty() || displayDigits.isEmpty()) return

        val totalWidth = digitWidth * displayDigits.size
        val startX = (width - totalWidth) / 2f
        val startY = padding.toFloat()

        // 绘制每个数字卡片
        for (i in displayDigits.indices) {
            val animation = digitAnimations.getOrNull(i)
            val currentDigit = displayDigits[i]
            val targetDigit = animation?.targetValue ?: currentDigit
            val progress = animation?.animationProgress ?: 1f

            val cardLeft = startX + i * digitWidth
            val cardRect = Rect(
                cardLeft.toInt(),
                startY.toInt(),
                (cardLeft + digitWidth).toInt(),
                (startY + digitHeight).toInt()
            )

            // 绘制卡片背景
            canvas.drawRoundRect(
                cardLeft + cardPadding,
                startY + cardPadding,
                cardLeft + digitWidth - cardPadding,
                startY + digitHeight - cardPadding,
                cornerRadius,
                cornerRadius,
                android.graphics.Paint().apply { color = cardColor }
            )

            if (animation != null && progress < 1f && currentDigit != targetDigit) {
                // 绘制动画中的数字
                drawAnimatedDigit(canvas, cardLeft, startY, animation, progress)
            } else {
                // 绘制静态数字
                val bitmap = numberBitmaps.getOrNull(currentDigit)
                bitmap?.let {
                    canvas.drawBitmap(it, null, cardRect, null)
                }
            }
        }
    }

    /**
     * 绘制动画中的数字
     */
    private fun drawAnimatedDigit(
        canvas: Canvas,
        left: Float,
        top: Float,
        animation: DigitAnimation,
        progress: Float
    ) {
        val currentValue = animation.currentValue
        val targetValue = animation.targetValue

        // 计算滚动偏移
        val offset = -(progress * digitHeight)

        // 绘制当前数字（向上滚出）
        val currentBitmap = numberBitmaps.getOrNull(currentValue)
        currentBitmap?.let {
            val srcRect = Rect(0, 0, it.width, it.height)
            val dstRect = Rect(
                left.toInt(),
                (top + offset).toInt(),
                (left + digitWidth).toInt(),
                (top + offset + digitHeight).toInt()
            )
            canvas.drawBitmap(it, srcRect, dstRect, null)
        }

        // 绘制目标数字（从下方滚入）
        val targetBitmap = numberBitmaps.getOrNull(targetValue)
        targetBitmap?.let {
            val srcRect = Rect(0, 0, it.width, it.height)
            val dstRect = Rect(
                left.toInt(),
                (top + offset + digitHeight).toInt(),
                (left + digitWidth).toInt(),
                (top + offset + digitHeight * 2).toInt()
            )
            canvas.drawBitmap(it, srcRect, dstRect, null)
        }
    }

    /**
     * 处理内存释放
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cleanup()
    }

    /**
     * 清理资源，防止内存泄漏
     */
    fun cleanup() {
        stopAllAnimations()
        numberBitmaps.forEach { it.recycle() }
        numberBitmaps.clear()
    }

    /**
     * 工具函数：在列表前面补0
     */
    private fun List<Int>.padStart(size: Int, value: Int): List<Int> {
        if (this.size >= size) return this
        return List(size - this.size) { value } + this
    }
}