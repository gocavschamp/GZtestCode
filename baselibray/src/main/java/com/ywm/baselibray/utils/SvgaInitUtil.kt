package com.ywm.baselibray.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.toColorInt
import com.blankj.utilcode.util.LogUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.opensource.svgaplayer.SVGACallback
import com.opensource.svgaplayer.SVGADrawable
import com.opensource.svgaplayer.SVGADynamicEntity
import com.opensource.svgaplayer.SVGAImageView
import com.opensource.svgaplayer.SVGAParser
import com.opensource.svgaplayer.SVGAParser.ParseCompletion
import com.opensource.svgaplayer.SVGAParser.PlayCallback
import com.opensource.svgaplayer.SVGAVideoEntity
import com.ywm.baselibray.weiget.GlideCircleTransform
import java.io.File

/**
 *
 * @Date 2024/8/1
 * @projectName VideoChat
 * @description：
 */
class SvgaInitUtil {
    fun initSvga(
        fileName: String, context: Context, svgaImageView: SVGAImageView?, onComplete: ()->Unit) {
        val svgaParser = SVGAParser(context)
        svgaParser.init(context)
        svgaParser.decodeFromAssets(fileName, object : ParseCompletion {
            override fun onError() {
                LogUtils.e("SVGAERR")
            }

            override fun onComplete(svgaVideoEntity: SVGAVideoEntity) {
                val svgaDrawable = SVGADrawable(svgaVideoEntity)
                svgaImageView?.setImageDrawable(svgaDrawable)
                onComplete.invoke()
                // audio_anim_iv.startAnimation();
            }
        }, object : PlayCallback {

            override fun onPlay(file: List<File>) {
            }
        })
    }
    fun initSvgaWithListener(
        fileName: String, context: Context, svgaImageView: SVGAImageView?, onComplete: (parseSuccess: Boolean)->Unit) {
        val svgaParser = SVGAParser(context)
        svgaParser.init(context)
        svgaParser.decodeFromAssets(fileName, object : ParseCompletion {
            override fun onError() {
                LogUtils.e("SVGA Error")
                onComplete.invoke(false)
            }

            override fun onComplete(svgaVideoEntity: SVGAVideoEntity) {
                val svgaDrawable = SVGADrawable(svgaVideoEntity)
                svgaImageView?.setImageDrawable(svgaDrawable)
                onComplete.invoke(true)
                // audio_anim_iv.startAnimation();
            }
        }, object : PlayCallback {

            override fun onPlay(file: List<File>) {
            }
        })
    }
    fun initSvgaWithInfo(
        fileName: String, context: Context, svgaImageView: SVGAImageView?,
        avatarUrl: String?,imgKey: String?,
        text1: String?,text1Key: String?,
        text2: String?,text2Key: String?,
        onComplete: (parseSuccess: Boolean)->Unit) {
        val svgaParser = SVGAParser(context)
        svgaParser.init(context)
        svgaParser.decodeFromAssets(fileName, object : ParseCompletion {
            override fun onError() {
                LogUtils.e("SVGA Error")
                onComplete.invoke(false)
            }

            override fun onComplete(svgaVideoEntity: SVGAVideoEntity) {
                val dynamicEntity = SVGADynamicEntity()
                Glide.with(context)
                    .asBitmap() // 指定加载为 Bitmap
                    .load(avatarUrl?:"")
                    .transform(
                        GlideCircleTransform(context)
                    ) // 图片 URL
                    .into(object : CustomTarget<Bitmap?>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap?>?
                        ) {
                            imgKey?.let {
                                dynamicEntity.setDynamicImage(resource, imgKey)
                            }
                            val textPaint = TextPaint()
                            textPaint.setColor("#222222".toColorInt()) //字体颜色
                            textPaint.textSize = UIUtils.sp2pix(10f).toFloat() //字体大小
                               val textPaint2 = TextPaint()
                            textPaint2.setColor(Color.WHITE) //字体颜色
                            textPaint2.textSize = UIUtils.sp2pix(10f).toFloat() //字体大小
                            val staticLayout = StaticLayout.Builder.obtain(
                                text1 ?: "",
                                0,
                                text1?.length ?: 0,
                                textPaint,
                                UIUtils.getPixels(120f)
                            ).setText(text1 ?: "")
                                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                                .setEllipsize(TextUtils.TruncateAt.MARQUEE)
//                                .setEllipsizedWidth(UIUtils.getPixels(120f))
                                .setIncludePad(false)
                                .setLineSpacing(0f,1f)
                                .setMaxLines(1).build()
                            val staticLayout2 = StaticLayout.Builder.obtain(
                                text2 ?: "",
                                0,
                                text1?.length ?: 0,
                                textPaint2,
                                UIUtils.getPixels(200f)
                            ).setText(text2 ?: "")
                                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                                .setEllipsize(TextUtils.TruncateAt.MARQUEE)
//                                .setEllipsizedWidth(UIUtils.getPixels(200f))
                                .setIncludePad(false)
                                .setLineSpacing(0f,1f)
                                .setMaxLines(1).build()
//                            val staticLayout = StaticLayout(
//                                text1?:"",
//                                textPaint,
//                                UIUtils.getPixels(60f),
//                                Layout.Alignment.ALIGN_NORMAL,
//                                1.0f,
//                                0.0f,
//                                false
//                            )
//                            val staticLayout2 = StaticLayout(
//                                text1?:"",
//                                textPaint2,
//                                UIUtils.getPixels(60f),
//                                Layout.Alignment.ALIGN_CENTER,
//                                1.0f,
//                                0.0f,
//                                false
//                            )
                            dynamicEntity.setDynamicText(staticLayout, text1Key?:"")
                            dynamicEntity.setDynamicText(staticLayout2, text2Key?:"")
//                            dynamicEntity.setDynamicText(text1?:"",textPaint, text1Key?:"")
//                            dynamicEntity.setDynamicText(text2?:"",textPaint, text2Key?:"")
                            val svgaDrawable = SVGADrawable(svgaVideoEntity,dynamicEntity)
//                            val svgaDrawable = SVGADrawable(svgaVideoEntity)
                            svgaImageView?.setImageDrawable(svgaDrawable)
                            svgaImageView?.stepToFrame(0,false)
                            onComplete.invoke(true)
                svgaImageView?.startAnimation()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })
            }
        }, object : PlayCallback {

            override fun onPlay(file: List<File>) {
                LogUtils.e("SVGA onPlay ${file.size} ${file.toString()}")

            }
        })
    }
    fun addPlayListener(svgaImageView: SVGAImageView?, listener: SVGACallback?) {
        svgaImageView?.callback = listener
    }
}
