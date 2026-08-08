package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.firstapplication.databinding.ActivityChineseWordBinding
import com.google.android.material.card.MaterialCardView

/**
 * 汉字乐园入口页：五张模块卡片，分别进入五个二级页面
 * ①基础笔画 ChineseStrokeActivity ②笔画演示 ChineseDemoActivity ③汉字识字 ChineseReadingActivity
 * ④仿写描红 ChineseTracingActivity ⑤每日一句 ChineseDailyActivity
 */
class ChineseWordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChineseWordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChineseWordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 浅色渐变背景 → 深色状态栏图标
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        bindCardClick(binding.cardStroke) { ChineseStrokeActivity.start(this) }
        bindCardClick(binding.cardDemo) { ChineseDemoActivity.start(this) }
        bindCardClick(binding.cardReading) { ChineseReadingActivity.start(this) }
        bindCardClick(binding.cardTracing) { ChineseTracingActivity.start(this) }
        bindCardClick(binding.cardDaily) { ChineseDailyActivity.start(this) }
    }

    private fun bindCardClick(card: MaterialCardView, action: () -> Unit) {
        card.setOnClickListener { action() }
    }

    companion object {
        /** 识字库总字数（首页进度条用） */
        val CHAR_TOTAL = HanziLibrary.EXTENDED_HANZI.size

        fun start(context: Context) {
            context.startActivity(Intent(context, ChineseWordActivity::class.java))
        }
    }
}
