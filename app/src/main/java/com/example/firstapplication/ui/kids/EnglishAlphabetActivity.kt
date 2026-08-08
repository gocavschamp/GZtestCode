package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import com.example.firstapplication.databinding.ActivityEnglishAlphabetBinding
import com.example.firstapplication.databinding.ItemEnglishLetterWordBinding

/**
 * 英语乐园二级页①：字母表
 * 26 个字母含音标 + 每字母 2 个示例单词（emoji 上图下英文+中文），◀/▶ 切换字母，点击卡片朗读
 */
class EnglishAlphabetActivity : EnglishBaseActivity() {

    private lateinit var binding: ActivityEnglishAlphabetBinding
    private var currentLetterIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnglishAlphabetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        binding.btnBack.setOnClickListener { finish() }

        EnglishLibrary.ALPHABET.forEachIndexed { index, info ->
            val chip = createChip(info.letter, index == 0)
            chip.setOnClickListener {
                currentLetterIndex = index
                refreshLetterChips()
                showLetter(info)
            }
            binding.alphabetList.addView(chip)
        }
        binding.btnSpeakLetter.setOnClickListener {
            speakLetter(EnglishLibrary.ALPHABET[currentLetterIndex])
        }
        binding.btnPrevLetter.setOnClickListener { moveLetter(-1) }
        binding.btnNextLetter.setOnClickListener { moveLetter(1) }
        showLetter(EnglishLibrary.ALPHABET[0])
    }

    /** 字母切换：上一个/下一个 */
    private fun moveLetter(delta: Int) {
        val next = (currentLetterIndex + delta).coerceIn(0, EnglishLibrary.ALPHABET.size - 1)
        if (next == currentLetterIndex) return
        currentLetterIndex = next
        refreshLetterChips()
        showLetter(EnglishLibrary.ALPHABET[next])
        val child = binding.alphabetList.getChildAt(next)
        binding.alphabetScroll.smoothScrollTo(child.left - 24, 0)
    }

    /** 刷新字母 chip 选中样式 */
    private fun refreshLetterChips() {
        for (i in 0 until binding.alphabetList.childCount) {
            val tv = binding.alphabetList.getChildAt(i) as TextView
            tv.background = chipBackground(i == currentLetterIndex)
            tv.setTextColor(if (i == currentLetterIndex) 0xFF7A3C00.toInt() else Color.WHITE)
        }
    }

    private fun showLetter(info: EnglishLibrary.LetterInfo) {
        binding.tvLetterBig.text = "${info.letter} ${info.lowercase}"
        binding.tvLetterPhonetic.text = info.phonetic
        binding.letterWordList.removeAllViews()
        info.words.forEach { sample ->
            binding.letterWordList.addView(createLetterWordCard(sample.word, sample.chinese, sample.emoji))
        }
    }

    /** 字母页示例单词卡（紧凑卡：emoji 上、英文+中文下） */
    private fun createLetterWordCard(word: String, chinese: String, emoji: String) = with(ItemEnglishLetterWordBinding.inflate(layoutInflater)) {
        tvLetterEmoji.text = emoji
        tvLetterWord.text = word
        tvLetterChinese.text = chinese
        root.setOnClickListener { KidsTts.speakEnglish(word) }
        root
    }

    private fun speakLetter(info: EnglishLibrary.LetterInfo) {
        KidsTts.speakEnglish(info.letter)
        binding.root.postDelayed({
            info.words.forEach { sample -> KidsTts.speakEnglish(sample.word) }
        }, 900)
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, EnglishAlphabetActivity::class.java))
        }
    }
}
