package com.ywm.baselibray.weiget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Shader.TileMode
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.ywm.baselibray.R


/**
 * Created  on 24/11/18_Mon
 */
open class ColorfulShineTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var outlineColor: Int = Color.TRANSPARENT
    private var outlineWidth: Float = 0F
    var gradientColors: IntArray = intArrayOf()
    private var shadowColor: Int = Color.TRANSPARENT
    private var shadowDx: Float = 0F
    private var shadowDy: Float = 0F
    private var shadowRadius: Float = 0F

    var gradientShader: Shader? = null
    private var outlineShader: Shader? = null
    private var gradientOrientation: Int = 0 // Default to vertical

    // 流光特效相关属性
    private var shineEnabled: Boolean = false
    private var shineShader: LinearGradient? = null
    private var shineMatrix: Matrix? = null
    private var shineTranslate: Int = 0
    private var shineSpeed: Int = 10 // 流光速度，值越小越快
    private var shineColors: IntArray =
        intArrayOf(Color.TRANSPARENT, Color.WHITE, Color.TRANSPARENT)

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ColorfulTextView, 0, 0).apply {
            try {
                outlineColor = getColor(R.styleable.ColorfulTextView_outlineColor, outlineColor)
                outlineWidth = getDimension(R.styleable.ColorfulTextView_outlineWidth, outlineWidth)
                shadowColor = getColor(R.styleable.ColorfulTextView_shadowColor, shadowColor)
                shadowDx = getDimension(R.styleable.ColorfulTextView_shadowDx, shadowDx)
                shadowDy = getDimension(R.styleable.ColorfulTextView_shadowDy, shadowDy)
                shadowRadius = getDimension(R.styleable.ColorfulTextView_shadowRadius, shadowRadius)

                val gradientColorsId = getResourceId(R.styleable.ColorfulTextView_gradientColors, 0)
                if (gradientColorsId != 0) {
                    gradientColors = resources.getIntArray(gradientColorsId)
                }
                gradientOrientation = getInt(R.styleable.ColorfulTextView_gradientOrientation, 0)

                // 流光特效属性
                shineEnabled = getBoolean(R.styleable.ColorfulTextView_shineEnabled, shineEnabled)
                shineSpeed = getInt(R.styleable.ColorfulTextView_shineSpeed, shineSpeed)

                val shineColorsId = getResourceId(R.styleable.ColorfulTextView_shineColors, 0)
                if (shineColorsId != 0) {
                    shineColors = resources.getIntArray(shineColorsId)
                }

                val fontFamilyId = getResourceId(R.styleable.ColorfulTextView_fontFamily, 0)
                val textStyle = getInt(R.styleable.ColorfulTextView_textStyle, Typeface.NORMAL)

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

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onDraw(canvas: Canvas) {
        // 绘制轮廓（如果有）
        if (outlineWidth > 0) {
            paint.clearShadowLayer()
            paint.style = Paint.Style.STROKE
            paint.textSize = textSize
            paint.typeface = typeface
            if (outlineShader == null) {
                outlineShader = LinearGradient(
                    0f,
                    0f,
                    0f,
                    height.toFloat(),
                    outlineColor,
                    outlineColor,
                    TileMode.CLAMP
                )
            }
            paint.shader = outlineShader
            paint.strokeWidth = outlineWidth
            super.onDraw(canvas)
        }

        // 设置渐变着色器（参考ColorfulTextView的实现）
        if (gradientColors.isNotEmpty() && gradientShader == null) {
            if (gradientColors.size == 1) {
                gradientColors = intArrayOf(gradientColors[0], gradientColors[0])
            }
            val layout = layout
            if (layout != null) {
                val line = layout.getLineForOffset(0)
                val startX = layout.getLineLeft(line)
                val endX = layout.getLineRight(line)
                val textWidth = endX - startX

                gradientShader = when (gradientOrientation) {
                    0 -> LinearGradient(
                        startX, 0f, endX, 0f, // 使用Layout计算的精确位置
                        gradientColors, null, TileMode.CLAMP
                    )

                    else -> LinearGradient( // Vertical
                        0f, 0f, 0f, textSize,
                        gradientColors,
                        null,
                        TileMode.CLAMP
                    )
                }
            }
        }

        // 设置流光特效
        if (shineEnabled && measuredWidth > 0) {
            setupShineShader()
            updateShineAnimation()
        }

        paint.style = Paint.Style.FILL
        
        // 设置着色器：如果流光和渐变都可用，使用混合着色器
        paint.shader = when {
            shineEnabled && shineShader != null && gradientColors.isNotEmpty() && gradientShader != null -> {
                // 使用ComposeShader混合流光和渐变效果
                createCombinedShader()
            }
            shineEnabled && shineShader != null -> shineShader
            gradientColors.isNotEmpty() && gradientShader != null -> gradientShader
            else -> null
        }
        
        paint.textSize = textSize
        paint.typeface = typeface
        paint.color = currentTextColor
        paint.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        super.onDraw(canvas)
    }

    /**
     * 设置流光着色器
     */
    private fun setupShineShader() {
        if (shineShader == null && measuredWidth > 0) {
            val shineWidth = measuredWidth / 8
            shineShader = LinearGradient(
                0f,
                0f,
                shineWidth.toFloat(),
                0f,
                shineColors,
                null,
                TileMode.CLAMP
            )
            shineMatrix = Matrix()
        }
    }

    /**
     * 更新流光动画
     */
    private fun updateShineAnimation() {
        if (shineEnabled && shineMatrix != null && shineShader != null) {
            shineTranslate += measuredWidth / shineSpeed
            if (shineTranslate > 1.5 * measuredWidth) {
                shineTranslate = -measuredWidth / 2
            }
            shineMatrix!!.setTranslate(shineTranslate.toFloat(), 0f)
            shineShader!!.setLocalMatrix(shineMatrix)
            postInvalidateDelayed(50) // 控制流光动画的流畅度
        }
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        gradientShader = null
        shineShader = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gradientShader = null
        shineShader = null
        shineMatrix = null
        shineTranslate = 0
    }

    /**
     * 设置流光特效是否启用
     */
    fun setShineEnabled(enabled: Boolean) {
        if (shineEnabled != enabled) {
            shineEnabled = enabled
            if (enabled) {
                shineShader = null
                shineMatrix = null
                shineTranslate = 0
            }
            invalidate()
        }
    }

    /**
     * 设置流光速度
     * @param speed 速度值，越小越快，建议范围5-20
     */
    fun setShineSpeed(speed: Int) {
        if (speed > 0) {
            shineSpeed = speed
            invalidate()
        }
    }

    /**
     * 设置流光颜色
     * @param colors 颜色数组，通常为[透明色, 高亮色, 透明色]
     */
    fun setShineColors(colors: IntArray) {
        shineColors = colors
        shineShader = null
        invalidate()
    }

    /**
     * 开始流光动画
     */
    fun startShine() {
        setShineEnabled(true)
    }

    /**
     * 停止流光动画
     */
    fun stopShine() {
        setShineEnabled(false)
    }

    /**
     * 切换流光动画状态
     */
    fun toggleShine() {
        setShineEnabled(!shineEnabled)
    }

    /**
     * 检查流光特效是否启用
     */
    fun isShineEnabled(): Boolean {
        return shineEnabled
    }
    
    /**
     * 智能设置彩虹字体色值
     * 如果色值大于三个，自动开启流光特效；如果少于三个，不开启流光特效
     * @param colors 颜色数组
     */
    fun setRainbowColors(colors: IntArray) {
        if (colors.isEmpty()) return
        
        // 更新渐变颜色
        gradientColors = colors
        gradientShader = null // 重置着色器，下次绘制时重新创建
        
        // 根据颜色数量智能控制流光特效
        if (colors.size >= 3) {
            // 颜色数量大于等于3个，自动开启流光特效
            if (!shineEnabled) {
                shineEnabled = true
                shineShader = null // 重置流光着色器
                android.util.Log.d("ColorfulShineTextView", "自动开启流光特效，颜色数量: ${colors.size}")
            }
        } else {
            // 颜色数量少于3个，自动关闭流光特效
            if (shineEnabled) {
                shineEnabled = false
                android.util.Log.d("ColorfulShineTextView", "自动关闭流光特效，颜色数量: ${colors.size}")
            }
        }
        
        // 刷新显示
        invalidate()
    }
    
    /**
     * 智能设置彩虹字体色值（可变参数版本）
     * @param colors 颜色参数，支持可变参数
     */
     @JvmName("setRainbowColorsVararg")
    fun setRainbowColors(vararg colors: Int) {
        setRainbowColors(colors)
    }
    
    /**
     * 智能设置彩虹字体色值（颜色列表版本）
     * @param colorList 颜色列表
     */
    @JvmName("setRainbowColorsArray")
    fun setRainbowColors(colorList: List<Int>) {
        setRainbowColors(colorList.toIntArray())
    }
    
    /**
     * 智能设置彩虹字体色值（颜色资源版本）
     * @param context Context
     * @param colorResIds 颜色资源ID数组
     */
    fun setRainbowColors(context: Context, vararg colorResIds: Int) {
        val colors = colorResIds.map { ContextCompat.getColor(context, it) }.toIntArray()
        setRainbowColors(colors)
    }
    
    /**
     * 智能设置彩虹字体色值（颜色资源列表版本）
     * @param context Context
     * @param colorResIdList 颜色资源ID列表
     */
    fun setRainbowColors(context: Context, colorResIdList: List<Int>) {
        val colors = colorResIdList.map { ContextCompat.getColor(context, it) }.toIntArray()
        setRainbowColors(colors)
    }
    
    /**
     * 创建混合着色器：流光效果叠加在渐变之上
     */
    private fun createCombinedShader(): Shader? {
        return try {
            // 使用ComposeShader来混合流光和渐变效果
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.graphics.ComposeShader(shineShader!!, gradientShader!!, android.graphics.PorterDuff.Mode.SCREEN)
            } else {
                // 对于低版本，使用ADD模式或优先显示流光效果
                android.graphics.ComposeShader(shineShader!!, gradientShader!!, android.graphics.PorterDuff.Mode.ADD)
            }
        } catch (e: Exception) {
            // 如果混合失败，优先显示流光效果
            shineShader
        }
    }
    

}