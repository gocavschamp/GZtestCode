//package com.dd.base.svga
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.Typeface
//import android.os.Handler
//import android.text.TextPaint
//import android.text.TextUtils
//import android.util.AttributeSet
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.LifecycleObserver
//import androidx.lifecycle.OnLifecycleEvent
//import androidx.lifecycle.lifecycleScope
//import com.bumptech.glide.Glide
//import com.dd.base.MainClassUtils
//import com.dd.base.devicejudge.DeviceJudgeScore
//import com.dd.base.ktextension.findFragmentActivity
//import com.immomo.module_log.LogUtils
//import com.immomo.svgaplayer.*
//import com.immomo.svgaplayer.adaptercallback.SVGAImgLoadCallBack
//import com.immomo.svgaplayer.adaptercallback.SVGAResLoadCallBack
//import com.immomo.svgaplayer.bean.*
//import com.immomo.svgaplayer.listener.SVGAClickAreaListener
//import com.immomo.svgaplayer.setting.SVGAAdapterContainer
//import com.immomo.svgaplayer.setting.SVGAEntityCacheLoader
//import com.immomo.vchat.bb.common.ktextension.dp
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import org.json.JSONObject
//import java.io.File
//import java.net.URL
//
///**
// * Created by miaojun on 2018/11/30.
// *
// */
//open class AmarSVGAImageView : ClickSVGAImageView, SVGAParser.ParseCompletion, LifecycleObserver {
//
//    companion object {
//        @JvmField
//        var blackListSvga: ArrayList<String?> = arrayListOf()
//    }
//
//    private var mResourceUrl: String? = null
//    private var mSVGAParser: SVGAParser? = null
//    private var mStopPlay: Boolean = false
//
//    private val mInsertImgSimList: MutableList<InsertImgBean> = mutableListOf()
//    private val mInsertTextSimList: MutableList<InsertTextBean> = mutableListOf()
//    private val mClickGoto: HashMap<String, String> = hashMapOf()
//    private var startFrame: Int = 0
//    private var stepToPercentage: Double = Double.NaN
//    protected var autoPlay = true
//    private var loadStart = false
//    private val lowLevelAnim: Boolean
//
//    var removeLifecycleObserverByDetachToWindow = false
//
//    constructor(context: Context) : super(context) {
//        initView()
//        lowLevelAnim = !DeviceJudgeScore.apngCanDoAnim(context, false)
//    }
//
//    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
//        initView()
//        lowLevelAnim = !DeviceJudgeScore.apngCanDoAnim(context, false)
//    }
//
//    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
//        context,
//        attrs,
//        defStyleAttr
//    ) {
//        initView()
//        lowLevelAnim = !DeviceJudgeScore.apngCanDoAnim(context, false)
//    }
//
//    private fun initView() {
//        if (context is AppCompatActivity && "class ${MainClassUtils.MAIN_ACTIVITY_CLASS_FULL_NAME}" != context.javaClass.toString()) {
//            (context as AppCompatActivity).lifecycle.addObserver(this)
//        }
//        removerOb()
//    }
//    fun removerOb(){
//        removeLifecycleObserverByDetachToWindow = true
//    }
//
//    /**
//     * 最简单的直接执行动画
//     */
//    fun startSVGAAnim(url: String?, loop: Int) {
//        startSVGAAnimWithListener(url, loop, null)
//    }
//
//    /**
//     * 执行动画&关心动画回调
//     */
//    fun startSVGAAnimWithListener(url: String?, loop: Int, animListener: SVGAAnimListenerAdapter?) {
//        loadSVGAAnimWithListener(url, loop, animListener, true)
//    }
//
//    open fun loadSVGAAnimWithListener(
//        url: String?,
//        loop: Int,
//        animListener: SVGAAnimListenerAdapter?,
//        autoPlay: Boolean
//    ) {
//        if (isAnimating) {
//            stopAnimation()
//        }
//        mStopPlay = false
//        loops = loop
//        mResourceUrl = url
//        this.autoPlay = autoPlay
//        setCallback(animListener)
//        loadSVGA()
//    }
//
//    fun startSVGAAnimAndStepToFrame(
//        url: String?,
//        loop: Int,
//        animListener: SVGAAnimListenerAdapter?,
//        startFrame: Int
//    ) {
//        startSVGAAnimWithListener(url, loop, animListener)
//        this.startFrame = startFrame
//    }
//
//    fun startSVGAAnimAndStepToPercentage(
//        url: String?,
//        loop: Int,
//        animListener: SVGAAnimListenerAdapter?,
//        stepToPercentage: Double
//    ) {
//        startSVGAAnimWithListener(url, loop, animListener)
//        this.stepToPercentage = stepToPercentage
//    }
//
//    fun insertClickArea(
//        clickKey: String?,
//        itemClickAreaListener: SVGAClickAreaListener?
//    ): AmarSVGAImageView {
//        clickKey?.let {
//            mClickKeyList.add(clickKey)
//
//        }
//        itemClickAreaListener?.let {
//            mItemClickAreaListener = it
//        }
//
//        return this
//    }
//
//    fun insertClickArea(
//        clickKeyList: List<String>?,
//        itemClickAreaListener: SVGAClickAreaListener?
//    ): AmarSVGAImageView {
//        clickKeyList?.let {
//            mClickKeyList.addAll(clickKeyList)
//        }
//        itemClickAreaListener?.let {
//            mItemClickAreaListener = it
//        }
//        return this
//    }
//
//    private fun insertClickArea(insertClickBean: BaseInsertBean?): AmarSVGAImageView {
//        insertClickBean?.let {
//            if (!TextUtils.isEmpty(it.key) && !TextUtils.isEmpty(it.action)) {
//                mClickKeyList.add(it.key)
//                mClickGoto[it.key] = it.action
//            }
//        }
//
//        return this
//    }
//
//    private fun insertImgBean(imgBean: InsertImgBean?): AmarSVGAImageView {
//        imgBean?.let {
//            mInsertImgSimList.add(it)
//            if (it.isClick) {
//                insertClickArea(it)
//            }
//        }
//        return this
//    }
//
//    private fun insertTextBean(textBean: InsertTextBean?): AmarSVGAImageView {
//        textBean?.let {
//            mInsertTextSimList.add(it)
//            if (it.isClick) {
//                insertClickArea(it)
//            }
//        }
//        return this
//    }
//
//    fun insertBean(bean: BaseInsertBean?): AmarSVGAImageView {
//        if (bean is InsertClickBean) {
//            insertClickArea(bean)
//        }
//        if (bean is InsertImgBean) {
//            insertImgBean(bean)
//        }
//        if (bean is InsertTextBean) {
//            insertTextBean(bean)
//        }
//        return this
//    }
//
//    fun insertBeanList(beanList: List<BaseInsertBean>?): AmarSVGAImageView {
//        beanList?.let {
//            for (bean in it) {
//                insertBean(bean)
//            }
//        }
//        return this
//    }
//
//    /**
//     * 用于替换文本为镜像位图的方法
//     * 当整个 SVGA 被翻转时，文本需要预先翻转才能正常显示
//     */
//    fun replaceTextWithFlippedBitmap(key: String, text: String, viewWidth : Int,viewHeight : Int,textSize: Float, textColor: Int, isBold: Boolean = true) {
//        try {
//            val scaledTextSize = textSize.dp()
//
//            // 创建文本位图
//            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
//                color = textColor
//                this.textSize = scaledTextSize
//                typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
//            }
//
//            val bounds = android.graphics.Rect()
//            paint.getTextBounds(text, 0, text.length, bounds)
//
//            val bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
//            val canvas = android.graphics.Canvas(bitmap)
//
//            // 计算文本绘制位置，让文本在位图中居中
//            // 计算文本绘制位置，让文本在位图中居中
//            val x = viewWidth / 2f
//            val y = viewHeight / 2f - (paint.descent() + paint.ascent()) / 2f
//            canvas.drawText(text, x, y, paint.apply { textAlign = Paint.Align.CENTER })
//            LogUtils.e("AmarSVGAImageView", "replaceTextWithFlippedBitmap scaledTextSize:$textSize x:$x y:$y width:$viewWidth height:$viewHeight")
//
//            // 水平镜像位图
//            val matrix = android.graphics.Matrix().apply { preScale(-1f, 1f) }
//            val flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, viewWidth, viewHeight, matrix, true)
//
//            // 设置到动态实体
//            mSVGAEntity?.setDynamicImage(key, flippedBitmap)
//        } catch (e: Exception) {
//            LogUtils.e("AmarSVGAImageView", "replaceTextWithFlippedBitmap error: ${e.message}")
//        }
//    }
//
//    fun startSVGAAnimWithJson(
//        jsonStr: String?,
//        loop: Int,
//        itemClickAreaListener: SVGAClickAreaListener?,
//        animListener: SVGAAnimListenerAdapter?
//    ) {
//        if (TextUtils.isEmpty(jsonStr)) {
//            onError(ErrorConstant.ERROR_MMSVGA_JSON)
//            return
//
//        }
//        var jsonObject: JSONObject? = null
//        try {
//            jsonObject = JSONObject(jsonStr)
//        } catch (e: Exception) {
//            onError(ErrorConstant.ERROR_MMSVGA_JSON)
//        }
//        jsonObject?.let {
//            startSVGAAnimWithJson(it, loop, itemClickAreaListener, animListener)
//        }
//    }
//
//    fun startSVGAAnimWithJson(
//        jsonObject: JSONObject?,
//        loop: Int,
//        itemClickAreaListener: SVGAClickAreaListener?,
//        animListener: SVGAAnimListenerAdapter?
//    ) {
//        if (jsonObject == null) {
//            onError(ErrorConstant.ERROR_MMSVGA_JSON)
//            return
//        }
//        val templateUrl = jsonObject.optString("templateUrl")
//        val itemList = jsonObject.optJSONArray("itemslist")
//        if (!TextUtils.isEmpty(templateUrl)) {
//            if (itemList != null && itemList.length() != 0) {
//                for (itemIndex in 0 until itemList.length()) {
//                    val itemJson = itemList.optJSONObject(itemIndex)
//                    if (itemJson != null && !TextUtils.isEmpty(itemJson.optString("key"))) {
//                        if (itemJson.optBoolean("isClick")) {
//                            itemClickAreaListener?.let {
//                                mItemClickAreaListener = it
//                                mClickKeyList.add(itemJson.optString("key"))
//                            } ?: let {
//                                mClickGoto[itemJson.optString("key")] = itemJson.optString("action")
//                            }
//                        }
//
//                        if (itemJson.optInt("type") == 2 && !TextUtils.isEmpty(itemJson.optString("imageUrl"))) {
//                            val imgBean = InsertImgBean()
//                            imgBean.key = itemJson.optString("key")
//                            imgBean.imgUrl = itemJson.optString("imageUrl")
//                            imgBean.isCircle = itemJson.optBoolean("isCircle")
//                            imgBean.radius = itemJson.optInt("radius")
//                            imgBean.corner.bitmapFilletCorner = itemJson.optInt("corner")
//                            mInsertImgSimList.add(imgBean)
//                            continue
//                        }
//
//                        if (itemJson.optInt("type") == 1 && !TextUtils.isEmpty(itemJson.optString("text"))) {
//                            val textBean = InsertTextBean()
//                            textBean.key = itemJson.optString("key")
//                            textBean.text = itemJson.optString("text")
//                            textBean.textColor = Color.parseColor(itemJson.optString("textColor"))
//                            textBean.textSize = itemJson.optInt("textSize").toFloat()
//                            textBean.isBold = itemJson.optBoolean("isBold")
//                            textBean.textAlignType = itemJson.optInt("textAlignType")
//                            textBean.singleLine = itemJson.optBoolean("singleLine")
//                            textBean.ellipsize = itemJson.optInt("ellipsize")
//                            mInsertTextSimList.add(textBean)
//                        }
//                    }
//                }
//            }
//            startSVGAAnimWithListener(templateUrl, loop, animListener)
//        } else {
//            onError(ErrorConstant.ERROR_MMSVGA_RESURL)
//        }
//    }
//
//    fun insertDrawerGoto(gotoMap: HashMap<String, String>) {
//        if (gotoMap.size != 0) {
//            mClickKeyList.addAll(gotoMap.keys)
//            mItemClickAreaListener = object : SVGAClickAreaListener {
//                override fun onClick(clickKey: String) {
//                    gotoMap[clickKey]?.let {
//                        SVGAAdapterContainer.mSVGAGotoAdapter?.executeGoto(context, clickKey, it)
//                    }
//                }
//            }
//        }
//    }
//
//    private fun loadSVGA() {
//        loadStart = true
//        if (TextUtils.isEmpty(mResourceUrl)) {
//            onError(ErrorConstant.ERROR_MMSVGA_RESURL)
//            return
//        }
//
//        mSVGAParser ?: context?.let {
//            mSVGAParser = SVGAParser(it)
//        }
//
//        mSVGAEntity?.clearDynamicObjects() ?: let {
//            mSVGAEntity = SVGADynamicEntity()
//        }
//
//        insertDrawerImg(mInsertImgSimList)
//
//        insertDrawerText(mInsertTextSimList)
//
//        insertDrawerGoto(mClickGoto)
//
//        setClickArea()
//
//        mResourceUrl?.let { it ->
//            SVGAEntityCacheLoader.get().getEntity(it)?.let { svgaVideoEntity ->
//                onComplete(svgaVideoEntity)
//                return
//            }
//            when {
//                it.startsWith("http") -> {
//                    loadNetSVGA(it)
//                }
//
//                fileIsExists(it) -> {
//                    loadLocalResource(it)
//                }
//
//                else -> {
//                    mSVGAParser?.parse("svga/${it}", this)
//                }
//            }
//        }
//
//
//    }
//
//    private fun loadNetSVGA(url: String) {
//        SVGAAdapterContainer.mSVGAResLoadAdapter?.loadSVGARes(
//            true,
//            url,
//            object : SVGAResLoadCallBack {
//                override fun onResLoadSuccess(filePath: String) {
//                    LogUtils.d("SVGAParser", "onResLoadSuccess")
//                    loadLocalResource(filePath)
//                }
//
//                override fun onResLoadFail() {
//                    Handler(context.mainLooper).post {
//                        onError(ErrorConstant.ERROR_MMSVGA_RES_REMOTE_LOAD)
//                    }
//                }
//            }) ?: let {
//            mSVGAParser?.parse(URL(url), this)
//        }
//
//    }
//
//    private fun loadLocalResource(path: String) {
//        mSVGAParser?.parseFile(path, this@AmarSVGAImageView, true)
//    }
//
//    private fun fileIsExists(path: String): Boolean {
//        try {
//            val file = File(path)
//            if (!file.exists()) {
//                return false
//            }
//        } catch (e: Exception) {
//            return false
//        }
//        return true
//    }
//
//    fun insertDrawerImg(imgList: MutableList<InsertImgBean>) {
//        for (insertImgBean in imgList) {
//            if (!TextUtils.isEmpty(insertImgBean.key) && !TextUtils.isEmpty(insertImgBean.imgUrl)) {
//
//                if (insertImgBean.isCircle) {
//                    // 这个东西load出来的bitmap是圆的
//                    SVGAAdapterContainer.mSVGAImgLoadAdapter?.loadSVGAImg(
//                        insertImgBean.imgUrl,
//                        object : SVGAImgLoadCallBack {
//                            override fun onImgLoadSuccess(bitmap: Bitmap) {
//                                mSVGAEntity?.setDynamicCircleImage(insertImgBean.key, bitmap)
//                            }
//
//                            override fun onImgLoadFail() {
//                                //图片加载失败（暂不处理）
//                            }
//                        }) ?: let {
//                        mSVGAEntity?.setDynamicImage(insertImgBean.imgUrl, insertImgBean.key)
//                    }
//                } else {
//                    (context.findFragmentActivity()?.lifecycleScope ?: CoroutineScope(Dispatchers.Main)).launch {
//                        kotlin.runCatching {
//                            val bitmap = withContext(Dispatchers.IO) {
//                                Glide.with(context).asBitmap().load(insertImgBean.imgUrl).submit().get()
//                            }
//                            mSVGAEntity?.setDynamicRadiusImage(insertImgBean.key, bitmap, insertImgBean.radius, insertImgBean.corner)
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    fun insertDrawerText(textList: MutableList<InsertTextBean>) {
//        for (textBean in textList) {
//            if (!TextUtils.isEmpty(textBean.key) && !TextUtils.isEmpty(textBean.text)) {
//                val textPaint = TextPaint()
//                textPaint.typeface = textBean.typeFace
//                textPaint.color = textBean.textColor
//                textPaint.textSize = if (lowLevelAnim) textBean.textSize / 2 else textBean.textSize
//                if (textBean.isBold) {
//                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
//                }
//
//                if (textBean.singleLine) {
//                    mSVGAEntity?.setDynamicText(
//                        textBean.key,
//                        BoringLayoutBean(
//                            textBean.text,
//                            textPaint,
//                            textBean.getAlignType(),
//                            textBean.getEllipsize()
//                        )
//                    )
//                } else {
//                    mSVGAEntity?.setDynamicText(
//                        textBean.key,
//                        StaticLayoutBean(textBean.text, textPaint, textBean.getAlignType())
//                    )
//                }
//
//            }
//        }
//    }
//
//    //解析完成
//    override fun onComplete(videoItem: SVGAVideoEntity) {
//        LogUtils.d("SVGAParser", "onComplete")
//
////        记录sprite size超过500的
////        if (videoItem.sprites.size > 500) {
////            if (!blackListSvga.contains(mResourceUrl)) {
////                blackListSvga.add(mResourceUrl)
////            }
////            LogCacheDataHelper.addNewLog("SVGA_LOG", mResourceUrl ?: "", false)
////            //onError("")
////            //return
////        }
//
//        if (mStopPlay) {
//            return
//        }
//        loadStart = false
//        mResourceUrl?.let {
//            SVGAEntityCacheLoader.get().addCache(it, videoItem)
//        }
//        mSVGAEntity?.let {
//            SVGADrawable(videoItem, it)
//        }?.let {
//            setImageDrawable(it)
//            getCallBack()?.onLoadSuccess(videoItem)
//            if (!autoPlay) {
//                it.cleared = false
//                return
//            }
//            if (startFrame != 0) {
//                stepToFrame(startFrame, true)
//            } else if (!stepToPercentage.isNaN()) {
//                stepToPercentage(stepToPercentage, true)
//            } else {
//                startAnimation()
//            }
//        }
//    }
//
//    //解析失败
//    override fun onError(errorMsg: String) {
//        LogUtils.d("SVGAParser", "onError")
//
//        if (loadStart) {
//            getCallBack()?.loadResError(errorMsg)
//            loadStart = false
//        }
//    }
//
//    /**
//     * 同一view设置多个不同的动画且存在相同key
//     * 需要清空
//     */
//    override fun clearInsertData() {
//        super.clearInsertData()
//        mInsertTextSimList.clear()
//        mInsertImgSimList.clear()
//        mClickGoto.clear()
//    }
//
//    fun stopAnimCompletely() {
//        mStopPlay = true
//        stopAnimation(true)
//    }
//
//    override fun onAttachedToWindow() {
//        super.onAttachedToWindow()
//    }
//
//    override fun onDetachedFromWindow() {
//        if (removeLifecycleObserverByDetachToWindow) {
//            (context as? AppCompatActivity)?.lifecycle?.apply {
//                kotlin.runCatching {
//                    removeObserver(this@AmarSVGAImageView)
//                }
//            }
//        }
//        clearInsertData()
//        mSVGAParser?.onDestroy()
//        super.onDetachedFromWindow()
//        stopAnimCompletely()
//        clearAnimation()
//        clearAnimatorListeners()
//    }
//
//    /**
//     * Releases all resources associated with this AmarSVGAImageView.
//     *
//     * This function performs the following cleanup operations:
//     * - Stops any ongoing animation
//     * - Clears the image drawable
//     * - Removes any callback
//     * - Clears inserted data
//     * - Clears any ongoing animation
//     * - Removes all animator listeners
//     *
//     * This should be called when the view is no longer needed to prevent memory leaks and ensure proper cleanup.
//     */
//    private fun release() {
//        stopAnimCompletely()
//        setImageDrawable(null)
//        setCallback(null)
//        clearInsertData()
//        clearAnimation()
//        clearAnimatorListeners() // 清除动画监听器
//    }
//
//    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
//    fun onActivityDestroy() {
//        (context as? AppCompatActivity)?.lifecycle?.apply {
//            kotlin.runCatching {
//                removeObserver(this@AmarSVGAImageView)
//            }
//        }
//        if (drawable != null && drawable is com.immomo.svgaplayer.SVGADrawable) {
//            stopAnimCompletely()
//        }
//        clearAnimatorListeners() // 清除动画监听器
//    }
//
//}
