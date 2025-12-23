package com.ywm.baselibray.floatview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.ywm.baselibray.R
// FloatingNoticeView.kt
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup

class FloatingNoticeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var textMessage: TextView
    private var dismissRunnable: Runnable? = null
    private var onDismissCallback: (() -> Unit)? = null
    private var exitAnimator: Animator? = null // 持有动画引用以便管理

    init {
        // 初始化布局
        LayoutInflater.from(context).inflate(R.layout.layout_floating_notice, this, true)
        textMessage = findViewById(R.id.textMessage)
        // 初始位置设置在屏幕右侧之外
        translationX = width.toFloat()
    }

    /**
     * 设置通知内容
     */
    fun setNoticeMessage(message: NoticeMessage) {
        textMessage.text = message.content
    }

    /**
     * 显示通知并自动消失
     * @param delayMillis 停留时间（毫秒）
     * @param onDismiss 消失后的回调
     */
    fun showWithAutoDismiss(delayMillis: Long = 3000L, onDismiss: () -> Unit) {
        // 1. 先清理之前的任何未完成的任务和动画
        dismissRunnable?.let { removeCallbacks(it) }
        exitAnimator?.cancel() // 取消可能正在进行的旧动画
        onDismissCallback = null // 清空旧回调

        // 2. 设置新的回调
        this.onDismissCallback = onDismiss

        // 3. 开始入场动画并设置延迟消失
        startEnterAnimation()
        dismissRunnable = Runnable {
            startExitAnimation {
                removeFromParent()
                // 4. 【核心修复】确保回调被调用，并添加异常捕获
                try {
                    onDismissCallback?.invoke()
                } catch (e: Exception) {
                    Log.e("FloatingNoticeView", "onDismissCallback failed: ${e.message}")
                } finally {
                    // 5. 最终清理，避免重复调用
                    onDismissCallback = null
                }
            }
        }
        postDelayed(dismissRunnable!!, delayMillis)
    }

    /**
     * 入场动画（从右侧滑入）
     */
    private fun startEnterAnimation() {
        val animator = ObjectAnimator.ofFloat(this, "translationX", translationX, 0f)
        animator.duration = 500
        animator.start()
    }

    /**
     * 退场动画（向左侧滑出）
     */
    private fun startExitAnimation(onEnd: () -> Unit) {
        // 1. 取消可能存在的旧动画
        exitAnimator?.cancel()

        // 2. 创建新动画
        exitAnimator = ObjectAnimator.ofFloat(this, "translationX", 0f, -width.toFloat()).apply {
            duration = 500
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    // 3. 【核心修复】确保onEnd被调用，并分离动画引用
                    try {
                        onEnd.invoke()
                    } catch (e: Exception) {
                        Log.e("FloatingNoticeView", "Exit animation onEnd failed: ${e.message}")
                    } finally {
                        exitAnimator = null // 动画结束，释放引用
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    // 4. 如果动画被取消，也尝试执行清理和回调，防止悬挂
                    try {
                        onEnd.invoke()
                    } catch (e: Exception) {
                        Log.e("FloatingNoticeView", "Exit animation onCancel failed: ${e.message}")
                    } finally {
                        exitAnimator = null
                    }
                }
            })
            start()
        }
    }

    /**
     * 从父视图中移除自己
     */
    private fun removeFromParent() {
        (parent as? ViewGroup)?.removeView(this@FloatingNoticeView)
    }

    /**
     * 关键：在视图被移除时清理资源，防止内存泄漏[5,9](@ref)
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
            // 在视图被移除时，主动取消所有异步操作
            dismissRunnable?.let { removeCallbacks(it) }
            exitAnimator?.cancel() // 取消动画也会触发 onAnimationCancel，从而执行回调
            // 注意：此处不直接清空 onDismissCallback，因为取消动画会触发回调进行清理
    }
}

// NoticeMessage.kt
data class NoticeMessage(
    val content: String, // 通知内容，例如 "xxx成为了尊敬的VIP用户"
    val timestamp: Long = System.currentTimeMillis() // 时间戳，用于排序或显示
)