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

import android.media.MediaExtractor
import android.media.MediaFormat
import com.tencent.qgame.animplayer.file.FileContainer
import com.tencent.qgame.animplayer.file.IFileContainer
import com.tencent.qgame.animplayer.inter.IAnimListener
import com.tencent.qgame.animplayer.mask.MaskConfig
import com.tencent.qgame.animplayer.plugin.AnimPluginManager
import com.tencent.qgame.animplayer.util.ALog

class AnimPlayer(val animView: IAnimView) {

    companion object {
        private const val TAG = "${Constant.TAG}.AnimPlayer"
    }

    var animListener: IAnimListener? = null
    var decoder: Decoder? = null
    var audioPlayer: AudioPlayer? = null
    var fps: Int = 0
        set(value) {
            decoder?.fps = value
            field = value
        }
    // 设置默认的fps <= 0 表示以vapc配置为准 > 0  表示以此设置为准
    var defaultFps: Int = 0
    var playLoop: Int = 0
        set(value) {
            decoder?.playLoop = value
            audioPlayer?.playLoop = value
            field = value
        }
    var supportMaskBoolean : Boolean = false
    var maskEdgeBlurBoolean : Boolean = false
    // 是否兼容老版本 默认不兼容
    var enableVersion1 : Boolean = false
    // 视频模式
    var videoMode: Int = Constant.VIDEO_MODE_SPLIT_HORIZONTAL
    var isDetachedFromWindow = false
    var isSurfaceAvailable = false
    var startRunnable: Runnable? = null
    var isStartRunning = false // 启动时运行状态
    var isMute = false // 是否静音

    val configManager = AnimConfigManager(this)
    val pluginManager = AnimPluginManager(this)
    // 添加新属性：是否自动检测VAP格式
    var autoDetectVapFormat: Boolean = true
    fun onSurfaceTextureDestroyed() {
        isSurfaceAvailable = false
        isStartRunning = false
        decoder?.destroy()
        audioPlayer?.destroy()
    }

    fun onSurfaceTextureAvailable(width: Int, height: Int) {
        isSurfaceAvailable = true
        startRunnable?.run()
        startRunnable = null
    }


    fun onSurfaceTextureSizeChanged(width: Int, height: Int) {
        decoder?.onSurfaceSizeChanged(width, height)
    }

    fun startPlay(fileContainer: IFileContainer) {
        isStartRunning = true
        prepareDecoder()
        if (decoder?.prepareThread() == false) {
            isStartRunning = false
            decoder?.onFailed(Constant.REPORT_ERROR_TYPE_CREATE_THREAD, Constant.ERROR_MSG_CREATE_THREAD)
            decoder?.onVideoComplete()
            return
        }

        // 在线程中解析配置
        decoder?.renderThread?.handler?.post {
            val result = configManager.parseConfig(fileContainer, enableVersion1, videoMode, defaultFps)
            ALog.i(TAG, "parseConfig result=$result, enableVersion1=$enableVersion1")

            if (result != Constant.OK) {
                // 配置解析失败
                ALog.w(TAG, "Config parse failed (code=$result)")

                // 创建默认配置（尺寸未知，使用占位值）
                // 实际的视频尺寸会在Decoder.preparePlay中更新
                val config = AnimConfig().apply {
                    isDefaultConfig = true
                    // 关键：根据enableVersion1决定视频格式
                    if (enableVersion1) {
                        // 启用VAP模式：当作VAP格式处理
                        videoFormat = AnimConfig.FORMAT_VAP
                        hasAlpha = true
                        ALog.i(TAG, "enableVersion1=true, treating as VAP format")
                    } else {
                        // 普通MP4模式
                        createForNormalMP4(1, 1, Constant.VIDEO_MODE_SPLIT_HORIZONTAL, if (defaultFps > 0) defaultFps else 30)
                        ALog.i(TAG, "enableVersion1=false, treating as normal MP4")
                    }
                }
                configManager.config = config
            }

            ALog.i(TAG, "Config: ${configManager.config}")
            val config = configManager.config

            // 调用监听器的配置就绪回调
            if (config != null && (config.isDefaultConfig || config.isNormalMP4 || animListener?.onVideoConfigReady(config) == true)) {
                innerStartPlay(fileContainer)
            } else {
                ALog.i(TAG, "onVideoConfigReady return false")
                isStartRunning = false
                decoder?.onVideoComplete()
            }
        }
    }
    // 辅助方法：尝试从文件获取视频尺寸
    private fun innerStartPlay(fileContainer: IFileContainer) {
        synchronized(AnimPlayer::class.java) {
            if (isSurfaceAvailable) {
                isStartRunning = false
                decoder?.start(fileContainer)
                if (!isMute) {
                    audioPlayer?.start(fileContainer)
                }
            } else {
                 startRunnable = Runnable {
                    innerStartPlay(fileContainer)
                 }
                animView.prepareTextureView()
            }
        }
    }

    fun stopPlay() {
        decoder?.stop()
        audioPlayer?.stop()
    }

    fun isRunning(): Boolean {
        return isStartRunning // 启动过程运行状态
                || (decoder?.isRunning ?: false) // 解码过程运行状态

    }

    private fun prepareDecoder() {
        if (decoder == null) {
            decoder = HardDecoder(this).apply {
                playLoop = this@AnimPlayer.playLoop
                fps = this@AnimPlayer.fps
            }
        }
        if (audioPlayer == null) {
            audioPlayer = AudioPlayer(this).apply {
                playLoop = this@AnimPlayer.playLoop
            }
        }
    }

    fun updateMaskConfig(maskConfig: MaskConfig?) {
        configManager.config?.maskConfig = configManager.config?.maskConfig ?: MaskConfig()
        configManager.config?.maskConfig?.safeSetMaskBitmapAndReleasePre(maskConfig?.alphaMaskBitmap)
        configManager.config?.maskConfig?.maskPositionPair = maskConfig?.maskPositionPair
        configManager.config?.maskConfig?.maskTexPair = maskConfig?.maskTexPair
    }
    // 检测是否为VAP格式的启发式方法
    private fun shouldTreatAsVap(videoInfo: VideoInfo): Boolean {
        // 方法1：根据enableVersion1标志
        if (enableVersion1) {
            ALog.i(TAG, "enableVersion1=true, treating as VAP")
            return true
        }

        // 方法2：检查视频宽高比
        // VAP格式通常有特殊的宽高比（如2:1左右分割）
        if (videoInfo.width > videoInfo.height * 1.5) {
            // 宽度远大于高度，可能是左右分割的VAP
            ALog.i(TAG, "Video aspect ratio suggests VAP format: ${videoInfo.width}:${videoInfo.height}")
            return true
        }

        // 方法3：检查文件名
        videoInfo.fileName?.let { fileName ->
            val lowerName = fileName.lowercase()
            if (lowerName.contains("vap") || lowerName.contains("split") || lowerName.contains("alpha")) {
                ALog.i(TAG, "File name suggests VAP format: $fileName")
                return true
            }
        }

        // 方法4：检查视频尺寸是否能被2整除（对于左右分割）
        if (videoInfo.width % 2 == 0) {
            // 尝试检查左右两半是否相似（更复杂的检测）
            // 这里可以添加更复杂的检测逻辑
        }

        return false
    }

    // 视频信息类
    data class VideoInfo(
        val width: Int,
        val height: Int,
        val frameRate: Int,
        val hasAlpha: Boolean,
        val fileName: String?
    )

}