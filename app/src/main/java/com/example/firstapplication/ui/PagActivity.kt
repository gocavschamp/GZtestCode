package com.example.firstapplication.ui

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firstapplication.R
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.firstapplication.databinding.ActivityPagBinding
import com.ywm.baselibray.pag.PagAdapter
import com.ywm.baselibray.pag.PagItem
import com.ywm.baselibray.pag.PagPlayerManager
import com.ywm.baselibray.pag.RecyclablePagView
@Route(path = "/module/main")
class PagActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagBinding
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var editText: EditText
//    private lateinit var sendButton: Button
//    private lateinit var messageAdapter: MessageAdapter
//    private val messages = mutableListOf<Message>()
    private lateinit var pollingHandler: Handler
    private var pollingRunnable: Runnable? = null
    private val pollingInterval = 10000L // 轮询间隔，单位：毫秒 (例如 5秒)
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PagAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagBinding.inflate(layoutInflater)
        val root = binding.root
        setContentView(root)
        // 初始化 PAG 播放管理器
        PagPlayerManager.getInstance().init(applicationContext)

        initRecyclerView()
        loadData()

    }
    private fun initRecyclerView() {
        binding.sendButton.setOnClickListener {
            // 发送消息
            val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(0))
            if (viewHolder is PagAdapter.PagViewHolder) {
                // 直接使用 viewHolder.pagView，避免类型转换问题
                if (viewHolder.isPlaying()){
                    viewHolder.pause()
                }else{
                    viewHolder.resume()
                }
            }
        }
        recyclerView = findViewById(R.id.recyclerView)
        adapter = PagAdapter()

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 添加滚动监听，优化性能
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        // 滚动时暂停所有 PAG 播放
                        pauseAllPagAnimations()
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        // 停止滚动时恢复可见项的播放
                        resumeVisiblePagAnimations()
                    }
                }
            }
        })
    }

    private fun loadData() {
        val pagItems = listOf(
            PagItem("https://static.litme.live//litme/a97087690ac958749ba677abcd9ec08c.pag"),
            PagItem("https://static.litme.live//litme/a97087690ac958749ba677abcd9ec08c.pag"),
            PagItem("https://pag.qq.com/file/like.pag"),
            // 添加更多 PAG 项目...
        )
        adapter.setData(pagItems)
    }
    private fun pauseAllPagAnimations() {
        for (i in 0 until recyclerView.childCount) {
            val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i))
            if (viewHolder is PagAdapter.PagViewHolder) {
                // 直接使用 viewHolder.pagView，避免类型转换问题
                viewHolder.pause()
            }
        }
    }

    private fun resumeVisiblePagAnimations() {
        for (i in 0 until recyclerView.childCount) {
            val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i))
            if (viewHolder is PagAdapter.PagViewHolder) {
                // 直接使用 viewHolder.pagView，避免类型转换问题
                viewHolder.resume()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        pauseAllPagAnimations()
    }

    override fun onResume() {
        super.onResume()
        if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
            resumeVisiblePagAnimations()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 可选：在 Activity 销毁时释放所有资源
        // PagPlayerManager.getInstance().destroy()
    }
}