package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.firstapplication.databinding.ActivityEnglishBinding
import com.google.android.material.card.MaterialCardView

/**
 * 英语乐园入口页：四张模块卡片，分别进入四个二级页面
 * ①字母表 EnglishAlphabetActivity ②单词句子 EnglishWordsActivity ③每日打卡 EnglishCheckinActivity ④互动游戏 EnglishGameActivity
 */
class EnglishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnglishBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnglishBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 橙黄渐变浅色背景 → 深色状态栏图标
        KidsStatusBar.immersive(this, binding.root, lightStatusBar = true)
        KidsTts.init(this)

        bindCardClick(binding.cardAlphabet) { EnglishAlphabetActivity.start(this) }
        bindCardClick(binding.cardWords) { EnglishWordsActivity.start(this) }
        bindCardClick(binding.cardCheckin) { EnglishCheckinActivity.start(this) }
        bindCardClick(binding.cardGame) { EnglishGameActivity.start(this) }
    }

    private fun bindCardClick(card: MaterialCardView, action: () -> Unit) {
        card.setOnClickListener { action() }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, EnglishActivity::class.java))
        }
    }
}
