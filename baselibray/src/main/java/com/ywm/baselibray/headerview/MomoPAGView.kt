//package com.immomo.vchat.bb.common.widget
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.text.TextUtils
//import android.util.AttributeSet
//import androidx.annotation.Keep
//import com.bumptech.glide.Glide
//import com.bumptech.glide.load.DataSource
//import com.bumptech.glide.load.engine.DiskCacheStrategy
//import com.bumptech.glide.load.engine.GlideException
//import com.bumptech.glide.request.RequestListener
//import com.bumptech.glide.request.RequestOptions
//import com.bumptech.glide.request.target.Target
//import com.immomo.vchat.bb.common.VChatKit
//import com.immomo.vchat.bb.common.util.PathUtil
//import com.immomo.vchat.bb.common.util.common.FileUtils
//import com.immomo.vchat.bb.common.util.download.OkHttpDownloadUtil
//import com.mm.mmutil.MD5Utils
//import okhttp3.Call
//import org.libpag.PAGFile
//import org.libpag.PAGImage
//import org.libpag.PAGText
//import org.libpag.PAGView
//import java.io.File
//
///**
// * PAG支持网络下载
// * 作者:yl
// */
//open class MomoPAGView : AnimationScaleFixPAGView {
//
//    private var downloadCall: Call? = null
//
//    private var onPagFileDownloadListener: OnPagFileDownloadListener? = null
//
//    open var onPAGViewAnimationListener: OnPAGViewAnimationListener? = null
//
//    open var mPagUrl: String? = null
//
//    var onLoadPAGFileListener:OnLoadPAGFileListener? = null
//
//
//    constructor(context: Context) : super(context) {
//        initView()
//    }
//
//    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
//        initView()
//    }
//
//    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
//        context,
//        attrs,
//        defStyleAttr
//    ) {
//        initView()
//    }
//
//    private fun initView() {
//        addListener(getPagListener())
//    }
//
//    open fun getPagListener(): PAGViewListener {
//        return object : PAGViewListener {
//            override fun onAnimationStart(p0: PAGView?) {
//                onPAGViewAnimationListener?.onStart()
//            }
//
//            override fun onAnimationEnd(p0: PAGView?) {
//                onPAGViewAnimationListener?.onEnd()
//            }
//
//            override fun onAnimationCancel(p0: PAGView?) {
//                onPAGViewAnimationListener?.onEnd()
//            }
//
//            override fun onAnimationRepeat(p0: PAGView?) {
//            }
//
//            override fun onAnimationUpdate(view: PAGView?) {
//
//            }
//        }
//    }
//
//
//    open fun onLoadPAGFile(pagFile:PAGFile?) {
//        pagFile?.let {
//            onLoadPAGFileListener?.onLoadPAGFile(it)
//        }
//    }
//
//    fun loadPAGAnim(
//        pagUrl: String?,
//        elements: List<PAGDynamicEffectElement?>?,
//        repeatCount: Int = 1
//    ) {
//        mPagUrl = pagUrl
//        pagUrl ?: return
//        if (TextUtils.isEmpty(pagUrl)) {
//            onPAGViewAnimationListener?.onFail()
//            return
//        }
//        stop()
//        composition = null
//        val cacheFileName = MD5Utils.getMD5(pagUrl)?.plus(".pag")
//        val pagFilePath = PathUtil.getPagFileCacheDir() + File.separator + cacheFileName
//        val existAndNotEmpty = FileUtils.isExistAndNotEmpty(pagFilePath)
//        if (existAndNotEmpty) {
//            val pagFile = PAGFile.Load(pagFilePath)
//            if(pagFile!=null) {
//                onLoadPAGFile(pagFile)
//                composition = pagFile
//                setupElements(elements)
//                setRepeatCount(repeatCount)
//                play()
//            }
//        } else {
//            val downloadTmpFileName = "$cacheFileName.tmp"
//            if (downloadCall?.isExecuted() == true) {
//                downloadCall?.cancel()
//            }
//            onPagFileDownloadListener?.release()
//            onPagFileDownloadListener = OnPagFileDownloadListener(
//                this, pagFilePath,
//                elements, repeatCount
//            )
//            downloadCall = OkHttpDownloadUtil.get().download(
//                pagUrl, PathUtil.getPagFileCacheDir(),
//                downloadTmpFileName, onPagFileDownloadListener
//            )
//        }
//    }
//
//    fun setupElements(elements: List<PAGDynamicEffectElement?>?) {
//        elements ?: return
//        for (element in elements) {
//            element ?: return
//            if (element.type == 1) {
//                val options = if (element.disableCircleCrop != 1) {
//                    RequestOptions() //禁用磁盘缓存
//                        .diskCacheStrategy(DiskCacheStrategy.NONE) //禁用内存缓存
//                        .circleCrop()
//                        .skipMemoryCache(true)
//                } else {
//                    RequestOptions() //禁用磁盘缓存
//                        .diskCacheStrategy(DiskCacheStrategy.NONE) //禁用内存缓存
//                        .skipMemoryCache(true)
//                }
//                Glide.with(VChatKit.getAppContext())
//                    .asBitmap()
//                    .load(element.value)
//                    .apply(options).addListener(object : RequestListener<Bitmap?> {
//                        override fun onLoadFailed(
//                            e: GlideException?,
//                            o: Any,
//                            target: Target<Bitmap?>,
//                            b: Boolean
//                        ): Boolean {
//                            return false
//                        }
//
//                        override fun onResourceReady(
//                            bitmap: Bitmap?,
//                            o: Any,
//                            target: Target<Bitmap?>,
//                            dataSource: DataSource,
//                            b: Boolean
//                        ): Boolean {
//                            if (composition is PAGFile) {
//                                (composition as PAGFile).replaceImage(
//                                    element.index,
//                                    PAGImage.FromBitmap(bitmap)
//                                )
//                            }
//                            return false
//                        }
//                    }).submit()
//            } else if (element.type == 3) {
//                element.bitmap?.let {
//                    if (!it.isRecycled && composition is PAGFile) {
//                        (composition as PAGFile).replaceImage(
//                            element.index,
//                            PAGImage.FromBitmap(it)
//                        )
//                    }
//                }
//            } else {
//                if (composition is PAGFile) {
//                    val text: PAGText? = (composition as PAGFile).getTextData(element.index)
//                    if (text != null) {
//                        text.text = element.value
//                        (composition as PAGFile).replaceText(element.index, text)
//                    }
//                }
//            }
//        }
//    }
//
//    fun clearAndStopPAGAnimation() {
//        if (downloadCall?.isExecuted() == true) {
//            downloadCall?.cancel()
//        }
//        downloadCall = null
//        onPagFileDownloadListener?.release()
//        onPagFileDownloadListener = null
//        onPAGViewAnimationListener = null
//        stop()
//    }
//
//    override fun onDetachedFromWindow() {
//        super.onDetachedFromWindow()
//        if (downloadCall?.isExecuted() == true) {
//            downloadCall?.cancel()
//        }
//        downloadCall = null
//        onPagFileDownloadListener?.release()
//        onPagFileDownloadListener = null
//    }
//
//    class OnPagFileDownloadListener(
//        private var momoPagView: MomoPAGView? = null,
//        private var pagFilePath: String? = null,
//        private var elements: List<PAGDynamicEffectElement?>? = null,
//        private var repeatCount: Int = 1
//    ) : OkHttpDownloadUtil.OnDownloadListener {
//
//        override fun onDownloadSuccess(file: File) {
//            if (pagFilePath != null && FileUtils.rename(file.absolutePath, pagFilePath)) {
//                momoPagView?.post {
//                    val pagFile = PAGFile.Load(pagFilePath)
//                    momoPagView?.onLoadPAGFile(pagFile)
//                    momoPagView?.composition = pagFile
//                    elements?.let {
//                        momoPagView?.setupElements(it)
//                    }
//                    momoPagView?.setRepeatCount(repeatCount)
//                    momoPagView?.play()
//                }
//            } else {
//                momoPagView?.post {
//                    momoPagView?.onPAGViewAnimationListener?.onFail()
//                }
//            }
//        }
//
//        override fun onDownloading(progress: Int) {}
//
//        override fun onDownloadFailed(e: java.lang.Exception?) {
//            momoPagView?.post {
//                momoPagView?.onPAGViewAnimationListener?.onFail()
//            }
//        }
//
//        override fun onDownloadStart() {}
//
//        fun release() {
//            momoPagView = null
//            pagFilePath = null
//            elements = null
//        }
//    }
//
//    interface OnPAGViewAnimationListener {
//
//        fun onStart()
//
//        fun onEnd()
//
//        fun onFail()
//    }
//
//    fun pagVoice() {
//        if (composition != null && composition is PAGFile) {
//            (composition as PAGFile).audioBytes()
//        }
//    }
//}
//
//interface OnLoadPAGFileListener {
//    fun onLoadPAGFile(pagFile:PAGFile)
//}
//
//@Keep
//data class PAGDynamicEffectElement(
//    var index: Int = 0,
//    var value: String? = null,
//    var type: Int? = 0,
//    var disableCircleCrop:Int = 0,
//    var bitmap: Bitmap? = null
//) {
//    override fun toString(): String {
//        return "PAGDynamicEffectElement(index=$index, value=$value, type=$type)"
//    }
//}