package com.example.baseapi.floatview

import android.app.Application
import com.alibaba.android.arouter.launcher.ARouter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext

object FloatViewRouter {

    @JvmStatic
    @JvmOverloads
    fun initFloatView(
        app: Application,
    ) {
        (ARouter.getInstance().build("/service/floatview")
            .navigation() as FloatViewService).init(app)
    }
 @JvmStatic
    @JvmOverloads
    fun addNotice(
        user: String,
    ) {
        (ARouter.getInstance().build("/service/floatview")
            .navigation() as FloatViewService).addNotice(user)
    }

}