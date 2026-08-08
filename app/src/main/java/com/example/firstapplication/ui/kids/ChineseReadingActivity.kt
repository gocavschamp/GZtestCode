package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firstapplication.databinding.ActivityChineseReadingBinding
import com.example.firstapplication.databinding.ItemKidsCharGridBinding

/**
 * 汉字乐园二级页③：汉字识字（500 常用字网格）
 * 点击朗读并标记学会（✅），位置本地记录，下次进入直接滚动到上次练习位置续学
 */
class ChineseReadingActivity : ChineseBaseActivity() {

    private lateinit var binding: ActivityChineseReadingBinding
    private val readingAdapter = ReadingAdapter()
    private val learned = mutableSetOf<String>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChineseReadingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        binding.btnBack.setOnClickListener { finish() }

        learned.addAll(KidsProgressStore.getLearnedChars(this))
        binding.charGrid.layoutManager = GridLayoutManager(this, 6)
        binding.charGrid.adapter = readingAdapter
        readingAdapter.submitList(HanziLibrary.EXTENDED_HANZI)

        // 续学：从上次练习位置开始
        currentIndex = KidsProgressStore.getChineseReadingIndex(this)
            .coerceIn(0, HanziLibrary.EXTENDED_HANZI.size - 1)
        binding.charGrid.post {
            (binding.charGrid.layoutManager as GridLayoutManager)
                .scrollToPositionWithOffset(currentIndex, 0)
        }
        updateProgress()
    }

    private fun updateProgress() {
        val last = HanziLibrary.EXTENDED_HANZI[currentIndex]
        binding.tvReadingProgress.text =
            "已学 ${learned.size} / ${HanziLibrary.HANZI_TOTAL} 字 · 上次学到「$last」"
    }

    /** 识字网格 adapter：点击朗读 + 标记学会 + 记录位置 */
    inner class ReadingAdapter : RecyclerView.Adapter<ReadingAdapter.VH>() {

        private var list = listOf<String>()

        fun submitList(newList: List<String>) {
            list = newList
            notifyDataSetChanged()
        }

        inner class VH(val b: ItemKidsCharGridBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemKidsCharGridBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val char = list[position]
            val b = holder.b
            b.tvGridChar.text = char
            b.tvGridLearned.text = if (char in learned) "✅" else ""
            b.root.setOnClickListener {
                KidsTts.speak(char)
                if (char !in learned) {
                    learned.add(char)
                    KidsProgressStore.markCharLearned(this@ChineseReadingActivity, char)
                    notifyItemChanged(position)
                }
                currentIndex = position
                KidsProgressStore.setChineseReadingIndex(this@ChineseReadingActivity, position)
                updateProgress()
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ChineseReadingActivity::class.java))
        }
    }
}
