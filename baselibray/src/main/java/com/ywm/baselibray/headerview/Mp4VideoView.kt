//package com.dd.base.weight
//
//import android.content.Context
//import android.graphics.Matrix
//import android.graphics.SurfaceTexture
//import android.graphics.drawable.Drawable
//import android.media.MediaPlayer
//import android.text.TextUtils
//import android.util.AttributeSet
//import android.view.Surface
//import android.view.TextureView
//import com.bumptech.glide.Glide
//import com.bumptech.glide.request.target.CustomViewTarget
//import com.bumptech.glide.request.transition.Transition
//import com.dd.base.utils.LogEventUtils
//import com.immomo.android.audio_room_biz.util.safe
//import java.io.File
//
///**
// * 赫兹MP4 view 输入url 播放 带缓存下载
// * @author MOMO
// */
//class Mp4VideoView : TextureView, TextureView.SurfaceTextureListener,
//    MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnPreparedListener {
//
//    /**
//     * MP4文件本地路径
//     */
//    private var mp4FilePath: String? = null
//
//    /**
//     * 是否循环播放
//     */
//    private var videoLoop: Boolean? = true
//
//    /**
//     * 是否自动调整视频播放尺寸
//     */
//    private var autoAdjustVideoSize: Boolean? = true
//
//    /**
//     * MediaPlayer
//     */
//    private val mediaPlayer by lazy {
//        MediaPlayer()
//    }
//
//    /**
//     * surfaceTexture width
//     */
//    private var surfaceViewWidth = 0
//
//    /**
//     * surfaceTexture height
//     */
//    private var surfaceViewHeight = 0
//
//    /**
//     * TextureView的SurfaceTexture
//     */
//    private var surfaceTexture: Surface? = null
//
//    /**
//     * 视频是否静音
//     */
//    var isMuteVideoAudioEffect = false
//
//    /**
//     * 播放状态回调监听
//     */
//    var mp4VideoViewListener: OnMp4VideoViewListener? = null
//
//    constructor(context: Context) : super(context) {
//        initView()
//    }
//
//    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
//        initView()
//    }
//
//    constructor(context: Context, attributeSet: AttributeSet?, defaultStyleAttr: Int) : super(
//        context,
//        attributeSet,
//        defaultStyleAttr
//    ) {
//        initView()
//    }
//
//    private fun initView() {
//        surfaceTextureListener = this
//    }
//
//    override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int) {
//        if (autoAdjustVideoSize == true) {
//            //适配尺寸变化
//            surfaceViewWidth = width
//            surfaceViewHeight = height
//            val videoWidth = mp?.videoWidth ?: 0
//            val videoHeight = mp?.videoHeight ?: 0
//            //调整视频缩放尺寸
//            adjustAspectRatio(videoWidth, videoHeight, width, height)
//        }
//    }
//
//    override fun onPrepared(mp: MediaPlayer?) {
//        //准备好了开始播放
//        refreshMediaPlayerMuteStatus()
//        val videoWidth = mp?.videoWidth ?: 0
//        val videoHeight = mp?.videoHeight ?: 0
//        //调整视频缩放尺寸
//        adjustAspectRatio(
//            videoWidth,
//            videoHeight,
//            surfaceViewWidth,
//            surfaceViewHeight
//        )
//        //设置是否循环播放
//        mediaPlayer.isLooping = videoLoop ?: true
//        //开始播放
//        mediaPlayer.start()
//        //回调状态
//        mp4VideoViewListener?.mp4VideoPlayState(success = true)
//    }
//
//    override fun onSurfaceTextureAvailable(
//        surface: SurfaceTexture,
//        width: Int,
//        height: Int
//    ) {
//        surfaceViewWidth = width
//        surfaceViewHeight = height
//        surfaceTexture = Surface(surface)
//        prepareAndStartPlayVideo()
//    }
//
//    override fun onSurfaceTextureSizeChanged(
//        surface: SurfaceTexture,
//        width: Int,
//        height: Int
//    ) {
//
//    }
//
//    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//        return false
//    }
//
//    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//
//    }
//
//    override fun onDetachedFromWindow() {
//        super.onDetachedFromWindow()
//        stopPlayVideo(true)
//    }
//
//    private fun stopPlayVideo(releaseSurface:Boolean = false) {
//        kotlin.runCatching {
//            mediaPlayer.setOnPreparedListener(null)
//            mediaPlayer.setOnVideoSizeChangedListener(null)
//            mediaPlayer.stop()
//            if (releaseSurface) {
//                surfaceTexture?.release()
//                surfaceTexture = null
//            }
//        }.onFailure {
//            it.printStackTrace()
//        }
//    }
//
//    /**
//     * @param mp4Url mp4网络地址
//     * @param videoLoop 是否循环播放
//     * @param autoAdjustVideoSize 是否自适应调整view的大小到 视频的尺寸 默认是
//     * @param isStaticFrame 是否是静态帧
//     * @param callback 静态帧回调
//     */
//    fun startPlayNetVideo(
//        mp4Url: String?,
//        videoLoop: Boolean? = true,
//        autoAdjustVideoSize: Boolean? = true,
//        isStaticFrame: Boolean? = false,
//        callback: ((path: String) -> Unit)? = null
//    ) {
//        stopPlayVideo()
//
//        this.videoLoop = videoLoop
//        this.autoAdjustVideoSize = autoAdjustVideoSize
//        //Glide下载播放
//        Glide.with(this).asFile().load(mp4Url)
//            .into(object : CustomViewTarget<Mp4VideoView, File>(this) {
//                override fun onLoadFailed(errorDrawable: Drawable?) {
//                    //下载资源失败
//                    mp4VideoViewListener?.mp4VideoPlayState(false)
//                    LogEventUtils.logEvent(
//                        "0-203",
//                        "error" to "download fail",
//                        "url" to mp4Url.safe()
//                    )
//                }
//
//                override fun onResourceCleared(placeholder: Drawable?) {
//                }
//
//                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
//                    if (resource.exists()) {
//                        if (isStaticFrame == true) {
//                            callback?.invoke(resource.absolutePath)
//                        }else{
//                            playVideoByInternal(resource.absolutePath)
//                        }
//                    } else {
//                        //下载资源失败
//                        mp4VideoViewListener?.mp4VideoPlayState(false)
//
//                        LogEventUtils.logEvent(
//                            "0-203",
//                            "error" to "file not exist",
//                            "url" to mp4Url.safe()
//                        )
//                    }
//                }
//            })
//    }
//
//    /**
//     * 播放本地视频
//     */
//    private fun playVideoByInternal(localPathMp4: String) {
//        kotlin.runCatching {
//            mp4FilePath = localPathMp4
//            mp4VideoViewListener?.localMp4FilePath(localPathMp4)
//            prepareAndStartPlayVideo()
//        }.onFailure {
//            mp4VideoViewListener?.mp4VideoPlayState(success = false)
//            LogEventUtils.logEvent(
//                "0-203",
//                "error" to "play error",
//                "url" to localPathMp4.safe()
//            )
//        }
//    }
//
//    /**
//     * 初始化并播放MediaPlayer
//     */
//    private fun prepareAndStartPlayVideo() {
//        if (!TextUtils.isEmpty(mp4FilePath) && surfaceTexture != null) {
//            kotlin.runCatching {
//                mediaPlayer.reset()
//                mediaPlayer.setDataSource(mp4FilePath)
//                mediaPlayer.setSurface(surfaceTexture)
//                mediaPlayer.setOnVideoSizeChangedListener(this)
//                mediaPlayer.setOnPreparedListener(this)
//                mediaPlayer.prepareAsync()
//            }.onFailure {
//                mp4VideoViewListener?.mp4VideoPlayState(success = false)
//                LogEventUtils.logEvent(
//                    "0-203",
//                    "error" to "init player fail",
//                )
//            }
//        }
//    }
//
//    /**
//     * 缩放视频，保证是全屏播放
//     */
//    private fun adjustAspectRatio(
//        videoWidth: Int,
//        videoHeight: Int,
//        viewWidth: Int,
//        viewHeight: Int
//    ) {
//        val aspectRatio = videoHeight.toFloat() / videoWidth
//        val viewRatio = viewHeight.toFloat() / viewWidth
//        var scaleX = 1.0f
//        var scaleY = 1.0f
//        if (aspectRatio > viewRatio) {
//            scaleX = viewRatio / aspectRatio
//        } else {
//            scaleY = aspectRatio / viewRatio
//        }
//        val matrix = Matrix()
//        matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
//        setTransform(matrix)
//    }
//
//    /**
//     * 刷新视频播放静音状态
//     */
//    private fun refreshMediaPlayerMuteStatus() {
//        kotlin.runCatching {
//            if (isMuteVideoAudioEffect) {
//                mediaPlayer.setVolume(0f, 0f)
//            } else {
//                mediaPlayer.setVolume(0.8f, 0.8f)
//            }
//        }.onFailure {
//            it.printStackTrace()
//        }
//    }
//
//    interface OnMp4VideoViewListener {
//        /**
//         * 当前文件的缓存目录
//         */
//        fun localMp4FilePath(path: String?){}
//
//        /**
//         * 是否成功播放
//         */
//        fun mp4VideoPlayState(success: Boolean?)
//    }
//}