/*
 * Tencent is pleased to support the open source community by making vap available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.qgame.animplayer

import android.os.Build
import android.os.HandlerThread
import android.os.Handler
import com.tencent.qgame.animplayer.file.IFileContainer
import com.tencent.qgame.animplayer.inter.IAnimListener
import com.tencent.qgame.animplayer.util.ALog
import com.tencent.qgame.animplayer.util.SpeedControlUtil


abstract class Decoder(val player: AnimPlayer) : IAnimListener {

    companion object {
        private const val TAG = "${Constant.TAG}.Decoder"

        fun createThread(handlerHolder: HandlerHolder, name: String): Boolean {
            try {
                if (handlerHolder.thread == null || handlerHolder.thread?.isAlive == false) {
                    handlerHolder.thread = HandlerThread(name).apply {
                        start()
                        handlerHolder.handler = Handler(looper)
                    }
                }
                return true
            } catch (e: OutOfMemoryError) {
                ALog.e(TAG, "createThread OOM", e)
            }
            return false
        }

        fun quitSafely(thread: HandlerThread?): HandlerThread? {
            thread?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    thread.quitSafely()
                } else {
                    thread.quit()
                }
            }
            return null
        }
    }

    var render: IRenderListener? = null
    val renderThread = HandlerHolder(null, null)
    val decodeThread = HandlerHolder(null, null)
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    var fps: Int = 0
        set(value) {
            speedControlUtil.setFixedPlaybackRate(value)
            field = value
        }
    var playLoop = 0 // 循环播放次数
    var isRunning = false // 是否正在运行
    var isStopReq = false // 是否需要停止
    val speedControlUtil by lazy { SpeedControlUtil() }

    abstract fun start(fileContainer: IFileContainer)

    fun stop() {
        isStopReq = true
    }

    abstract fun destroy()

    fun prepareThread(): Boolean {
        return createThread(renderThread, "anim_render_thread") && createThread(decodeThread, "anim_decode_thread")
    }

    fun prepareRender(needYUV: Boolean): Boolean {
        if (render == null) {
            ALog.i(TAG, "prepareRender")
            player.animView.getSurfaceTexture()?.apply {
                if (needYUV) {
                    ALog.i(TAG, "use yuv render")
                    render = YUVRender(this)
                } else {
                    render = Render(this).apply {
                        updateViewPort(surfaceWidth, surfaceHeight)
                    }
                }
            }
        }
        return render != null
    }

    fun preparePlay(videoWidth: Int, videoHeight: Int) {
        // 尝试获取配置，如果配置解析失败，则创建默认配置
        if (player.configManager.config == null) {
            ALog.w(TAG, "Config parse failed, create default config for normal MP4")
            player.configManager.config = createDefaultConfig(videoWidth, videoHeight)
        }

        player.configManager.config?.apply {
            render?.setAnimConfig(this)
        }
        player.pluginManager.onRenderCreate()
//        player.configManager.defaultConfig(videoWidth, videoHeight)
//        player.configManager.config?.apply {
//            render?.setAnimConfig(this)
//        }
//        player.pluginManager.onRenderCreate()
    }
    protected fun createDefaultConfig(videoWidth: Int, videoHeight: Int): AnimConfig {
        val config = AnimConfig().apply {
            this.videoWidth = videoWidth
            this.videoHeight = videoHeight
            this.width = videoWidth
            this.height = videoHeight
            this.fps = this@Decoder.fps
            this.hasAlpha = false  // 普通 MP4 没有 Alpha 通道
            this.needBlend = false // 不需要融合动画

            // 设置默认的矩形区域（整个画面）
            alphaPointRect = PointRect(0, 0, videoWidth, videoHeight)
            rgbPointRect = PointRect(0, 0, videoWidth, videoHeight)

            // 如果是老版本兼容模式，根据 videoMode 调整
            if (player.enableVersion1) {
                when (player.videoMode) {
                    Constant.VIDEO_MODE_SPLIT_HORIZONTAL -> {
                        // 左右分离：alpha 左，rgb 右
                        alphaPointRect = PointRect(0, 0, videoWidth / 2, videoHeight)
                        rgbPointRect = PointRect(videoWidth / 2, 0, videoWidth / 2, videoHeight)
                    }
                    Constant.VIDEO_MODE_SPLIT_VERTICAL -> {
                        // 上下分离：alpha 上，rgb 下
                        alphaPointRect = PointRect(0, 0, videoWidth, videoHeight / 2)
                        rgbPointRect = PointRect(0, videoHeight / 2, videoWidth, videoHeight / 2)
                    }
                    Constant.VIDEO_MODE_SPLIT_HORIZONTAL_REVERSE -> {
                        // 左右分离：rgb 左，alpha 右
                        rgbPointRect = PointRect(0, 0, videoWidth / 2, videoHeight)
                        alphaPointRect = PointRect(videoWidth / 2, 0, videoWidth / 2, videoHeight)
                    }
                    Constant.VIDEO_MODE_SPLIT_VERTICAL_REVERSE -> {
                        // 上下分离：rgb 上，alpha 下
                        rgbPointRect = PointRect(0, 0, videoWidth, videoHeight / 2)
                        alphaPointRect = PointRect(0, videoHeight / 2, videoWidth, videoHeight / 2)
                    }
                    // 其他情况使用默认设置
                }
            }
        }
        return config
    }

//    fun preparePlay(videoWidth: Int, videoHeight: Int) {
//        // 尝试获取配置，如果配置解析失败，则创建默认配置
//        if (player.configManager.config == null) {
//            ALog.w(TAG, "Config parse failed, create default config for normal MP4")
//            player.configManager.config = createDefaultConfig(videoWidth, videoHeight)
//        }
//
//        player.configManager.config?.apply {
//            render?.setAnimConfig(this)
//        }
//        player.pluginManager.onRenderCreate()
//    }
    /**
     * decode过程中视频尺寸变化
     * 主要是没有16进制对齐的老视频
     */
    fun videoSizeChange(newWidth: Int, newHeight: Int) {
        if (newWidth <= 0 || newHeight <= 0) return
        val config = player.configManager.config ?: return
        if (config.videoWidth != newWidth || config.videoHeight != newHeight) {
            ALog.i(TAG, "videoSizeChange old=(${config.videoWidth},${config.videoHeight}), new=($newWidth,$newHeight)")
            config.videoWidth = newWidth
            config.videoHeight = newHeight
            render?.setAnimConfig(config)
        }
    }


    fun destroyThread() {
        if (player.isDetachedFromWindow) {
            ALog.i(TAG, "destroyThread")
            renderThread.handler?.removeCallbacksAndMessages(null)
            decodeThread.handler?.removeCallbacksAndMessages(null)
            renderThread.thread = quitSafely(renderThread.thread)
            decodeThread.thread = quitSafely(decodeThread.thread)
            renderThread.handler = null
            decodeThread.handler = null
        }
    }

    fun onSurfaceSizeChanged(width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        render?.updateViewPort(width, height)
    }

    override fun onVideoStart() {
        ALog.i(TAG, "onVideoStart")
        player.animListener?.onVideoStart()
    }

    override fun onVideoRender(frameIndex: Int, config: AnimConfig?) {
        ALog.d(TAG, "onVideoRender")
        player.animListener?.onVideoRender(frameIndex, config)
    }

    override fun onVideoComplete() {
        ALog.i(TAG, "onVideoComplete")
        player.animListener?.onVideoComplete()
    }

    override fun onVideoDestroy() {
        ALog.i(TAG, "onVideoDestroy")
        player.animListener?.onVideoDestroy()
    }

    override fun onFailed(errorType: Int, errorMsg: String?) {
        ALog.e(TAG, "onFailed errorType=$errorType, errorMsg=$errorMsg")
        player.animListener?.onFailed(errorType, errorMsg)
    }
}

data class HandlerHolder(var thread: HandlerThread?, var handler: Handler?)