package com.ywm.baselibray.weiget
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.Shader.TileMode
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.ywm.baselibray.R

open class ShimmerTextView @JvmOverloads constructor(
context: Context,
attrs: AttributeSet? = null,
defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    // 描边相关
    private var outlineColor: Int = Color.TRANSPARENT
    private var outlineWidth: Float = 0F
    private var outlineShader: Shader? = null

    // 渐变相关
    var gradientColors: IntArray = intArrayOf()
    var gradientShader: Shader? = null
    private var gradientOrientation: Int = 0 // 0=水平, 1=垂直

    // 阴影相关
    private var shadowColor: Int = Color.TRANSPARENT
    private var shadowDx: Float = 0F
    private var shadowDy: Float = 0F
    private var shadowRadius: Float = 0F

    // 流光相关
    private var shimmerEnabled: Boolean = false
    private var shimmerColor: Int = Color.parseColor("#AAFFFFFF") // 流光高亮颜色
    private var shimmerBaseColor: Int = Color.TRANSPARENT // 流光基础色（透明）
    private var shimmerWidthRatio: Float = 0.5f // 流光宽度占比
    private var shimmerSpeed: Long = 1500L // 动画周期（毫秒）
    private var shimmerAngle: Float = 40f // 流光角度

    // 流光动画
    private var shimmerAnimator: ValueAnimator? = null
    private var shimmerTranslateX: Float = 0f
    private var shimmerGradient: LinearGradient? = null
    private val shimmerMatrix = Matrix()

    // 用于跟踪 attached 状态，解决 RecyclerView 复用问题
    private var isViewAttached: Boolean = false
    private var wasAnimatingBeforeDetach: Boolean = false

    // 合成 shader（渐变 + 流光）
    private var compositeShader: ComposeShader? = null


    init {
        // 从 XML 属性读取配置
        context.theme.obtainStyledAttributes(attrs, R.styleable.ShimmerTextView, 0, 0).apply {
            try {
                // 描边属性
                outlineColor = getColor(R.styleable.ShimmerTextView_shimmeroutlineColor, outlineColor)
                outlineWidth = getDimension(R.styleable.ShimmerTextView_shimmeroutlineWidth, outlineWidth)

                // 阴影属性
                shadowColor = getColor(R.styleable.ShimmerTextView_shimmershadowColor, shadowColor)
                shadowDx = getDimension(R.styleable.ShimmerTextView_shimmershadowDx, shadowDx)
                shadowDy = getDimension(R.styleable.ShimmerTextView_shimmershadowDy, shadowDy)
                shadowRadius = getDimension(R.styleable.ShimmerTextView_shimmershadowRadius, shadowRadius)

                // 渐变属性
                val gradientColorsId = getResourceId(R.styleable.ShimmerTextView_shimmergradientColors, 0)
                if (gradientColorsId != 0) {
                    gradientColors = resources.getIntArray(gradientColorsId)
                }
                gradientOrientation = getInt(R.styleable.ShimmerTextView_shimmergradientOrientation, 0)

                // 流光属性
                shimmerEnabled = getBoolean(R.styleable.ShimmerTextView_shimmerEnabled, shimmerEnabled)
                shimmerColor = getColor(R.styleable.ShimmerTextView_shimmerColor, shimmerColor)
//                shimmerWidth = getFloat(R.styleable.ShimmerTextView_shimmerWidth, shimmerWidth)
                shimmerSpeed = getInt(R.styleable.ShimmerTextView_shimmerSpeed, shimmerSpeed.toInt()).toLong()
                shimmerAngle = getFloat(R.styleable.ShimmerTextView_shimmerAngle, shimmerAngle)

                // 字体属性
                val fontFamilyId = getResourceId(R.styleable.ShimmerTextView_shimmerfontFamily, 0)
                val textStyle = getInt(R.styleable.ShimmerTextView_shimmertextStyle, Typeface.NORMAL)

                val typeface = if (fontFamilyId != 0) {
                    ResourcesCompat.getFont(context, fontFamilyId)?.let {
                        Typeface.create(it, textStyle)
                    }
                } else {
                    Typeface.defaultFromStyle(textStyle)
                }

                setTypeface(typeface)
            } finally {
                recycle()
            }
        }
// 禁用硬件加速以支持 ComposeShader
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isViewAttached = true
        // 如果之前在动画中被 detach，恢复动画
        if (wasAnimatingBeforeDetach && shimmerEnabled) {
            startShimmerAnimation()
        } else if (shimmerEnabled) {
            startShimmerAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        // 记录 detach 前的动画状态
        wasAnimatingBeforeDetach = shimmerAnimator?.isRunning == true
        // 停止动画，防止内存泄漏
        stopShimmerAnimation()
        isViewAttached = false
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 尺寸变化时重置 shader
        resetShaders()

        // 如果启用了流光效果，重新创建 shader 和动画
        if (shimmerEnabled && w > 0 && h > 0) {
            createShimmerGradient()
            if (isViewAttached) {
                startShimmerAnimation()
            }
        }
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        // 文本变化时重置 shader
        gradientShader = null
        compositeShader = null
    }

    private fun resetShaders() {
        gradientShader = null
        outlineShader = null
        shimmerGradient = null
        compositeShader = null
    }

    override fun onDraw(canvas: Canvas) {
        val viewWidth = width
        val viewHeight = height

        if (viewWidth <= 0 || viewHeight <= 0) {
            super.onDraw(canvas)
            return
        }

        // 绘制描边
        if (outlineWidth > 0) {
            drawOutline(canvas)
        }

        // 设置渐变 shader
        setupGradientShader()

        // 绘制主文字
        paint.style = Paint.Style.FILL
        paint.textSize = textSize
        paint.typeface = typeface
        paint.color = currentTextColor
        paint.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)

        // 如果启用流光且有渐变
        if (shimmerEnabled && shimmerGradient != null) {
            // 更新流光位置
            updateShimmerMatrix()

            if (gradientShader != null) {
                // 合成渐变和流光
                compositeShader = ComposeShader(
                    gradientShader!!,
                    shimmerGradient!!,
                    PorterDuff.Mode.SRC_OVER
                )
                paint.shader = compositeShader
            } else {
                // 只有流光
                paint.shader = shimmerGradient
            }
        } else {
            paint.shader = gradientShader
        }

        super.onDraw(canvas)
    }

    private fun drawOutline(canvas: Canvas) {
        paint.clearShadowLayer()
        paint.style = Paint.Style.STROKE
        paint.textSize = textSize
        paint.typeface = typeface
        if (outlineShader == null) {
            outlineShader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                outlineColor, outlineColor,
                TileMode.CLAMP
            )
        }
        paint.shader = outlineShader
        paint.strokeWidth = outlineWidth
        super.onDraw(canvas)
    }

    private fun setupGradientShader() {
        if (gradientColors.isNotEmpty() && gradientShader == null) {
            if (gradientColors.size == 1) {
                gradientColors = intArrayOf(gradientColors[0], gradientColors[0])
            }
            val layout = layout ?: return
            val line = layout.getLineForOffset(0)
            val startX = layout.getLineLeft(line)
            val endX = layout.getLineRight(line)

            gradientShader = when (gradientOrientation) {
                0 -> LinearGradient(
                    startX, 0f, endX, 0f,
                    gradientColors, null, Shader.TileMode.CLAMP
                )

                else -> LinearGradient(
                    0f, 0f, 0f, textSize,
                    gradientColors, null, TileMode.CLAMP
                )
            }
        }
    }

    /**
     * 创建流光渐变
     */
    private fun createShimmerGradient() {
        val shimmerWidth = width * shimmerWidthRatio

        // 创建斜向的流光渐变
        // 流光从透明 -> 高亮色 -> 透明
        val angleRad = Math.toRadians(shimmerAngle.toDouble())
        val cos = Math.cos(angleRad).toFloat()
        val sin = Math.sin(angleRad).toFloat()

        // 计算渐变的起点和终点
        val halfWidth = shimmerWidth / 2
        val startX = -halfWidth * cos
        val startY = -halfWidth * sin
        val endX = halfWidth * cos
        val endY = halfWidth * sin

        shimmerGradient = LinearGradient(
            startX, startY, endX, endY,
            intArrayOf(
                shimmerBaseColor,
                adjustAlpha(shimmerColor, 0.3f),
                shimmerColor,
                adjustAlpha(shimmerColor, 0.3f),
                shimmerBaseColor
            ),
            floatArrayOf(0f, 0.35f, 0.5f, 0.65f, 1f),
            TileMode.CLAMP
        )
    }

    /**
     * 更新流光矩阵位置
     */
    private fun updateShimmerMatrix() {
        shimmerGradient?.let { gradient ->
            shimmerMatrix.reset()
            shimmerMatrix.setTranslate(shimmerTranslateX, 0f)
            gradient.setLocalMatrix(shimmerMatrix)
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    /**
     * 启动流光动画
     */
    fun startShimmerAnimation() {
        if (!isViewAttached || width <= 0) return

        stopShimmerAnimation()

        // 创建 shader（如果还没有）
        if (shimmerGradient == null) {
            createShimmerGradient()
        }

        // 动画范围：从左边界外到右边界外
        val startValue = -width * shimmerWidthRatio
        val endValue = width + width * shimmerWidthRatio

        shimmerAnimator = ValueAnimator.ofFloat(startValue, endValue).apply {
            duration = shimmerSpeed
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                shimmerTranslateX = animation.animatedValue as Float
                // 重置 composite shader 以便重新合成
                compositeShader = null
                invalidate()
            }
            start()
        }
    }

    /**
     * 停止流光动画
     */
    fun stopShimmerAnimation() {
        shimmerAnimator?.cancel()
        shimmerAnimator?.removeAllUpdateListeners()
        shimmerAnimator = null
    }

    /**
     * 设置是否启用流光效果
     */
    fun setShimmerEnabled(enabled: Boolean) {
        shimmerEnabled = enabled
        if (enabled && isViewAttached && width > 0) {
            createShimmerGradient()
            startShimmerAnimation()
        } else {
            stopShimmerAnimation()
            shimmerGradient = null
            compositeShader = null
            invalidate()
        }
    }

    /**
     * 获取流光是否启用
     */
    fun isShimmerEnabled(): Boolean = shimmerEnabled

    /**
     * 设置流光颜色
     */
    fun setShimmerColor(color: Int) {
        shimmerColor = color
        shimmerGradient = null
        compositeShader = null
        if (shimmerEnabled && width > 0) {
            createShimmerGradient()
            invalidate()
        }
    }

    /**
     * 获取流光颜色
     */
    fun getShimmerColor(): Int = shimmerColor

    /**
     * 设置流光速度（动画周期，毫秒）
     */
    fun setShimmerSpeed(speed: Long) {
        shimmerSpeed = speed
        if (shimmerEnabled && isViewAttached) {
            startShimmerAnimation() // 重新启动动画
        }
    }

    /**
     * 获取流光速度
     */
    fun getShimmerSpeed(): Long = shimmerSpeed

    /**
     * 设置流光角度
     */
    fun setShimmerAngle(angle: Float) {
        shimmerAngle = angle
        shimmerGradient = null
        compositeShader = null
        if (shimmerEnabled && width > 0) {
            createShimmerGradient()
            invalidate()
        }
    }

    /**
     * 获取流光角度
     */
    fun getShimmerAngle(): Float = shimmerAngle

    /**
     * 设置流光宽度比例 (0.1 - 1.0)
     */
    fun setShimmerWidthRatio(ratio: Float) {
        shimmerWidthRatio = ratio.coerceIn(0.1f, 1f)
        shimmerGradient = null
        compositeShader = null
        if (shimmerEnabled && width > 0) {
            createShimmerGradient()
            if (isViewAttached) {
                startShimmerAnimation()
            }
        }
    }

    /**
     * 获取流光宽度比例
     */
    fun getShimmerWidthRatio(): Float = shimmerWidthRatio

    /**
     * 检查流光动画是否正在运行
     */
    fun isShimmerAnimating(): Boolean {
        return shimmerAnimator?.isRunning == true
    }

    // ==================== 以下方法用于 RecyclerView 优化 ====================

    /**
     * 在 RecyclerView.Adapter 的 onViewRecycled 中调用
     * 用于释放资源
     */
    fun onRecycled() {
        stopShimmerAnimation()
        resetShaders()
    }

    /**
     * 在绑定数据后调用，恢复动画状态
     */
    fun onBind() {
        if (shimmerEnabled && isViewAttached) {
            post {
                if (width > 0) {
                    createShimmerGradient()
                    startShimmerAnimation()
                }
            }
        }
    }

    /**
     * 重置所有状态
     */
    fun reset() {
        stopShimmerAnimation()
        shimmerTranslateX = 0f
        resetShaders()
        wasAnimatingBeforeDetach = false
        invalidate()
    }

    // ==================== 描边相关方法 ====================

    fun setOutlineColor(color: Int) {
        outlineColor = color
        outlineShader = null
        invalidate()
    }

    fun setOutlineWidth(width: Float) {
        outlineWidth = width
        invalidate()
    }

    // ==================== 渐变相关方法 ====================

    @JvmName("setGradientColorsL")
    fun setGradientColors(colors: IntArray) {
        gradientColors = colors
        gradientShader = null
        compositeShader = null
        invalidate()
    }

    fun setGradientOrientation(orientation: Int) {
        gradientOrientation = orientation
        gradientShader = null
        compositeShader = null
        invalidate()
    }
}
