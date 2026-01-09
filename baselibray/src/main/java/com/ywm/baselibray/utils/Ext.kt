package com.ywm.baselibray.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import com.ywm.baselibray.R

class Ext {
}
fun FrameLayout.enableRightSwipeToDismissSimple() {
    Log.d("SwipeDebug", "enableRightSwipeToDismissSimple called")

    var startX = 0f
    var startY = 0f
    var isTrackingGesture = false
    var isSwiping = false

    setOnTouchListener { v, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                Log.d("SwipeDebug", "ACTION_DOWN at (${event.x}, ${event.y})")
                startX = event.x
                startY = event.y
                isTrackingGesture = true
                isSwiping = false

                // 请求父容器不要拦截事件
                v.parent?.requestDisallowInterceptTouchEvent(true)

                // 返回true表示我们要处理这个触摸序列
                true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isTrackingGesture) {
                    return@setOnTouchListener false
                }

                val dx = event.x - startX
                val dy = event.y - startY

                // 检查是否是水平滑动
                if (!isSwiping && Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 20) {
                    isSwiping = true
                    Log.d("SwipeDebug", "Starting horizontal swipe, dx=$dx")
                }

                if (isSwiping && dx > 0) {
                    // 实时跟随手指移动
                    v.translationX = dx
                    // 添加淡出效果
                    v.alpha = 1 - (dx / v.width) * 0.5f
                    Log.d("SwipeDebug", "Swiping, translationX=${v.translationX}, alpha=${v.alpha}")
                    return@setOnTouchListener true
                }

                false
            }

            MotionEvent.ACTION_UP -> {
                Log.d("SwipeDebug", "ACTION_UP, isSwiping=$isSwiping")

                if (isSwiping) {
                    val dx = event.x - startX

                    // 如果滑动超过宽度的一半，完成动画
                    if (dx > v.width / 2) {
                        Log.d("SwipeDebug", "Swiped enough, completing animation")
                        completeSwipeOut(v, dx)
                    } else {
                        Log.d("SwipeDebug", "Not enough swipe, resetting")
                        resetSwipePosition(v)
                    }

                    isTrackingGesture = false
                    isSwiping = false
                    return@setOnTouchListener true
                }

                // 如果不是滑动，传递点击事件
                isTrackingGesture = false
                false
            }

            MotionEvent.ACTION_CANCEL -> {
                Log.d("SwipeDebug", "ACTION_CANCEL")
                if (isSwiping) {
                    resetSwipePosition(v)
                }
                isTrackingGesture = false
                isSwiping = false
                true
            }

            else -> {
                false
            }
        }
    }
}

private fun completeSwipeOut(view: View, currentTranslation: Float) {
    val screenWidth = view.resources.displayMetrics.widthPixels.toFloat()

    ObjectAnimator.ofFloat(view, "translationX", currentTranslation, screenWidth).apply {
        duration = 300
        interpolator = AccelerateInterpolator()
        addUpdateListener {
            view.alpha = 1 - (view.translationX / screenWidth) * 0.5f
        }
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                Log.d("SwipeDebug", "Swipe out animation complete")
                // 可选：隐藏视图
                // view.visibility = View.GONE

                // 重置以便重复使用
                view.postDelayed({
                    view.translationX = 0f
                    view.alpha = 1f
                }, 500)
            }
        })
        start()
    }
}

