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

import android.os.SystemClock
import com.tencent.qgame.animplayer.file.IFileContainer
import com.tencent.qgame.animplayer.util.ALog
import org.json.JSONObject
import java.nio.charset.Charset

/**
 * 配置管理
 */
class AnimConfigManager(val player: AnimPlayer) {

    companion object {
        private const val TAG = "${Constant.TAG}.AnimConfigManager"
    }

    var config: AnimConfig? = null
    var isParsingConfig = false // 是否正在读取配置

    /**
     * 解析配置
     * @return true 解析成功 false 解析失败
     */
    fun parseConfig(fileContainer: IFileContainer, enableVersion1: Boolean, defaultVideoMode: Int, defaultFps: Int): Int {
        try {
            isParsingConfig = true
            // 解析vapc
            val time = SystemClock.elapsedRealtime()
            val result = parse(fileContainer, defaultVideoMode, defaultFps)
            ALog.i(TAG, "parseConfig cost=${SystemClock.elapsedRealtime() - time}ms enableVersion1=$enableVersion1 result=$result")
            if (!result) {
                isParsingConfig = false
                return Constant.REPORT_ERROR_TYPE_PARSE_CONFIG
            }
            // 重要：如果enableVersion1为true且是默认配置，将其标记为VAP格式
            if (config?.isDefaultConfig == true && enableVersion1) {
                ALog.i(TAG, "enableVersion1=true and default config, treating as VAP format")
                config?.videoFormat = AnimConfig.FORMAT_VAP
                config?.isNormalMP4 = false
                config?.hasAlpha = true
            }
            // 插件解析配置
            val resultCode = config?.let {
                player.pluginManager.onConfigCreate(it)
            } ?: Constant.OK
            isParsingConfig = false
            return resultCode
        } catch (e : Throwable) {
            ALog.e(TAG, "parseConfig error $e", e)
            isParsingConfig = false
            return Constant.REPORT_ERROR_TYPE_PARSE_CONFIG
        }
    }

    /**
     * 默认配置解析（兼容老视频格式）
     */
    fun defaultConfig(_videoWidth: Int, _videoHeight: Int, enableVersion1: Boolean = false) {
        val currentConfig = config ?: return
        // 重要：对于普通MP4，不执行任何分割逻辑
        if (currentConfig.isNormalMP4) {
            // 普通MP4：使用整个画面，不分割
            config?.apply {
                videoWidth = _videoWidth
                videoHeight = _videoHeight
                width = _videoWidth
                height = _videoHeight
                // 根据是否有透明通道设置区域
                if (hasAlpha) {
                    alphaPointRect = PointRect(0, 0, _videoWidth, _videoHeight)
                    rgbPointRect = PointRect(0, 0, _videoWidth, _videoHeight)
                } else {
                    alphaPointRect = PointRect(0, 0, 0, 0)  // 无alpha通道
                    rgbPointRect = PointRect(0, 0, _videoWidth, _videoHeight)
                }
                ALog.i(TAG, "Normal MP4 defaultConfig: full screen, no split")
            }
            return
        }

        if (currentConfig.isDefaultConfig == false) return
        // 重要：只在视频尺寸发生变化时才更新
        if (currentConfig.videoWidth == _videoWidth && currentConfig.videoHeight == _videoHeight) {
            return
        }
        config?.apply {
            videoWidth = _videoWidth
            videoHeight = _videoHeight
            // 重要：根据视频格式决定如何设置
            when (videoFormat) {
                AnimConfig.FORMAT_NORMAL_MP4_NO_ALPHA -> {
                    // 普通MP4（无透明通道）：整个画面作为rgb，不分割
                    width = _videoWidth
                    height = _videoHeight
                    alphaPointRect = PointRect(0, 0, width, height)
                    rgbPointRect = PointRect(0, 0, width, height)
                    hasAlpha = false
                    ALog.i(TAG, "Normal MP4 (no alpha): use full screen, no split")
                }
                AnimConfig.FORMAT_NORMAL_MP4_WITH_ALPHA -> {
                    // 普通MP4（有透明通道）：整个画面包含RGBA数据
                    width = _videoWidth
                    height = _videoHeight
                    alphaPointRect = PointRect(0, 0, width, height)
                    rgbPointRect = PointRect(0, 0, width, height)
                    hasAlpha = true
                    ALog.i(TAG, "Normal MP4 (with alpha): use full screen, no split")
                }
                AnimConfig.FORMAT_VAP -> {
                    // VAP动画：保持原有的分割逻辑
                    if (enableVersion1) {
                        when (defaultVideoMode) {
                            Constant.VIDEO_MODE_SPLIT_HORIZONTAL -> {
                                width = _videoWidth / 2
                                height = _videoHeight
                                alphaPointRect = PointRect(0, 0, width, height)
                                rgbPointRect = PointRect(width, 0, width, height)
                                hasAlpha = true
                            }
                            Constant.VIDEO_MODE_SPLIT_VERTICAL -> {
                                width = _videoWidth
                                height = _videoHeight / 2
                                alphaPointRect = PointRect(0, 0, width, height)
                                rgbPointRect = PointRect(0, height, width, height)
                                hasAlpha = true
                            }
                            Constant.VIDEO_MODE_SPLIT_HORIZONTAL_REVERSE -> {
                                width = _videoWidth / 2
                                height = _videoHeight
                                rgbPointRect = PointRect(0, 0, width, height)
                                alphaPointRect = PointRect(width, 0, width, height)
                                hasAlpha = true
                            }
                            Constant.VIDEO_MODE_SPLIT_VERTICAL_REVERSE -> {
                                width = _videoWidth
                                height = _videoHeight / 2
                                rgbPointRect = PointRect(0, 0, width, height)
                                alphaPointRect = PointRect(0, height, width, height)
                                hasAlpha = true
                            }
                            else -> {
                                width = _videoWidth / 2
                                height = _videoHeight
                                alphaPointRect = PointRect(0, 0, width, height)
                                rgbPointRect = PointRect(width, 0, width, height)
                                hasAlpha = true
                            }
                        }
                    } else {
                        // VAP动画但未启用版本1：使用原始配置，不分割
                        width = _videoWidth
                        height = _videoHeight
                        alphaPointRect = PointRect(0, 0, width, height)
                        rgbPointRect = PointRect(0, 0, width, height)
                        hasAlpha = true
                    }
                }
                else -> {
                    // 未知格式：当作普通MP4处理
                    width = _videoWidth
                    height = _videoHeight
                    alphaPointRect = PointRect(0, 0, width, height)
                    rgbPointRect = PointRect(0, 0, width, height)
                    hasAlpha = false
                    ALog.w(TAG, "Unknown format, treat as normal MP4")
                }
            }
        }
    }


