package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firstapplication.R
import com.example.firstapplication.databinding.ActivityChineseWordBinding
import com.example.firstapplication.databinding.ItemKidsCharGridBinding

/**
 * 汉字乐园：基础笔画 / 笔画演示 / 汉字识字(500字) / 仿写描红 / 每日一句 五个子页
 * 基础笔画与演示字：34 个精细数据（StrokeData）；识字库：约 500 常用汉字（TTS 朗读）
 */
class ChineseWordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChineseWordBinding

    private val gridAdapter = GridCharAdapter()
    private val learnedSet = HashSet<String>()
    private var currentDailyIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChineseWordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root)

        learnedSet.addAll(KidsProgressStore.getLearnedChars(this))
        setupTabs()
        setupBasicStrokePage()
        setupStrokePage()
        setupReadingPage()
        setupTracingPage()
        setupDailyPage()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                binding.pageBasicStroke.isVisible = tab.position == 0
                binding.pageStrokes.isVisible = tab.position == 1
                binding.pageReading.isVisible = tab.position == 2
                binding.pageTracing.isVisible = tab.position == 3
                binding.pageDaily.isVisible = tab.position == 4
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}

            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
    }

    // ============ 页面0：基础笔画 ============
    private fun setupBasicStrokePage() {
        HanziLibrary.BASIC_STROKES.forEachIndexed { index, info ->
            val btn = createCharChip(info.name, index)
            btn.setOnClickListener {
                binding.basicStrokeView.setCharacter(info.name)
                binding.basicStrokeView.startAnimation()
                binding.tvStrokeName.text = "${info.name} ${info.pinyin}"
                binding.tvStrokeExample.text = "示例字: ${info.example}"
            }
            binding.basicStrokeList.addView(btn)
        }
        binding.btnSpeakStroke.setOnClickListener {
            KidsTts.speak("${binding.tvStrokeName.text}")
        }
        binding.basicStrokeView.post {
            val first = HanziLibrary.BASIC_STROKES.first()
            binding.basicStrokeView.setCharacter(first.name)
            binding.basicStrokeView.startAnimation()
        }
    }

    // ============ 页面1：笔画演示 ============
    private fun setupStrokePage() {
        HanziLibrary.BASE_CHARS.forEachIndexed { index, info ->
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

    // ============ 页面2：汉字识字（500 字网格） ============
    private fun setupReadingPage() {
        binding.recyclerReading.layoutManager = GridLayoutManager(this, 5)
        binding.recyclerReading.adapter = gridAdapter
        binding.tvReadingHint.text = "✨ 点击卡片朗读并标记学会 · 共 ${HanziLibrary.HANZI_TOTAL} 字"
        gridAdapter.submitList(HanziLibrary.EXTENDED_HANZI)
    }

    // ============ 页面3：仿写描红 ============
    private fun setupTracingPage() {
        HanziLibrary.BASE_CHARS.forEachIndexed { index, info ->
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
                val info = HanziLibrary.BASE_CHARS.firstOrNull { it.char == binding.tracingView.getCharacter() }
                KidsProgressStore.markCharLearned(this, binding.tracingView.getCharacter(), info?.pinyin ?: "")
                learnedSet.add(binding.tracingView.getCharacter())
                gridAdapter.notifyDataSetChanged()
                Toast.makeText(this, "作品已保存到本地 ✓", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "保存失败，请先描一描哦", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ============ 页面4：每日一句 ============
    private fun setupDailyPage() {
        currentDailyIndex = (System.currentTimeMillis() % HanziLibrary.DAILY_SENTENCES.size).toInt()
        showDailySentence()
        binding.btnNextSentence.setOnClickListener {
            currentDailyIndex = (currentDailyIndex + 1) % HanziLibrary.DAILY_SENTENCES.size
            showDailySentence()
            KidsTts.speak(HanziLibrary.DAILY_SENTENCES[currentDailyIndex])
        }
        binding.btnSpeakDaily.setOnClickListener {
            KidsTts.speak(HanziLibrary.DAILY_SENTENCES[currentDailyIndex])
        }
    }

    private fun showDailySentence() {
        binding.tvDailySentence.text = HanziLibrary.DAILY_SENTENCES[currentDailyIndex]
        binding.tvDailySentence.animate().alpha(0.2f).setDuration(120).withEndAction {
            binding.tvDailySentence.animate().alpha(1f).setDuration(220).start()
        }.start()
    }

    private fun createCharChip(char: String, index: Int): TextView {
        val colors = intArrayOf(
            Color.parseColor("#FFB74D"), Color.parseColor("#4FC3F7"), Color.parseColor("#AED581"),
            Color.parseColor("#F06292"), Color.parseColor("#BA68C8"), Color.parseColor("#4DB6AC"),
            Color.parseColor("#FF8A65"), Color.parseColor("#7986CB")
        )
        val tv = TextView(this)
        tv.text = char
        tv.textSize = 22f
        tv.setTextColor(Color.WHITE)
        tv.gravity = Gravity.CENTER
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

    // ============ 识字网格 Adapter ============
    inner class GridCharAdapter : RecyclerView.Adapter<GridCharAdapter.VH>() {

        private var list = listOf<String>()

        fun submitList(newList: List<String>) {
            list = newList
            notifyDataSetChanged()
        }

        inner class VH(val itemBinding: ItemKidsCharGridBinding) : RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val itemBinding = ItemKidsCharGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(itemBinding)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val char = list[position]
            val b = holder.itemBinding
            b.tvGridChar.text = char
            b.tvGridLearned.text = if (char in learnedSet) "✅" else ""
            b.root.setOnClickListener {
                // 朗读 + 标记学会
                KidsTts.speak(char)
                if (learnedSet.add(char)) {
                    KidsProgressStore.markCharLearned(this@ChineseWordActivity, char)
                    b.tvGridLearned.text = "✅"
                    notifyItemChanged(position)
                }
            }
        }
    }

    companion object {
        /** 汉字库总字数（首页进度条用） */
        const val CHAR_TOTAL = 500

        fun start(context: Context) {
            context.startActivity(Intent(context, ChineseWordActivity::class.java))
        }
    }
}
