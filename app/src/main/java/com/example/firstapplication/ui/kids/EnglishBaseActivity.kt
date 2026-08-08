package com.example.firstapplication.ui.kids

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.firstapplication.databinding.ItemEnglishFlashcardBinding
import com.example.firstapplication.databinding.ItemEnglishSentenceBinding
import com.example.firstapplication.ui.kids.EnglishLibrary.EnglishWord
import com.example.firstapplication.ui.kids.EnglishLibrary.Sentence

/**
 * 英语乐园二级页面基类：共享 dp 换算、闪卡吸附、圆角 chip、单词/句子闪卡绑定工具
 * 四个二级页（字母表 / 单词句子 / 每日打卡 / 互动游戏）都继承自本类
 */
abstract class EnglishBaseActivity : AppCompatActivity() {

    protected fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /** 闪卡 RecyclerView：一次一张，滑动吸附，停止时回调 (position, total) */
    protected fun attachSnap(rv: RecyclerView, onIdle: (pos: Int, total: Int) -> Unit) {
        val helper = LinearSnapHelper()
        helper.attachToRecyclerView(rv)
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val lm = rv.layoutManager as? LinearLayoutManager ?: return
                    val snap = helper.findSnapView(lm) ?: return
                    onIdle(lm.getPosition(snap), rv.adapter?.itemCount ?: 0)
                }
            }
        })
    }

    /** 圆角 chip：选中白底深字 / 未选中半透明深底白字 */
    protected fun createChip(text: String, selected: Boolean): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 15f
        tv.setTextColor(if (selected) 0xFF7A3C00.toInt() else Color.WHITE)
        tv.gravity = Gravity.CENTER
        tv.background = chipBackground(selected)
        tv.setPadding(22, 12, 22, 12)
        val lp = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.marginEnd = 10
        tv.layoutParams = lp
        return tv
    }

    protected fun chipBackground(selected: Boolean): GradientDrawable = GradientDrawable().apply {
        cornerRadius = 22f
        if (selected) {
            setColor(Color.WHITE)
        } else {
            setColor(0x33000000)
            setStroke(1, 0x66FFFFFF.toInt())
        }
    }

    /** 绑定单词闪卡：单词 + 音标 + 中文 + 2 个例句，点击朗读 */
    protected fun bindFlashcard(b: ItemEnglishFlashcardBinding, word: EnglishWord) {
        b.tvFlashWord.text = word.word
        b.tvFlashPhonetic.text = word.phonetic
        b.tvFlashChinese.text = word.chinese
        b.tvFlashEmoji.text = word.emoji
        b.flashExampleList.removeAllViews()
        EnglishLibrary.exampleSentencesFor(word).forEach { sentence ->
            val block = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 6, 0, 6)
                setOnClickListener { KidsTts.speakEnglish(sentence.en) }
            }
            block.addView(TextView(this).apply {
                text = sentence.en
                setTextColor(0xFF455A64.toInt())
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            block.addView(TextView(this).apply {
                text = "    ${sentence.cn}"
                setTextColor(0xFF78909C.toInt())
                textSize = 13f
            })
            b.flashExampleList.addView(block)
        }
        b.btnSpeakFlash.setOnClickListener { KidsTts.speakEnglish(word.word) }
        b.root.setOnClickListener { KidsTts.speakEnglish(word.word) }
    }

    /** 绑定句子闪卡：英文 + 中文，点击朗读 */
    protected fun bindSentence(b: ItemEnglishSentenceBinding, sentence: Sentence) {
        b.tvSentenceEn.text = sentence.en
        b.tvSentenceCn.text = sentence.cn
        b.root.setOnClickListener { KidsTts.speakEnglish(sentence.en) }
        b.btnSpeakSentence.setOnClickListener { KidsTts.speakEnglish(sentence.en) }
    }
}
