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
import android.opengl.GLES20
import com.tencent.qgame.animplayer.util.ALog
import com.tencent.qgame.animplayer.util.GlFloatArray
import com.tencent.qgame.animplayer.util.ShaderUtil.createProgram
import com.tencent.qgame.animplayer.util.TexCoordsUtil
import com.tencent.qgame.animplayer.util.VertexUtil
import java.nio.ByteBuffer
import java.nio.FloatBuffer

class YUVRender (surfaceTexture: SurfaceTexture): IRenderListener {

    companion object {
        private const val TAG = "${Constant.TAG}.YUVRender"
        // 普通MP4（YUV格式，无透明通道）的片段着色器
        private const val FRAGMENT_SHADER_NORMAL_MP4_YUV = "precision mediump float;\n" +
                "uniform sampler2D sampler_y;\n" +
                "uniform sampler2D sampler_u;\n" +
                "uniform sampler2D sampler_v;\n" +
                "varying vec2 v_TexCoordinateRgb;\n" +
                "uniform mat3 convertMatrix;\n" +
                "uniform vec3 offset;\n" +
                "\n" +
                "void main() {\n" +
                "   highp vec3 yuvColorRGB;\n" +
                "   highp vec3 rgbColorRGB;\n" +
                "   yuvColorRGB.x = texture2D(sampler_y,v_TexCoordinateRgb).r;\n" +
                "   yuvColorRGB.y = texture2D(sampler_u,v_TexCoordinateRgb).r;\n" +
                "   yuvColorRGB.z = texture2D(sampler_v,v_TexCoordinateRgb).r;\n" +
                "   yuvColorRGB += offset;\n" +
                "   rgbColorRGB = convertMatrix * yuvColorRGB; \n" +
                "   gl_FragColor=vec4(rgbColorRGB, 1.0);\n" +
                "}"
    }

    private val vertexArray = GlFloatArray()
    private val alphaArray = GlFloatArray()
    private val rgbArray = GlFloatArray()

    private var shaderProgram = 0
    private var normalMP4ShaderProgram = 0

    //顶点位置
    private var avPosition = 0
    private var normalMP4_avPosition = 0

    //rgb纹理位置
    private var rgbPosition = 0
    private var normalMP4_rgbPosition = 0

    //alpha纹理位置
    private var alphaPosition = 0

    //shader  yuv变量
    //shader  yuv变量
    private var samplerY = 0
    private var samplerU = 0
    private var samplerV = 0
    private var normalMP4_samplerY = 0
    private var normalMP4_samplerU = 0
    private var normalMP4_samplerV = 0
    private var convertMatrixUniform = 0
    private var convertOffsetUniform = 0
    private var normalMP4_convertMatrixUniform = 0
    private var normalMP4_convertOffsetUniform = 0
    private var textureId = IntArray(3)
    //YUV数据
    private var widthYUV = 0
    private var heightYUV = 0
    private var y: ByteBuffer? = null
    private var u: ByteBuffer? = null
    private var v: ByteBuffer? = null

    private val eglUtil: EGLUtil = EGLUtil()

    // 像素数据向GPU传输时默认以4字节对齐
    private var unpackAlign = 4

    // YUV offset
    private val YUV_OFFSET = floatArrayOf(
            0f, -0.501960814f, -0.501960814f
    )

    // RGB coefficients
    private val YUV_MATRIX = floatArrayOf(
            1f, 1f, 1f,
            0f, -0.3441f, 1.772f,
            1.402f, -0.7141f, 0f
    )
    // 新增：当前是否为普通MP4模式
    private var isNormalMP4Mode = false
    // 当前使用的着色器
    private var currentShaderProgram = 0

    init {
        eglUtil.start(surfaceTexture)
        initRender()
    }

