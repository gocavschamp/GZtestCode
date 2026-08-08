package com.example.firstapplication.ui.kids

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.firstapplication.databinding.ActivityKidsH5GameBinding

/**
 * H5 小游戏二级页：全屏 WebView 播放单个本地小游戏
 * 由 KidsH5Activity 游戏列表点击进入，顶部返回栏返回列表
 */
class KidsH5GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKidsH5GameBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKidsH5GameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root)

        val file = intent.getStringExtra(EXTRA_GAME) ?: GAME_NUMBER
        binding.tvTitle.text = intent.getStringExtra(EXTRA_TITLE) ?: "小游戏"

        binding.webGame.settings.javaScriptEnabled = true
        binding.webGame.settings.domStorageEnabled = true
        binding.webGame.settings.allowFileAccess = true
        binding.webGame.webViewClient = WebViewClient()

        binding.btnBack.setOnClickListener { showExitConfirm() }
        binding.webGame.loadUrl("file:///android_asset/h5/$file")
    }

    override fun onBackPressed() {
        if (binding.webGame.canGoBack()) {
            binding.webGame.goBack()
        } else {
            showExitConfirm()
        }
    }

    /** 退出确认弹窗：避免小朋友误触退出 */
    private fun showExitConfirm() {
        AlertDialog.Builder(this)
            .setTitle("退出游戏")
            .setMessage("确定要退出小游戏吗？")
            .setPositiveButton("退出") { _, _ -> finish() }
            .setNegativeButton("继续玩", null)
            .show()
    }

    companion object {
        private const val EXTRA_GAME = "extra_game"
        private const val EXTRA_TITLE = "extra_title"
        private const val GAME_NUMBER = "number_match.html"

        fun start(context: Context, file: String, title: String) {
            context.startActivity(
                Intent(context, KidsH5GameActivity::class.java)
                    .putExtra(EXTRA_GAME, file)
                    .putExtra(EXTRA_TITLE, title)
            )
        }
    }
}
