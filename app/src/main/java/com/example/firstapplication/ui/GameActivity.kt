package com.example.firstapplication.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.firstapplication.R
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt

class GameActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GameActivity"
        private const val GAME_URL = "https://juejin.cn/post/7571641339688812544?searchId=2025120210471078B707701B471D206EFC"

        // Intent参数
        private const val EXTRA_KEEP_IN_BACKGROUND = "keep_in_background"
        private const val EXTRA_TRANSPARENT_MODE = "transparent_mode"
        private const val EXTRA_OPAQUE_RATIO = "opaque_ratio"
        private const val EXTRA_OPAQUE_HEIGHT = "opaque_height"
        private const val EXTRA_OPAQUE_WIDTH = "opaque_width"
        private const val EXTRA_OPAQUE_POSITION = "opaque_position"
        private const val EXTRA_SHOW_CONTROLS = "show_controls"

        // 位置常量
        const val POSITION_TOP = 0
        const val POSITION_BOTTOM = 1
        const val POSITION_CENTER = 2
        const val POSITION_LEFT = 3
        const val POSITION_RIGHT = 4
        const val POSITION_CUSTOM = 5

        /**
         * 启动游戏Activity
         * @param context 上下文
         * @param keepInBackground 是否在后台保留（按返回键时移到后台而不是销毁）
         * @param transparentMode 是否为透明模式
         * @param opaqueRatio 不透明部分占屏幕的比例（0.1-1.0），仅当transparentMode=true时有效
         * @param opaqueHeight 不透明部分的高度（像素），如果设置了此值，opaqueRatio将被忽略
         * @param opaqueWidth 不透明部分的宽度（像素），如果设置了此值，opaqueRatio将被忽略
         * @param opaquePosition 不透明部分的位置（POSITION_TOP等）
         * @param showControls 是否显示控制面板
         */
        fun launch(
            context: Context,
            keepInBackground: Boolean = true,
            transparentMode: Boolean = false,
            opaqueRatio: Float = 1.0f,
            opaqueHeight: Int = 0,
            opaqueWidth: Int = 0,
            opaquePosition: Int = POSITION_CENTER,
            showControls: Boolean = false
        ) {
            val intent = Intent(context, GameActivity::class.java).apply {
                // 使用NEW_TASK，这样GameActivity会在新的任务栈中启动
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
//                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                // 设置参数
                putExtra(EXTRA_KEEP_IN_BACKGROUND, keepInBackground)
                putExtra(EXTRA_TRANSPARENT_MODE, transparentMode)
                putExtra(EXTRA_OPAQUE_RATIO, opaqueRatio)
                putExtra(EXTRA_OPAQUE_HEIGHT, opaqueHeight)
                putExtra(EXTRA_OPAQUE_WIDTH, opaqueWidth)
                putExtra(EXTRA_OPAQUE_POSITION, opaquePosition)
                putExtra(EXTRA_SHOW_CONTROLS, showControls)
            }

            context.startActivity(intent)
        }

        /**
         * 简单启动（默认参数）
         */
        fun launchSimple(context: Context) {
            launch(context)
        }

        /**
         * 启动为透明窗口模式
         */
        fun launchAsTransparentWindow(
            context: Context,
            opaqueRatio: Float = 0.8f,
            position: Int = POSITION_CENTER
        ) {
            launch(
                context = context,
                keepInBackground = true,
                transparentMode = true,
                opaqueRatio = opaqueRatio,
                opaquePosition = position,
                showControls = true
            )
        }
    }

    // UI组件
    private lateinit var webView: WebView
    private lateinit var webViewContainer: FrameLayout
    private lateinit var rootContainer: FrameLayout
    private lateinit var opaqueContainer: FrameLayout
    private lateinit var controlPanel: LinearLayout
    private lateinit var btnClose: ImageButton
    private lateinit var btnMinimize: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var tvStatus: TextView
