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
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.tencent.qgame.animplayer.util.ALog
import com.tencent.qgame.animplayer.util.GlFloatArray
import com.tencent.qgame.animplayer.util.ShaderUtil
import com.tencent.qgame.animplayer.util.TexCoordsUtil
import com.tencent.qgame.animplayer.util.VertexUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

class Render(surfaceTexture: SurfaceTexture): IRenderListener {

    companion object {
        private const val TAG = "${Constant.TAG}.Render"
        // 普通MP4（无透明通道）的片段着色器
        private const val FRAGMENT_SHADER_NORMAL_MP4_NO_ALPHA = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "uniform samplerExternalOES texture;\n" +
                "varying vec2 v_TexCoordinateAlpha;\n" +
                "varying vec2 v_TexCoordinateRgb;\n" +
                "\n" +
                "void main () {\n" +
                "    // 普通MP4（无透明通道）：直接使用RGB纹理坐标，alpha设为1.0\n" +
                "    vec4 rgbColor = texture2D(texture, v_TexCoordinateRgb);\n" +
                "    gl_FragColor = vec4(rgbColor.rgb, 1.0);\n" +
                "}"

        // 普通MP4（有透明通道，RGBA格式）的片段着色器
        private const val FRAGMENT_SHADER_NORMAL_MP4_WITH_ALPHA = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "uniform samplerExternalOES texture;\n" +
                "varying vec2 v_TexCoordinateAlpha;\n" +
                "varying vec2 v_TexCoordinateRgb;\n" +
                "\n" +
                "void main () {\n" +
                "    // 普通MP4（有透明通道，RGBA格式）：整个画面包含RGBA数据\n" +
                "    // 由于alpha和rgb在同一区域，我们可以使用任意一个纹理坐标\n" +
                "    vec4 rgbaColor = texture2D(texture, v_TexCoordinateRgb);\n" +
                "    gl_FragColor = rgbaColor;\n" +
                "}"
    }
    private var uHasAlphaLocation: Int = 0
    private var hasAlpha: Boolean = false
    // VAP动画着色器
    private var vapShaderProgram = 0
    // 普通MP4无透明通道着色器
    private var normalMP4NoAlphaShaderProgram = 0
    // 普通MP4有透明通道着色器
    private var normalMP4WithAlphaShaderProgram = 0
    private val vertexArray = GlFloatArray()
    private val alphaArray = GlFloatArray()
    private val rgbArray = GlFloatArray()
    private var surfaceSizeChanged = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private val eglUtil: EGLUtil = EGLUtil()
    private var shaderProgram = 0
    private var genTexture = IntArray(1)
    private var uTextureLocation: Int = 0
    private var aPositionLocation: Int = 0
    private var aTextureAlphaLocation: Int = 0
    private var aTextureRgbLocation: Int = 0
    // 新增：用于普通MP4的着色器
    private var normalMP4ShaderProgram = 0
    private var normalMP4_uTextureLocation: Int = 0
    private var normalMP4_aPositionLocation: Int = 0
    private var normalMP4_aTextureRgbLocation: Int = 0


    private var currentShaderProgram = 0


    // VAP动画着色器变量位置
    private var vap_uTextureLocation: Int = 0
    private var vap_aPositionLocation: Int = 0
    private var vap_aTextureAlphaLocation: Int = 0
    private var vap_aTextureRgbLocation: Int = 0

    // 普通MP4无透明通道着色器变量位置
    private var normalMP4NoAlpha_uTextureLocation: Int = 0
    private var normalMP4NoAlpha_aPositionLocation: Int = 0
    private var normalMP4NoAlpha_aTextureRgbLocation: Int = 0

    // 普通MP4有透明通道着色器变量位置
    private var normalMP4WithAlpha_uTextureLocation: Int = 0
    private var normalMP4WithAlpha_aPositionLocation: Int = 0
    private var normalMP4WithAlpha_aTextureRgbLocation: Int = 0
    // 新增：当前是否为普通MP4模式
    private var isNormalMP4Mode = false

    init {
        eglUtil.start(surfaceTexture)
        initRender()
    }

    private fun setVertexBuf(config: AnimConfig) {
        vertexArray.setArray(VertexUtil.create(config.width, config.height, PointRect(0, 0, config.width, config.height), vertexArray.array))
    }

    private fun setTexCoords(config: AnimConfig) {
        val alpha = TexCoordsUtil.create(config.videoWidth, config.videoHeight, config.alphaPointRect, alphaArray.array)
        val rgb = TexCoordsUtil.create(config.videoWidth, config.videoHeight, config.rgbPointRect, rgbArray.array)
        alphaArray.setArray(alpha)
        rgbArray.setArray(rgb)
    }

