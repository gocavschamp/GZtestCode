package com.ywm.baselibray.floatview

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.ViewGroup
import java.lang.ref.WeakReference
import java.util.LinkedList
import java.util.Queue

// 在自定义Application或初始化类中
// GlobalNoticeManager.kt
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

object GlobalNoticeManager : Application.ActivityLifecycleCallbacks {

    // 消息队列（线程安全）
    private val messageQueue: Queue<NoticeMessage> = LinkedList()
    // 原子布尔值，标记当前是否有通知正在显示
    private val isShowing = AtomicBoolean(false)
    // 主线程Handler，用于在UI线程执行操作
    private val mainHandler = Handler(Looper.getMainLooper())
    // 当前正在显示的悬浮窗视图
    private var currentFloatingView: FloatingNoticeView? = null
    // 对当前Activity的弱引用，防止内存泄漏
    private var currentActivityRef: java.lang.ref.WeakReference<Activity>? = null

    /**
     * 初始化方法，需要在Application的onCreate中调用
     */
    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * 供外部调用的接口，添加一条新通知（线程安全）
     */
    @Synchronized // 使用同步锁确保添加消息的原子性[6,8](@ref)
    fun addNotice(message: NoticeMessage) {
        Log.d("PollingDemo", "float manager add not 通知: ${message}")

        messageQueue.offer(message)
        showNextIfPossible()
    }

    /**
     * 尝试显示下一条通知（内部方法）
     */
    private fun showNextIfPossible() {
        // 如果正在显示或有消息才处理，通过CAS操作避免竞态条件[7](@ref)
        Log.d("PollingDemo", "float manager showNextIfPossible 通知: ${isShowing}")

        if (!isShowing.compareAndSet(false, true)) {
            Log.d("GlobalNoticeManager", "Could not set isShowing to true, it was already true.")
            return
        }

        val message = messageQueue.poll()
        if (message == null) {
            // 队列为空，重置状态
            isShowing.set(false)
            return
        }

        // 切换到主线程执行UI操作
        mainHandler.post {
            currentActivityRef?.get()?.let { activity ->
                showInActivity(activity, message)
            } ?: run {
                // 没有活动的Activity，将消息重新放回队列并重置状态
                messageQueue.offer(message)
                isShowing.set(false)
            }
        }
    }

    /**
     * 在指定的Activity中显示通知
     */
    private fun showInActivity(activity: Activity, message: NoticeMessage) {
        Log.d("PollingDemo", "float manager showInActivity 通知: ${activity::class.simpleName}")

        // 创建或复用悬浮窗视图
        val floatingView = currentFloatingView ?: FloatingNoticeView(activity).also {
            currentFloatingView = it
        }

        // 找到Activity的内容根布局
        val contentParent = activity.window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content)
        // 如果视图还未被添加到父布局，则添加它
        if (floatingView.parent == null) {
            contentParent.addView(floatingView)
        }

        // 设置消息并开始显示动画
        floatingView.setNoticeMessage(message)
        floatingView.showWithAutoDismiss(2000L) { // 停留3秒
            // 通知消失后的回调

            onNoticeDismissed()
        }
    }

    /**
     * 当一条通知完全消失后调用
     */
    private fun onNoticeDismissed() {
        Log.d("PollingDemo", "float manager onNoticeDismissed 通知: ${isShowing}")

        // 移除视图引用
//        currentFloatingView = null
        // 重置显示状态
        isShowing.set(false)
        // 尝试播放下一条消息
        showNextIfPossible()
    }

    // region ActivityLifecycleCallbacks 实现
    override fun onActivityStarted(activity: Activity) {
        // 更新当前Activity的弱引用
        currentActivityRef = java.lang.ref.WeakReference(activity)
        // 当新Activity启动时，尝试显示等待中的消息
        if (messageQueue.isNotEmpty()) {
            showNextIfPossible()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        // 当Activity停止时，移除其中的悬浮窗，但保留FloatingNoticeView实例以供复用
        currentFloatingView?.let { view ->
            if (view.parent != null) {
                (view.parent as? android.view.ViewGroup)?.removeView(view)
            }
        }
        // 如果停止的正是当前Activity，则清空引用
        if (currentActivityRef?.get() == activity) {
            currentActivityRef = null
        }
    }

    // 其他生命周期方法空实现
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
    // endregion
}