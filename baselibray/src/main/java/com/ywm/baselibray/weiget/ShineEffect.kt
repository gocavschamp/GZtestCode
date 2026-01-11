package com.ywm.baselibray.weiget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

// ShineEffect.kt
// ShineEffect.kt
class ShineEffect(private val targetView: View) {
    private val shineDrawable = ShineDrawable()
    private var animator: ValueAnimator? = null
    private var isStarted = false
    private var shouldAutoStart = false

    // 监听可见性变化
    private val onLayoutChangeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        updateDrawableBounds()
    }

    // 监听View的attach/detach状态
    private val onAttachStateChangeListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            if (shouldAutoStart) {
                startShine()
            }
        }

        override fun onViewDetachedFromWindow(v: View) {
            stopShine()
        }
    }

    init {
        setup()
    }

    private fun setup() {
        // 设置Drawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            targetView.foreground = shineDrawable
        } else {
            // API 23以下使用Overlay或background
            targetView.background = CompoundDrawableWrapper(targetView.background, shineDrawable)
        }

        // 监听布局变化以更新Drawable边界
        targetView.addOnLayoutChangeListener(onLayoutChangeListener)

        // 监听attach/detach状态
        targetView.addOnAttachStateChangeListener(onAttachStateChangeListener)

        // 初始更新边界
        updateDrawableBounds()
    }

    private fun updateDrawableBounds() {
        if (targetView.width > 0 && targetView.height > 0) {
            shineDrawable.setBounds(0, 0, targetView.width, targetView.height)
        }
    }

    fun startShine() {
        if (isStarted || targetView.width == 0) return

        animator?.cancel()
        isStarted = true

        // 创建扫光动画
        animator = ObjectAnimator.ofFloat(
            -targetView.width * 0.3f,
            targetView.width * 1.3f
        ).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                shineDrawable.setOffset(value, targetView.height / 2f)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (isStarted) {
                        // 循环动画
                        start()
                    }
                }
            })
            start()
        }
    }

    fun stopShine() {
        isStarted = false
        animator?.cancel()
        animator = null
        // 重置位置
        shineDrawable.setOffset(-targetView.width * 0.3f, targetView.height / 2f)
        targetView.invalidate()
    }

    /**
     * 设置是否自动启动（根据View的可见性）
     */
    fun setAutoStart(autoStart: Boolean) {
        this.shouldAutoStart = autoStart

        if (autoStart) {
            // 检查当前状态
            if (targetView.isAttachedToWindow &&
                targetView.visibility == View.VISIBLE &&
                targetView.width > 0) {
                startShine()
            }
        } else {
            stopShine()
        }
    }

    /**
     * 处理可见性变化
     */
    fun onVisibilityChanged(isVisible: Boolean) {
        if (shouldAutoStart) {
            if (isVisible) {
                startShine()
            } else {
                stopShine()
            }
        }
    }

    /**
     * 清理资源
     */
    fun release() {
        stopShine()
        targetView.removeOnLayoutChangeListener(onLayoutChangeListener)
        targetView.removeOnAttachStateChangeListener(onAttachStateChangeListener)
    }
}

// 用于API 23以下的Drawable包装器
class CompoundDrawableWrapper(
    private val original: Drawable?,
    private val overlay: Drawable
) : Drawable() {

    override fun draw(canvas: Canvas) {
        original?.draw(canvas)
        overlay.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        original?.alpha = alpha
        overlay.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        original?.colorFilter = colorFilter
        overlay.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return original?.intrinsicWidth ?: overlay.intrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        return original?.intrinsicHeight ?: overlay.intrinsicHeight
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        original?.setBounds(left, top, right, bottom)
        overlay.setBounds(left, top, right, bottom)
    }
}