    override fun initRender() {
//        shaderProgram = ShaderUtil.createProgram(RenderConstant.VERTEX_SHADER, RenderConstant.FRAGMENT_SHADER)
        // 初始化VAP着色器
        shaderProgram = ShaderUtil.createProgram(RenderConstant.VERTEX_SHADER, RenderConstant.FRAGMENT_SHADER)
        uTextureLocation = GLES20.glGetUniformLocation(shaderProgram, "texture")
        aPositionLocation = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        aTextureAlphaLocation = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinateAlpha")
        aTextureRgbLocation = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinateRgb")

        // 初始化普通MP4着色器
//        normalMP4ShaderProgram = ShaderUtil.createProgram(RenderConstant.VERTEX_SHADER, FRAGMENT_SHADER_NORMAL_MP4)
        normalMP4_uTextureLocation = GLES20.glGetUniformLocation(normalMP4ShaderProgram, "texture")
        normalMP4_aPositionLocation = GLES20.glGetAttribLocation(normalMP4ShaderProgram, "vPosition")
        normalMP4_aTextureRgbLocation = GLES20.glGetAttribLocation(normalMP4ShaderProgram, "vTexCoordinateRgb")

        // 初始化VAP动画着色器
        vapShaderProgram = ShaderUtil.createProgram(RenderConstant.VERTEX_SHADER, RenderConstant.FRAGMENT_SHADER)
        vap_uTextureLocation = GLES20.glGetUniformLocation(vapShaderProgram, "texture")
        vap_aPositionLocation = GLES20.glGetAttribLocation(vapShaderProgram, "vPosition")
        vap_aTextureAlphaLocation = GLES20.glGetAttribLocation(vapShaderProgram, "vTexCoordinateAlpha")
        vap_aTextureRgbLocation = GLES20.glGetAttribLocation(vapShaderProgram, "vTexCoordinateRgb")

        // 初始化普通MP4无透明通道着色器
        normalMP4NoAlphaShaderProgram = ShaderUtil.createProgram(RenderConstant.VERTEX_SHADER, FRAGMENT_SHADER_NORMAL_MP4_NO_ALPHA)
        normalMP4NoAlpha_uTextureLocation = GLES20.glGetUniformLocation(normalMP4NoAlphaShaderProgram, "texture")
        normalMP4NoAlpha_aPositionLocation = GLES20.glGetAttribLocation(normalMP4NoAlphaShaderProgram, "vPosition")
        normalMP4NoAlpha_aTextureRgbLocation = GLES20.glGetAttribLocation(normalMP4NoAlphaShaderProgram, "vTexCoordinateRgb")

        // 初始化普通MP4有透明通道着色器
        normalMP4WithAlphaShaderProgram = ShaderUtil.createProgram(RenderConstant.VERTEX_SHADER, FRAGMENT_SHADER_NORMAL_MP4_WITH_ALPHA)
        normalMP4WithAlpha_uTextureLocation = GLES20.glGetUniformLocation(normalMP4WithAlphaShaderProgram, "texture")
        normalMP4WithAlpha_aPositionLocation = GLES20.glGetAttribLocation(normalMP4WithAlphaShaderProgram, "vPosition")
        normalMP4WithAlpha_aTextureRgbLocation = GLES20.glGetAttribLocation(normalMP4WithAlphaShaderProgram, "vTexCoordinateRgb")

        GLES20.glGenTextures(genTexture.size, genTexture, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, genTexture[0])
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    override fun renderFrame() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (surfaceSizeChanged && surfaceWidth>0 && surfaceHeight>0) {
            surfaceSizeChanged = false
            GLES20.glViewport(0,0, surfaceWidth, surfaceHeight)
        }
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

    override fun releaseTexture() {
        GLES20.glDeleteTextures(genTexture.size, genTexture, 0)
    }

    /**
     * 设置视频配置
     */
    override fun setAnimConfig(config: AnimConfig) {
        setVertexBuf(config)
        // 对于普通MP4，直接使用整个纹理坐标，不进行复杂计算
        if (config.isNormalMP4) {
            ALog.i(TAG, "Normal MP4: setting full texture coordinates")

            // 对于普通MP4无透明通道：整个纹理都是RGB，使用0到1的纹理坐标
            val fullTexCoords = floatArrayOf(
                0f, 0f,   // 左下
                1f, 0f,   // 右下
                0f, 1f,   // 左上
                1f, 1f    // 右上
            )

            // 设置RGB纹理坐标（整个纹理）
            rgbArray.setArray(fullTexCoords)

            // 设置alpha纹理坐标（使用相同的或设为0）
            // 由于是普通MP4无透明通道，alpha纹理坐标不会被使用
            alphaArray.setArray(fullTexCoords)

            ALog.i(TAG, "Normal MP4 texture coordinates set: full texture (0,0) to (1,1)")
        } else {
            setTexCoords(config)
        }
// 检查是否为普通MP4
        isNormalMP4Mode = config.isNormalMP4
        // 根据视频格式选择着色器
        when (config.videoFormat) {
            AnimConfig.FORMAT_VAP -> {
                currentShaderProgram = vapShaderProgram
                ALog.i(TAG, "setAnimConfig: Using VAP shader")
            }
            AnimConfig.FORMAT_NORMAL_MP4_NO_ALPHA -> {
                currentShaderProgram = normalMP4NoAlphaShaderProgram
                ALog.i(TAG, "setAnimConfig: Using normal MP4 (no alpha) shader")
            }
            AnimConfig.FORMAT_NORMAL_MP4_WITH_ALPHA -> {
                currentShaderProgram = normalMP4WithAlphaShaderProgram
                ALog.i(TAG, "setAnimConfig: Using normal MP4 (with alpha) shader")
            }
            else -> {
                currentShaderProgram = normalMP4NoAlphaShaderProgram
                ALog.w(TAG, "setAnimConfig: Unknown format, using default shader")
            }
        }
        ALog.i(TAG, "setAnimConfig: isNormalMP4Mode = $isNormalMP4Mode")
    }

    /**
     * 显示区域大小变化
     */
    override fun updateViewPort(width: Int, height: Int) {
        if (width <=0 || height <=0) return
        surfaceSizeChanged = true
        surfaceWidth = width
        surfaceHeight = height
    }

    override fun swapBuffers() {
        eglUtil.swapBuffers()
    }

    /**
     * mediaCodec渲染使用的
     */
    override fun getExternalTexture(): Int {
        return genTexture[0]
    }
    private fun draw() {
        when (currentShaderProgram) {
            vapShaderProgram -> drawVAP()
            normalMP4NoAlphaShaderProgram -> drawNormalMP4NoAlpha()
            normalMP4WithAlphaShaderProgram -> drawNormalMP4WithAlpha()
            else -> drawNormalMP4NoAlpha() // 默认
        }
    }
    private fun drawVAP() {
        GLES20.glUseProgram(shaderProgram)
        // 设置顶点坐标
        vertexArray.setVertexAttribPointer(aPositionLocation)
        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, genTexture[0])
        GLES20.glUniform1i(uTextureLocation, 0)

        // 设置纹理坐标
        // alpha 通道坐标
        alphaArray.setVertexAttribPointer(aTextureAlphaLocation)
        // rgb 通道坐标
        rgbArray.setVertexAttribPointer(aTextureRgbLocation)

        // draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun drawNormalMP4NoAlpha() {
        ALog.d(TAG, "drawNormalMP4NoAlpha: vertex array size=${vertexArray.array.size}, rgb array size=${rgbArray.array.size}")
        GLES20.glUseProgram(normalMP4NoAlphaShaderProgram)
        // 设置顶点坐标
        vertexArray.setVertexAttribPointer(normalMP4NoAlpha_aPositionLocation)
        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, genTexture[0])
        GLES20.glUniform1i(normalMP4NoAlpha_uTextureLocation, 0)

        // 对于普通MP4（无透明通道），我们只需要rgb纹理坐标
        rgbArray.setVertexAttribPointer(normalMP4NoAlpha_aTextureRgbLocation)
// 打印纹理坐标
        if (rgbArray.array.size >= 8) {
            ALog.d(TAG, "RGB texture coordinates: (${rgbArray.array[0]}, ${rgbArray.array[1]}) to (${rgbArray.array[6]}, ${rgbArray.array[7]})")
        }
        // draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun drawNormalMP4WithAlpha() {
        GLES20.glUseProgram(normalMP4WithAlphaShaderProgram)
        // 设置顶点坐标
        vertexArray.setVertexAttribPointer(normalMP4WithAlpha_aPositionLocation)
        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, genTexture[0])
        GLES20.glUniform1i(normalMP4WithAlpha_uTextureLocation, 0)

        // 对于普通MP4（有透明通道），整个画面是RGBA格式，使用rgb纹理坐标即可
        rgbArray.setVertexAttribPointer(normalMP4WithAlpha_aTextureRgbLocation)

        // draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
}