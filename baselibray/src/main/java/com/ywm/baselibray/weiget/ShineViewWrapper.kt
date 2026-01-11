package com.ywm.baselibray.weiget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class ShineViewWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var shineEffect: ShineEffect? = null
    private var isAutoStart = true

    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        // 从XML属性读取配置
//        val typedArray = context.obtainStyledAttributes(
//            attrs,
//            R.styleable.ShinViewWrapper
//        )
//        isAutoStart = typedArray.getBoolean(
//            R.styleable.ShinViewWrapper_autoStart,
//            true
//        )
//        typedArray.recycle()

        // 延迟初始化，等待子View布局完成
        post {
            setupShineEffect()
        }
    }

    private fun setupShineEffect() {
        if (childCount > 0) {
            val targetView = getChildAt(0)
            shineEffect = ShineEffect(targetView).apply {
                setAutoStart(isAutoStart)
            }
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        shineEffect?.onVisibilityChanged(visibility == View.VISIBLE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        shineEffect?.setAutoStart(isAutoStart)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        shineEffect?.stopShine()
    }

    fun startShine() {
        shineEffect?.startShine()
    }

    fun stopShine() {
        shineEffect?.stopShine()
    }

    fun setAutoStart(autoStart: Boolean) {
        this.isAutoStart = autoStart
        shineEffect?.setAutoStart(autoStart)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // 确保至少有一个子View
        if (childCount == 0) {
            throw IllegalStateException("ShineViewWrapper must contain exactly one child view")
        }
    }
}
