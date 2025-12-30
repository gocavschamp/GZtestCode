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

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import com.tencent.qgame.animplayer.file.IFileContainer
import com.tencent.qgame.animplayer.util.ALog
import com.tencent.qgame.animplayer.util.MediaUtil

class HardDecoder(player: AnimPlayer) : Decoder(player), SurfaceTexture.OnFrameAvailableListener {


    companion object {
        private const val TAG = "${Constant.TAG}.HardDecoder"
    }

    private var surface: Surface? = null
    private var glTexture: SurfaceTexture? = null
    private val bufferInfo by lazy { MediaCodec.BufferInfo() }
    private var needDestroy = false

    // 动画的原始尺寸
    private var videoWidth = 0
    private var videoHeight = 0

    // 动画对齐后的尺寸
    private var alignWidth = 0
    private var alignHeight = 0

    // 动画是否需要走YUV渲染逻辑的标志位
    private var needYUV = false
    private var outputFormat: MediaFormat? = null
    // 添加标志位，标识是否正在使用普通 MP4 模式
    private var isNormalMP4Mode = false
    // 视频颜色格式
    private var colorFormat: Int = 0
    override fun start(fileContainer: IFileContainer) {
        isStopReq = false
        needDestroy = false
        isRunning = true

        isNormalMP4Mode = false
//        renderThread.handler?.post {
//            startPlay(fileContainer)
//        }
        renderThread.handler?.post {
            try {
                startPlay(fileContainer)
            } catch (e: Throwable) {
                // 如果正常流程失败，尝试降级为普通 MP4 模式
                ALog.w(TAG, "Normal VAP play failed, try fallback to normal MP4 mode: $e")
                try {
                    startAsNormalMP4(fileContainer)
                } catch (fallbackError: Throwable) {
                    ALog.e(TAG, "Fallback failed: $fallbackError")
                    onFailed(Constant.REPORT_ERROR_TYPE_DECODE_EXC,
                        "Both VAP and fallback failed: ${e.message}")
                }
            }
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        if (isStopReq) return
        ALog.d(TAG, "onFrameAvailable")
        renderData()
    }

    private fun renderData() {
        renderThread.handler?.post {
            try {
                glTexture?.apply {
                    updateTexImage()
                    render?.renderFrame()
                    player.pluginManager.onRendering()
                    render?.swapBuffers()
                }
            } catch (e: Throwable) {
                ALog.e(TAG, "render exception=$e", e)
            }
        }
    }

    private fun startPlay(fileContainer: IFileContainer) {

        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var format: MediaFormat? = null
        var trackIndex = 0

        try {
            extractor = MediaUtil.getExtractor(fileContainer)
            trackIndex = MediaUtil.selectVideoTrack(extractor)
            if (trackIndex < 0) {
                throw RuntimeException("No video track found")
            }
            extractor.selectTrack(trackIndex)
            format = extractor.getTrackFormat(trackIndex)
            if (format == null) throw RuntimeException("format is null")

            // 是否支持h265
            if (MediaUtil.checkIsHevc(format)) {
                if (Build.VERSION.SDK_INT  < Build.VERSION_CODES.LOLLIPOP
                    || !MediaUtil.checkSupportCodec(MediaUtil.MIME_HEVC)) {

                    onFailed(Constant.REPORT_ERROR_TYPE_HEVC_NOT_SUPPORT,
                        "${Constant.ERROR_MSG_HEVC_NOT_SUPPORT} " +
                                "sdk:${Build.VERSION.SDK_INT}" +
                                ",support hevc:" + MediaUtil.checkSupportCodec(MediaUtil.MIME_HEVC))
                    release(null, null)
                    return
                }
            }

            videoWidth = format.getInteger(MediaFormat.KEY_WIDTH)
            videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT)
            // 防止没有INFO_OUTPUT_FORMAT_CHANGED时导致alignWidth和alignHeight不会被赋值一直是0
            alignWidth = videoWidth
            alignHeight = videoHeight
            ALog.i(TAG, "Video size is $videoWidth x $videoHeight")
// 检测颜色格式，判断是否有透明通道
            colorFormat = try {
                format.getInteger(MediaFormat.KEY_COLOR_FORMAT)
            } catch (e: Exception) {
                0
            }

            ALog.i(TAG, "Video color format: $colorFormat")

            // 判断是否为RGBA格式（有透明通道）
            val hasAlphaChannel = colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format32bitABGR8888 ||
                    colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888 ||
                    colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format32bitBGRA8888 ||
                    colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_Format24bitBGR888
            // 由于使用mediacodec解码老版本素材时对宽度1500尺寸的视频进行数据对齐，解码后的宽度变成1504，导致采样点出现偏差播放异常
            // 所以当开启兼容老版本视频模式并且老版本视频的宽度不能被16整除时要走YUV渲染逻辑
            // 但是这样直接判断有风险，后期想办法改
            // 如果有透明通道，更新配置
            if (hasAlphaChannel && player.configManager.config?.isDefaultConfig == true) {
                ALog.i(TAG, "Video has alpha channel, updating config")
                player.configManager.config?.videoFormat = AnimConfig.FORMAT_NORMAL_MP4_WITH_ALPHA
                player.configManager.config?.hasAlpha = true
            }
            needYUV = videoWidth % 16 != 0 && player.enableVersion1
            if (hasAlphaChannel) {
                needYUV = false
                ALog.i(TAG, "Video has alpha channel, force use normal render mode")
            }
            try {
                if (!prepareRender(needYUV)) {
                    throw RuntimeException("render create fail")
                }
            } catch (t: Throwable) {
//               // 即使渲染器创建失败，也尝试继续
                ALog.w(TAG, "Render prepare failed, try to continue: ${t.message}")
            }

            // 准备播放，这里可能抛出配置解析错误
            try {
                preparePlay(videoWidth, videoHeight)
                render?.apply {
                    glTexture = SurfaceTexture(getExternalTexture()).apply {
                        setOnFrameAvailableListener(this@HardDecoder)
                        setDefaultBufferSize(videoWidth, videoHeight)
                    }
                    clearFrame()
                }
            } catch (configError: Throwable) {
                // 配置解析失败，降级为普通 MP4 模式
                ALog.w(TAG, "Config parse failed, fallback to normal MP4: ${configError.message}")
                throw RuntimeException("Config parse failed, need fallback")
            }



        } catch (e: Throwable) {
            // 如果是配置解析错误，重新抛出以便降级处理
            if (e.message?.contains("Config parse failed") == true ||
                e.message?.contains("parse config fail") == true) {
                throw e
            }
            ALog.e(TAG, "MediaExtractor exception e=$e", e)
            onFailed(Constant.REPORT_ERROR_TYPE_EXTRACTOR_EXC, "${Constant.ERROR_MSG_EXTRACTOR_EXC} e=$e")
            release(decoder, extractor)
            return
        }

        try {
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            ALog.i(TAG, "Video MIME is $mime")
            decoder = MediaCodec.createDecoderByType(mime).apply {
                if (needYUV) {
                    format.setInteger(
                            MediaFormat.KEY_COLOR_FORMAT,
                            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                    )
                    configure(format, null, null, 0)
                } else {
                    surface = Surface(glTexture)
                    configure(format, surface, null, 0)
                }

                start()
                decodeThread.handler?.post {
                    try {
                        startDecode(extractor, this)
                    } catch (e: Throwable) {
                        ALog.e(TAG, "MediaCodec exception e=$e", e)
                        onFailed(Constant.REPORT_ERROR_TYPE_DECODE_EXC, "${Constant.ERROR_MSG_DECODE_EXC} e=$e")
                        release(decoder, extractor)
                    }
                }
            }
        } catch (e: Throwable) {
            ALog.e(TAG, "MediaCodec configure exception e=$e", e)
            onFailed(Constant.REPORT_ERROR_TYPE_DECODE_EXC, "${Constant.ERROR_MSG_DECODE_EXC} e=$e")
            release(decoder, extractor)
            return
        }
    }
    /**
     * 降级模式：普通 MP4 播放
     */
    private fun startAsNormalMP4(fileContainer: IFileContainer) {
        ALog.i(TAG, "Starting in normal MP4 mode")
        isNormalMP4Mode = true

        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null

        try {
            extractor = MediaUtil.getExtractor(fileContainer)
            val trackIndex = MediaUtil.selectVideoTrack(extractor)
            if (trackIndex < 0) {
                throw RuntimeException("No video track found")
            }
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)

            videoWidth = format.getInteger(MediaFormat.KEY_WIDTH)
            videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT)
            alignWidth = videoWidth
            alignHeight = videoHeight

