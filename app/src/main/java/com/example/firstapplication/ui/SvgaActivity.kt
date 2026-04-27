package com.example.firstapplication.ui

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firstapplication.R
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils
import com.example.firstapplication.databinding.ActivityPagBinding
import com.example.firstapplication.databinding.ActivitySvgaBinding
import com.opensource.svgaplayer.SVGACallback
import com.opensource.svgaplayer.utils.SVGARange
import com.ywm.baselibray.pag.PagAdapter
import com.ywm.baselibray.pag.PagItem
import com.ywm.baselibray.pag.PagPlayerManager
import com.ywm.baselibray.pag.RecyclablePagView
import com.ywm.baselibray.utils.SvgaInitUtil
import java.util.LinkedList

@Route(path = "/module/main")
class SvgaActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivitySvgaBinding
    private var svgaInitUtil: SvgaInitUtil? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySvgaBinding.inflate(layoutInflater)
        val root = viewBinding.root
        setContentView(root)
        // 初始化 PAG 播放管理器

        viewBinding.sendButton.setOnClickListener {
            linkedList.add("svga/win_effect.svga")
            linkedList.add("svga/bigwin.svga")
            linkedList.add("svga/magicwin.svga")
            createAnimNew(linkedList.pollFirst())
        }
        viewBinding.sendButton1.setOnClickListener {
            linkedList.add("svga/bigwin.svga")
            createAnimNew(linkedList.pollFirst())
        }
        viewBinding.sendButton2.setOnClickListener {
            linkedList.add("svga/magicwin.svga")
            createAnimNew(linkedList.pollFirst())
        }
        viewBinding.sendButton3.setOnClickListener {
            linkedList.add("svga/room_pk_countdown.svga")
            createAnimNew(linkedList.pollFirst())
        }
        viewBinding.sendButton4.setOnClickListener {
            linkedList.add("svga/room_pk_start_anim.svga")
            createAnimNew(linkedList.pollFirst())

        }
        viewBinding.sendButton5.setOnClickListener {
            linkedList.add("svga/room_start_pk.svga")
            createAnimNew(linkedList.pollFirst())
        }
//        initRecyclerView()
//        loadData()

    }

    val linkedList = LinkedList<String>()
    var isAnimRunningData = false
    private fun createAnimNew(user: String) {
        LogUtils.e("-----luck user create-----${user.toString()}")
        if (isAnimRunningData)return
        viewBinding?.root?.isVisible = true
        viewBinding?.svgaView?.isVisible = true
        viewBinding?.svgaView?.stopAnimation(true)
        viewBinding?.svgaView?.callback = null
        //ToDo delete 判断中奖等级 播放
        //todo 另根据中奖倍数展示样式，低倍展示WIN样式、高倍展示BIG WIN、最高倍展示MAGIC WIN
        var winSaga = user
//        var winSaga = "svga/bigwin.svga"
//        var winSaga = "svga/magicwin.svga"
        if (svgaInitUtil == null) {
            svgaInitUtil = SvgaInitUtil()
        }
        svgaInitUtil?.addPlayListener(viewBinding?.svgaView, object : SVGACallback {
            override fun onFinished() {
                LogUtils.e("-----luck user onFinished-----onFinished")

            }

            override fun onPause() {
            }

            override fun onRepeat() {
            }

            override fun onStep(frame: Int, percentage: Double) {
                if (percentage == 1.0){
                    LogUtils.e("-----luck user onStep end-----$percentage")
                    isAnimRunningData = false
                    if (linkedList.size>0){
                        createAnimNew(linkedList.pollFirst())
                    }
return
                }
                if (frame == 42){
                    if (linkedList.size==0)return
                    viewBinding?.svgaView?.pauseAnimation()
                    parserSvag(linkedList.pollFirst(), "8888"){
                        if (it) {
                            viewBinding?.root?.isVisible = true
                            viewBinding?.svgaView?.isVisible = true
                            viewBinding?.svgaView?.stepToFrame(13,true)
                        } else {
                            viewBinding?.root?.isVisible = false
                            viewBinding?.svgaView?.isVisible = false
                            viewBinding?.svgaView?.stopAnimation()
                            //                isAnimRunningData = false
                        }
                    }

                }
            }
        })
        isAnimRunningData = true
        parserSvag(winSaga, "0000"){
            if (it) {
                viewBinding?.root?.isVisible = true
                viewBinding?.svgaView?.isVisible = true
                viewBinding?.svgaView?.startAnimation()
            } else {
                viewBinding?.root?.isVisible = false
                viewBinding?.svgaView?.isVisible = false
                viewBinding?.svgaView?.stopAnimation()
                //                isAnimRunningData = false
            }
        }

    }

    private fun parserSvag(winSaga: String, user: String,onComplete: (parseSuccess: Boolean)->Unit) {
        viewBinding?.svgaView?.isVisible = true
        svgaInitUtil?.initSvgaWithInfo(
            fileName = winSaga, context = Utils.getApp(), svgaImageView = viewBinding?.svgaView,
            avatarUrl = "https://pic.pngsucai.com/01/00/07/30e91009b3544dcc.webp",
            text1 = "nickName $user", text1Key = "text_1",
            text2 = "xxx${user}", text2Key = "text_2",
            imgKey = "avatar"
        ) { parseSuccess: Boolean ->
            LogUtils.e("-----luck user parseSuccess-----${parseSuccess}")
           onComplete.invoke(parseSuccess)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 可选：在 Activity 销毁时释放所有资源
        // PagPlayerManager.getInstance().destroy()
    }
}