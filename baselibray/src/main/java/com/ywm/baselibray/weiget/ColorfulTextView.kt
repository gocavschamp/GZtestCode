package com.ywm.baselibray.weiget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Shader.TileMode
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.ywm.baselibray.R


/**
 * Created  on 24/11/18_Mon
 */
open class ColorfulTextView @JvmOverloads constructor(
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

    override fun onDraw(canvas: Canvas) {
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
            paint.setShader(outlineShader)
            paint.strokeWidth = outlineWidth
            super.onDraw(canvas)
        }

        if (gradientColors.isNotEmpty() && gradientShader == null) {
            if (gradientColors.size == 1) {
                gradientColors = intArrayOf(gradientColors[0], gradientColors[0])
            }
            val layout = layout
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

        paint.style = Paint.Style.FILL
        paint.shader = gradientShader
        paint.textSize = textSize
        paint.typeface = typeface
        paint.color = currentTextColor
        paint.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        super.onDraw(canvas)
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        gradientShader = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gradientShader = null
    }
}
