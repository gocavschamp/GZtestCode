package com.example.firstapplication
import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.http.HttpResponseCache
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.alibaba.android.arouter.BuildConfig
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.Utils
import com.example.baseapi.floatview.FloatViewRouter
import com.tencent.qgame.animplayer.util.ALog

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Utils.init(this)

            // 开启调试模式和日志，在Release版本中应关闭
//        if (BuildConfig.DEBUG) {
            ARouter.openLog()
            ARouter.openDebug()
        ALog.isDebug = true
//        }
            ARouter.init(this)
        FloatViewRouter.initFloatView(this)
    }
}