    private fun parse(fileContainer: IFileContainer, defaultVideoMode: Int, defaultFps: Int): Boolean {

        val config = AnimConfig()
        this.config = config


        // 查找vapc box
        fileContainer.startRandomRead()
        val boxHead = ByteArray(8)
        var head: BoxHead? = null
        var vapcStartIndex: Long = 0
        while (fileContainer.read(boxHead, 0, boxHead.size) == 8) {
            val h = parseBoxHead(boxHead) ?: break
            if ("vapc" == h.type) {
                h.startIndex = vapcStartIndex
                head = h
                break
            }
            vapcStartIndex += h.length
            fileContainer.skip(h.length - 8L)
        }

        if (head == null) {
            ALog.i(TAG, "vapc box head not found, treat as normal MP4")
            // 没有找到vapc box，按普通MP4处理
            config.apply {
                isDefaultConfig = true
                this.defaultVideoMode = defaultVideoMode
                fps = defaultFps
                // 创建普通MP4配置（需要在后续知道视频尺寸后补充）
// 暂时标记为未知格式，由上层逻辑决定
                videoFormat = AnimConfig.FORMAT_UNKNOWN
            }
            player.fps = config.fps
            return true
        }

        // 读取vapc box
        val vapcBuf = ByteArray(head.length - 8) // ps: OOM exception
        fileContainer.read(vapcBuf, 0 , vapcBuf.size)
        fileContainer.closeRandomRead()

//        val json = String(vapcBuf, 0, vapcBuf.size, Charset.forName("UTF-8"))
//        val jsonObj = JSONObject(json)
//        config.jsonConfig = jsonObj
//        val result = config.parse(jsonObj)
//        if (defaultFps > 0) {
//            config.fps = defaultFps
//        }
//        player.fps = config.fps
//        return result
        try {
            val json = String(vapcBuf, 0, vapcBuf.size, Charset.forName("UTF-8"))
            ALog.d(TAG, "vapc json: $json")
            val jsonObj = JSONObject(json)
            config.jsonConfig = jsonObj
            val result = config.parse(jsonObj)
            if (defaultFps > 0) {
                config.fps = defaultFps
            }
            player.fps = config.fps
            return result
        } catch (e: Exception) {
            // JSON解析失败，按普通MP4处理
            ALog.w(TAG, "vapc box json parse failed, treat as normal MP4: $e")
            config.apply {
                isDefaultConfig = true
                this.defaultVideoMode = defaultVideoMode
                fps = defaultFps
                isNormalMP4 = true
                // JSON解析失败，标记为VAP格式（因为找到了vapc box）
                videoFormat = AnimConfig.FORMAT_VAP
                hasAlpha = true
            }
            player.fps = config.fps
            return true
        }
    }

    private fun parseBoxHead(boxHead: ByteArray): BoxHead? {
        if (boxHead.size != 8) return null
        val head = BoxHead()
        var length: Int = 0
        length = length or (boxHead[0].toInt() and 0xff shl 24)
        length = length or (boxHead[1].toInt() and 0xff shl 16)
        length = length or (boxHead[2].toInt() and 0xff shl 8)
        length = length or (boxHead[3].toInt() and 0xff)
        head.length = length
        head.type = String(boxHead, 4, 4, Charset.forName("US-ASCII"))
        return head
    }

    private class BoxHead {
        var startIndex: Long = 0
        var length: Int = 0
        var type: String? = null
    }



}