//    private lateinit var sizeSlider: Slider

    // 参数
    private var keepInBackground = true
    private var transparentMode = false
    private var opaqueRatio = 1.0f
    private var opaqueHeight = 0
    private var opaqueWidth = 0
    private var opaquePosition = POSITION_CENTER
    private var showControls = false

    // 状态
    private var webViewState: Bundle? = null
    private var isInBackgroundMode = false
    private var originalWindowFlags = 0
    private var screenWidth = 0
    private var screenHeight = 0
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate - savedInstanceState: ${savedInstanceState != null}")

        // 获取屏幕尺寸
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        // 初始化参数
        initParameters(intent, savedInstanceState)

        // 设置窗口属性
        setupWindow()

        // 设置布局
        setContentView(R.layout.activity_game)

        // 初始化UI
        initViews()
        // 现在才设置不透明容器的大小（因为需要先初始化视图）
        if (transparentMode) {
            updateOpaqueContainerSize()
        }
        // 初始化WebView
        initWebView()

        // 设置控制面板
        setupControls()

        // 加载或恢复游戏
        if (savedInstanceState != null) {
            restoreWebViewState(savedInstanceState)
        } else {
            loadGameUrl()
        }

        Log.d(TAG, "GameActivity created with params: " +
                "keepInBackground=$keepInBackground, " +
                "transparentMode=$transparentMode, " +
                "opaqueRatio=$opaqueRatio, " +
                "position=$opaquePosition")
    }

    /**
     * 初始化参数
     */
    private fun initParameters(intent: Intent?, savedInstanceState: Bundle?) {
        // 优先从savedInstanceState恢复，然后从intent获取
        if (savedInstanceState != null) {
            keepInBackground = savedInstanceState.getBoolean(EXTRA_KEEP_IN_BACKGROUND, true)
            transparentMode = savedInstanceState.getBoolean(EXTRA_TRANSPARENT_MODE, false)
            opaqueRatio = savedInstanceState.getFloat(EXTRA_OPAQUE_RATIO, 1.0f)
            opaqueHeight = savedInstanceState.getInt(EXTRA_OPAQUE_HEIGHT, 0)
            opaqueWidth = savedInstanceState.getInt(EXTRA_OPAQUE_WIDTH, 0)
            opaquePosition = savedInstanceState.getInt(EXTRA_OPAQUE_POSITION, POSITION_CENTER)
            showControls = savedInstanceState.getBoolean(EXTRA_SHOW_CONTROLS, false)
        } else if (intent != null) {
            keepInBackground = intent.getBooleanExtra(EXTRA_KEEP_IN_BACKGROUND, true)
            transparentMode = intent.getBooleanExtra(EXTRA_TRANSPARENT_MODE, false)
            opaqueRatio = intent.getFloatExtra(EXTRA_OPAQUE_RATIO, 1.0f).coerceIn(0.1f, 1.0f)
            opaqueHeight = intent.getIntExtra(EXTRA_OPAQUE_HEIGHT, 0)
            opaqueWidth = intent.getIntExtra(EXTRA_OPAQUE_WIDTH, 0)
            opaquePosition = intent.getIntExtra(EXTRA_OPAQUE_POSITION, POSITION_CENTER)
            showControls = intent.getBooleanExtra(EXTRA_SHOW_CONTROLS, false)
        }
    }

    /**
     * 设置窗口属性
     */
    private fun setupWindow() {
        // 保存原始窗口标志
        originalWindowFlags = window.attributes.flags

        // 设置全屏和保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (transparentMode) {
            // 透明模式
            window.setBackgroundDrawableResource(android.R.color.transparent)

            // 设置窗口透明
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = Color.TRANSPARENT
                window.navigationBarColor = Color.TRANSPARENT
            }

            // 设置窗口半透明
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

            // 使内容延伸到状态栏
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )

            // 设置窗口尺寸和位置