private fun resetSwipePosition(view: View) {
    ObjectAnimator.ofFloat(view, "translationX", view.translationX, 0f).apply {
        duration = 200
        interpolator = OvershootInterpolator()
        start()
    }

    ObjectAnimator.ofFloat(view, "alpha", view.alpha, 1f).apply {
        duration = 200
        start()
    }
}
// 修复版本：添加更详细的事件处理和调试
fun FrameLayout.enableRightSwipeToDismiss() {
    Log.d("SwipeDebug", "enableRightSwipeToDismiss called")

    // 确保 View 是可用状态
    this.isEnabled = true
    this.isClickable = true
    this.isFocusable = true
    this.isFocusableInTouchMode = true

    val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            Log.d("SwipeDebug", "onDown event at (${e.x}, ${e.y})")
            // 返回 true 表示我们想处理后续事件
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            Log.d("SwipeDebug", "onFling triggered")

            if (e1 != null && e2 != null) {
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y

                Log.d("SwipeDebug", "Fling: dx=$dx, dy=$dy, velocityX=$velocityX, velocityY=$velocityY")

                // 修正：右滑应该是从右向左，所以dx应该为负值？
                // 实际上，右滑（从屏幕右侧进入）意味着手指从右向左移动，所以e2.x < e1.x
                // 但你可能想要检测从左向右的滑动来滑出屏幕？
                // 根据你的需求：如果希望"向右滑出屏幕"，那么手指应该从左向右移动
                if (dx > 100 && Math.abs(dx) > Math.abs(dy) && velocityX > 500) {
                    Log.d("SwipeDebug", "Right swipe detected! Starting animation")
                    startRightSwipeOutAnimation(this@enableRightSwipeToDismiss)
                    return true
                }
            }
            return false
        }
    })

    // 使用一个变量来跟踪是否正在滑动
    var isSwiping = false

    // 设置触摸监听
    setOnTouchListener { v, event ->
        Log.d("SwipeDebug", "onTouch: action=${event.actionMasked}, x=${event.x}, y=${event.y}")

        // 首先让 GestureDetector 处理事件
        val handledByGestureDetector = gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 请求父容器不要拦截触摸事件
                v.parent?.requestDisallowInterceptTouchEvent(true)
                isSwiping = false
                // 返回 true 以接收后续事件
                true
            }
            MotionEvent.ACTION_MOVE -> {
                if (handledByGestureDetector) {
                    isSwiping = true
                    true
                } else {
                    // 如果没有检测到滑动，允许事件传递
                    false
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 如果是滑动结束，消费事件
                if (isSwiping) {
                    isSwiping = false
                    true
                } else {
                    // 如果不是滑动，传递点击事件
                    false
                }
            }
            else -> {
                false
            }
        }
    }

    Log.d("SwipeDebug", "Touch listener set up complete")

    // 为了调试，添加一个OnClickListener看看点击事件是否正常
    setOnClickListener {
        Log.d("SwipeDebug", "FrameLayout clicked directly")
    }
}
// 修复版本：添加更详细的事件处理和调试
//@SuppressLint("ClickableViewAccessibility")
//fun FrameLayout.enableRightSwipeToDismiss() {
//    Log.d("SwipeDebug", "enableRightSwipeToDismiss called")
//
//    // 确保 View 是可用状态
//    this.isEnabled = true
//    this.isClickable = true
//    this.isFocusable = true
//    this.isFocusableInTouchMode = true
//
//    val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
//
//        override fun onDown(e: MotionEvent): Boolean {
//            Log.d("SwipeDebug", "onDown event at (${e.x}, ${e.y})")
//            // 返回 true 表示我们想处理后续事件
//            return true
//        }
//
//        override fun onFling(
//            e1: MotionEvent?,
//            e2: MotionEvent,
//            velocityX: Float,
//            velocityY: Float
//        ): Boolean {
//            Log.d("SwipeDebug", "onFling triggered")
//
//            if (e1 != null && e2 != null) {
//                val dx = e2.x - e1.x
//                val dy = e2.y - e1.y
//
//                Log.d("SwipeDebug", "Fling: dx=$dx, dy=$dy, velocityX=$velocityX, velocityY=$velocityY")
//
//                // 修正：右滑应该是从右向左，所以dx应该为负值？
//                // 实际上，右滑（从屏幕右侧进入）意味着手指从右向左移动，所以e2.x < e1.x
//                // 但你可能想要检测从左向右的滑动来滑出屏幕？
//                // 根据你的需求：如果希望"向右滑出屏幕"，那么手指应该从左向右移动
//                if (dx > 100 && Math.abs(dx) > Math.abs(dy) && velocityX > 500) {
//                    Log.d("SwipeDebug", "Right swipe detected! Starting animation")
//                    startRightSwipeOutAnimation(this@enableRightSwipeToDismiss)
//                    return true
//                }
//            }
//            return false
//        }
//    })
//
//    // 使用一个变量来跟踪是否正在滑动
//    var isSwiping = false
//
//    // 设置触摸监听
//    setOnTouchListener { v, event ->
//        Log.d("SwipeDebug", "onTouch: action=${event.actionMasked}, x=${event.x}, y=${event.y}")
//
//        // 首先让 GestureDetector 处理事件
//        val handledByGestureDetector = gestureDetector.onTouchEvent(event)
//
//        when (event.actionMasked) {
//            MotionEvent.ACTION_DOWN -> {
//                // 请求父容器不要拦截触摸事件
//                v.parent?.requestDisallowInterceptTouchEvent(true)
//                isSwiping = false
//                // 返回 true 以接收后续事件
//                true
//            }
//            MotionEvent.ACTION_MOVE -> {
//                if (handledByGestureDetector) {
//                    isSwiping = true
//                    true
//                } else {
//                    // 如果没有检测到滑动，允许事件传递
//                    false
//                }
//            }
//            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                // 如果是滑动结束，消费事件
//                if (isSwiping) {
//                    isSwiping = false
//                    true
//                } else {
//                    // 如果不是滑动，传递点击事件
//                    false
//                }
//            }
//            else -> {
//                false
//            }
//        }
//    }
//
//    Log.d("SwipeDebug", "Touch listener set up complete")
//
//    // 为了调试，添加一个OnClickListener看看点击事件是否正常
//    setOnClickListener {
//        Log.d("SwipeDebug", "FrameLayout clicked directly")
//    }
//}
// 修复版本：添加更详细的事件处理和调试
@SuppressLint("ClickableViewAccessibility")
//fun FrameLayout.enableRightSwipeToDismiss() {
//    Log.d("SwipeDebug", "enableRightSwipeToDismiss called")
//
//    // 确保 View 是可用状态
//    this.isEnabled = true
//    this.isClickable = true
//    this.isFocusable = true
//    this.isFocusableInTouchMode = true
//
//    val gestureDetector =
//        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
//
//            override fun onDown(e: MotionEvent): Boolean {
//                Log.d("SwipeDebug", "onDown event at (${e.x}, ${e.y})")
//                // 返回 true 表示我们想处理后续事件
//                return true
//            }
//
//            override fun onFling(
//                e1: MotionEvent?,
//                e2: MotionEvent,
//                velocityX: Float,
//                velocityY: Float
//            ): Boolean {
//                Log.d("SwipeDebug", "onFling triggered")
//
//                if (e1 != null && e2 != null) {
//                    val dx = e2.x - e1.x
//                    val dy = e2.y - e1.y
//
//                    Log.d(
//                        "SwipeDebug",
//                        "Fling: dx=$dx, dy=$dy, velocityX=$velocityX, velocityY=$velocityY"
//                    )
//
//                    // 修正：右滑应该是从右向左，所以dx应该为负值？
//                    // 实际上，右滑（从屏幕右侧进入）意味着手指从右向左移动，所以e2.x < e1.x
//                    // 但你可能想要检测从左向右的滑动来滑出屏幕？
//                    // 根据你的需求：如果希望"向右滑出屏幕"，那么手指应该从左向右移动
//                    if (dx > 100 && Math.abs(dx) > Math.abs(dy) && velocityX > 500) {
//                        Log.d("SwipeDebug", "Right swipe detected! Starting animation")
//                        startRightSwipeOutAnimation(this@enableRightSwipeToDismiss)
//                        return true
//                    }
//                }
//                return false
//            }
//        })
//
//    // 使用一个变量来跟踪是否正在滑动
//    var isSwiping = false
//
//    // 设置触摸监听
//    setOnTouchListener { v, event ->
//        Log.d("SwipeDebug", "onTouch: action=${event.actionMasked}, x=${event.x}, y=${event.y}")
//
//        // 首先让 GestureDetector 处理事件
//        val handledByGestureDetector = gestureDetector.onTouchEvent(event)
//
//        when (event.actionMasked) {
//            MotionEvent.ACTION_DOWN -> {
//                // 请求父容器不要拦截触摸事件
//                v.parent?.requestDisallowInterceptTouchEvent(true)
//                isSwiping = false
//                // 返回 true 以接收后续事件
//                true
//            }
//            MotionEvent.ACTION_MOVE -> {
//                if (handledByGestureDetector) {
//                    isSwiping = true
//                    true
//                } else {
//                    // 如果没有检测到滑动，允许事件传递
//                    false
//                }
//            }
//            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                // 如果是滑动结束，消费事件
//                if (isSwiping) {
//                    isSwiping = false
//                    true
//                } else {
//                    // 如果不是滑动，传递点击事件
//                    false
//                }
//            }
//            else -> {
//                false
//            }
//        }
//    }
//
//    Log.d("SwipeDebug", "Touch listener set up complete")
//
//    // 为了调试，添加一个OnClickListener看看点击事件是否正常
//    setOnClickListener {
//        Log.d("SwipeDebug", "FrameLayout clicked directly")
//    }
//}
// 增强的动画函数
private fun startRightSwipeOutAnimation(view: View) {
    Log.d("SwipeDebug", "startRightSwipeOutAnimation called")

    // 检查是否已经在动画中
    if (view.getTag(R.id.swipe_animating) == true) {
        Log.d("SwipeDebug", "Animation already in progress, skipping")
        return
    }

    view.setTag(R.id.swipe_animating, true)

    val screenWidth = view.resources.displayMetrics.widthPixels.toFloat()
    Log.d("SwipeDebug", "Screen width: $screenWidth, View width: ${view.width}")

    // 方法1：使用 ViewPropertyAnimator
    view.animate()
        .translationX(screenWidth)
        .alpha(0.5f)
        .scaleX(0.8f)
        .scaleY(0.8f)
        .setDuration(300)
        .setInterpolator(AccelerateInterpolator())
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                Log.d("SwipeDebug", "Animation started")
            }

            override fun onAnimationEnd(animation: Animator) {
                Log.d("SwipeDebug", "Animation ended")
                view.setTag(R.id.swipe_animating, false)
                // 可选：隐藏视图
                // view.visibility = View.GONE

                // 重置位置以便重复使用
                view.postDelayed({
                    view.translationX = 0f
                    view.alpha = 1f
                    view.scaleX = 1f
                    view.scaleY = 1f
                    Log.d("SwipeDebug", "View properties reset")
                }, 100)
            }

            override fun onAnimationCancel(animation: Animator) {
                Log.d("SwipeDebug", "Animation cancelled")
                view.setTag(R.id.swipe_animating, false)
            }
        })
        .start()
}
// 使用自定义触摸处理逻辑
@SuppressLint("ClickableViewAccessibility")
fun FrameLayout.enableRightSwipeToDismissV2() {
    Log.d("SwipeDebug", "enableRightSwipeToDismissV2 called")

    var startX = 0f
    var startY = 0f
    var isSwiping = false
    var hasMoved = false

    // 添加一个自定义的触摸监听器
    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                isSwiping = false
                hasMoved = false
                Log.d("SwipeDebug", "ACTION_DOWN at ($startX, $startY)")

                // 允许父容器不拦截触摸事件，确保子View可以接收点击
                v.parent?.requestDisallowInterceptTouchEvent(true)
                true  // 消费DOWN事件以接收后续事件
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - startX
                val dy = event.y - startY

                // 检查是否主要是水平移动
                if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 10) {
                    hasMoved = true

                    // 只处理向右滑动（dx > 0）
                    if (dx > 0) {
                        isSwiping = true

                        // 实时跟随手指移动
                        v.translationX = dx

                        // 添加淡出效果
                        val alpha = 1 - (dx / v.width) * 0.5f
                        v.alpha = alpha.coerceIn(0.5f, 1f)

                        Log.d("SwipeDebug", "Swiping right: dx=$dx, translationX=${v.translationX}, alpha=$alpha")
                        return@setOnTouchListener true
                    }
                }
                false
            }

            MotionEvent.ACTION_UP -> {
                Log.d("SwipeDebug", "ACTION_UP: isSwiping=$isSwiping, hasMoved=$hasMoved")

                if (isSwiping) {
                    val dx = event.x - startX
                    val vWidth = v.width.toFloat()

                    // 如果滑动超过宽度的一半，或者滑动速度很快，则完成动画
                    if (dx > vWidth * 0.3f) {
                        Log.d("SwipeDebug", "Swiped enough ($dx > ${vWidth * 0.3f}), completing animation")
                        completeSwipeOut(v)
                    } else {
                        Log.d("SwipeDebug", "Swiped back ($dx <= ${vWidth * 0.3f}), resetting")
                        resetViewPosition(v)
                    }
                    true
                } else {
                    if (!hasMoved) {
                        // 没有移动，可能是一个点击事件，传递给子View
                        Log.d("SwipeDebug", "No movement, passing as click to children")
                        performClick()
                    }
                    false
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                Log.d("SwipeDebug", "ACTION_CANCEL")
                if (isSwiping) {
                    resetViewPosition(v)
                }
                true
            }

            else -> false
        }
    }

    // 确保点击事件正常工作
    setOnClickListener {
        Log.d("SwipeDebug", "FrameLayout clicked")
    }
}

