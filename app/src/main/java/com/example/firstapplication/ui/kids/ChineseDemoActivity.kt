package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import com.example.firstapplication.databinding.ActivityChineseDemoBinding

/**
 * 汉字乐园二级页②：笔画演示（34 个精细教学字）
 * 一笔一画动画 + 朗读（只读汉字本身）+ ◀上一个/下一个▶ 切换，位置本地记录，下次进入续学
 */
class ChineseDemoActivity : ChineseBaseActivity() {

    private lateinit var binding: ActivityChineseDemoBinding
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChineseDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        binding.btnBack.setOnClickListener { finish() }

        HanziLibrary.BASE_CHARS.forEachIndexed { index, info ->
            val chip = createChip(info.char, index == 0)
            chip.setOnClickListener { selectChar(index) }
            binding.charList.addView(chip)
        }
        binding.btnReplay.setOnClickListener { binding.demoView.startAnimation() }
        binding.btnPrevDemo.setOnClickListener { selectChar(currentIndex - 1) }
        binding.btnNextDemo.setOnClickListener { selectChar(currentIndex + 1) }

        // 续学：从上次练习的字开始
        currentIndex = KidsProgressStore.getChineseDemoIndex(this)
            .coerceIn(0, HanziLibrary.BASE_CHARS.size - 1)
        selectChar(currentIndex, save = false)
    }

    /** 选中并展示汉字（默认会保存位置） */
    private fun selectChar(index: Int, save: Boolean = true) {
        if (index !in 0 until HanziLibrary.BASE_CHARS.size) return
        currentIndex = index
        val info = HanziLibrary.BASE_CHARS[index]
        binding.demoView.setCharacter(info.char)
        binding.demoView.startAnimation()
        binding.tvDemoChar.text = info.char
        binding.tvDemoInfo.text = "拼音 ${info.pinyin} · ${info.meaning} · 共 ${info.strokes} 画"
        binding.tvDemoIndicator.text = "第 ${index + 1} / ${HanziLibrary.BASE_CHARS.size} 个字"
        refreshChips()
        scrollChipIntoView()
        // 只读汉字本身，不读拼音
        KidsTts.speak(info.char)
        if (save) KidsProgressStore.setChineseDemoIndex(this, index)
    }

    /** 刷新 chip 选中样式 */
    private fun refreshChips() {
        for (i in 0 until binding.charList.childCount) {
            val tv = binding.charList.getChildAt(i) as TextView
            tv.background = chipBackground(i == currentIndex)
            tv.setTextColor(if (i == currentIndex) 0xFF7A3C00.toInt() else Color.WHITE)
        }
    }

    /** 让当前选中 chip 滚入可见区 */
    private fun scrollChipIntoView() {
        val child = binding.charList.getChildAt(currentIndex) ?: return
        binding.charScroll.smoothScrollTo(child.left - 24, 0)
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ChineseDemoActivity::class.java))
        }
    }
}
