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

import android.graphics.Bitmap
import android.graphics.MaskFilter
import com.tencent.qgame.animplayer.mask.MaskConfig
import com.tencent.qgame.animplayer.util.ALog
import org.json.JSONException
import org.json.JSONObject

/**
 * vapc里读取出来的基础配置
 */
class AnimConfig {

    companion object {
        private const val TAG = "${Constant.TAG}.AnimConfig"
        // 视频格式类型
        const val FORMAT_UNKNOWN = 0
        const val FORMAT_VAP = 1  // VAP动画（有JSON配置）
        const val FORMAT_NORMAL_MP4_NO_ALPHA = 2  // 普通MP4（无透明通道）
        const val FORMAT_NORMAL_MP4_WITH_ALPHA = 3  // 普通MP4（有透明通道，RGBA格式）
    }

    val version = 2 // 不同版本号不兼容
    var totalFrames = 0 // 总帧数
    var width = 0 // 需要显示视频的真实宽高
    var height = 0
    var videoWidth = 0 // 视频实际宽高
    var videoHeight = 0
    var orien = Constant.ORIEN_DEFAULT // 0-兼容模式 1-竖屏 2-横屏
    var fps = 0
    var isMix = false // 是否为融合动画
    var alphaPointRect = PointRect(0, 0 ,0 ,0) // alpha区域
    var rgbPointRect = PointRect(0, 0, 0, 0) // rgb区域
    var isDefaultConfig = false // 没有vapc配置时默认逻辑
    var defaultVideoMode = Constant.VIDEO_MODE_SPLIT_HORIZONTAL
    // 视频格式类型
    var videoFormat: Int = FORMAT_UNKNOWN

