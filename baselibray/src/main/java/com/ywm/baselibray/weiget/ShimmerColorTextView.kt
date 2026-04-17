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
import kotlin.math.max
import kotlin.math.roundToLong

open class ShimmerColorTextView @JvmOverloads constructor(
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
    private var shimmerSpeed: Long = 2500L // 动画周期（毫秒）
    private var shimmerAngle: Float = 40f // 流光角度
    private var currentTextWidth: Float = 0f // 当前文字宽度

    // 流光动画
    private var shimmerAnimator: ValueAnimator? = null
    private var shimmerTranslateX: Float = 0f
    private val shimmerMatrix = Matrix()

    // 用于跟踪 attached 状态，解决 RecyclerView 复用问题
    private var isViewAttached: Boolean = false
    private var wasAnimatingBeforeDetach: Boolean = false


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
//            createShimmerGradient()
            if (isViewAttached) {
                startShimmerAnimation()
            }
        }
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        gradientShader = null
        currentTextWidth = 0f

        if (shimmerEnabled && isViewAttached) {
            post {
                updateCurrentTextWidth()
                if (width > 0) {
                    startShimmerAnimation()
                }
            }
        }
    }

    private fun resetShaders() {
        gradientShader = null
        outlineShader = null
        shimmerMatrix.reset()
    }

    private fun updateCurrentTextWidth(): Float {
        val textStr = text?.toString().orEmpty()
        if (textStr.isEmpty()) {
            currentTextWidth = 0f
            return currentTextWidth
        }

        paint.textSize = textSize
        paint.typeface = typeface

        val layout = layout
        currentTextWidth = if (layout != null && layout.lineCount > 0) {
            var maxLineWidth = 0f
            for (i in 0 until layout.lineCount) {
                val w = layout.getLineRight(i) - layout.getLineLeft(i)
                if (w > maxLineWidth) maxLineWidth = w
            }
            maxLineWidth
        } else {
            paint.measureText(textStr)
        }

        return currentTextWidth
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

        // 如果启用流光，更新 shader 矩阵位置
        if (shimmerEnabled && gradientShader != null) {
            updateShimmerMatrix()
            paint.shader = gradientShader
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
        super.draw(canvas)
    }

    private fun setupGradientShader() {
        if (gradientColors.isNotEmpty() && gradientShader == null) {
            if (gradientColors.size == 1) {
                gradientColors = intArrayOf(gradientColors[0], gradientColors[0])
            }

            val textWidth = if (currentTextWidth > 0f) currentTextWidth else updateCurrentTextWidth()
            val shaderWidth = max(textWidth, width.toFloat().coerceAtLeast(1f))

            gradientShader = when (gradientOrientation) {
                0 -> LinearGradient(
                    0f, 0f, shaderWidth, 0f,
                    gradientColors, null, TileMode.REPEAT
                )

                else -> LinearGradient(
                    0f, 0f, 0f, textSize,
                    gradientColors, null, TileMode.REPEAT
                )
            }
        }
    }

    /**
     * 更新流光矩阵位置，实现颜色波动效果
     */
    private fun updateShimmerMatrix() {
        gradientShader?.let { shader ->
            shimmerMatrix.reset()
            // 根据角度计算平移距离
            val angleRad = Math.toRadians(shimmerAngle.toDouble())
            val translateDistance = shimmerTranslateX * Math.cos(angleRad).toFloat()
            shimmerMatrix.setTranslate(translateDistance, 0f)
            shader.setLocalMatrix(shimmerMatrix)
        }
    }

    /**
     * 启动流光动画
     */
    fun startShimmerAnimation() {
        if (!isViewAttached || width <= 0) return

        stopShimmerAnimation()

        val textWidth = if (currentTextWidth > 0f) currentTextWidth else updateCurrentTextWidth()
        val baseDistance = width * 2f
        val animationDistance = max(textWidth, width.toFloat()) * 2f
        val computedDuration = (animationDistance / baseDistance * shimmerSpeed).roundToLong().coerceAtLeast(1L)

        shimmerAnimator = ValueAnimator.ofFloat(0f, animationDistance).apply {
            duration = computedDuration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                shimmerTranslateX = animation.animatedValue as Float
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
            startShimmerAnimation()
        } else {
            stopShimmerAnimation()
            shimmerMatrix.reset()
            invalidate()
        }
    }

    /**
     * 获取流光是否启用
     */
    fun isShimmerEnabled(): Boolean = shimmerEnabled

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
        if (shimmerEnabled && isViewAttached) {
            startShimmerAnimation()
        }
    }

    /**
     * 获取流光角度
     */
    fun getShimmerAngle(): Float = shimmerAngle

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
        shimmerMatrix.reset()
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
        invalidate()
    }

    fun setGradientOrientation(orientation: Int) {
        gradientOrientation = orientation
        gradientShader = null
        invalidate()
    }
}
