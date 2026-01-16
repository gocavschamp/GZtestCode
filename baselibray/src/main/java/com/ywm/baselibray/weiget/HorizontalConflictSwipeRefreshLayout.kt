package com.ywm.baselibray.weiget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.blankj.utilcode.util.Utils
import com.ywm.baselibray.utils.UIUtils
import kotlin.math.abs

class HorizontalConflictSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    private var startX = 0f
    private var startY = 0f
    private var isBeingDragged = false
    private var isHorizontalDrag = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 记录按下位置
                startX = ev.x
                startY = ev.y
                isBeingDragged = false
                isHorizontalDrag = false
            }

            MotionEvent.ACTION_MOVE -> {
                // 如果已经判断为水平滑动，则不拦截事件
                if (isHorizontalDrag) {
                    return false
                }

                val currentX = ev.x
                val currentY = ev.y
                val dx = abs(currentX - startX)
                val dy = abs(currentY - startY)

                // 判断是否是水平滑动
                if (dx > dy && dx > touchSlop) {
                    isHorizontalDrag = true
                    return false // 不拦截，让子View（ViewPager2）处理
                }

                // 如果是垂直滑动，检查是否可以刷新
                if (dy > dx && dy > touchSlop) {
                    isBeingDragged = true
                    return super.onInterceptTouchEvent(ev)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isBeingDragged = false
                isHorizontalDrag = false
            }
        }

        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // 如果是水平滑动，不处理触摸事件
        if (isHorizontalDrag) {
            return false
        }

        return super.onTouchEvent(ev)
    }

    companion object {
        // 获取系统触摸阈值
        private val touchSlop = ViewConfiguration.get(Utils.getApp()).scaledTouchSlop
    }
}