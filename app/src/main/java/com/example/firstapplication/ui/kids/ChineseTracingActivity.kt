package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.example.firstapplication.databinding.ActivityChineseTracingBinding

/**
 * 汉字乐园二级页④：仿写描红（34 个精细教学字）
 * 田字格描红写字 + 保存作品 + ◀上一个/下一个▶ 切换，位置本地记录，下次进入续学
 */
class ChineseTracingActivity : ChineseBaseActivity() {

    private lateinit var binding: ActivityChineseTracingBinding
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChineseTracingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        binding.btnBack.setOnClickListener { finish() }

        HanziLibrary.BASE_CHARS.forEachIndexed { index, info ->
            val chip = createChip(info.char, index == 0)
            chip.setOnClickListener { selectChar(index) }
            binding.traceList.addView(chip)
        }
        binding.btnClearTrace.setOnClickListener {
            binding.tracingView.clearInk()
            Toast.makeText(this, "已清空，重新写吧", Toast.LENGTH_SHORT).show()
        }
        binding.btnSaveTrace.setOnClickListener {
            val path = binding.tracingView.saveToLocal()
            Toast.makeText(this, if (path != null) "作品已保存" else "先写几个字再保存哦", Toast.LENGTH_SHORT).show()
        }
        binding.btnPrevTrace.setOnClickListener { selectChar(currentIndex - 1) }
        binding.btnNextTrace.setOnClickListener { selectChar(currentIndex + 1) }

        // 续学：从上次练习的字开始
        currentIndex = KidsProgressStore.getChineseTracingIndex(this)
            .coerceIn(0, HanziLibrary.BASE_CHARS.size - 1)
        selectChar(currentIndex, save = false)
    }

    /** 选中并展示汉字（默认会保存位置） */
    private fun selectChar(index: Int, save: Boolean = true) {
        if (index !in 0 until HanziLibrary.BASE_CHARS.size) return
        currentIndex = index
        val info = HanziLibrary.BASE_CHARS[index]
        binding.tracingView.setCharacter(info.char)
        binding.tvTraceInfo.text = "「${info.char}」 拼音 ${info.pinyin} · 共 ${info.strokes} 画"
        binding.tvTraceIndicator.text = "第 ${index + 1} / ${HanziLibrary.BASE_CHARS.size} 个字"
        refreshChips()
        scrollChipIntoView()
        if (save) KidsProgressStore.setChineseTracingIndex(this, index)
    }

    /** 刷新 chip 选中样式 */
    private fun refreshChips() {
        for (i in 0 until binding.traceList.childCount) {
            val tv = binding.traceList.getChildAt(i) as TextView
            tv.background = chipBackground(i == currentIndex)
            tv.setTextColor(if (i == currentIndex) 0xFF7A3C00.toInt() else Color.WHITE)
        }
    }

    /** 让当前选中 chip 滚入可见区 */
    private fun scrollChipIntoView() {
        val child = binding.traceList.getChildAt(currentIndex) ?: return
        binding.traceScroll.smoothScrollTo(child.left - 24, 0)
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ChineseTracingActivity::class.java))
        }
    }
}