//            updateOpaqueContainerSize()
        } else {
            // 不透明模式
            window.setBackgroundDrawableResource(android.R.color.black)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = Color.BLACK
                window.navigationBarColor = Color.BLACK
            }
        }
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        rootContainer = findViewById(R.id.root_container)
        opaqueContainer = findViewById(R.id.opaque_container)
        webViewContainer = findViewById(R.id.webview_container)
        controlPanel = findViewById(R.id.control_panel)
        btnClose = findViewById(R.id.btn_close)
        btnMinimize = findViewById(R.id.btn_minimize)
        btnSettings = findViewById(R.id.btn_settings)
        btnRefresh = findViewById(R.id.btn_refresh)
        tvStatus = findViewById(R.id.tv_status)
//        sizeSlider = findViewById(R.id.size_slider)
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "handleOnBackPressed - keepInBackground=$keepInBackground")

                // 如果WebView可以返回，则返回上一页
                if (webView.canGoBack()) {
                    webView.goBack()
                    return
                }

                // 根据设置处理返回键
                if (keepInBackground) {
                    moveToBackground()
                } else {
                    finishGame()
                }
            }
        }
        // 根据透明模式设置背景
        if (transparentMode) {
            rootContainer.setBackgroundColor(Color.TRANSPARENT)
            opaqueContainer.setBackgroundColor(Color.WHITE)

            // 设置圆角
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                opaqueContainer.clipToOutline = true
                opaqueContainer.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View?, outline: Outline?) {
                        val radius = 20f
                        outline?.setRoundRect(0, 0, view?.width?:0, view?.height?:0, radius)
                    }
                }
            }
        } else {
            rootContainer.setBackgroundColor(Color.BLACK)
            opaqueContainer.setBackgroundColor(Color.BLACK)
        }

        // 设置控制面板可见性
        controlPanel.isVisible = showControls
    }

    /**
     * 更新不透明容器大小
     */
    private fun updateOpaqueContainerSize() {
        if (!transparentMode) return

        val layoutParams = opaqueContainer.layoutParams as FrameLayout.LayoutParams

        // 计算宽度和高度
//        val width = if (opaqueWidth > 0) {
//            opaqueWidth
//        } else {
//            (screenWidth * opaqueRatio).toInt()
//        }
        val width = screenWidth
        val height = if (opaqueHeight > 0) {
            opaqueHeight
        } else {
            (screenHeight * opaqueRatio).toInt()
        }

        layoutParams.width = width.coerceAtMost(screenWidth)
        layoutParams.height = height.coerceAtMost(screenHeight)

        opaquePosition = POSITION_BOTTOM
        // 根据位置设置重力
        when (opaquePosition) {
            POSITION_TOP -> {
                layoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }
            POSITION_BOTTOM -> {
                layoutParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            POSITION_LEFT -> {
                layoutParams.gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
            }
            POSITION_RIGHT -> {
                layoutParams.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            }
            POSITION_CENTER -> {
                layoutParams.gravity = Gravity.CENTER
            }
            POSITION_CUSTOM -> {
                // 自定义位置，可以拖拽
                layoutParams.gravity = Gravity.TOP or Gravity.LEFT
            }
        }

        opaqueContainer.layoutParams = layoutParams

        // 更新控制面板
//        sizeSlider.value = opaqueRatio

        Log.d(TAG, "Opaque container size: ${layoutParams.width}x${layoutParams.height}")
    }

    /**
     * 初始化WebView
     */
    private fun initWebView() {

        webView = WebView(applicationContext).apply {
            // 基本配置
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT

                // 性能优化
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false

                // 安全设置
                allowFileAccess = false
                allowContentAccess = false

                // 透明背景支持
                if (transparentMode) {
                    setBackgroundColor(Color.TRANSPARENT)
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
            }

            // 设置背景
            if (transparentMode) {
                setBackgroundColor(Color.TRANSPARENT)
                setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            }

            // WebView客户端
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    tvStatus.text = "加载中..."
                    Log.d(TAG, "Page started: $url")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    tvStatus.text = "加载完成"
                    Log.d(TAG, "Page finished: $url")

                    // 注入CSS使页面背景透明（如果需要）
//                    if (transparentMode) {
//                        injectTransparentCSS()
//                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    tvStatus.text = "加载错误: ${error?.description}"
                    Log.e(TAG, "WebView error: ${error?.description}")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.d(TAG, "Console: ${consoleMessage.message()}")
                    return true
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (newProgress < 100) {
                        tvStatus.text = "加载中... $newProgress%"
                    }
                }
            }
        }

        // 添加到容器
        webViewContainer.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        Log.d(TAG, "WebView initialized")
    }

    /**
     * 注入透明CSS
     */
    private fun injectTransparentCSS() {
        val cssCode = """
            <style>
                body { background-color: transparent !important; }
                html { background-color: transparent !important; }
                * { background-color: transparent !important; }
            </style>
        """.trimIndent()

        val jsCode = """
            var style = document.createElement('style');
            style.type = 'text/css';
            style.innerHTML = '$cssCode';
            document.head.appendChild(style);
        """.trimIndent()

        webView.evaluateJavascript(jsCode, null)
    }

    /**
     * 设置控制面板
     */
    private fun setupControls() {
        // 关闭按钮
        btnClose.setOnClickListener {
            finishGame()
        }

        // 最小化按钮
        btnMinimize.setOnClickListener {
            moveToBackground()
        }

        // 设置按钮
        btnSettings.setOnClickListener {
            toggleSettingsPanel()
        }

        // 刷新按钮
        btnRefresh.setOnClickListener {
            webView.reload()
            tvStatus.text = "刷新中..."
        }

        // 大小滑块
//        sizeSlider.value = opaqueRatio
//        sizeSlider.addOnChangeListener { _, value, fromUser ->
//            if (fromUser && transparentMode) {
//                opaqueRatio = value
//                updateOpaqueContainerSize()
//                tvStatus.text = "大小: ${(value * 100).roundToInt()}%"
//            }
//        }

        // 拖拽功能（如果位置是自定义的）
        if (opaquePosition == POSITION_CUSTOM && transparentMode) {
            setupDrag()
        }
    }

