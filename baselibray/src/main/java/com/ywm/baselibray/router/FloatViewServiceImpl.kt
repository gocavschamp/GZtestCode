package com.ywm.baselibray.router

import android.app.Application
import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.baseapi.floatview.FloatViewService
import com.ywm.baselibray.floatview.GlobalNoticeManager
import com.ywm.baselibray.floatview.NoticeMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.apply
import kotlin.let

@Route(path = "/service/floatview")
class FloatViewServiceImpl : FloatViewService {

    override fun init(app: Application) {
        GlobalNoticeManager.init(app)
    }

    override fun addNotice(user: String) {
        val message = NoticeMessage("用户${user}成功升级为VIP！")
        GlobalNoticeManager.addNotice(message)
    }

    override fun init(context: Context?) {
    }

    companion object {
    }
}