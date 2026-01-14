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
import com.example.firstapplication.ui.fragment.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ywm.baselibray.utils.BottomSheetDialogUtil
import com.ywm.baselibray.utils.showAsBottomSheet
import com.ywm.baselibray.utils.showBottomSheet
import kotlin.math.max
import kotlin.math.min

/**
 * 简单使用demo
 */
class DialogDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AnimSimpleDemoActivity"
    }

    private lateinit var binding: ActivityDialogBinding
    // 视频信息


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置全屏透明状态栏
        binding = ActivityDialogBinding.inflate(layoutInflater)
        val root = binding.root
        setContentView(root)
        // 文件加载完成后会调用init方法
        init()

    }

    private fun init() {
        // 初始化日志
        // 设置状态栏颜色
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_700)
        binding.picker.setOnClickListener {
// 方式1：使用工具类
            val contentView = LayoutInflater.from(this).inflate(R.layout.item_message, null)

            BottomSheetDialogUtil.showBottomSheet(
                activity = this,
                contentView = contentView,
                cancelable = true,  // 可点击外部关闭
                fullScreen = false,
                onDismiss = {
                    Toast.makeText(this, "弹窗关闭", Toast.LENGTH_SHORT).show()
                }
            )
        }
        binding.picker1.setOnClickListener {
// 方式2：使用Activity扩展函数
            val contentView = LayoutInflater.from(this).inflate(R.layout.item_message, null)

            showBottomSheet(
                contentView = contentView,
                cancelable = true,
                onDismiss = {
                    // 关闭回调
                }
            )

        }
        binding.picker2.setOnClickListener {
// 方式3：使用View扩展函数
            val contentView = LayoutInflater.from(this).inflate(R.layout.item_message, null)

            contentView.showAsBottomSheet(
                cancelable = true,
                fullScreen = false
            )
        }

    }



    override fun onPause() {
        super.onPause()
        // 页面切换是停止播放
    }







}

