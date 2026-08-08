package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firstapplication.databinding.ActivityEnglishWordsBinding
import com.example.firstapplication.databinding.ItemEnglishFlashcardBinding
import com.example.firstapplication.databinding.ItemEnglishSentenceBinding
import com.example.firstapplication.ui.kids.EnglishLibrary.EnglishWord
import com.example.firstapplication.ui.kids.EnglishLibrary.Sentence

/**
 * 英语乐园二级页②：单词句子（闪卡切换）
 * 15 类 330 词 + 60 句常用句子，一次一张滑动切换 + 指示器 + ◀上一个/下一个▶ 按钮
 */
class EnglishWordsActivity : EnglishBaseActivity() {

    private lateinit var binding: ActivityEnglishWordsBinding

    private var currentCategoryIndex = 0
    private var currentWordPos = 0

    private val wordAdapter = WordAdapter()
    private val sentenceAdapter = SentenceAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnglishWordsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        binding.btnBack.setOnClickListener { finish() }
        setupWordsPage()
    }

    private fun setupWordsPage() {
        EnglishLibrary.CATEGORIES.forEachIndexed { index, category ->
            val chip = createChip(category.name, index == 0)
            chip.setOnClickListener {
                currentCategoryIndex = index
                currentWordPos = 0
                refreshCategoryChips()
                binding.recyclerWords.adapter = wordAdapter
                wordAdapter.submitList(EnglishLibrary.CATEGORIES[index].words)
                binding.tvWordsHint.text = "✨ 左右滑动切换卡片 · 共 ${EnglishLibrary.CATEGORIES[index].words.size} 个单词"
            }
            binding.categoryList.addView(chip)
        }
        val sentenceChip = createChip("💬 常用句子", false)
        sentenceChip.setOnClickListener {
            currentCategoryIndex = -1
            currentWordPos = 0
            refreshCategoryChips()
            binding.recyclerWords.adapter = sentenceAdapter
            sentenceAdapter.submitList(EnglishLibrary.SENTENCES)
            binding.tvWordsHint.text = "✨ 左右滑动切换卡片 · 共 ${EnglishLibrary.SENTENCES.size} 句"
        }
        binding.categoryList.addView(sentenceChip)

        val lm = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerWords.layoutManager = lm
        binding.recyclerWords.adapter = wordAdapter
        attachSnap(binding.recyclerWords) { pos, total ->
            currentWordPos = pos
            binding.tvWordsIndicator.text = "第 ${pos + 1} / $total 张"
        }
        binding.btnPrevWord.setOnClickListener { moveWord(-1) }
        binding.btnNextWord.setOnClickListener { moveWord(1) }
        wordAdapter.submitList(EnglishLibrary.CATEGORIES[0].words)
        binding.tvWordsHint.text = "✨ 左右滑动切换卡片 · 共 ${EnglishLibrary.CATEGORIES[0].words.size} 个单词"
        binding.tvWordsIndicator.text = "第 1 / ${EnglishLibrary.CATEGORIES[0].words.size} 张"
    }

    private fun refreshCategoryChips() {
        for (i in 0 until binding.categoryList.childCount) {
            val tv = binding.categoryList.getChildAt(i) as TextView
            tv.background = chipBackground(i == currentCategoryIndex)
            tv.setTextColor(if (i == currentCategoryIndex) 0xFF7A3C00.toInt() else Color.WHITE)
        }
    }

    /** 单词/句子闪卡切换：上一个/下一个 */
    private fun moveWord(delta: Int) {
        val total = binding.recyclerWords.adapter?.itemCount ?: 0
        if (total <= 0) return
        val next = (currentWordPos + delta).coerceIn(0, total - 1)
        if (next == currentWordPos) return
        currentWordPos = next
        binding.recyclerWords.smoothScrollToPosition(next)
        binding.tvWordsIndicator.text = "第 ${next + 1} / $total 张"
    }

    // ==================== Adapters ====================

    /** 单词闪卡 adapter（卡片含 2 个例句） */
    inner class WordAdapter : RecyclerView.Adapter<WordAdapter.VH>() {

        private var list = listOf<EnglishWord>()

        fun submitList(newList: List<EnglishWord>) {
            list = newList
            notifyDataSetChanged()
            binding.tvWordsIndicator.text = "第 1 / ${newList.size} 张"
        }

        inner class VH(val b: ItemEnglishFlashcardBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemEnglishFlashcardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            bindFlashcard(holder.b, list[position])
        }
    }

    /** 句子闪卡 adapter */
    inner class SentenceAdapter : RecyclerView.Adapter<SentenceAdapter.VH>() {

        private var list = listOf<Sentence>()

        fun submitList(newList: List<Sentence>) {
            list = newList
            notifyDataSetChanged()
            binding.tvWordsIndicator.text = "第 1 / ${newList.size} 张"
        }

        inner class VH(val b: ItemEnglishSentenceBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemEnglishSentenceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            bindSentence(holder.b, list[position])
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, EnglishWordsActivity::class.java))
        }
    }
}
