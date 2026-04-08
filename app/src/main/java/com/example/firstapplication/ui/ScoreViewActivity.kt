package com.example.firstapplication.ui

import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import com.example.firstapplication.R
import com.example.firstapplication.databinding.ActivityDialogBinding
import com.example.firstapplication.databinding.ActivityScaleBinding
import com.example.firstapplication.databinding.ActivityScoreViewBinding
import com.example.firstapplication.ui.fragment.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ywm.baselibray.utils.BottomSheetDialogUtil
import com.ywm.baselibray.utils.showAsBottomSheet
import com.ywm.baselibray.utils.showBottomSheet
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * 简单使用demo
 */
class ScoreViewActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AnimSimpleDemoActivity"
    }

    private lateinit var binding: ActivityScoreViewBinding
    // 视频信息


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置全屏透明状态栏
        binding = ActivityScoreViewBinding.inflate(layoutInflater)
        val root = binding.root
        setContentView(root)
        // 文件加载完成后会调用init方法
        init()

    }

    private fun init() {
        binding.scoreview.setScore(976854321)
        // 初始化日志
        // 设置状态栏颜色
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_700)
        binding.picker.setOnClickListener {
// 方式1：使用工具类
        }
        binding.picker1.setOnClickListener {
// 方式2：使用Activity扩展函数

        }
        binding.picker2.setOnClickListener {
// 方式3：使用View扩展函数
        }
        simulateScoreChange()
    }
    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        binding.scoreview.cleanup()
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
    }
    private fun simulateScoreChange() {
        // 3秒后更新积分
        Handler(Looper.getMainLooper()).postDelayed({
            binding.scoreview.setScore(Random.nextInt(100000, 1000000))
            simulateScoreChange()
        }, 2000)
    }



    override fun onPause() {
        super.onPause()
        // 页面切换是停止播放
    }







}