//    /**
//     * 设置拖拽功能
//     */
    private fun setupDrag() {
        opaqueContainer.setOnTouchListener(object : View.OnTouchListener {
            private var dX = 0f
            private var dY = 0f

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val newX = event.rawX + dX
                        val newY = event.rawY + dY

                        // 限制在屏幕内
                        val layoutParams = view.layoutParams as FrameLayout.LayoutParams
                        val maxX = screenWidth - layoutParams.width
                        val maxY = screenHeight - layoutParams.height

                        view.x = newX.coerceIn(0f, maxX.toFloat())
                        view.y = newY.coerceIn(0f, maxY.toFloat())

                        return true
                    }
                }
                return false
            }
        })
    }

    /**
     * 切换设置面板
     */
    private fun toggleSettingsPanel() {
        val isVisible = controlPanel.isVisible
        controlPanel.isVisible = !isVisible

        if (!isVisible) {
            // 显示时更新状态
            updateStatusDisplay()
        }
    }

    /**
     * 更新状态显示
     */
    private fun updateStatusDisplay() {
        val status = """
            模式: ${if (transparentMode) "透明窗口" else "全屏"}
            大小: ${(opaqueRatio * 100).roundToInt()}%
            后台保留: ${if (keepInBackground) "是" else "否"}
            控制面板: ${if (showControls) "显示" else "隐藏"}
        """.trimIndent()

        tvStatus.text = status
    }

    /**
     * 加载游戏URL
     */
    private fun loadGameUrl() {
        Log.d(TAG, "Loading game URL: $GAME_URL")
        webView.loadUrl(GAME_URL)
        tvStatus.text = "加载中..."
    }

    /**
     * 恢复WebView状态
     */
    private fun restoreWebViewState(savedInstanceState: Bundle) {
        webViewState = savedInstanceState.getBundle("webview_state")
        if (webViewState != null) {
            Log.d(TAG, "Restoring WebView from saved state")
            webView.restoreState(webViewState!!)
            tvStatus.text = "状态已恢复"
        } else {
            loadGameUrl()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState")

        // 保存WebView状态
        webViewState = Bundle().apply {
            webView.saveState(this)
        }
        outState.putBundle("webview_state", webViewState)

        // 保存参数
        outState.putBoolean(EXTRA_KEEP_IN_BACKGROUND, keepInBackground)
        outState.putBoolean(EXTRA_TRANSPARENT_MODE, transparentMode)
        outState.putFloat(EXTRA_OPAQUE_RATIO, opaqueRatio)
        outState.putInt(EXTRA_OPAQUE_HEIGHT, opaqueHeight)
        outState.putInt(EXTRA_OPAQUE_WIDTH, opaqueWidth)
        outState.putInt(EXTRA_OPAQUE_POSITION, opaquePosition)
        outState.putBoolean(EXTRA_SHOW_CONTROLS, showControls)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "onRestoreInstanceState")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent - Activity已经存在，被重新唤起")

        // 更新参数（如果intent中有新参数）
        initParameters(intent, null)

        // 如果参数有变化，更新UI
        if (intent?.hasExtra(EXTRA_TRANSPARENT_MODE) == true ||
            intent?.hasExtra(EXTRA_OPAQUE_RATIO) == true) {
            recreate()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")

        // 恢复WebView
        webView.onResume()

        // 如果有保存的状态，恢复它
        webViewState?.let {
            webView.restoreState(it)
            webViewState = null // 恢复后清除
            tvStatus.text = "已恢复"
        }

        isInBackgroundMode = false
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")

        // 暂停WebView
        webView.onPause()

        // 保存当前状态到变量
        if (!isChangingConfigurations) {
            webViewState = Bundle().apply {
                webView.saveState(this)
            }
            tvStatus.text = "已保存状态"
        }
    }

    override fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        return super.getOnBackInvokedDispatcher()
    }


