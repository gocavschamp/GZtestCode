package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import com.example.firstapplication.databinding.ActivityChineseStrokeBinding

/**
 * 汉字乐园二级页①：基础笔画（22 种）
 * 动画演示笔画写法 + 发音（只读笔画名，不读拼音）+ ◀上一个/下一个▶ 切换，位置本地记录，下次进入续学
 */
class ChineseStrokeActivity : ChineseBaseActivity() {

    private lateinit var binding: ActivityChineseStrokeBinding
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChineseStrokeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        binding.btnBack.setOnClickListener { finish() }

        HanziLibrary.BASIC_STROKES.forEachIndexed { index, info ->
            val chip = createChip(info.name, index == 0)
            chip.setOnClickListener { selectStroke(index) }
            binding.strokeList.addView(chip)
        }
        binding.btnSpeakStroke.setOnClickListener {
            KidsTts.speak(HanziLibrary.BASIC_STROKES[currentIndex].name)
        }
        binding.btnPrevStroke.setOnClickListener { selectStroke(currentIndex - 1) }
        binding.btnNextStroke.setOnClickListener { selectStroke(currentIndex + 1) }

        // 续学：从上次练习的笔画开始
        currentIndex = KidsProgressStore.getChineseStrokeIndex(this)
            .coerceIn(0, HanziLibrary.BASIC_STROKES.size - 1)
        selectStroke(currentIndex, save = false)
    }

    /** 选中并展示笔画（默认会保存位置） */
    private fun selectStroke(index: Int, save: Boolean = true) {
        if (index !in 0 until HanziLibrary.BASIC_STROKES.size) return
        currentIndex = index
        val info = HanziLibrary.BASIC_STROKES[index]
        // 演示该笔画写法，同时显示包含该笔画的字（当前笔画高亮，其余笔画灰色轮廓）
        binding.strokeView.showStrokeInChar(info.name)
        // 写笔画时同步朗读笔画名
        KidsTts.speak(info.name)
        binding.tvStrokeName.text = "${info.name} ${info.pinyin}"
        val target = StrokeAnimationView.StrokeData.exampleChars[info.name]
        binding.tvStrokeExample.text = if (target != null) {
            "对比字「${target.first}」第 ${target.second + 1} 画 · 示例字：${info.example}"
        } else {
            "示例字：${info.example}"
        }
        binding.tvStrokeIndicator.text = "第 ${index + 1} / ${HanziLibrary.BASIC_STROKES.size} 个笔画"
        refreshChips()
        scrollChipIntoView()
        if (save) KidsProgressStore.setChineseStrokeIndex(this, index)
    }

    /** 刷新笔画 chip 选中样式 */
    private fun refreshChips() {
        for (i in 0 until binding.strokeList.childCount) {
            val tv = binding.strokeList.getChildAt(i) as TextView
            tv.background = chipBackground(i == currentIndex)
            tv.setTextColor(if (i == currentIndex) 0xFF7A3C00.toInt() else Color.WHITE)
        }
    }

    /** 让当前选中 chip 滚入可见区 */
    private fun scrollChipIntoView() {
        val child = binding.strokeList.getChildAt(currentIndex) ?: return
        binding.strokeScroll.smoothScrollTo(child.left - 24, 0)
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ChineseStrokeActivity::class.java))
        }
    }
}
