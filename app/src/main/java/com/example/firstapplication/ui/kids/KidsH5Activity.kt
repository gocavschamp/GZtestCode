package com.example.firstapplication.ui.kids

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.firstapplication.databinding.ActivityKidsH5Binding

/**
 * H5 小游戏容器：加载本地 assets/h5 下的 HTML 小游戏
 * - 数字翻牌（数字记忆配对）
 * - 汉字连连看（经典连通消除）
 * - 拼图（3x3 emoji 滑动拼图）
 */
class KidsH5Activity : AppCompatActivity() {

    private lateinit var binding: ActivityKidsH5Binding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKidsH5Binding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root)

        binding.webGame.settings.javaScriptEnabled = true
        binding.webGame.settings.domStorageEnabled = true
        binding.webGame.settings.allowFileAccess = true
        binding.webGame.webViewClient = WebViewClient()

        binding.btnGameNumber.setOnClickListener { loadGame(GAME_NUMBER) }
        binding.btnGameHanzi.setOnClickListener { loadGame(GAME_HANZI) }
        binding.btnGamePuzzle.setOnClickListener { loadGame(GAME_PUZZLE) }
        loadGame(GAME_NUMBER)
    }

    private fun loadGame(file: String) {
        binding.webGame.loadUrl("file:///android_asset/h5/$file")
    }

    override fun onBackPressed() {
        if (binding.webGame.canGoBack()) {
            binding.webGame.goBack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val GAME_NUMBER = "number_match.html"
        private const val GAME_HANZI = "hanzi_link.html"
        private const val GAME_PUZZLE = "puzzle.html"

        fun start(context: Context) {
            context.startActivity(Intent(context, KidsH5Activity::class.java))
        }
    }
}
