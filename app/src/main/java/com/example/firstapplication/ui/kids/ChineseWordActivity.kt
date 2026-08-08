package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firstapplication.R
import com.example.firstapplication.databinding.ActivityChineseWordBinding
import com.example.firstapplication.databinding.ItemKidsCharBinding

/**
 * 汉字识字模块：笔画演示 / 汉字识字 / 仿写描红 三个子页
 * 内置 32 个常用汉字（含拼音/含义/笔画数），全部支持笔画演示与语音朗读
 */
class ChineseWordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChineseWordBinding
    private val charAdapter = CharAdapter()

    /** 基础汉字库：字 + 拼音 + 含义 + 笔画数 */
    private val charLibrary = listOf(
        // —— 数字与基础 ——
        CharInfo("一", "yī", "数字 1", 1),
        CharInfo("二", "èr", "数字 2", 2),
        CharInfo("三", "sān", "数字 3", 3),
        CharInfo("四", "sì", "数字 4", 4),
        CharInfo("五", "wǔ", "数字 5", 4),
        CharInfo("六", "liù", "数字 6", 4),
        CharInfo("七", "qī", "数字 7", 2),
        CharInfo("八", "bā", "数字 8", 2),
        CharInfo("九", "jiǔ", "数字 9", 2),
        CharInfo("十", "shí", "数字 10", 2),
        // —— 自然与人 ——
        CharInfo("人", "rén", "人", 2),
        CharInfo("口", "kǒu", "嘴巴 / 口", 3),
        CharInfo("日", "rì", "太阳 / 日子", 4),
        CharInfo("月", "yuè", "月亮 / 月份", 4),
        CharInfo("山", "shān", "山", 3),
        CharInfo("水", "shuǐ", "水", 4),
        CharInfo("火", "huǒ", "火", 4),
        CharInfo("木", "mù", "树木", 4),
        CharInfo("土", "tǔ", "泥土", 3),
        CharInfo("田", "tián", "田地", 5),
        CharInfo("牛", "niú", "小牛", 4),
        CharInfo("马", "mǎ", "小马", 3),
        // —— 常用字 ——
        CharInfo("大", "dà", "大", 3),
        CharInfo("小", "xiǎo", "小", 3),
        CharInfo("天", "tiān", "天空", 4),
        CharInfo("上", "shàng", "上面", 3),
        CharInfo("下", "xià", "下面", 3),
        CharInfo("中", "zhōng", "中间", 3),
        CharInfo("王", "wáng", "大王", 4),
        CharInfo("门", "mén", "大门", 3),
        CharInfo("目", "mù", "眼睛 / 目", 5),
        CharInfo("耳", "ěr", "耳朵", 6),
        CharInfo("手", "shǒu", "小手", 4),
        CharInfo("头", "tóu", "脑袋", 5)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChineseWordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupStrokePage()
        setupReadingPage()
        setupTracingPage()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                binding.pageStrokes.isVisible = tab.position == 0
                binding.pageReading.isVisible = tab.position == 1
                binding.pageTracing.isVisible = tab.position == 2
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}

            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
    }

    // ============ 页面1：笔画演示 ============
    private fun setupStrokePage() {
        charLibrary.forEachIndexed { index, info ->
            val btn = createCharChip(info.char, index)
            btn.setOnClickListener {
                binding.strokeView.setCharacter(info.char)
                binding.strokeView.startAnimation()
                KidsTts.speak(info.char)
            }
            binding.strokeCharList.addView(btn)
        }
        binding.btnReplayStroke.setOnClickListener {
            binding.strokeView.startAnimation()
            KidsTts.speak(binding.strokeView.getCharacter())
        }
        binding.strokeView.post {
            binding.strokeView.startAnimation()
            KidsTts.speak(binding.strokeView.getCharacter())
        }
    }

    private fun createCharChip(char: String, index: Int): TextView {
        val colors = intArrayOf(
            Color.parseColor("#FFB74D"), Color.parseColor("#4FC3F7"), Color.parseColor("#AED581"),
            Color.parseColor("#F06292"), Color.parseColor("#BA68C8"), Color.parseColor("#4DB6AC"),
            Color.parseColor("#FF8A65"), Color.parseColor("#7986CB")
        )
        val tv = TextView(this)
        tv.text = char
        tv.textSize = 24f
        tv.setTextColor(Color.WHITE)
        tv.gravity = android.view.Gravity.CENTER
        tv.setBackgroundColor(colors[index % colors.size])
        tv.setPadding(20, 10, 20, 10)
        val lp = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.marginEnd = 12
        tv.layoutParams = lp
        return tv
    }

    // ============ 页面2：汉字识字 ============
    private fun setupReadingPage() {
        binding.recyclerReading.layoutManager = LinearLayoutManager(this)
        binding.recyclerReading.adapter = charAdapter
        charAdapter.submitList(charLibrary)
    }

    // ============ 页面3：仿写描红 ============
    private fun setupTracingPage() {
        charLibrary.forEachIndexed { index, info ->
            val btn = createCharChip(info.char, index)
            btn.setOnClickListener {
                binding.tracingView.setCharacter(info.char)
                KidsTts.speak(info.char)
            }
            binding.tracingCharList.addView(btn)
        }
        binding.btnClearTracing.setOnClickListener {
            binding.tracingView.clearInk()
        }
        binding.btnSaveTracing.setOnClickListener {
            val path = binding.tracingView.saveToLocal()
            if (path != null) {
                val info = charLibrary.firstOrNull { it.char == binding.tracingView.getCharacter() }
                KidsProgressStore.markCharLearned(this, binding.tracingView.getCharacter(), info?.pinyin ?: "")
                Toast.makeText(this, "作品已保存到本地 ✓", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "保存失败，请先描一描哦", Toast.LENGTH_SHORT).show()
            }
        }
    }

    data class CharInfo(val char: String, val pinyin: String, val meaning: String, val strokes: Int)

    inner class CharAdapter :
        RecyclerView.Adapter<CharAdapter.VH>() {

        private var list = listOf<CharInfo>()

        fun submitList(newList: List<CharInfo>) {
            list = newList
            notifyDataSetChanged()
        }

        inner class VH(val itemBinding: ItemKidsCharBinding) : RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val itemBinding = ItemKidsCharBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(itemBinding)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val info = list[position]
            val b = holder.itemBinding
            b.tvChar.text = info.char
            b.tvPinyin.text = info.pinyin
            b.tvMeaning.text = "意思: ${info.meaning} · ${info.strokes}画"
            val learned = KidsProgressStore.getLearnedChars(this@ChineseWordActivity)
            b.tvLearned.text = if (info.char in learned) "✅ 已学会" else ""
            b.btnSpeak.setOnClickListener {
                // 喇叭按钮：朗读汉字与拼音
                KidsTts.speak("${info.char}，${info.pinyin}")
            }
            b.root.setOnClickListener {
                // 点击卡片：朗读 + 标记学会
                KidsTts.speak("${info.char}，${info.pinyin}，${info.meaning}")
                KidsProgressStore.markCharLearned(this@ChineseWordActivity, info.char, info.pinyin)
                b.tvLearned.text = "✅ 已学会"
                Toast.makeText(this@ChineseWordActivity, "学习了 \"${info.char}\" (${info.pinyin})", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        /** 汉字库总字数（首页进度条用） */
        const val CHAR_TOTAL = 34

        fun start(context: Context) {
            context.startActivity(Intent(context, ChineseWordActivity::class.java))
        }
    }
}
