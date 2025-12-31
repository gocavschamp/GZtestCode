package com.example.firstapplication.ui.widgit

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.max

class ShineTextView : AppCompatTextView {

    companion object {
        private const val TAG = "ShineTextView"

        // 预定义的流光颜色
        private val DEFAULT_SHINE_COLORS = intArrayOf(
            0x66FFFFFF.toInt(), // 半透明白色
            0x66FFFFFF.toInt(), // 半透明白色
        )
    }

    // 流光效果相关变量
    private var mLinearGradient: LinearGradient? = null
    private val mGradientMatrix = Matrix()
    private var mTranslate = 0f
    private var mAnimating = false

    // 流光参数
    private var mShaderWidth = 0f
    private var mShaderColors: IntArray = DEFAULT_SHINE_COLORS

    // 动画速度控制 (px/帧)
    private var mAnimationSpeed = 2f

    // 延迟初始化标志
    private var isViewReady = false

    // 方案1：使用三个独立的构造函数，避免 @JvmOverloads 可能的问题
    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        // 这里什么都不做，确保构造函数安全
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "onAttachedToWindow")

        // 延迟初始化，确保视图已附加到窗口
        if (!isViewReady) {
            isViewReady = true

            // 只在第一次附加时设置默认值
            // 不要在这里设置文本属性，让XML或代码控制

            Log.d(TAG, "View is ready")
        }

        // 当视图获得焦点时开始动画
        if (visibility == VISIBLE && windowVisibility == VISIBLE) {
            startShimmer()
        }
    }

    override fun onDetachedFromWindow() {
        Log.d(TAG, "onDetachedFromWindow - cleaning up")
        stopShimmer()
        mLinearGradient = null
        isViewReady = false
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 确保宽度大于0
        if (w > 0 && h > 0) {
            setupShader(w)
        } else {
            // 如果宽度为0，延迟设置
            post {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    setupShader(measuredWidth)
                }
            }
        }
    }

    private fun setupShader(viewWidth: Int) {
        if (viewWidth <= 0) return

        try {
            mShaderWidth = viewWidth / 3f

            mLinearGradient = LinearGradient(
                -mShaderWidth, 0f, 0f, 0f,
                mShaderColors,
                floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f), // 颜色位置分布
                Shader.TileMode.CLAMP
            )

            // 如果动画正在运行，立即设置shader
            if (mAnimating) {
                paint.shader = mLinearGradient
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up shader", e)
        }
    }

    override fun onDraw(canvas: Canvas) {
        // 检查视图是否已准备好
        if (!isViewReady || measuredWidth <= 0 || measuredHeight <= 0) {
            // 使用普通方式绘制，避免shader影响
            val currentShader = paint.shader
            paint.shader = null
            super.onDraw(canvas)
            paint.shader = currentShader
            return
        }

        // 在调用父类绘制前更新shader矩阵
        updateShaderMatrix()
        super.onDraw(canvas)
    }

    private fun updateShaderMatrix() {
        if (mLinearGradient == null || !mAnimating) return

        try {
            mTranslate += mAnimationSpeed
            if (mTranslate > measuredWidth + mShaderWidth) {
                mTranslate = -mShaderWidth
            }

            mGradientMatrix.setTranslate(mTranslate, 0f)
            mLinearGradient?.setLocalMatrix(mGradientMatrix)

            // 只在动画运行时才请求重绘
            if (mAnimating) {
                postInvalidateOnAnimation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating shader matrix", e)
            stopShimmer() // 出错时停止动画
        }
    }

    /**
     * 开始流光动画
     */
    fun startShimmer() {
        if (mAnimating || !isViewReady) return

        try {
            mAnimating = true
            mTranslate = -mShaderWidth

            // 确保shader已设置
            if (mLinearGradient != null) {
                paint.shader = mLinearGradient
            } else {
                // 如果shader还没设置，先设置它
                setupShader(measuredWidth)
                paint.shader = mLinearGradient
            }

            invalidate()
            Log.d(TAG, "Shimmer animation started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting shimmer", e)
            mAnimating = false
        }
    }

    /**
     * 停止流光动画
     */
    fun stopShimmer() {
        if (!mAnimating) return

        mAnimating = false
        paint.shader = null // 移除shader，恢复正常文本
        Log.d(TAG, "Shimmer animation stopped")
    }

    /**
     * 设置流光颜色
     */
    fun setShimmerColors(colors: IntArray) {
        if (colors.size >= 3) {
            mShaderColors = colors
            setupShader(measuredWidth)
            if (mAnimating) {
                invalidate()
            }
        }
    }

    /**
     * 设置动画速度
     */
    fun setAnimationSpeed(speed: Float) {
        mAnimationSpeed = max(0.1f, speed)
    }

    /**
     * 检查动画是否运行中
     */
    fun isShimmering(): Boolean = mAnimating

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)

        // 当可见性改变时控制动画
        if (visibility == VISIBLE && isViewReady && windowVisibility == VISIBLE) {
            startShimmer()
        } else {
            stopShimmer()
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)

        // 当窗口可见性改变时控制动画
        if (visibility == VISIBLE && isViewReady && this.visibility == VISIBLE) {
            startShimmer()
        } else {
            stopShimmer()
        }
    }
}