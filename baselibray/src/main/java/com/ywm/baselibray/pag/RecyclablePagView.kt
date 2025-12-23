package com.ywm.baselibray.pag

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.ywm.baselibray.pag.PagPlayerManager
import org.libpag.PAGView

class RecyclablePagView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PAGView(context, attrs, defStyleAttr), LifecycleEventObserver {

    private var pagUrl: String? = null
    private var isAutoPlay = true
    private var repeatCount = -1
    private var isViewAttached = false
    private var isReleased = false

    private val playerManager by lazy { PagPlayerManager.getInstance() }

    init {
        // 设置监听器来检测视图的可见性
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                isViewAttached = true
                isReleased = false
                if (isAutoPlay && pagUrl != null && !isReleased) {
                    resumeAnimation()
                }
            }

            override fun onViewDetachedFromWindow(v: View) {
                isViewAttached = false
                if (!isReleased) {
                    pauseAnimation()
                }
            }
        })
    }

    /**
     * 设置 PAG 动画 URL
     */
    fun setPagUrl(
        url: String?,
        autoPlay: Boolean = true,
        repeatCount: Int = -1,
        listener: PagPlayerManager.PagLoadListener? = null
    ) {
        // 如果已经释放，先重置状态
        if (isReleased) {
            isReleased = false
        }

        // 如果 URL 相同且正在播放，则不做处理
        if (this.pagUrl == url && composition != null && isPlaying) {
            listener?.onSuccess()
            return
        }

        // 释放之前的资源
        if (this.pagUrl != null && this.pagUrl != url) {
            playerManager.releasePagView(this, this.pagUrl)
        }

        this.pagUrl = url
        this.isAutoPlay = autoPlay
        this.repeatCount = repeatCount

        if (url.isNullOrEmpty()) {
            playerManager.releasePagView(this)
            listener?.onError("URL is null or empty")
            return
        }

        playerManager.loadPagAnimation(url, this, autoPlay, repeatCount, object : PagPlayerManager.PagLoadListener {
            override fun onLoading() {
                listener?.onLoading()
            }

            override fun onSuccess() {
                listener?.onSuccess()
            }

            override fun onError(error: String) {
                listener?.onError(error)
            }
        })
    }

    /**
     * 暂停播放 - 重命名为 pauseAnimation 避免与父类冲突
     */
     fun pauseAnimation() {
        if (!isReleased) {
            playerManager.pause(this)
        }
    }

    /**
     * 恢复播放 - 重命名为 resumeAnimation 避免与父类冲突
     */
     fun resumeAnimation() {
        if (isViewAttached && pagUrl != null && !isReleased) {
            playerManager.resume(this)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        if (!isReleased) {
            playerManager.releasePagView(this, pagUrl)
            pagUrl = null
            isReleased = true
        }
    }

    /**
     * 获取当前 URL
     */
    fun getPagUrl(): String? = pagUrl

    /**
     * 是否已释放
     */
    fun isReleased(): Boolean = isReleased

    /**
     * 绑定生命周期
     */
    fun bindLifecycle(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (isViewAttached && !isReleased) {
                    resumeAnimation()
                }
            }
            Lifecycle.Event.ON_PAUSE -> {
                if (!isReleased) {
                    pauseAnimation()
                }
            }
            Lifecycle.Event.ON_DESTROY -> {
                release()
                source.lifecycle.removeObserver(this)
            }
            else -> {}
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 不立即释放资源，等待 RecyclerView 复用
        if (!isReleased) {
            pauseAnimation()
        }
    }

    /**
     * 重写父类的 pause 方法，确保我们的逻辑也被执行
     */
    override fun pause() {
        super.pause()
        pauseAnimation()
    }

    /**
     * 开始播放 - 重写父类方法，确保我们的逻辑也被执行
     */
    override fun play() {
        super.play()
        // 可以在这里添加自定义逻辑
    }
//    override fun isPlaying(): Boolean {
//        return composition != null && isPlaying
//    }
}