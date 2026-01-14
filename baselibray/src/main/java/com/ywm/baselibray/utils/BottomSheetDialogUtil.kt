package com.ywm.baselibray.utils

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ywm.baselibray.R
import com.ywm.baselibray.utils.BottomSheetDialogUtil.showBottomSheet

/**
 * BottomSheetDialog工具类
 * 封装从底部弹出对话框的常用功能
 */
object BottomSheetDialogUtil {

    /**
     * 显示底部弹窗
     *
     * @param activity Activity上下文
     * @param contentView 弹窗内容View
     * @param cancelable 是否点击外部可关闭
     * @param fullScreen 是否全屏显示（高度全屏）
     * @param onDismiss 弹窗关闭回调
     * @return BottomSheetDialog实例
     */
    fun showBottomSheet(
        activity: Activity,
        contentView: View,
        cancelable: Boolean = true,
        fullScreen: Boolean = false,
        onDismiss: (() -> Unit)? = null
    ): BottomSheetDialog {
        return showBottomSheet(
            activity = activity,
            contentView = contentView,
            cancelable = cancelable,
            cancelableOnTouchOutside = cancelable,
            fullScreen = fullScreen,
            onDismiss = onDismiss
        )
    }

    /**
     * 显示底部弹窗（更详细的参数控制）
     *
     * @param activity Activity上下文
     * @param contentView 弹窗内容View
     * @param cancelable 是否可取消（按返回键）
     * @param cancelableOnTouchOutside 是否点击外部可关闭
     * @param fullScreen 是否全屏显示（高度全屏）
     * @param peekHeight 初始展开高度，0表示包裹内容
     * @param expandedHeight 展开高度（像素），-1表示全屏，0表示包裹内容
     * @param skipCollapsed 是否跳过折叠状态（直接展开或关闭）
     * @param onDismiss 弹窗关闭回调
     * @return BottomSheetDialog实例
     */
    fun showBottomSheet(
        activity: Activity,
        contentView: View,
        cancelable: Boolean = true,
        cancelableOnTouchOutside: Boolean = true,
        fullScreen: Boolean = false,
        cornerRadius: Float = 20.dp.toFloat(),
        backgroundColor: Int = Color.WHITE,
        peekHeight: Int = 0,
        expandedHeight: Int = 0,
        skipCollapsed: Boolean = false,
        onDismiss: (() -> Unit)? = null
    ): BottomSheetDialog {
        // 创建BottomSheetDialog
        val dialog = object : BottomSheetDialog(activity) {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                // 设置圆角背景，避免白边
                window?.apply {
                    // 设置背景透明
                    setBackgroundDrawableResource(android.R.color.transparent)
                    // 设置导航栏透明
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        navigationBarColor = android.graphics.Color.TRANSPARENT
                    }

                    // 设置状态栏透明（可选）
                    // addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                }
            }
        }

        // 设置内容视图
        dialog.setContentView(contentView)
// 设置圆角背景
        applyCornerRadius(dialog, cornerRadius, backgroundColor, fullScreen)
        // 设置对话框属性
        dialog.setCancelable(cancelable)
        dialog.setCanceledOnTouchOutside(cancelableOnTouchOutside)

        // 设置窗口属性
        dialog.window?.apply {
            // 设置从底部弹出
            setGravity(Gravity.BOTTOM)

            // 设置布局参数
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                if (fullScreen) ViewGroup.LayoutParams.MATCH_PARENT
                else ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }


        // 设置关闭监听
        dialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        // 显示对话框
        dialog.show()

