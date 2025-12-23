package com.example.basetools.engine

//class LGlideEngine : LImageEngine {
//
//    private val glideOptions by lazy { RequestOptions().centerCrop() }
//
//    override fun load(context: Context, imageView: ImageView, path: String?, @DrawableRes placeholderRes: Int, resizeX: Int, resizeY: Int) {
//        Glide.with(context)
//            .load(path)
//            .apply(glideOptions.placeholder(placeholderRes).override(resizeX, resizeY))
//            .into(imageView)
//    }
//
//    //pauseOnScroll打开时，以下两个必须写，否"滑动时暂停加载"不会生效
//    //加载暂停
//    override fun pause(context: Context) {
//        Glide.with(context).pauseRequests()
//    }
//
//    //恢复加载
//    override fun resume(context: Context) {
//        Glide.with(context).resumeRequestsRecursive()
//    }
//}