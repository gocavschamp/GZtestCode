package com.ywm.baselibray.mine
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.ywm.baselibray.R

import android.os.Bundle

@Route(path = "/other/mine")
class MineActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mine)

    }

}