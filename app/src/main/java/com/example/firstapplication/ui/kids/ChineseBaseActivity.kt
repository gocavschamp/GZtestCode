package com.example.firstapplication.ui.kids

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * 汉字乐园二级页面基类：共享 dp 换算、圆角 chip 工具
 * 五个二级页（基础笔画 / 笔画演示 / 汉字识字 / 仿写描红 / 每日一句）都继承自本类
 */
abstract class ChineseBaseActivity : AppCompatActivity() {

    protected fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /** 圆角 chip：选中白底深字 / 未选中半透明深底白字 */
    protected fun createChip(text: String, selected: Boolean): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 15f
        tv.setTextColor(if (selected) 0xFF7A3C00.toInt() else Color.WHITE)
        tv.gravity = Gravity.CENTER
        tv.background = chipBackground(selected)
        tv.setPadding(22, 12, 22, 12)
        val lp = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.marginEnd = 10
        tv.layoutParams = lp
        return tv
    }

    protected fun chipBackground(selected: Boolean): GradientDrawable = GradientDrawable().apply {
        cornerRadius = 22f
        if (selected) {
            setColor(Color.WHITE)
        } else {
            setColor(0x33000000)
            setStroke(1, 0x66FFFFFF.toInt())
        }
    }
}
