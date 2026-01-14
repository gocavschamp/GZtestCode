package com.ywm.baselibray.utils

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

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
        ).apply {
        }
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