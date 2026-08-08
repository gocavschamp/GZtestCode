package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firstapplication.R
import com.example.firstapplication.databinding.ActivityEnglishCheckinBinding
import com.example.firstapplication.databinding.ItemEnglishCheckinDayBinding
import com.example.firstapplication.databinding.ItemEnglishFlashcardBinding
import com.example.firstapplication.databinding.ItemEnglishSentenceBinding
import com.example.firstapplication.ui.kids.EnglishLibrary.EnglishWord
import com.example.firstapplication.ui.kids.EnglishLibrary.Sentence
import java.util.Calendar

/**
 * 英语乐园二级页③：每日打卡
 * 每月 30 天日历，每天 10 个单词（各配 2 例句）+ 2 个句子，闪卡滑动 + ◀上一个/下一个▶ 按钮，本地记录打卡掩码
 */
class EnglishCheckinActivity : EnglishBaseActivity() {

    private lateinit var binding: ActivityEnglishCheckinBinding

    // 打卡状态
    private var checkinMask = 0
    private var selectedCheckinDay = 0
    private var currentCheckinPos = 0

    private val checkinAdapter = CheckinAdapter()
    private val checkinContentAdapter = CheckinContentAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnglishCheckinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        binding.btnBack.setOnClickListener { finish() }
        checkinMask = KidsProgressStore.getEnglishCheckinMask(this)
        selectedCheckinDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceIn(1, 30)
        setupCheckinPage()
    }

    private fun setupCheckinPage() {
        binding.recyclerCheckin.layoutManager = GridLayoutManager(this, 6)
        binding.recyclerCheckin.adapter = checkinAdapter
        binding.recyclerCheckin.isNestedScrollingEnabled = false
        checkinAdapter.submitList((1..30).toList())
        refreshCheckinHeader()

        binding.recyclerCheckinContent.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerCheckinContent.adapter = checkinContentAdapter
        attachSnap(binding.recyclerCheckinContent) { pos, total ->
            currentCheckinPos = pos
            binding.tvCheckinIndicator.text = "第 ${pos + 1} / $total 张"
        }
        binding.btnPrevCheckin.setOnClickListener { moveCheckin(-1) }
        binding.btnNextCheckin.setOnClickListener { moveCheckin(1) }

        binding.btnCheckinDone.setOnClickListener {
            if (selectedCheckinDay <= 0) return@setOnClickListener
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            if (selectedCheckinDay != today) return@setOnClickListener
            checkinMask = KidsProgressStore.markEnglishCheckin(this, selectedCheckinDay)
            refreshCheckinHeader()
            checkinAdapter.notifyDataSetChanged()
            KidsTts.speakEnglish("Great job! You are awesome!")
            refreshCheckinContent()
        }

        // 默认选中今天
        showCheckinContent(selectedCheckinDay)
    }

    private fun refreshCheckinHeader() {
        val count = Integer.bitCount(checkinMask)
        binding.tvCheckinTitle.text = "📅 本月打卡 $count / 30 天"
    }

    /** 展示某天内容：10 个单词卡（各配 2 例句）+ 2 个句子卡，闪卡滑动切换 */
    private fun showCheckinContent(day: Int) {
        selectedCheckinDay = day
        currentCheckinPos = 0
        val plan = EnglishLibrary.CHECK_IN_DAYS[day - 1]
        val cards: MutableList<Any> = mutableListOf()
        cards.addAll(plan.words)
        cards.addAll(plan.sentences)
        checkinContentAdapter.submitList(cards)
        binding.recyclerCheckinContent.scrollToPosition(0)
        binding.tvCheckinIndicator.text = "第 1 / ${cards.size} 张"
        refreshCheckinContent()
    }

    /** 打卡内容闪卡切换：上一个/下一个 */
    private fun moveCheckin(delta: Int) {
        val total = binding.recyclerCheckinContent.adapter?.itemCount ?: 0
        if (total <= 0) return
        val next = (currentCheckinPos + delta).coerceIn(0, total - 1)
        if (next == currentCheckinPos) return
        currentCheckinPos = next
        binding.recyclerCheckinContent.smoothScrollToPosition(next)
        binding.tvCheckinIndicator.text = "第 ${next + 1} / $total 张"
    }

    /** 更新打卡按钮状态 */
    private fun refreshCheckinContent() {
        val day = selectedCheckinDay
        if (day <= 0) return
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val done = (checkinMask and (1 shl (day - 1))) != 0
        binding.btnCheckinDone.isEnabled = false
        binding.btnCheckinDone.alpha = 0.55f
        when {
            done -> {
                binding.btnCheckinDone.text = "✅ 第 $day 天已完成打卡"
                binding.btnCheckinDone.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.kids_grass)
            }
            day == today -> {
                binding.btnCheckinDone.text = "✅ 完成今日打卡"
                binding.btnCheckinDone.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.kids_grass)
                binding.btnCheckinDone.isEnabled = true
                binding.btnCheckinDone.alpha = 1f
            }
            day < today -> {
                binding.btnCheckinDone.text = "⏰ 第 $day 天已错过，今天起要坚持哦"
                binding.btnCheckinDone.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.kids_coral)
            }
            else -> {
                binding.btnCheckinDone.text = "🔮 第 $day 天还没到"
                binding.btnCheckinDone.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.kids_sky_blue_deep)
            }
        }
    }

    // ==================== Adapters ====================

    /** 打卡日历网格 adapter */
    inner class CheckinAdapter : RecyclerView.Adapter<CheckinAdapter.VH>() {

        private var list = listOf<Int>()

        fun submitList(newList: List<Int>) {
            list = newList
            notifyDataSetChanged()
        }

        inner class VH(val b: ItemEnglishCheckinDayBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemEnglishCheckinDayBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val day = list[position]
            val b = holder.b
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            val done = (checkinMask and (1 shl (day - 1))) != 0
            b.tvCheckinDay.text = day.toString()
            b.tvCheckinDay.textSize = if (day == today) 16f else 14f
            // 选中日高亮描边
            val border = if (day == selectedCheckinDay) 3 else 0
            when {
                done -> {
                    b.root.setCardBackgroundColor(ContextCompat.getColor(this@EnglishCheckinActivity, R.color.kids_grass))
                    b.tvCheckinStatus.text = "✓"
                    b.tvCheckinStatus.setTextColor(Color.WHITE)
                }
                day == today -> {
                    b.root.setCardBackgroundColor(ContextCompat.getColor(this@EnglishCheckinActivity, R.color.kids_orange))
                    b.tvCheckinStatus.text = "今天"
                    b.tvCheckinStatus.setTextColor(0xE6FFFFFF.toInt())
                }
                day < today -> {
                    b.root.setCardBackgroundColor(ContextCompat.getColor(this@EnglishCheckinActivity, R.color.kids_coral))
                    b.tvCheckinStatus.text = "✗"
                    b.tvCheckinStatus.setTextColor(Color.WHITE)
                }
                else -> {
                    b.root.setCardBackgroundColor(0x33000000)
                    b.tvCheckinStatus.text = ""
                }
            }
            b.root.strokeWidth = border
            b.root.strokeColor = Color.WHITE
            b.root.setOnClickListener { showCheckinContent(day) }
        }
    }

    /** 打卡内容闪卡 adapter：viewType 0=单词（flashcard 带 2 例句） 1=句子 */
    inner class CheckinContentAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var list = listOf<Any>()

        fun submitList(newList: List<Any>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int =
            if (list[position] is EnglishWord) 0 else 1

        override fun getItemCount(): Int = list.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            if (viewType == 0) {
                VH(ItemEnglishFlashcardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            } else {
                VHS(ItemEnglishSentenceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = list[position]) {
                is EnglishWord -> bindFlashcard((holder as VH).b, item)
                is Sentence -> bindSentence((holder as VHS).b, item)
            }
        }

        inner class VH(val b: ItemEnglishFlashcardBinding) : RecyclerView.ViewHolder(b.root)
        inner class VHS(val b: ItemEnglishSentenceBinding) : RecyclerView.ViewHolder(b.root)
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, EnglishCheckinActivity::class.java))
        }
    }
}