        return dialog
    }
    /**
     * 应用圆角背景
     */
    private fun applyCornerRadius(
        dialog: BottomSheetDialog,
        radius: Float,
        backgroundColor: Int,
        fullScreen: Boolean
    ) {
        // 延迟执行，确保布局已完成
        dialog.window?.decorView?.post {
            try {
                // 获取BottomSheet的父容器
                val parentView = dialog.window?.decorView?.findViewById<View>(android.R.id.content)

                // 查找BottomSheet视图
                val bottomSheetView = findBottomSheetView(dialog)

                bottomSheetView?.let { sheet ->
                    // 创建圆角背景
                    val background = GradientDrawable().apply {
                        setColor(backgroundColor)

                        // 设置圆角：只有顶部有圆角
                        if (fullScreen) {
                            // 全屏时四角都圆角
                            cornerRadii = floatArrayOf(
                                radius, radius, radius, radius,
                                radius, radius, radius, radius
                            )
                        } else {
                            // 非全屏时只有顶部圆角
                            cornerRadii = floatArrayOf(
                                radius, radius,  // 左上角
                                radius, radius,  // 右上角
                                0f, 0f,          // 右下角
                                0f, 0f           // 左下角
                            )
                        }
                    }

                    // 设置背景
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        sheet.background = background
                    } else {
                        @Suppress("DEPRECATION")
                        sheet.setBackgroundDrawable(background)
                    }

                    // 清除默认的背景
                    sheet.setPadding(0, 0, 0, 0)

                    // 获取Behavior并设置
                    val behavior = BottomSheetBehavior.from(sheet)

                    // 设置初始状态
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED

                    // 设置最大高度（如果需要）
                    if (!fullScreen) {
                        val layoutParams = sheet.layoutParams as? CoordinatorLayout.LayoutParams
                        layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        sheet.layoutParams = layoutParams
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 备用方案：为内容视图设置圆角
                applyFallbackCornerRadius(dialog, radius, backgroundColor)
            }
        }
    }
    /**
     * 备用方案：为内容设置圆角
     */
    private fun applyFallbackCornerRadius(
        dialog: BottomSheetDialog,
        radius: Float,
        backgroundColor: Int
    ) {
        try {
            // 获取内容视图
            val contentContainer = dialog.findViewById<ViewGroup>(com.google.android.material.R.id.container)
                ?: dialog.findViewById<ViewGroup>(android.R.id.content)

            contentContainer?.let { container ->
                // 获取实际内容视图
                val contentView = if (container.childCount > 0) {
                    container.getChildAt(0)
                } else {
                    container
                }

                // 为内容视图设置圆角
                val background = GradientDrawable().apply {
                    setColor(backgroundColor)
                    cornerRadii = floatArrayOf(
                        radius, radius,  // 左上角
                        radius, radius,  // 右上角
                        0f, 0f,          // 右下角
                        0f, 0f           // 左下角
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    contentView.background = background
                } else {
                    @Suppress("DEPRECATION")
                    contentView.setBackgroundDrawable(background)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

    /**
     * 查找BottomSheet视图
     */
    private fun findBottomSheetView(dialog: BottomSheetDialog): View? {
        return try {
            // 方法1：通过Window的decorView查找
            val decorView = dialog.window?.decorView
            if (decorView is ViewGroup) {
                findBottomSheetViewRecursive(decorView)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 递归查找BottomSheet视图
     */
    private fun findBottomSheetViewRecursive(viewGroup: ViewGroup): View? {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            // 判断是否是BottomSheet的容器
            if (child is FrameLayout && child.layoutParams is CoordinatorLayout.LayoutParams) {
                return child
            }

            if (child is ViewGroup) {
                val found = findBottomSheetViewRecursive(child)
                if (found != null) return found
            }
        }
        return null
    }
    /**
     * 显示带有圆角的底部弹窗
     *
     * @param activity Activity上下文
     * @param contentView 弹窗内容View
     * @param radius 圆角半径（像素）
     * @param cancelable 是否点击外部可关闭
     * @return BottomSheetDialog实例
     */
    fun showBottomSheetWithRadius(
        activity: Activity,
        contentView: View,
        radius: Float = 16f,
        cancelable: Boolean = true
    ): BottomSheetDialog {
        return showBottomSheet(
            activity = activity,
            contentView = contentView,
            cancelable = cancelable
        )
    }

    /**
     * 显示模态底部弹窗（不可通过点击外部关闭）
     *
     * @param activity Activity上下文
     * @param contentView 弹窗内容View
     * @param fullScreen 是否全屏显示
     * @return BottomSheetDialog实例
     */
    fun showModalBottomSheet(
        activity: Activity,
        contentView: View,
        fullScreen: Boolean = false
    ): BottomSheetDialog {
        return showBottomSheet(
            activity = activity,
            contentView = contentView,
            cancelable = false,
            cancelableOnTouchOutside = false,
            fullScreen = fullScreen,
            skipCollapsed = true
        )
    }

    /**
     * 显示固定高度的底部弹窗
     *
     * @param activity Activity上下文
     * @param contentView 弹窗内容View
     * @param height 弹窗高度（像素）
     * @param cancelable 是否点击外部可关闭
     * @return BottomSheetDialog实例
     */
    fun showFixedHeightBottomSheet(
        activity: Activity,
        contentView: View,
        height: Int,
        cancelable: Boolean = true
    ): BottomSheetDialog {
        return showBottomSheet(
            activity = activity,
            contentView = contentView,
            cancelable = cancelable,
            expandedHeight = height
        )
    }

    /**
     * 显示百分比高度的底部弹窗
     *
     * @param activity Activity上下文
     * @param contentView 弹窗内容View
     * @param percentage 屏幕高度的百分比（0.0-1.0）
     * @param cancelable 是否点击外部可关闭
     * @return BottomSheetDialog实例
     */
    fun showPercentageHeightBottomSheet(
        activity: Activity,
        contentView: View,
        percentage: Float,
        cancelable: Boolean = true
    ): BottomSheetDialog {
        val displayMetrics = activity.resources.displayMetrics
        val height = (displayMetrics.heightPixels * percentage).toInt()

        return showFixedHeightBottomSheet(
            activity = activity,
            contentView = contentView,
            height = height,
            cancelable = cancelable
        )
    }

/**
 * 使用扩展函数方式，直接在Activity上调用
 */
fun Activity.showBottomSheet(
    contentView: View,
    cancelable: Boolean = true,
    cancelableOnTouchOutside: Boolean = true,
    fullScreen: Boolean = false,
    onDismiss: (() -> Unit)? = null
): BottomSheetDialog {
    return BottomSheetDialogUtil.showBottomSheet(
        activity = this,
        contentView = contentView,
        cancelable = cancelable,
        cancelableOnTouchOutside = cancelableOnTouchOutside,
        fullScreen = fullScreen,
        onDismiss = onDismiss
    )
}

/**
 * 使用扩展函数方式，直接在View上调用
 */
fun View.showAsBottomSheet(
    cancelable: Boolean = true,
    cancelableOnTouchOutside: Boolean = true,
    fullScreen: Boolean = false,
    onDismiss: (() -> Unit)? = null
): BottomSheetDialog? {
    val activity = context as? AppCompatActivity ?: return null

    return BottomSheetDialogUtil.showBottomSheet(
        activity = activity,
        contentView = this,
        cancelable = cancelable,
        cancelableOnTouchOutside = cancelableOnTouchOutside,
        fullScreen = fullScreen,
        onDismiss = onDismiss
    )
}