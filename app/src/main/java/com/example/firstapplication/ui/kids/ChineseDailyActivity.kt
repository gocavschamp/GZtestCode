package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.firstapplication.databinding.ActivityChineseDailyBinding

/**
 * 汉字乐园二级页⑤：每日一句（44 句日常句子）
 * 朗读句子 + ◀上一个/下一个▶ 切换，位置本地记录，下次进入续学
 */
class ChineseDailyActivity : ChineseBaseActivity() {

    private lateinit var binding: ActivityChineseDailyBinding
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChineseDailyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSpeakDaily.setOnClickListener {
            KidsTts.speak(HanziLibrary.DAILY_SENTENCES[currentIndex])
        }
        binding.btnPrevDaily.setOnClickListener { selectSentence(currentIndex - 1) }
        binding.btnNextDaily.setOnClickListener { selectSentence(currentIndex + 1) }

        // 续学：从上次练习的句子开始
        currentIndex = KidsProgressStore.getChineseDailyIndex(this)
            .coerceIn(0, HanziLibrary.DAILY_SENTENCES.size - 1)
        selectSentence(currentIndex, save = false)
    }

    /** 选中并展示句子（默认会保存位置） */
    private fun selectSentence(index: Int, save: Boolean = true) {
        if (index !in 0 until HanziLibrary.DAILY_SENTENCES.size) return
        currentIndex = index
        binding.tvDailySentence.text = HanziLibrary.DAILY_SENTENCES[index]
        binding.tvDailyIndex.text = "第 ${index + 1} 句"
        binding.tvDailyIndicator.text =
            "第 ${index + 1} / ${HanziLibrary.DAILY_SENTENCES.size} 句"
        if (save) KidsProgressStore.setChineseDailyIndex(this, index)
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ChineseDailyActivity::class.java))
        }
    }
}