// 完成滑动动画
private fun completeSwipeOut(view: View) {
    val screenWidth = view.resources.displayMetrics.widthPixels.toFloat()
    val currentTranslation = view.translationX

    ObjectAnimator.ofFloat(view, "translationX", currentTranslation, screenWidth).apply {
        duration = 300
        interpolator = AccelerateInterpolator()
        addUpdateListener {
            val progress = (view.translationX - currentTranslation) / (screenWidth - currentTranslation)
            view.alpha = 1 - progress * 0.5f
            Log.d("SwipeDebug", "Animating progress: $progress, translationX=${view.translationX}")
        }
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                Log.d("SwipeDebug", "Swipe out animation complete")
                // view.visibility = View.GONE  // 可选：隐藏视图

                // 重置以便重复使用
                view.postDelayed({
                    view.translationX = 0f
                    view.alpha = 1f
                    Log.d("SwipeDebug", "View position reset")
                }, 500)
            }
        })
        start()
    }
}

// 重置视图位置
private fun resetViewPosition(view: View) {
    ObjectAnimator.ofFloat(view, "translationX", view.translationX, 0f).apply {
        duration = 200
        interpolator = OvershootInterpolator(2f)
        start()
    }

    ObjectAnimator.ofFloat(view, "alpha", view.alpha, 1f).apply {
        duration = 200
        start()
    }
}