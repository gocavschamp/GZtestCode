package com.ywm.baselibray.weiget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout

// 自定义 FrameLayout
class SwipeDismissConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var gestureDetector: GestureDetector
    private var isAnimating = false

    init {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // 检测右滑手势：从右向左滑动（X轴速度为正且大于阈值）
                if (e1 != null && e2 != null) {
                    val dx = e2.x - e1.x
                    // 右滑条件：滑动距离足够且X轴速度足够大
                    if (dx > minSwipeDistance && velocityX > minSwipeVelocity) {
                        startSwipeOutAnimation()
                        return true
                    }
                }
                return false
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                // 也可以使用 onScroll 检测滑动
                if (e1 != null && e2 != null) {
                    val dx = e2.x - e1.x
                    // 检测水平滑动比例（水平/垂直）
                    if (Math.abs(dx) > Math.abs(e2.y - e1.y) * 2) {
                        // 水平滑动为主
                        if (dx > minSwipeDistance) {
                            startSwipeOutAnimation()
                            return true
                        }
                    }
                }
                return super.onScroll(e1, e2, distanceX, distanceY)
            }
        })
    }

    companion object {
        private const val minSwipeDistance = 100  // 最小滑动距离（像素）
        private const val minSwipeVelocity = 500  // 最小滑动速度（像素/秒）
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // 将事件传递给 GestureDetector
        gestureDetector.onTouchEvent(ev)
        // 不拦截子 View 的事件，只在检测到右滑时处理
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 在父容器未拦截时，自己处理事件
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun startSwipeOutAnimation() {
        if (isAnimating) return

        isAnimating = true
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()

        // 创建右滑动画
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = AccelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedFraction

                // 向右滑出屏幕
                translationX = screenWidth * progress

                // 可选：添加淡出效果
                alpha = 1 - progress * 0.5f

//                // 可选：添加缩小效果
//                val scale = 1 - progress * 0.2f
//                scaleX = scale
//                scaleY = scale
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                    // 动画结束后可选操作
                     visibility = View.GONE
                    onDismissListener.invoke()
                }
            })

            start()
        }
    }

private var onDismissListener: () -> Unit? = {}
    fun setOndismissListener(onDismissListener: () -> Unit? ) {
        this.onDismissListener = onDismissListener
    }
}