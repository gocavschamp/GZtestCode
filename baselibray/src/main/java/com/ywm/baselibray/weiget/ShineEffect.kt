package com.ywm.baselibray.weiget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

// ShineEffect.kt
class ShineEffect(targetView: View) {
    // 使用弱引用避免内存泄漏
    private val targetViewRef: WeakReference<View> = WeakReference(targetView)
    
    // 获取目标View，如果已被回收则返回null
    private val targetView: View?
        get() = targetViewRef.get()
        
    private val shineDrawable = ShineDrawable()
    private var animator: ValueAnimator? = null
    private var isStarted = false
    private var shouldAutoStart = false
    private var isReleased = false
    
    // 添加生命周期状态标记
    private var isAttached = false
    
    // Context生命周期监听器
    private var lifecycleObserver: LifecycleEventObserver? = null
    private var lifecycle: Lifecycle? = null

    // 监听可见性变化
    private val onLayoutChangeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        updateDrawableBounds()
    }

    // 监听View的attach/detach状态
    private val onAttachStateChangeListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            if (shouldAutoStart) {
                startShine()
            }
        }

        override fun onViewDetachedFromWindow(v: View) {
            stopShine()
        }
    }

    init {
        setup()
        setupLifecycleObserver()
    }

    private fun setup() {
        // 设置Drawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            targetView?.foreground = shineDrawable
        } else {
            // API 23以下使用Overlay或background
            targetView?.background = CompoundDrawableWrapper(targetView?.background, shineDrawable)
        }

        // 监听布局变化以更新Drawable边界
        targetView?.addOnLayoutChangeListener(onLayoutChangeListener)

        // 监听attach/detach状态
        targetView?.addOnAttachStateChangeListener(onAttachStateChangeListener)

        // 初始更新边界
        updateDrawableBounds()
    }
    
    /**
     * 设置Context生命周期监听
     */
    private fun setupLifecycleObserver() {
        val target = targetView ?: return
        val context = target.context
        
        // 简化Lifecycle获取逻辑
        lifecycle = if (context is LifecycleOwner) {
            // 标准方式：Context是LifecycleOwner（如AppCompatActivity）
            context.lifecycle
        } else {
            // 其他情况返回null，使用备用方案
            null
        }
        
        lifecycleObserver = LifecycleEventObserver { source, event ->
            when (event) {
                Lifecycle.Event.ON_DESTROY -> {
                    // Context被销毁时自动释放资源
                    release()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // 暂停时停止动画
                    if (shouldAutoStart) {
                        stopShine()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    // 恢复时重新启动动画
                    if (shouldAutoStart) {
                        startShine()
                    }
                }
                else -> {
                    // 其他事件不处理
                }
            }
        }
        
        lifecycleObserver?.let { observer ->
            lifecycle?.addObserver(observer)
        }
        
        // 如果没有找到Lifecycle，添加一个备用的Context监听
        if (lifecycle == null) {
            setupFallbackContextMonitor(context)
        }
    }

    /**
     * 备用的Context监听方案
     */
    private fun setupFallbackContextMonitor(context: android.content.Context) {
        // 通过View的attach/detach状态来间接监听Context生命周期
        // 当View detached时，Context可能即将被销毁
        targetView?.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                // View重新附加到窗口
            }
            
            override fun onViewDetachedFromWindow(v: View) {
                // View从窗口分离，Context可能即将被销毁
                // 延迟一段时间后检查View是否仍然detached，如果是则释放资源
                v.postDelayed({
                    if (!v.isAttachedToWindow) {
                        release()
                    }
                }, 1000) // 延迟1秒检查
            }
        })
    }

    private fun updateDrawableBounds() {
        targetView?.let {
            if (it.width > 0 && it.height > 0) {
                shineDrawable.setBounds(0, 0, it.width, it.height)
            }

        }
    }

    fun startShine() {
        if (isReleased) return
        
        val target = targetView ?: return
        
        // RecyclerView适配：检查View是否可见且已布局
        if (isStarted || target.width == 0 || target.visibility != View.VISIBLE || !target.isAttachedToWindow) {
            // 如果条件不满足，延迟重试（适用于RecyclerView的延迟布局）
            if (!isStarted && target.isAttachedToWindow) {
                target.postDelayed({
                    if (!isStarted && !isReleased) {
                        startShine()
                    }
                }, 50)
            }
            return
        }

        animator?.cancel()
        isStarted = true

        // 创建扫光动画
        animator = ObjectAnimator.ofFloat(
            -target.width * 0.3f,
            target.width * 1.3f
        ).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                if (isReleased) {
                    cancel()
                    return@addUpdateListener
                }
                val currentTarget = targetView
                if (currentTarget != null && currentTarget.width > 0) {
                    val value = animation.animatedValue as Float
                    shineDrawable.setOffset(value, currentTarget.height / 2f)
                    // RecyclerView适配：确保Drawable边界正确
                    updateDrawableBounds()
                } else {
                    cancel()
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (isStarted && !isReleased) {
                        // 循环动画
                        start()
                    }
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    // 动画被取消时清理资源
                    cleanupAnimation()
                }
                
                override fun onAnimationStart(animation: Animator) {
                    // RecyclerView适配：动画开始时确保Drawable边界正确
                    updateDrawableBounds()
                }
            })
            start()
        }
    }

    fun stopShine() {
        isStarted = false
        animator?.cancel()
        animator = null
        // 重置位置
        targetView?:return
        shineDrawable.setOffset(-targetView!!.width * 0.3f, targetView!!.height / 2f)
        targetView?.invalidate()
    }

    /**
     * 设置是否自动启动（根据View的可见性）
     */
    fun setAutoStart(autoStart: Boolean) {
        if (isReleased) return
        
        this.shouldAutoStart = autoStart

        if (autoStart) {
            // 检查当前状态
            val target = targetView
            if (target != null && 
                target.isAttachedToWindow &&
                target.visibility == View.VISIBLE) {
                
                // RecyclerView适配：如果宽度为0，延迟启动
                if (target.width > 0) {
                    startShine()
                } else {
                    target.postDelayed({
                        if (shouldAutoStart && !isReleased) {
                            startShine()
                        }
                    }, 100)
                }
            }
        } else {
            stopShine()
        }
    }
    
    /**
     * RecyclerView专用：当Item进入屏幕时调用
     */
    fun onItemVisible() {
        if (isReleased) return
        
        if (shouldAutoStart) {
            val target = targetView
            if (target != null && target.visibility == View.VISIBLE) {
                // 立即启动或延迟启动
                if (target.width > 0 && target.isLaidOut) {
                    startShine()
                } else {
                    target.post {
                        if (shouldAutoStart && !isReleased) {
                            startShine()
                        }
                    }
                }
            }
        }
    }
    
    /**
     * RecyclerView专用：当Item离开屏幕时调用
     */
    fun onItemInvisible() {
        if (isReleased) return
        
        // 停止动画但不释放资源，因为View可能被复用
        stopShine()
    }
    
    /**
     * 清理动画资源
     */
    private fun cleanupAnimation() {
        animator?.removeAllUpdateListeners()
        animator?.removeAllListeners()
        animator = null
    }

    /**
     * 处理可见性变化
     */
    fun onVisibilityChanged(isVisible: Boolean) {
        if (shouldAutoStart) {
            if (isVisible) {
                startShine()
            } else {
                stopShine()
            }
        }
    }

    /**
     * 清理资源
     */
    fun release() {
        stopShine()
        targetView?.removeOnLayoutChangeListener(onLayoutChangeListener)
        targetView?.removeOnAttachStateChangeListener(onAttachStateChangeListener)
        
        // 清理Drawable引用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (targetView?.foreground === shineDrawable) {
                targetView?.foreground = null
            }
        } else {
            if (targetView?.background is CompoundDrawableWrapper) {
                targetView?.background = null
            }
        }
    }
}

// 用于API 23以下的Drawable包装器
class CompoundDrawableWrapper(
    private val original: Drawable?,
    private val overlay: Drawable
) : Drawable() {

    override fun draw(canvas: Canvas) {
        original?.draw(canvas)
        overlay.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        original?.alpha = alpha
        overlay.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        original?.colorFilter = colorFilter
        overlay.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return original?.intrinsicWidth ?: overlay.intrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        return original?.intrinsicHeight ?: overlay.intrinsicHeight
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        original?.setBounds(left, top, right, bottom)
        overlay.setBounds(left, top, right, bottom)
    }
}