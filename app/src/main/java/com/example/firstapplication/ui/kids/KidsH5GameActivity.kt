package com.example.firstapplication.ui.kids

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.firstapplication.databinding.ActivityKidsH5GameBinding
import com.opensource.svgaplayer.SVGACallback
import com.opensource.svgaplayer.SVGADrawable
import com.opensource.svgaplayer.SVGADynamicEntity
import com.opensource.svgaplayer.SVGAParser
import com.opensource.svgaplayer.SVGAImageView
import com.opensource.svgaplayer.SVGAVideoEntity
import java.io.IOException

/**
 * H5 小游戏二级页：全屏 WebView 播放单个本地小游戏
 * 由 KidsH5Activity 游戏列表点击进入，顶部返回栏返回列表
 */
class KidsH5GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKidsH5GameBinding
    private var isChessGame = false
    private var svgaParser: SVGAParser? = null
    private var svgaEntityCache: SVGAVideoEntity? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKidsH5GameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemBars()

        val file = intent.getStringExtra(EXTRA_GAME) ?: GAME_NUMBER
        isChessGame = file == GAME_CHESS

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
                // 方案B：页面加载完成后预加载吃子过场 SVGA（首次吃子零等待）
                if (isChessGame) {
                    preloadChessSvga()
                }
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

        binding.btnExit.setOnClickListener { showExitConfirm() }

        // 方案B：仅象棋注入吃子过场桥（原生 SVGA 覆盖层接管；未接管时 H5 自行降级）
        if (isChessGame) {
            binding.webGame.addJavascriptInterface(ChessSvgaBridge(), "AndroidBridge")
        }
        binding.webGame.loadUrl("file:///android_asset/h5/$file")
    }

    override fun onBackPressed() {
        if (binding.webGame.canGoBack()) {
            binding.webGame.goBack()
        } else {
            showExitConfirm()
        }
    }

    /** 隐藏系统状态栏与导航栏，实现真正全屏 */
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

    // ================= 方案B：象棋吃子过场 SVGA 覆盖层 =================

    /** 预加载吃子过场 SVGA 到内存缓存（decode 异步执行，不阻塞主线程） */
    private fun preloadChessSvga() {
        if (svgaParser != null || svgaEntityCache != null) return
        val parser = SVGAParser(applicationContext)
        svgaParser = parser
        parser.init(applicationContext)
        parser.decodeFromAssets(
            SVGA_CUTSCENE,
            object : SVGAParser.ParseCompletion {
                override fun onError() {
                    Log.w(TAG, "过场SVGA解析失败，将降级到 H5 3D 过场: $SVGA_CUTSCENE")
                }

                override fun onComplete(svgaVideoEntity: SVGAVideoEntity) {
                    svgaEntityCache = svgaVideoEntity
                    Log.d(TAG, "过场SVGA预加载完成: ${svgaVideoEntity.frames} 帧")
                }
            },
            object : SVGAParser.PlayCallback {
                override fun onPlay(file: List<java.io.File>) {}
            }
        )
    }

    /**
     * 吃子过场桥：H5 同步调用，返回 1=原生已接管播放；0=未接管（H5 降级）
     * 运行在 WebView 子线程；仅做对象构造与资源读取，UI 操作切主线程
     */
    private inner class ChessSvgaBridge {
        @android.webkit.JavascriptInterface
        fun playCapture(eaterSide: String, eaterType: Int, eatenSide: String, eatenType: Int): Int {
            val entity = svgaEntityCache ?: return 0
            val eatenName = pieceName(eatenSide, eatenType) ?: return 0
            val dynamic = SVGADynamicEntity()
            // 标题文字（模板 key: title），例：⚔️ 吃！车
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFD54F")
                textSize = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 58f, resources.displayMetrics
                )
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val title = "⚔️ 吃！$eatenName"
            val titleLayout = StaticLayout.Builder
                .obtain(title, 0, title.length, titlePaint, resources.displayMetrics.widthPixels)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()
            dynamic.setDynamicText(titleLayout, SVGA_KEY_TITLE)
            // 立绘（模板 key: eater/eaten），缺失时跳过，使用模板默认图
            loadPortrait(eaterSide, eaterType)?.let { dynamic.setDynamicImage(it, SVGA_KEY_EATER) }
            loadPortrait(eatenSide, eatenType)?.let { dynamic.setDynamicImage(it, SVGA_KEY_EATEN) }

            val drawable = SVGADrawable(entity, dynamic)
            runOnUiThread {
                binding.svgaCutscene.stopAnimation(true)
                binding.svgaCutscene.setImageDrawable(drawable)
                binding.svgaCutscene.loops = 1 // 只播一遍，结束后 onFinished 隐藏覆盖层
                binding.svgaCutscene.fillMode = SVGAImageView.FillMode.Forward
                binding.svgaCutscene.visibility = View.VISIBLE
                binding.svgaCutscene.callback = object : SVGACallback {
                    override fun onFinished() {
                        binding.svgaCutscene.visibility = View.GONE
                    }

                    override fun onPause() {}
                    override fun onRepeat() {}
                    override fun onStep(frame: Int, percentage: Double) {}
                }
                binding.svgaCutscene.startAnimation()
            }
            return 1
        }
    }

    /** 棋子类型索引 → 中文名 */
    private fun pieceName(side: String, type: Int): String? {
        val names = if (side == "red") RED_NAMES else BLACK_NAMES
        return names.getOrNull(type)
    }

    /** 读取棋子立绘（h5/chess_assets/cs_<type>_<side>.png），文件缺失返回 null */
    private fun loadPortrait(side: String, type: Int): Bitmap? {
        val key = TYPE_KEYS.getOrNull(type) ?: return null
        val path = "h5/chess_assets/cs_${key}_$side.png"
        return try {
            assets.open(path).use { android.graphics.BitmapFactory.decodeStream(it) }
        } catch (e: IOException) {
            Log.d(TAG, "立绘缺失，使用模板默认图: $path")
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理桥与覆盖层，防止 WebView/动画资源泄漏
        if (::binding.isInitialized) {
            binding.webGame.removeJavascriptInterface("AndroidBridge")
            binding.svgaCutscene.stopAnimation(true)
            binding.svgaCutscene.setImageDrawable(null)
            binding.svgaCutscene.callback = null
            binding.svgaCutscene.visibility = View.GONE
        }
        svgaParser = null
        svgaEntityCache = null
    }

    companion object {
        private const val TAG = "KidsH5"
        private const val EXTRA_GAME = "extra_game"
        private const val EXTRA_TITLE = "extra_title"
        private const val GAME_NUMBER = "number_match.html"
        private const val GAME_CHESS = "chinese_chess.html"

        // 方案B：过场 SVGA 模板与动态替换 key
        private const val SVGA_CUTSCENE = "svga/chess_cutscene.svga"
        private const val SVGA_KEY_TITLE = "title"
        private const val SVGA_KEY_EATER = "eater"
        private const val SVGA_KEY_EATEN = "eaten"
        private val RED_NAMES = arrayOf("帅", "仕", "相", "马", "车", "炮", "兵")
        private val BLACK_NAMES = arrayOf("将", "士", "象", "马", "车", "炮", "卒")
        private val TYPE_KEYS = arrayOf("king", "advisor", "elephant", "horse", "rook", "cannon", "pawn")

        fun start(context: Context, file: String, title: String) {
            context.startActivity(
                Intent(context, KidsH5GameActivity::class.java)
                    .putExtra(EXTRA_GAME, file)
                    .putExtra(EXTRA_TITLE, title)
            )
        }
    }
}
