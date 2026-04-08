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
import android.content.res.TypedArray
import kotlin.math.max

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

    // 数字间距（默认8dp）
    private var digitSpacing = dpToPx(8f)

    // 数字宽度（设置为0表示使用图片原始宽高比）
    private var digitWidth = 0

    // 数字高度（设置为0表示使用图片原始宽高比）
    private var digitHeight = 0

    // 是否保持图片原始宽高比
    private var keepOriginalAspectRatio = true

    // 数字原始宽度（从第一张图片获取）
    private var originalDigitWidth = 0

    // 数字原始高度（从第一张图片获取）
    private var originalDigitHeight = 0

    // 最少显示位数
    private var minDigits = 7

    // 每个数字的动画状态
    private data class DigitAnimation(
        var currentValue: Int,
        var targetValue: Int,
        var animator: ValueAnimator? = null,
        var animationProgress: Float = 1f
    )

    private val digitAnimations = mutableListOf<DigitAnimation>()

    // 卡片背景颜色
    private var cardColor = 0xFF5D4037.toInt() // 深棕色
    private var cardPadding = 4f

    // 动画时长
    private val animationDuration = 300L

    init {
        // 读取自定义属性
        initAttributes(context, attrs, defStyleAttr)
        loadNumberBitmaps()
    }

    /**
     * 初始化自定义属性
     */
    private fun initAttributes(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val typedArray: TypedArray = context.obtainStyledAttributes(
            attrs, R.styleable.ScoreBoardView, defStyleAttr, 0
        )

        try {
            digitSpacing = typedArray.getDimensionPixelSize(
                R.styleable.ScoreBoardView_digitSpacing,
                dpToPx(8f)
            )

            digitWidth = typedArray.getDimensionPixelSize(
                R.styleable.ScoreBoardView_digitWidth,
                0
            )

            digitHeight = typedArray.getDimensionPixelSize(
                R.styleable.ScoreBoardView_digitHeight,
                0
            )

            keepOriginalAspectRatio = typedArray.getBoolean(
                R.styleable.ScoreBoardView_keepOriginalAspectRatio,
                true
            )

            cardColor = typedArray.getColor(
                R.styleable.ScoreBoardView_cardColor,
                0xFF5D4037.toInt()
            )

            cardPadding = typedArray.getDimension(
                R.styleable.ScoreBoardView_cardPadding,
                4f
            )

            minDigits = typedArray.getInteger(
                R.styleable.ScoreBoardView_minDigits,
                7
            )
        } finally {
            typedArray.recycle()
        }
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

            // 记录第一张图片的原始尺寸
            if (originalDigitWidth == 0 && originalDigitHeight == 0 && bitmap != null) {
                originalDigitWidth = bitmap.width
                originalDigitHeight = bitmap.height
            }

            numberBitmaps.add(bitmap)
        }

        // 如果用户没有设置宽高，使用图片原始尺寸
        if (digitWidth == 0 && digitHeight == 0) {
            digitWidth = originalDigitWidth
            digitHeight = originalDigitHeight
        } else if (digitWidth == 0 && digitHeight != 0) {
            // 只设置了高度，宽度按比例计算
            val aspectRatio = originalDigitWidth.toFloat() / originalDigitHeight.toFloat()
            digitWidth = (digitHeight * aspectRatio).toInt()
        } else if (digitWidth != 0 && digitHeight == 0) {
            // 只设置了宽度，高度按比例计算
            val aspectRatio = originalDigitHeight.toFloat() / originalDigitWidth.toFloat()
            digitHeight = (digitWidth * aspectRatio).toInt()
        } else if (digitWidth != 0 && digitHeight != 0 && !keepOriginalAspectRatio) {
            // 同时设置了宽高，且不保持原始比例，使用用户设置的值
        } else if (digitWidth != 0 && digitHeight != 0 && keepOriginalAspectRatio) {
            // 同时设置了宽高，但需要保持原始比例，需要调整其中一个
            val originalRatio = originalDigitWidth.toFloat() / originalDigitHeight.toFloat()
            val userRatio = digitWidth.toFloat() / digitHeight.toFloat()

            if (userRatio > originalRatio) {
                // 用户设置的宽高比大于原始比例，调整高度
                digitHeight = (digitWidth / originalRatio).toInt()
            } else {
                // 用户设置的宽高比小于原始比例，调整宽度
                digitWidth = (digitHeight * originalRatio).toInt()
            }
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

        // 将积分转换为数字列表，最少显示minDigits位
        val newDigits = score.toDigits(minDigits)

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
            requestLayout()
            invalidate()
        } else {
            // 开始数字动画
            startDigitAnimation(oldScore, score, newDigits)
        }
    }

    /**
     * 设置最少显示位数
     * @param minDigits 最少位数
     */
    fun setMinDigits(minDigits: Int) {
        if (minDigits < 1) {
            throw IllegalArgumentException("最少位数必须大于0")
        }
        this.minDigits = minDigits

        // 重新显示当前积分
        if (currentScore > 0) {
            val newDigits = currentScore.toDigits(minDigits)
            displayDigits = newDigits.toMutableList()
            requestLayout()
            invalidate()
        }
    }

    /**
     * 设置数字间距
     * @param spacing 间距（像素）
     */
    fun setDigitSpacing(spacing: Int) {
        this.digitSpacing = spacing
        requestLayout()
        invalidate()
    }

    /**
     * 设置数字间距（dp单位）
     * @param spacingDp 间距（dp）
     */
    fun setDigitSpacingDp(spacingDp: Float) {
        this.digitSpacing = dpToPx(spacingDp)
        requestLayout()
        invalidate()
    }

    /**
     * 设置数字宽度
     * @param width 宽度（像素）
     */
    fun setDigitWidth(width: Int) {
        this.digitWidth = width
        if (digitHeight != 0 && keepOriginalAspectRatio) {
            val aspectRatio = originalDigitHeight.toFloat() / originalDigitWidth.toFloat()
            digitHeight = (digitWidth * aspectRatio).toInt()
        }
        requestLayout()
        invalidate()
    }

    /**
     * 设置数字高度
     * @param height 高度（像素）
     */
    fun setDigitHeight(height: Int) {
        this.digitHeight = height
        if (digitWidth != 0 && keepOriginalAspectRatio) {
            val aspectRatio = originalDigitWidth.toFloat() / originalDigitHeight.toFloat()
            digitWidth = (digitHeight * aspectRatio).toInt()
        }
        requestLayout()
        invalidate()
    }

    /**
     * 设置是否保持原始宽高比
     * @param keep 是否保持
     */
    fun setKeepOriginalAspectRatio(keep: Boolean) {
        this.keepOriginalAspectRatio = keep
        if (digitWidth != 0 && digitHeight != 0) {
            if (keep) {
                val originalRatio = originalDigitWidth.toFloat() / originalDigitHeight.toFloat()
                val userRatio = digitWidth.toFloat() / digitHeight.toFloat()

                if (userRatio > originalRatio) {
                    digitHeight = (digitWidth / originalRatio).toInt()
                } else {
                    digitWidth = (digitHeight * originalRatio).toInt()
                }
            }
        }
        requestLayout()
        invalidate()
    }

    /**
     * 设置卡片背景颜色
     * @param color 颜色值
     */
    fun setCardColor(color: Int) {
        this.cardColor = color
        invalidate()
    }

    /**
     * 设置卡片内边距
     * @param padding 内边距
     */
    fun setCardPadding(padding: Float) {
        this.cardPadding = padding
        invalidate()
    }

    /**
     * 开始数字动画
     */
    private fun startDigitAnimation(oldScore: Int, newScore: Int, newDigits: List<Int>) {
        val oldDigits = oldScore.toDigits(minDigits)
        val maxLength = maxOf(oldDigits.size, newDigits.size, minDigits)

        // 对齐数字位数
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
        if (numberBitmaps.isEmpty()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        // 确保数字位数至少为minDigits
        val digitCount = max(minDigits, displayDigits.size)

        // 计算总宽度：数字宽度总和 + 间距总和
        val totalDigitWidth = digitWidth * digitCount
        val totalSpacingWidth = digitSpacing * (digitCount - 1)
        val desiredWidth = totalDigitWidth + totalSpacingWidth
        val desiredHeight = digitHeight

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

        // 如果用户设置了宽高，这里不需要额外计算
        // 但我们可以根据View的大小调整数字大小
        if (digitHeight == 0 && h > 0) {
            digitHeight = (h * 0.8).toInt() // 使用View高度的80%
            val aspectRatio = originalDigitWidth.toFloat() / originalDigitHeight.toFloat()
            digitWidth = (digitHeight * aspectRatio).toInt()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (numberBitmaps.isEmpty() || displayDigits.isEmpty() || digitWidth <= 0 || digitHeight <= 0) {
            return
        }

        // 确保显示至少minDigits位数字
        val drawDigits = if (displayDigits.size < minDigits) {
            // 前面补0
            val zeros = List(minDigits - displayDigits.size) { 0 }
            zeros + displayDigits
        } else {
            displayDigits
        }

        val totalWidth = digitWidth * drawDigits.size + digitSpacing * (drawDigits.size - 1)
        val startX = (width - totalWidth) / 2f
        val startY = (height - digitHeight) / 2f

        // 绘制每个数字卡片
        for (i in drawDigits.indices) {
            val animationIndex = if (drawDigits.size > displayDigits.size) {
                i - (drawDigits.size - displayDigits.size)
            } else {
                i
            }

            val animation = digitAnimations.getOrNull(animationIndex)
            val currentDigit = drawDigits[i]
            val targetDigit = animation?.targetValue ?: currentDigit
            val progress = animation?.animationProgress ?: 1f

            val cardLeft = startX + i * (digitWidth + digitSpacing)
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
                cardPadding * 2,
                cardPadding * 2,
                android.graphics.Paint().apply {
                    color = cardColor
                    isAntiAlias = true
                }
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
     * dp转px
     */
    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    /**
     * 将整数转换为数字列表，最少minDigits位
     */
    private fun Int.toDigits(minDigits: Int): List<Int> {
        val str = this.toString()
        val digits = str.map { it.toString().toInt() }

        // 如果位数不足，前面补0
        return if (digits.size < minDigits) {
            val zeros = List(minDigits - digits.size) { 0 }
            zeros + digits
        } else {
            digits
        }
    }

    /**
     * 工具函数：在列表前面补0
     */
    private fun List<Int>.padStart(size: Int, value: Int): List<Int> {
        if (this.size >= size) return this
        return List(size - this.size) { value } + this
    }
}