package com.example.firstapplication.ui.kids

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
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
        // 允许 file:// 页面加载本地子资源（如 minecraft.html 引用的 lib/three.min.js）
        @Suppress("DEPRECATION")
        binding.webGame.settings.allowFileAccessFromFileURLs = true
        @Suppress("DEPRECATION")
        binding.webGame.settings.allowUniversalAccessFromFileURLs = true

        // 把 H5 的 console.log 转发到 Logcat（Tag=KidsH5），用于排查“点击进入世界没反应”
        binding.webGame.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                Log.d(TAG, "[${msg?.messageLevel()}] ${msg?.message()} @${msg?.lineNumber()}:${msg?.sourceId()}")
                return super.onConsoleMessage(msg)
            }
        }
        binding.webGame.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "onPageFinished url=$url")
                // 通知页面脚本：页面已加载完成（用于打印最终状态）
                view?.evaluateJavascript("try{window.__dbg&&window.__dbg('pageFinished');}catch(e){}", null)
            }

            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "onReceivedError(21+) url=${request?.url} code=${error?.errorCode} desc=${error?.description}")
            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(
                view: WebView?, errorCode: Int, description: String?, failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e(TAG, "onReceivedError(old) code=$errorCode desc=$description url=$failingUrl")
            }

            override fun onReceivedHttpError(
                view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                Log.e(TAG, "onReceivedHttpError url=${request?.url} status=${errorResponse?.statusCode}")
            }
        }

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
        private const val TAG = "KidsH5"
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
