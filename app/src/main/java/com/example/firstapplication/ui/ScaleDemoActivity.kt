package com.example.firstapplication.ui

import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import com.example.firstapplication.R
import com.example.firstapplication.databinding.ActivityScaleBinding
import com.example.firstapplication.ui.fragment.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlin.math.max
import kotlin.math.min

/**
 * 简单使用demo
 */
class ScaleDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AnimSimpleDemoActivity"
    }

    private lateinit var binding: ActivityScaleBinding
    private val dir by lazy {
        // 存放在sdcard应用缓存文件中
        getExternalFilesDir(null)?.absolutePath ?: Environment.getExternalStorageDirectory().path
    }
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    // 状态栏高度
    private var statusBarHeight = 0
    // 视频信息

    private val uiHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置全屏透明状态栏
//        window.apply {
//            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//            decorView.systemUiVisibility =
//                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//            statusBarColor = Color.TRANSPARENT
//        }
        binding = ActivityScaleBinding.inflate(layoutInflater)
        val root = binding.root
        binding.swipeLayout.setOnChildScrollUpCallback { parent, child ->
            // 当 AppBarLayout 完全展开（垂直偏移为0）时，才允许下拉刷新
            // 否则，返回 true 表示子View还能向上滚动，阻止下拉刷新
            binding.appBarLayout.top != 0
        }
//        binding.swipeLayout.isEnabled = false
        binding.swipeLayout.setOnRefreshListener {
            uiHandler.postDelayed({
                binding.swipeLayout.isRefreshing = false
            },2000)
        }
        setContentView(root)
        // 设置Toolbar
//        setSupportActionBar(binding.toolbar)
//        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 获取状态栏高度
        getStatusBarHeight()
        // 文件加载完成后会调用init方法
        init()
        // 初始化Toolbar透明度
        updateToolbarAlpha(0f)

    }
    private fun getStatusBarHeight() {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }

        // 设置Toolbar的顶部padding，避开状态栏
        binding.toolbar.updatePadding(top = statusBarHeight)
    }

    private fun init() {
        // 初始化日志
        // 设置状态栏颜色
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_700)

        initViewPager()
        initTabLayout()
        initListeners()
    }

private fun initViewPager() {
    viewPagerAdapter = ViewPagerAdapter(this)
    binding.viewPager.adapter = viewPagerAdapter
}

private fun initTabLayout() {
    // 将 TabLayout 与 ViewPager 关联
    TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
        tab.text = viewPagerAdapter.getTabTitle(position)
    }.attach()

    // 自定义 Tab 样式
    binding.tabLayout.apply {
        // 设置 Tab 宽度均匀分布
        tabMode = TabLayout.MODE_AUTO
        tabGravity = TabLayout.GRAVITY_CENTER

        // 添加图标（可选）
//        getTabAt(0)?.setIcon(com.example.basetools.R.drawable.ic_icon)
//        getTabAt(1)?.setIcon(com.huantansheng.easyphotos.R.drawable.ic_album_item_choose_easy_photos)
    }
}

private fun initListeners() {
    // Tab 选中监听
    binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            // Tab 选中时的操作
            tab?.let {
                // 可以在这里更新 UI
            }
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
            // Tab 未选中时的操作
        }

        override fun onTabReselected(tab: TabLayout.Tab?) {
            // Tab 重新选中时的操作
            // 通常用于点击当前选中的 Tab 时滚动到顶部
            binding.appBarLayout.setExpanded(true, true)
        }
    })

    // 监听 AppBarLayout 的折叠状态
    binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
        // verticalOffset 从 0（展开）到 -appBarLayout.totalScrollRange（完全折叠）
        Log.e("tag","hegiht $verticalOffset")
        val scrollRange = appBarLayout.totalScrollRange
        val progress = if (scrollRange != 0) {
            -verticalOffset / scrollRange.toFloat()
        } else {
            0f
        }

        // 限制在 0-1 之间
        val clampedProgress = max(0f, min(1f, progress))

        // 根据折叠进度更新 UI