// 注册回调
//    override fun onBackPressed() {
//        super.onBackPressed()
//        Log.d(TAG, "onBackPressed - keepInBackground=$keepInBackground")
//
//        // 如果WebView可以返回，则返回上一页
//        if (webView.canGoBack()) {
//            webView.goBack()
//            return
//        }
//
//        // 根据设置处理返回键
//        if (keepInBackground) {
//            moveToBackground()
//        } else {
//            finishGame()
//        }
//    }

    /**
     * 将Activity移到后台（不销毁）
     */
    private fun moveToBackground() {
        Log.d(TAG, "Moving activity to background")
        isInBackgroundMode = true

        // 保存状态
        webViewState = Bundle().apply {
            webView.saveState(this)
        }

        // 将Activity移到后台
        moveTaskToBack(true)

        // 使用无动画过渡
        overridePendingTransition(0, 0)

        tvStatus.text = "已最小化到后台"
    }

    /**
     * 完全结束游戏（销毁Activity和WebView）
     */
    fun finishGame() {
        Log.d(TAG, "Finishing game completely")

        // 清理WebView
        cleanupWebView()

        // 恢复窗口属性
        if (transparentMode) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }

        // 结束Activity
        finish()

        // 使用淡出动画
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    /**
     * 清理WebView资源
     */
    private fun cleanupWebView() {
        Log.d(TAG, "Cleaning up WebView")

        webView.apply {
            // 停止加载
            stopLoading()

            // 清除回调
//            webViewClient = null
            webChromeClient = null

            // 从父容器移除
            (parent as? ViewGroup)?.removeView(this)

            // 销毁WebView
            destroy()
        }

        webViewState = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy - isFinishing: $isFinishing, isChangingConfigurations: $isChangingConfigurations")

        if (isFinishing && !isInBackgroundMode) {
            // Activity正在被销毁，清理WebView
            cleanupWebView()
        }

        // 移除屏幕常亮标志
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * 动画改变窗口大小
     */
    fun animateWindowSize(targetRatio: Float, duration: Long = 300) {
        if (!transparentMode) return

        val animator = ValueAnimator.ofFloat(opaqueRatio, targetRatio).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                opaqueRatio = animation.animatedValue as Float
                updateOpaqueContainerSize()
            }
        }

        animator.start()
    }
}