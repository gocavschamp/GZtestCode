package com.example.firstapplication.ui.kids

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 幼儿教育模块沉浸式状态栏工具
 * - 内容延伸到状态栏/导航栏区域（配合 Theme.Kids 透明状态栏）
 * - 状态栏图标颜色：默认白色（适配深色渐变背景），可传 lightStatusBar=true 用深色图标（适配浅色背景）
 * - 根布局顶部自动避开状态栏，避免标题被遮挡
 */
object KidsStatusBar {

    fun immersive(activity: Activity, root: View, lightStatusBar: Boolean = false) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = lightStatusBar
            isAppearanceLightNavigationBars = false
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        // 顶部留出状态栏高度，避免内容被状态栏遮挡
        val baseTop = root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, baseTop + top, v.paddingRight, v.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    /** 动态切换状态栏图标颜色：true=深色图标（浅色背景），false=白色图标（深色背景） */
    fun setLightStatusBar(activity: Activity, light: Boolean) {
        WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            .isAppearanceLightStatusBars = light
    }
}