    override fun initRender() {
        shaderProgram = createProgram(YUVShader.VERTEX_SHADER, YUVShader.FRAGMENT_SHADER)
        //获取顶点坐标字段
        avPosition = GLES20.glGetAttribLocation(shaderProgram, "v_Position")
        //获取纹理坐标字段
        rgbPosition = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinateRgb")
        alphaPosition = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinateAlpha")

        //获取yuv字段
        samplerY = GLES20.glGetUniformLocation(shaderProgram, "sampler_y")
        samplerU = GLES20.glGetUniformLocation(shaderProgram, "sampler_u")
        samplerV = GLES20.glGetUniformLocation(shaderProgram, "sampler_v")
        convertMatrixUniform = GLES20.glGetUniformLocation(shaderProgram, "convertMatrix")
        convertOffsetUniform = GLES20.glGetUniformLocation(shaderProgram, "offset")

        // 普通MP4 YUV着色器（使用修改后的顶点着色器，不需要alpha纹理坐标）
        val vertexShaderForNormalMP4 = "attribute vec4 v_Position;\n" +
                "attribute vec2 vTexCoordinateRgb;\n" +
                "varying vec2 v_TexCoordinateRgb;\n" +
                "\n" +
                "void main() {\n" +
                "    v_TexCoordinateRgb = vTexCoordinateRgb;\n" +
                "    gl_Position = v_Position;\n" +
                "}"

        normalMP4ShaderProgram = createProgram(vertexShaderForNormalMP4, FRAGMENT_SHADER_NORMAL_MP4_YUV)
        normalMP4_avPosition = GLES20.glGetAttribLocation(normalMP4ShaderProgram, "v_Position")
        normalMP4_rgbPosition = GLES20.glGetAttribLocation(normalMP4ShaderProgram, "vTexCoordinateRgb")

        normalMP4_samplerY = GLES20.glGetUniformLocation(normalMP4ShaderProgram, "sampler_y")
        normalMP4_samplerU = GLES20.glGetUniformLocation(normalMP4ShaderProgram, "sampler_u")
        normalMP4_samplerV = GLES20.glGetUniformLocation(normalMP4ShaderProgram, "sampler_v")
        normalMP4_convertMatrixUniform = GLES20.glGetUniformLocation(normalMP4ShaderProgram, "convertMatrix")
        normalMP4_convertOffsetUniform = GLES20.glGetUniformLocation(normalMP4ShaderProgram, "offset")
        //创建3个纹理
        GLES20.glGenTextures(textureId.size, textureId, 0)

        //绑定纹理
        for (id in textureId) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        }
    }

    override fun renderFrame() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        ALog.i(TAG, "renderFrame: widthYUV=$widthYUV, heightYUV=$heightYUV, y=${y != null}, u=${u != null}, v=${v != null}, currentShaderProgram=$currentShaderProgram")
        draw()
    }

    override fun clearFrame() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        eglUtil.swapBuffers()
    }

    override fun destroyRender() {
        releaseTexture()
        eglUtil.release()
    }

    override fun setAnimConfig(config: AnimConfig) {
        ALog.i(TAG, "setAnimConfig: config.width=${config.width}, config.height=${config.height}, config.videoFormat=${config.videoFormat}")
        vertexArray.setArray(VertexUtil.create(config.width, config.height, PointRect(0, 0, config.width, config.height), vertexArray.array))
        val alpha = TexCoordsUtil.create(config.videoWidth, config.videoHeight, config.alphaPointRect, alphaArray.array)
        val rgb = TexCoordsUtil.create(config.videoWidth, config.videoHeight, config.rgbPointRect, rgbArray.array)
        alphaArray.setArray(alpha)
        rgbArray.setArray(rgb)

        // 根据视频格式选择着色器
        // 注意：YUV渲染器只支持无透明通道的视频
        when (config.videoFormat) {
            AnimConfig.FORMAT_VAP -> {
                currentShaderProgram = shaderProgram
                ALog.i(TAG, "setAnimConfig: Using VAP YUV shader")
            }
            AnimConfig.FORMAT_NORMAL_MP4_NO_ALPHA -> {
                currentShaderProgram = normalMP4ShaderProgram
                ALog.i(TAG, "setAnimConfig: Using normal MP4 YUV shader")
            }
            AnimConfig.FORMAT_NORMAL_MP4_WITH_ALPHA -> {
                // YUV格式不支持透明通道，强制使用无透明通道模式
                currentShaderProgram = normalMP4ShaderProgram
                ALog.w(TAG, "setAnimConfig: YUV format doesn't support alpha, using no-alpha shader")
            }
            else -> {
                currentShaderProgram = normalMP4ShaderProgram
                ALog.w(TAG, "setAnimConfig: Unknown format, using default YUV shader")
            }
        }
        ALog.i(TAG, "setAnimConfig: currentShaderProgram=$currentShaderProgram")
    }

    override fun getExternalTexture(): Int {
        return textureId[0]
    }

    override fun releaseTexture() {
        GLES20.glDeleteTextures(textureId.size, textureId, 0)
    }

    override fun swapBuffers() {
        eglUtil.swapBuffers()
    }

    override fun setYUVData(width: Int, height: Int, y: ByteArray?, u: ByteArray?, v: ByteArray?) {
        ALog.i(TAG, "setYUVData: width=$width, height=$height, y size=${y?.size}, u size=${u?.size}, v size=${v?.size}")
        synchronized(this) {
            widthYUV = width
            heightYUV = height
            this.y = if (y != null) ByteBuffer.wrap(y) else null
            this.u = if (u != null) ByteBuffer.wrap(u) else null
            this.v = if (v != null) ByteBuffer.wrap(v) else null

            // 当视频帧的u或者v分量的宽度不能被4整除时，用默认的4字节对齐会导致存取最后一行时越界，所以在向GPU传输数据前指定对齐方式
            if ((widthYUV / 2) % 4 != 0) {
                this.unpackAlign = if ((widthYUV / 2) % 2 == 0) 2 else 1
            }
            ALog.i(TAG, "setYUVData: widthYUV=$widthYUV, heightYUV=$heightYUV, unpackAlign=$unpackAlign")
        }
    }
    private fun draw() {
        ALog.i(TAG, "draw: widthYUV=$widthYUV, heightYUV=$heightYUV, y=${y != null}, u=${u != null}, v=${v != null}, currentShaderProgram=$currentShaderProgram, shaderProgram=$shaderProgram, normalMP4ShaderProgram=$normalMP4ShaderProgram")
        if (widthYUV > 0 && heightYUV > 0 && y != null && u != null && v != null) {
            if (currentShaderProgram == shaderProgram) {
                ALog.i(TAG, "draw: Calling drawVAP")
                drawVAP()
            } else {
                ALog.i(TAG, "draw: Calling drawNormalMP4")
                drawNormalMP4()
            }
        } else {
            ALog.w(TAG, "draw: Conditions not met - widthYUV=$widthYUV, heightYUV=$heightYUV, y=${y != null}, u=${u != null}, v=${v != null}")
        }
    }
    private fun drawNormalMP4() {
        GLES20.glUseProgram(normalMP4ShaderProgram)
        vertexArray.setVertexAttribPointer(normalMP4_avPosition)
        rgbArray.setVertexAttribPointer(normalMP4_rgbPosition)

        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, unpackAlign)

        //激活纹理0来绑定y数据
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, widthYUV, heightYUV, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, y)

        //激活纹理1来绑定u数据
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[1])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, widthYUV / 2, heightYUV / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, u)

        //激活纹理2来绑定v数据
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[2])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, widthYUV / 2, heightYUV / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, v)

        //给fragment_shader里面yuv变量设置值   0 1 标识纹理x
        GLES20.glUniform1i(normalMP4_samplerY, 0)
        GLES20.glUniform1i(normalMP4_samplerU, 1)
        GLES20.glUniform1i(normalMP4_samplerV, 2)

        GLES20.glUniform3fv(normalMP4_convertOffsetUniform, 1, FloatBuffer.wrap(YUV_OFFSET))
        GLES20.glUniformMatrix3fv(normalMP4_convertMatrixUniform, 1, false, YUV_MATRIX, 0)

        //绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        cleanup()
    }
    private fun cleanup() {
        // 只清理顶点属性，不要清空YUV数据，这些数据由硬解码器持续提供
        // 清空YUV数据会导致下一帧渲染时数据为空，造成闪烁
        GLES20.glDisableVertexAttribArray(avPosition)
        GLES20.glDisableVertexAttribArray(rgbPosition)
        GLES20.glDisableVertexAttribArray(alphaPosition)
        GLES20.glDisableVertexAttribArray(normalMP4_avPosition)
        GLES20.glDisableVertexAttribArray(normalMP4_rgbPosition)
    }
    private fun drawVAP() {
        GLES20.glUseProgram(shaderProgram)
        vertexArray.setVertexAttribPointer(avPosition)
        alphaArray.setVertexAttribPointer(alphaPosition)
        rgbArray.setVertexAttribPointer(rgbPosition)

        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, unpackAlign)

        //激活纹理0来绑定y数据
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, widthYUV, heightYUV, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, y)

        //激活纹理1来绑定u数据
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[1])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, widthYUV / 2, heightYUV / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, u)

        //激活纹理2来绑定v数据
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[2])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, widthYUV / 2, heightYUV / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, v)

        //给fragment_shader里面yuv变量设置值   0 1 标识纹理x
        GLES20.glUniform1i(samplerY, 0)
        GLES20.glUniform1i(samplerU, 1)
        GLES20.glUniform1i(samplerV, 2)

        GLES20.glUniform3fv(convertOffsetUniform, 1, FloatBuffer.wrap(YUV_OFFSET))
        GLES20.glUniformMatrix3fv(convertMatrixUniform, 1, false, YUV_MATRIX, 0)

        //绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        cleanup()
    }
}