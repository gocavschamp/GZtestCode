package com.ywm.baselibray.utils

import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView

/**
 * 富文本构建器，支持拼接文字、图标，设置颜色、下划线和点击事件
 * // 在 Kotlin 代码中调用
 * textView.setRichText {
 *     // 1. 拼接蓝色文字
 *     text(
 *         text = "点击这里",
 *         color = Color.BLUE,
 *         underline = true,
 *         onClick = {
 *             Toast.makeText(context, "文字被点击", Toast.LENGTH_SHORT).show()
 *         }
 *     )
 *
 *     // 2. 拼接一个星星图标 (24x24 px)
 *     icon(
 *         drawable = ContextCompat.getDrawable(context, R.drawable.ic_star)!!,
 *         width = 24,
 *         height = 24,
 *         onClick = {
 *             Toast.makeText(context, "星星被点击", Toast.LENGTH_SHORT).show()
 *         }
 *     )
 *
 *     // 3. 拼接普通红色文字（无下划线，无点击）
 *     text(
 *         text = " 普通红色文字",
 *         color = Color.RED
 *     )
 *
 *     // 4. 拼接一个箭头图标 + 点击
 *     icon(
 *         drawable = ContextCompat.getDrawable(context, R.drawable.ic_arrow)!!,
 *         width = 30,
 *         height = 30,
 *         onClick = {
 *             Toast.makeText(context, "箭头被点击", Toast.LENGTH_SHORT).show()
 *         }
 *     )
 * }
 */
class RichTextBuilder {

    private val builder = SpannableStringBuilder()

    /**
     * 追加普通/带样式的文字
     * @param text 文字内容
     * @param color 文字颜色 (Res Color Int)
     * @param underline 是否显示下划线
     * @param onClick 点击回调 (null 表示不可点击)
     */
    fun text(
        text: String,
        color: Int? = null,
        underline: Boolean = false,
        onClick: (() -> Unit)? = null
    ): RichTextBuilder {
        val start = builder.length
        builder.append(text)
        val end = builder.length

        // 1. 设置颜色
        color?.let {
            builder.setSpan(
                ForegroundColorSpan(it),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 2. 设置下划线 (如果用户显式要求)
        if (underline) {
            builder.setSpan(
                UnderlineSpan(),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 3. 设置点击事件
        onClick?.let {
            val clickSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    it.invoke()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    // 关键：如果同时有下划线需求，保留；否则移除 ClickableSpan 自带的默认下划线
                    ds.isUnderlineText = underline
                }
            }
            builder.setSpan(
                clickSpan,
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return this
    }

    /**
     * 追加图标 (ImageSpan)
     * @param drawable 图标资源
     * @param width 指定宽度 (dp 需自行转换，或传入像素值)
     * @param height 指定高度
     * @param onClick 点击回调 (null 表示不可点击)
     */
    fun icon(
        drawable: Drawable,
        width: Int? = null,
        height: Int? = null,
        verticalAlignment: Int? = null,
        onClick: (() -> Unit)? = null
    ): RichTextBuilder {
        // 处理尺寸：若未指定则使用 intrinsic 尺寸
        val w = width ?: drawable.intrinsicWidth
        val h = height ?: drawable.intrinsicHeight
        if (w > 0 && h > 0) {
            drawable.setBounds(0, 0, w, h)
        }

        val start = builder.length
        builder.append(" ") // 占位符，用于承载 ImageSpan
        val end = builder.length

        // 插入图片 (ALIGN_BOTTOM 使图片底部与文字基线对齐，视觉效果较好)
        builder.setSpan(
            ImageSpan(drawable, verticalAlignment?:ImageSpan.ALIGN_BOTTOM),
            start, end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 图标也支持点击
        onClick?.let {
            val clickSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    it.invoke()
                }
                override fun updateDrawState(ds: TextPaint) {
                    ds.isUnderlineText = false // 图标点击通常不需要下划线
                }
            }
            builder.setSpan(
                clickSpan,
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return this
    }

    /**
     * 构建最终 SpannableStringBuilder
     */
    fun build(): SpannableStringBuilder = builder
}

/**
 * 给 TextView 设置的便捷扩展函数
 * 自动启用 LinkMovementMethod 以响应点击
 */
fun TextView.setRichText(init: RichTextBuilder.() -> Unit) {
    val builder = RichTextBuilder()
    builder.init()
    text = builder.build()
    // 必须设置 MovementMethod，否则 ClickableSpan 不触发
    movementMethod = LinkMovementMethod.getInstance()
}