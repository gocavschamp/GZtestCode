package com.example.firstapplication.ui.kids

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firstapplication.databinding.ActivityPoetryBinding
import com.example.firstapplication.databinding.ItemPoetryBinding
import com.example.firstapplication.ui.kids.PoetryLibrary.Poem

/**
 * 古诗学堂：小学语文课本基础古诗
 * - 列表页：12 首古诗，每首带诗意主题配色
 * - 详情页：整首朗读 / 每字点击朗读（字 + 拼音），诗意渐变背景 + 内容相关元素装饰
 */
class PoetryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoetryBinding
    private var currentPoem: Poem? = null

    companion object {
        fun start(activity: android.app.Activity) {
            activity.startActivity(android.content.Intent(activity, PoetryActivity::class.java))
        }

        /** 颜色变亮，用于渐变圆底的高光端 */
        private fun lighten(color: Int, factor: Float): Int {
            return Color.argb(
                255,
                (Color.red(color) + (255 - Color.red(color)) * factor).toInt(),
                (Color.green(color) + (255 - Color.green(color)) * factor).toInt(),
                (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt()
            )
        }

        /** 颜色是否偏浅（浅色背景应使用深色状态栏图标） */
        private fun isLightColor(color: Int): Boolean {
            val lum = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
            return lum > 0.55
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoetryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 列表页为浅蓝渐变背景 → 深色状态栏图标
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        setupList()
        setupDetail()
    }

    override fun onBackPressed() {
        if (binding.pageDetail.visibility == View.VISIBLE) {
            showList()
        } else {
            super.onBackPressed()
        }
    }

    // ==================== 列表页 ====================

    private fun setupList() {
        binding.rclPoems.layoutManager = LinearLayoutManager(this)
        binding.rclPoems.adapter = PoemAdapter(object : (Int) -> Unit { override fun invoke(position: Int) {
            showDetail(PoetryLibrary.POEMS[position])
        } })
    }

    private class PoemAdapter(
        private val onClick: (Int) -> Unit
    ) : RecyclerView.Adapter<PoemAdapter.VH>() {

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val binding = ItemPoetryBinding.inflate(
                android.view.LayoutInflater.from(parent.context), parent, false
            )
            return VH(binding)
        }

        override fun getItemCount() = PoetryLibrary.POEMS.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val poem = PoetryLibrary.POEMS[position]
            holder.binding.apply {
                tvPoemTitle.text = poem.title
                tvPoemAuthor.text = poem.author
                tvPoemFirstLine.text = poem.lines.firstOrNull()?.text?.let {
                    if (it.length > 5) it.substring(0, 5) + "…" else it
                } ?: ""
                tvPoemEmoji.text = poem.bgEmoji
                // 每首诗意主题渐变圆底
                tvPoemEmoji.background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(lighten(poem.startColor, 0.25f), poem.endColor)
                ).apply {
                    shape = GradientDrawable.OVAL
                }
                root.setOnClickListener { onClick(holder.bindingAdapterPosition) }
            }
        }

        class VH(val binding: ItemPoetryBinding) : RecyclerView.ViewHolder(binding.root)
    }

    // ==================== 详情页 ====================

    private fun setupDetail() {
        binding.btnBack.setOnClickListener { showList() }
        binding.btnReadAll.setOnClickListener {
            val poem = currentPoem ?: return@setOnClickListener
            val fullText = poem.lines.joinToString("") { it.text }
            KidsTts.speak(fullText)
            // 朗读按钮按压反馈
            it.animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }.start()
        }
    }

    private fun showDetail(poem: Poem) {
        currentPoem = poem
        // 1. 诗意渐变背景（根布局背景也同步，覆盖状态栏区域）
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(poem.startColor, poem.endColor)
        )
        binding.bgLayer.background = gradient
        binding.root.background = gradient
        // 背景偏浅时用深色状态栏图标，深色背景用白色图标，保证状态栏始终可见
        KidsStatusBar.setLightStatusBar(this, isLightColor(poem.startColor))

        // 2. 背景元素装饰：贴合诗意的大号半透明 emoji
        binding.decorLayer.removeAllViews()
        val decorPositions = listOf(
            Gravity.TOP or Gravity.START to 28,
            Gravity.TOP or Gravity.END to 30,
            Gravity.CENTER to 34,
            Gravity.BOTTOM or Gravity.START to 26,
            Gravity.BOTTOM or Gravity.END to 32
        )
        decorPositions.forEach { (gravity, sizeSp) ->
            val deco = TextView(this).apply {
                text = poem.bgEmoji
                textSize = sizeSp.toFloat()
                alpha = 0.30f
            }
            binding.decorLayer.addView(
                deco,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    gravity
                ).apply { setMargins(24, 24, 24, 24) }
            )
        }

        // 3. 标题 / 作者 / 释义
        binding.tvPoemTitle.text = poem.title
        binding.tvPoemAuthor.text = poem.author
        binding.tvMeaning.text = poem.meaning

        // 4. 逐字渲染诗句：每行一行，每字一列（大字 + 小号拼音），点击朗读
        binding.poemContainer.removeAllViews()
        poem.lines.forEach { poemLine ->
            val lineRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, 4, 0, 4)
            }
            poemLine.chars.forEachIndexed { index, poemChar ->
                val isPunct = poemChar.pinyin == "_" || poemChar.pinyin.isEmpty()
                val charCell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    // 句内首个非标点字前留一个字的间距，体现诗句节奏
                    if (index == 0 && !isPunct) setPadding(18, 0, 0, 0)
                }
                val charTv = TextView(this).apply {
                    text = poemChar.char
                    setTextColor(0xFF37474F.toInt())
                    textSize = if (isPunct) 18f else 30f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    gravity = Gravity.CENTER
                }
                val pinyinTv = TextView(this).apply {
                    text = if (isPunct) "" else poemChar.pinyin
                    setTextColor(0xFF78909C.toInt())
                    textSize = 10f
                    gravity = Gravity.CENTER
                }
                // 每个字都可以点击朗读（标点不可点）
                if (!isPunct) {
                    charTv.setOnClickListener {
                        KidsTts.speak(poemChar.char + "，" + poemChar.pinyin)
                        charTv.animate().scaleX(1.35f).scaleY(1.35f).setDuration(120)
                            .withEndAction {
                                charTv.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                            }.start()
                    }
                }
                charCell.addView(pinyinTv, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ))
                charCell.addView(charTv, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ))
                lineRow.addView(charCell, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = 2 })
            }
            binding.poemContainer.addView(lineRow)
        }

        // 5. 切到详情页 + 入场动画
        binding.pageList.visibility = View.GONE
        binding.pageDetail.visibility = View.VISIBLE
        binding.pageDetail.alpha = 0f
        binding.pageDetail.animate().alpha(1f).setDuration(260).start()
        binding.tvPoemTitle.apply {
            scaleX = 0.6f
            scaleY = 0.6f
            animate().scaleX(1f).scaleY(1f).setDuration(320)
                .setInterpolator(OvershootInterpolator(2f)).start()
        }
    }

    private fun showList() {
        binding.pageDetail.visibility = View.GONE
        binding.pageList.visibility = View.VISIBLE
        currentPoem = null
        // 恢复列表页浅蓝背景与深色状态栏图标
        binding.root.background = androidx.core.content.ContextCompat.getDrawable(
            this, com.example.firstapplication.R.drawable.bg_kids_home
        )
        KidsStatusBar.setLightStatusBar(this, true)
    }
}