            ALog.i(TAG, "Normal MP4 video size is $videoWidth x $videoHeight")

            // 准备渲染器（强制使用普通渲染模式）
            if (!prepareRender(false)) {
                throw RuntimeException("render create fail")
            }

            // 创建默认配置
            val defaultConfig = createDefaultConfig(videoWidth, videoHeight)
            player.configManager.config = defaultConfig
            render?.setAnimConfig(defaultConfig)

            render?.apply {
                glTexture = SurfaceTexture(getExternalTexture()).apply {
                    setOnFrameAvailableListener(this@HardDecoder)
                    setDefaultBufferSize(videoWidth, videoHeight)
                }
                clearFrame()
            }

            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            ALog.i(TAG, "Normal MP4 MIME is $mime")

            decoder = MediaCodec.createDecoderByType(mime).apply {
                surface = Surface(glTexture)
                configure(format, surface, null, 0)
                start()
                decodeThread.handler?.post {
                    try {
                        startDecode(extractor, this)
                    } catch (e: Throwable) {
                        ALog.e(TAG, "Normal MP4 decode exception e=$e", e)
                        onFailed(Constant.REPORT_ERROR_TYPE_DECODE_EXC,
                            "${Constant.ERROR_MSG_DECODE_EXC} e=$e")
                        release(decoder, extractor)
                    }
                }
            }

        } catch (e: Throwable) {
            ALog.e(TAG, "Normal MP4 configure exception e=$e", e)
            onFailed(Constant.REPORT_ERROR_TYPE_DECODE_EXC,
                "Normal MP4 fallback failed: ${e.message}")
            release(decoder, extractor)
            return
        }
    }
    override fun onFailed(errorType: Int, errorMsg: String?) {
        // 如果是配置解析错误，尝试降级处理
        if (errorType == Constant.REPORT_ERROR_TYPE_PARSE_CONFIG && !isNormalMP4Mode) {
            ALog.w(TAG, "Config parse failed, but we're already in fallback or will try fallback")
        }
        super.onFailed(errorType, errorMsg)
    }
    private fun startDecode(extractor: MediaExtractor ,decoder: MediaCodec) {
        val TIMEOUT_USEC = 10000L
        var inputChunk = 0
        var outputDone = false
        var inputDone = false
        var frameIndex = 0
        var isLoop = false

        val decoderInputBuffers = decoder.inputBuffers

        while (!outputDone) {
            if (isStopReq) {
                ALog.i(TAG, "stop decode")
                release(decoder, extractor)
                return
            }

            if (!inputDone) {
                val inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC)
                if (inputBufIndex >= 0) {
                    val inputBuf = decoderInputBuffers[inputBufIndex]
                    val chunkSize = extractor.readSampleData(inputBuf, 0)
                    if (chunkSize < 0) {
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                        ALog.d(TAG, "decode EOS")
                    } else {
                        val presentationTimeUs = extractor.sampleTime
                        decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, presentationTimeUs, 0)
                        ALog.d(TAG, "submitted frame $inputChunk to dec, size=$chunkSize")
                        inputChunk++
                        extractor.advance()
                    }
                } else {
                    ALog.d(TAG, "input buffer not available")
                }
            }

            if (!outputDone) {
                val decoderStatus = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                when {
                    decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> ALog.d(TAG, "no output from decoder available")
                    decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> ALog.d(TAG, "decoder output buffers changed")
                    decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        outputFormat = decoder.outputFormat
                        outputFormat?.apply {
                            try {
                                // 有可能取到空值，做一层保护
                                val stride = getInteger("stride")
                                val sliceHeight = getInteger("slice-height")
                                if (stride > 0 && sliceHeight > 0) {
                                    alignWidth = stride
                                    alignHeight = sliceHeight
                                }
                            } catch (t: Throwable) {
                                ALog.e(TAG, "$t", t)
                            }
                        }
                        ALog.i(TAG, "decoder output format changed: $outputFormat")
                    }
                    decoderStatus < 0 -> {
                        throw RuntimeException("unexpected result from decoder.dequeueOutputBuffer: $decoderStatus")
                    }
                    else -> {
                        var loop = 0
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            loop = --playLoop
                            player.playLoop = playLoop // 消耗loop次数 自动恢复后能有正确的loop次数
                            outputDone = playLoop <= 0
                        }
                        val doRender = !outputDone
                        if (doRender) {
                            speedControlUtil.preRender(bufferInfo.presentationTimeUs)
                        }

                        if (needYUV && doRender) {
                            yuvProcess(decoder, decoderStatus)
                        }

                        // release & render
                        decoder.releaseOutputBuffer(decoderStatus, doRender && !needYUV)

                        if (frameIndex == 0 && !isLoop) {
                            onVideoStart()
                        }
                        player.pluginManager.onDecoding(frameIndex)
                        onVideoRender(frameIndex, player.configManager.config)

                        frameIndex++
                        ALog.d(TAG, "decode frameIndex=$frameIndex")
                        if (loop > 0) {
                            ALog.d(TAG, "Reached EOD, looping")
                            player.pluginManager.onLoopStart()
                            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            inputDone = false
                            decoder.flush()
                            speedControlUtil.reset()
                            frameIndex = 0
                            isLoop = true
                        }
                        if (outputDone) {
                            release(decoder, extractor)
                        }
                    }
                }
            }
        }

    }

    /**
     * 获取到解码后每一帧的YUV数据，裁剪出正确的尺寸
     */
    private fun yuvProcess(decoder: MediaCodec, outputIndex: Int) {
        val outputBuffer = decoder.outputBuffers[outputIndex]
        outputBuffer?.let {
            it.position(0)
            it.limit(bufferInfo.offset + bufferInfo.size)
            var yuvData = ByteArray(outputBuffer.remaining())
            outputBuffer.get(yuvData)

            if (yuvData.isNotEmpty()) {
                var yData = ByteArray(videoWidth * videoHeight)
                var uData = ByteArray(videoWidth * videoHeight / 4)
                var vData = ByteArray(videoWidth * videoHeight / 4)

                if (outputFormat?.getInteger(MediaFormat.KEY_COLOR_FORMAT) == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                    yuvData = yuv420spTop(yuvData)
                }

                yuvCopy(yuvData, 0, alignWidth, alignHeight, yData, videoWidth, videoHeight)
                yuvCopy(yuvData, alignWidth * alignHeight, alignWidth / 2, alignHeight / 2, uData, videoWidth / 2, videoHeight / 2)
                yuvCopy(yuvData, alignWidth * alignHeight * 5 / 4, alignWidth / 2, alignHeight / 2, vData, videoWidth / 2, videoHeight / 2)

                render?.setYUVData(videoWidth, videoHeight, yData, uData, vData)
                renderData()
            }
        }
    }

    private fun yuv420spTop(yuv420sp: ByteArray): ByteArray {
        val yuv420p = ByteArray(yuv420sp.size)
        val ySize = alignWidth * alignHeight
        System.arraycopy(yuv420sp, 0, yuv420p, 0, alignWidth * alignHeight)
        var i = ySize
        var j = ySize
        while (i < ySize * 3 / 2) {
            yuv420p[j] = yuv420sp[i]
            yuv420p[j + ySize / 4] = yuv420sp[i + 1]
            i += 2
            j++
        }
        return yuv420p
    }

    private fun yuvCopy(src: ByteArray, srcOffset: Int, inWidth: Int, inHeight: Int, dest: ByteArray, outWidth: Int, outHeight: Int) {
        for (h in 0 until inHeight) {
            if (h < outHeight) {
                System.arraycopy(src, srcOffset + h * inWidth, dest, h * outWidth, outWidth)
            }
        }
    }

    private fun release(decoder: MediaCodec?, extractor: MediaExtractor?) {
        renderThread.handler?.post {
            render?.clearFrame()
            try {
                ALog.i(TAG, "release")
                decoder?.apply {
                    stop()
                    release()
                }
                extractor?.release()
                glTexture?.release()
                glTexture = null
                speedControlUtil.reset()
                player.pluginManager.onRelease()
                render?.releaseTexture()
                surface?.release()
                surface = null
            } catch (e: Throwable) {
                ALog.e(TAG, "release e=$e", e)
            }
            isRunning = false
            onVideoComplete()
            if (needDestroy) {
                destroyInner()
            }
        }
    }

    override fun destroy() {
        if (isRunning) {
            needDestroy = true
            stop()
        } else {
            destroyInner()
        }
    }

    private fun destroyInner() {
        ALog.i(TAG, "destroyInner")
        renderThread.handler?.post {
            player.pluginManager.onDestroy()
            render?.destroyRender()
            render = null
            onVideoDestroy()
            destroyThread()
        }
    }
}