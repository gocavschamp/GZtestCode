package com.ywm.baselibray.weiget

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat

class RollingTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var currentTextView: TextView
    private var incomingView: TextView? = null
    private var outgoingView: TextView? = null
    private var isAnimating = false
    private var pendingText: CharSequence? = null

    var animationDuration: Long = 300
    var interpolator = AccelerateDecelerateInterpolator()

    init {
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        ensureTemplateTextView()
    }

    private fun ensureTemplateTextView(): TextView {
        if (::currentTextView.isInitialized) return currentTextView

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is TextView) {
                currentTextView = child
                return currentTextView
            }
        }

        currentTextView = createDefaultTextView()
        addView(currentTextView)
        return currentTextView
    }

    private fun createDefaultTextView(): TextView {
        return AppCompatTextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
    }

    private fun createTextViewFrom(template: TextView): TextView {
        return AppCompatTextView(context).apply {
            layoutParams = LayoutParams(template.layoutParams)
            setTextColor(template.currentTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, template.textSize)
            gravity = template.gravity
            typeface = template.typeface
            setLineSpacing(template.lineSpacingExtra, template.lineSpacingMultiplier)
            compoundDrawablePadding = template.compoundDrawablePadding
            setPaddingRelative(template.paddingStart, template.paddingTop, template.paddingEnd, template.paddingBottom)
            copyCompoundDrawables(from = template, to = this)
            id = View.NO_ID
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!::currentTextView.isInitialized) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val horizontalPadding = paddingLeft + paddingRight
        val verticalPadding = paddingTop + paddingBottom

        fun measureChildTextView(child: TextView) {
            val lp = child.layoutParams
            val childWidthSpec = getChildMeasureSpec(widthMeasureSpec, horizontalPadding, lp.width)
            val childHeightSpec = getChildMeasureSpec(heightMeasureSpec, verticalPadding, lp.height)
            child.measure(childWidthSpec, childHeightSpec)
        }

        measureChildTextView(currentTextView)

        var maxChildWidth = currentTextView.measuredWidth
        var maxChildHeight = currentTextView.measuredHeight

        val outView = outgoingView
        if (outView != null && outView.parent == this) {
            measureChildTextView(outView)
            maxChildWidth = maxOf(maxChildWidth, outView.measuredWidth)
            maxChildHeight = maxOf(maxChildHeight, outView.measuredHeight)
        }

        val inView = incomingView
        if (inView != null && inView.parent == this) {
            measureChildTextView(inView)
            maxChildWidth = maxOf(maxChildWidth, inView.measuredWidth)
            maxChildHeight = maxOf(maxChildHeight, inView.measuredHeight)
        }

        val desiredWidth = maxChildWidth + horizontalPadding
        val desiredHeight = maxChildHeight + verticalPadding

        val finalWidth = resolveSize(desiredWidth, widthMeasureSpec)
        val finalHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(finalWidth, finalHeight)
    }

    /**
     * 确保布局更新，例如在动画结束后重新测量
     */
    private fun requestLayoutIfNeeded() {
        requestLayout()
    }


    private fun copyCompoundDrawables(from: TextView, to: TextView) {
        val relative = TextViewCompat.getCompoundDrawablesRelative(from)
        if (relative.any { it != null }) {
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(to, relative[0], relative[1], relative[2], relative[3])
            return
        }

        val absolute = from.compoundDrawables
        if (absolute.any { it != null }) {
            to.setCompoundDrawablesWithIntrinsicBounds(absolute[0], absolute[1], absolute[2], absolute[3])
        }
    }

    fun setTextWithAnimation(text: CharSequence) {
        val template = ensureTemplateTextView()
        if (isAnimating) {
            pendingText = text
            return
        }
        if (template.text == text) return
        startRollAnimation(text)
    }

    fun setText(text: CharSequence) {
        val template = ensureTemplateTextView()
        if (isAnimating) {
            pendingText = null
            cancelAnimation()
        }
        template.text = text
        requestLayoutIfNeeded()
    }

    fun getText(): CharSequence = ensureTemplateTextView().text
    fun getTextView(): TextView = ensureTemplateTextView()

    private fun startRollAnimation(newText: CharSequence) {
        val template = ensureTemplateTextView()
        val oldText = template.text

        outgoingView?.animate()?.cancel()
        incomingView?.animate()?.cancel()
        outgoingView?.let { removeView(it) }
        incomingView?.let { removeView(it) }

        template.visibility = View.INVISIBLE

        val outView = createTextViewFrom(template).apply {
            text = oldText
        }
        val inView = createTextViewFrom(template).apply {
            text = newText
            alpha = 0f
        }

        addView(outView)
        addView(inView)

        isAnimating = true
        outgoingView = outView
        incomingView = inView

        post {
            val containerHeight = height.takeIf { it > 0 } ?: measuredHeight
            val startY = containerHeight.toFloat()

            outView.translationY = 0f
            inView.translationY = startY
            inView.alpha = 1f

            val originalHeight = layoutParams?.height
            layoutParams?.height = measuredHeight
            requestLayout()

            outView.animate()
                .translationY(-outView.height.toFloat())
                .setDuration(animationDuration)
                .setInterpolator(interpolator)
                .withEndAction {
                    outView.animate().cancel()
                    removeView(outView)
                }
                .start()

            inView.animate()
                .translationY(0f)
                .setDuration(animationDuration)
                .setInterpolator(interpolator)
                .withEndAction {
                    template.text = newText
                    template.visibility = View.VISIBLE

                    inView.animate().cancel()
                    removeView(inView)

                    outgoingView = null
                    incomingView = null
                    isAnimating = false

                    layoutParams?.height = originalHeight ?: LayoutParams.WRAP_CONTENT
                    requestLayout()

                    pendingText?.let {
                        pendingText = null
                        setTextWithAnimation(it)
                    }
                }
                .start()
        }
    }

    private fun cancelAnimation() {
        if (!isAnimating) return

        if (::currentTextView.isInitialized) {
            currentTextView.visibility = View.VISIBLE
        }

        outgoingView?.animate()?.cancel()
        incomingView?.animate()?.cancel()

        outgoingView?.let { removeView(it) }
        incomingView?.let { removeView(it) }

        outgoingView = null
        incomingView = null
        isAnimating = false
        pendingText = null
    }

    override fun onDetachedFromWindow() {
        cancelAnimation()
        super.onDetachedFromWindow()
    }
}