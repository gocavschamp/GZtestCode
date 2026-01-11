package com.ywm.baselibray.weiget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs

class SwipeDismissConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var gestureDetector: GestureDetector
    private var isAnimating = false
    private var startX = 0f
    private var startY = 0f
    private var isSwiping = false
    private var hasMoved = false
    private val touchSlop: Int
    private var lastInterceptedChild: View? = null
    private var isTouchHandledByChild = false

    init {
        val viewConfiguration = ViewConfiguration.get(context)
        touchSlop = viewConfiguration.scaledTouchSlop
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null && e2 != null) {
                    val dx = e2.x - e1.x
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
                if (e1 != null && e2 != null) {
                    val dx = e2.x - e1.x
                    if (abs(dx) > abs(e2.y - e1.y) * 2 && dx > minSwipeDistance) {
                        startSwipeOutAnimation()
                        return true
                    }
                }
                return false
            }
        })

        isClickable = true
        isFocusable = true
    }

    companion object {
        private const val minSwipeDistance = 100
        private const val minSwipeVelocity = 500
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                isSwiping = false
                hasMoved = false
                isTouchHandledByChild = false
                // 查找被触摸的子View
                lastInterceptedChild = findChildAt(this, ev.x, ev.y)
                // 如果子View可点击，先让子View处理
                if (lastInterceptedChild?.isClickable == true || lastInterceptedChild?.isLongClickable == true) {
                    return false
                }
                // 否则，自己处理
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isSwiping) {
                    return true
                }

                val dx = ev.x - startX
                val dy = ev.y - startY

                if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                    hasMoved = true
                    // 判断是否是水平滑动（右滑）
                    if (abs(dx) > abs(dy) && dx > 0) {
                        isSwiping = true
                        // 如果之前有子View在处理，发送CANCEL事件
                        if (lastInterceptedChild != null) {
                            val cancelEvent = MotionEvent.obtain(ev)
                            cancelEvent.action = MotionEvent.ACTION_CANCEL
                            lastInterceptedChild?.dispatchTouchEvent(cancelEvent)
                            cancelEvent.recycle()
                        }
                        // 请求父容器不要拦截事件
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }

                // 如果不是右滑，不拦截
                return false
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val shouldIntercept = isSwiping
                // 重置状态
                if (ev.actionMasked == MotionEvent.ACTION_UP) {
                    isSwiping = false
                    hasMoved = false
                }
                return shouldIntercept
            }
        }

        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 先让GestureDetector尝试处理
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isSwiping && hasMoved) {
                    // 检查是否开始滑动
                    val dx = event.x - startX
                    val dy = event.y - startY

                    if (abs(dx) > abs(dy) && dx > touchSlop) {
                        isSwiping = true
                    }
                }

                if (isSwiping) {
                    val dx = event.x - startX
                    val translationX = dx.coerceAtLeast(0f)
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isSwiping) {
                    val dx = event.x - startX

                    if (dx > width * 0.1f) {
                        startSwipeOutAnimation()
                    } else {
                    }

                    isSwiping = false
                    hasMoved = false
                    return true
                } else {
                    // 如果不是滑动，可能是点击
                    if (!hasMoved) {
                        performClick()
                    }
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                isSwiping = false
                hasMoved = false
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    // 重写dispatchTouchEvent，确保正确处理事件分发
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // 如果正在动画中，不处理触摸事件
        if (isAnimating) {
            return true
        }

        // 如果检测到滑动，自己处理
        if (isSwiping) {
            return onTouchEvent(ev)
        }

        return super.dispatchTouchEvent(ev)
    }

    private fun startSwipeOutAnimation() {
        if (isAnimating) return

        isAnimating = true
        val currentTranslationX = translationX
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()

        ValueAnimator.ofFloat(currentTranslationX, screenWidth).apply {
            duration = 300
            interpolator = AccelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                translationX = value
                val progress = (value - currentTranslationX) / (screenWidth - currentTranslationX)
                alpha = 1 - progress * 0.5f
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                    visibility = View.GONE
                    resetViewToStartPosition()
                    onDismissListener.invoke()
                }
            })

            start()
        }
    }


    private fun resetViewToStartPosition() {
        translationX = 0f
        alpha = 1f
        scaleX = 1f
        scaleY = 1f
        rotation = 0f
    }

    private fun findChildAt(parent: ViewGroup, x: Float, y: Float): View? {
        for (i in parent.childCount - 1 downTo 0) {
            val child = parent.getChildAt(i)
            if (child.visibility == View.VISIBLE) {
                val rect = Rect()
                child.getHitRect(rect)
                if (rect.contains(x.toInt(), y.toInt())) {
                    return child
                }
            }
        }
        return null
    }

    private var onDismissListener: () -> Unit = {}
    fun setOnDismissListener(onDismissListener: () -> Unit) {
        this.onDismissListener = onDismissListener
    }
}