//        updateToolbarAlpha(clampedProgress)
//        updateTitleVisibility(clampedProgress)
////        updateStatusBarColor(clampedProgress)
//        updateIconColor(clampedProgress)
    }

}

    /**
     * 更新Toolbar背景透明度
     * @param progress 折叠进度，0=完全展开，1=完全折叠
     */
    private fun updateToolbarAlpha(progress: Float) {
        // Toolbar背景从透明渐变到白色
        val alpha = (progress * 255).toInt()
        val color = Color.argb(alpha, 255, 255, 255)
        binding.toolbar.setBackgroundColor(color)

        // 同时更新CollapsingToolbarLayout的content scrim
        binding.collapsingToolbar.contentScrim?.alpha = alpha
    }

    /**
     * 更新标题显示状态
     */
    private fun updateTitleVisibility(progress: Float) {
        // 展开时隐藏收缩标题，显示展开标题
        // 收缩时显示收缩标题，隐藏展开标题

        val collapsedTitle = binding.tvCollapsedTitle

        // 使用交叉淡入淡出效果
        if (progress > 0.5f) {
            // 主要显示收缩标题
            collapsedTitle.alpha = (progress - 0.5f) * 2
        } else {
            // 主要显示展开标题
            collapsedTitle.alpha = 0f
        }

        // 更新收缩标题颜色（从白色渐变到黑色）
        if (progress > 0.7f) {
            val colorProgress = (progress - 0.7f) / 0.3f
            val colorValue = (colorProgress * 255).toInt()
            collapsedTitle.setTextColor(Color.BLACK)
        } else {
            collapsedTitle.setTextColor(Color.WHITE)
        }
    }

    /**
     * 更新状态栏颜色
     */
    private fun updateStatusBarColor(progress: Float) {
        // 状态栏从透明渐变到半透明黑色
        val alpha = (progress * 64).toInt() // 25%透明度
        val color = Color.argb(alpha, 0, 0, 0)
        window.statusBarColor = color

        // 更新状态栏图标颜色（浅色/深色）
        if (progress > 0.5f) {
            // 切换到深色状态栏图标
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            // 切换到浅色状态栏图标
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    /**
     * 更新图标颜色
     */
    private fun updateIconColor(progress: Float) {
        // 图标颜色从白色渐变到黑色
        if (progress > 0.7f) {
            val colorProgress = (progress - 0.7f) / 0.3f
            val colorValue = (colorProgress * 255).toInt()
            val color = Color.rgb(colorValue, colorValue, colorValue)

            binding.btnBack.setBackgroundResource(com.example.basetools.R.drawable.ic_icon)
            binding.btnMenu.setColorFilter(color)
        } else {
            binding.btnBack.setBackgroundResource(com.huantansheng.easyphotos.R.drawable.ic_arrow_back_easy_photos)
            binding.btnBack.setColorFilter(Color.WHITE)
            binding.btnMenu.setColorFilter(Color.WHITE)
        }
    }

    /**
     * 设置页面标题
     */
    fun setPageTitle(title: String) {
        binding.tvCollapsedTitle.text = title
        binding.collapsingToolbar.title = title
    }
private fun updateHeaderAlpha(progress: Float) {
    // 根据折叠进度调整头部透明度
    val alpha = 1f - progress
//    binding.headerLayout.alpha = alpha

    // 当完全折叠时隐藏某些元素
}

//    @Deprecated("Deprecated in Java")
//    override fun onBackPressed() {
//        super.onBackPressed()
//    // 如果头部是折叠状态，先展开
//    if (!binding.appBarLayout.isLifted) {
//        binding.appBarLayout.setExpanded(true, true)
//    } else {
//        super.onBackPressed()
//    }
//}


    override fun onPause() {
        super.onPause()
        // 页面切换是停止播放
    }







}