    // 兼容旧代码
    var isNormalMP4: Boolean
        get() = videoFormat == FORMAT_NORMAL_MP4_NO_ALPHA || videoFormat == FORMAT_NORMAL_MP4_WITH_ALPHA
        set(value) {
            if (value && videoFormat == FORMAT_UNKNOWN) {
                videoFormat = FORMAT_NORMAL_MP4_NO_ALPHA
            }
        }
    var maskConfig: MaskConfig ?= null
    var jsonConfig: JSONObject? = null
    // 添加新字段：是否为普通MP4（没有JSON配置）
//    var isNormalMP4: Boolean = false
    var hasAlpha: Boolean = false
        get() = when(videoFormat) {
            FORMAT_VAP -> true
            FORMAT_NORMAL_MP4_WITH_ALPHA -> true
            else -> false
        }
    var needBlend: Boolean = false
    /**
     * 创建VAP动画的默认配置（没有JSON配置时使用）
     */
    fun createForNormalMP4(videoWidth: Int, videoHeight: Int, defaultVideoMode: Int, fps: Int = 30) {
        this.videoFormat = FORMAT_VAP
        this.isDefaultConfig = true
        this.isNormalMP4 = false
        this.videoWidth = videoWidth
        this.videoHeight = videoHeight
        this.fps = fps
        this.hasAlpha = true
        this.needBlend = true

        // 根据视频模式设置分割方式
        when (defaultVideoMode) {
            Constant.VIDEO_MODE_SPLIT_HORIZONTAL -> {
                // 左右分割：左alpha，右rgb
                width = videoWidth / 2
                height = videoHeight
                alphaPointRect = PointRect(0, 0, width, height)
                rgbPointRect = PointRect(width, 0, width, height)
            }
            Constant.VIDEO_MODE_SPLIT_VERTICAL -> {
                // 上下分割：上alpha，下rgb
                width = videoWidth
                height = videoHeight / 2
                alphaPointRect = PointRect(0, 0, width, height)
                rgbPointRect = PointRect(0, height, width, height)
            }
            Constant.VIDEO_MODE_SPLIT_HORIZONTAL_REVERSE -> {
                // 左右分割：左rgb，右alpha
                width = videoWidth / 2
                height = videoHeight
                rgbPointRect = PointRect(0, 0, width, height)
                alphaPointRect = PointRect(width, 0, width, height)
            }
            Constant.VIDEO_MODE_SPLIT_VERTICAL_REVERSE -> {
                // 上下分割：上rgb，下alpha
                width = videoWidth
                height = videoHeight / 2
                rgbPointRect = PointRect(0, 0, width, height)
                alphaPointRect = PointRect(0, height, width, height)
            }
            else -> {
                // 默认左右分割
                width = videoWidth / 2
                height = videoHeight
                alphaPointRect = PointRect(0, 0, width, height)
                rgbPointRect = PointRect(width, 0, width, height)
            }
        }

        ALog.i("AnimConfig", "Created VAP config without JSON: $width x $height, split mode=$defaultVideoMode")
    }
    /**
     * 创建普通MP4的默认配置
     */
    fun createForNormalMP41(videoWidth: Int, videoHeight: Int, hasAlpha: Boolean = false, fps: Int = 30) {
        this.videoFormat = if (hasAlpha) FORMAT_NORMAL_MP4_WITH_ALPHA else FORMAT_NORMAL_MP4_NO_ALPHA
        this.isNormalMP4 = true
        this.isDefaultConfig = true
        this.videoWidth = videoWidth
        this.videoHeight = videoHeight
        this.width = videoWidth
        this.height = videoHeight
        this.fps = fps
        this.hasAlpha = hasAlpha  // 修复：根据参数设置hasAlpha
        this.needBlend = hasAlpha  // 只有有alpha通道才需要blend
        if (hasAlpha) {
            // RGBA格式的MP4：整个画面包含RGBA数据
            this.alphaPointRect = PointRect(0, 0, videoWidth, videoHeight)
            this.rgbPointRect = PointRect(0, 0, videoWidth, videoHeight)
        } else {
            // 普通MP4：整个画面只有RGB数据
            this.alphaPointRect = PointRect(0, 0, 0, 0)  // 无alpha通道，设置为空
            this.rgbPointRect = PointRect(0, 0, videoWidth, videoHeight)
        }
    }
    /**
     * @return 解析是否成功，失败按默认配置走
     */
    fun parse(json: JSONObject): Boolean {
        return try {
            json.getJSONObject("info").apply {
                val v = getInt("v")
                if (version != v) {
                    ALog.e(TAG, "current version=$version target=$v")
                    return false
                }
                totalFrames = getInt("f")
                width = getInt("w")
                height = getInt("h")
                videoWidth = getInt("videoW")
                videoHeight = getInt("videoH")
                orien = getInt("orien")
                fps = getInt("fps")
                isMix = getInt("isVapx") == 1
                val a = getJSONArray("aFrame") ?: return false
                alphaPointRect = PointRect(a.getInt(0), a.getInt(1), a.getInt(2), a.getInt(3))
                val c = getJSONArray("rgbFrame") ?: return false
                rgbPointRect = PointRect(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3))
                // 标记为VAP动画
                videoFormat = FORMAT_VAP
                hasAlpha = true  // VAP动画有Alpha通道

            }
            true
        } catch (e : JSONException) {
            ALog.e(TAG, "json parse fail $e", e)
            false
        }catch (e : Exception) {
            ALog.e(TAG, "json parse exception $e", e)
            false
        }
    }

    override fun toString(): String {
        return "AnimConfig(version=$version, totalFrames=$totalFrames, width=$width, height=$height, videoWidth=$videoWidth, videoHeight=$videoHeight, orien=$orien, fps=$fps, isMix=$isMix, alphaPointRect=$alphaPointRect, rgbPointRect=$rgbPointRect, isDefaultConfig=$isDefaultConfig)"
    }


}


data class PointRect(val x: Int, val y: Int, val w: Int, val h: Int)
data class RefVec2(val w: Int, val h: Int) //参考宽&高