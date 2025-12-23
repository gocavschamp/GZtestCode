package com.example.baseapi.floatview

import android.app.Application
import com.alibaba.android.arouter.facade.template.IProvider

interface FloatViewService : IProvider {

    fun init(
        app: Application
    )
    fun addNotice(
        msg: String
    )
}