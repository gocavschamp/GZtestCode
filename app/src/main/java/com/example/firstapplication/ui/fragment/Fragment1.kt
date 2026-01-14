package com.example.firstapplication.ui.fragment

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.firstapplication.R
import com.example.firstapplication.databinding.FragmentListBinding
import com.example.firstapplication.databinding.ItemTextBinding
import java.util.logging.Handler
import kotlin.math.abs

class Fragment1 : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView1.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerView1.adapter = SimpleAdapter((1..10).map { "Fragment 2 - Item $it" })
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = SimpleAdapter((1..10).map { "Fragment 1 - Item $it" })
        // 解决滑动冲突
        setupSwipeConflictResolution( binding.recyclerView1)
        setupSwipeConflictResolution( binding.recyclerView)
        // 设置触摸监听
//        binding.recyclerView1.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
//            private var startX = 0f
//
//            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
//                when (e.action) {
//                    MotionEvent.ACTION_DOWN -> {
//                        startX = e.x
//                        // 暂时禁用ViewPager2滑动
//                        getViewPager2()?.isUserInputEnabled = false
//                    }
//                    MotionEvent.ACTION_MOVE -> {
//                        val dx = abs(e.x - startX)
//                        if (dx > 10) { // 横向滑动阈值
//                            // RecyclerView处理横向滑动
//                            getViewPager2()?.isUserInputEnabled = false
//                        }
//                    }
//                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                        // 恢复ViewPager2滑动
//                        android.os.Handler(Looper.getMainLooper()).postDelayed({
//                            getViewPager2()?.isUserInputEnabled = true
//                        }, 100)
//                    }
//                }
//                return false
//            }
//        })
    }

    private fun setupSwipeConflictResolution(recyclerView: RecyclerView) {
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            private var startX = 0f
            private var startY = 0f

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = e.x
                        startY = e.y
                        // 禁用ViewPager2滑动
                        activity?.findViewById<ViewPager2>(R.id.viewPager)?.isUserInputEnabled = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = abs(e.x - startX)
                        val dy = abs(e.y - startY)

                        // 如果主要是横向滑动
                        if (dx > dy && dx > 10) {
                            // 检查RecyclerView是否还能继续滑动
                            val canScroll = if (e.x > startX) {
                                // 向右滑动
                                recyclerView.canScrollHorizontally(-1)
                            } else {
                                // 向左滑动
                                recyclerView.canScrollHorizontally(1)
                            }

                            if (!canScroll) {
                                // RecyclerView不能继续滑动了，恢复ViewPager2滑动
                                activity?.findViewById<ViewPager2>(R.id.viewPager)?.isUserInputEnabled = true
                                return false // 让ViewPager2处理
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // 延迟恢复ViewPager2滑动，避免立即切换页面
                        android.os.Handler(Looper.getMainLooper()).postDelayed({
                            activity?.findViewById<ViewPager2>(R.id.viewPager)?.isUserInputEnabled = true
                        }, 300)
                    }
                }
                return false
            }
        })
    }
    private fun getViewPager2(): ViewPager2? {
        return activity?.findViewById(R.id.viewPager)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class Fragment2 : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView1.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerView1.adapter = SimpleAdapter((1..10).map { "Fragment 2 - Item $it" })
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = SimpleAdapter((1..10).map { "Fragment 2 - Item $it" })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// RecyclerView 适配器
class SimpleAdapter(private val items: List<String>) :
    RecyclerView.Adapter<SimpleAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTextBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTextBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.textView.text = items[position]
    }

    override fun getItemCount(): Int = items.size
}
// 或者如果必须使用横向布局，则通过自定义LayoutManager解决
class NonScrollableLinearLayoutManager(context: Context) : LinearLayoutManager(context, HORIZONTAL, false) {
    override fun canScrollHorizontally(): Boolean = false
}
// EnhancedRecyclerViewTouchHandler.kt
class EnhancedRecyclerViewTouchHandler(
    private val recyclerView: RecyclerView,
    private val viewPager2: ViewPager2
) {

    private var startX = 0f
    private var startY = 0f
    private var isScrolling = false
    private var isRecyclerViewConsuming = false

    // 滑动阈值
    private val scrollThreshold = 10f

    init {
        setupTouchHandling()
    }

    private fun setupTouchHandling() {
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return handleTouchEvent(rv, e)
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                handleTouchEvent(rv, e)
            }
        })
    }

    private fun handleTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = e.x
                startY = e.y
                isScrolling = false
                isRecyclerViewConsuming = false

                // 禁止父View拦截，先让RecyclerView处理
                recyclerView.parent.requestDisallowInterceptTouchEvent(true)
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isScrolling) {
                    val dx = abs(e.x - startX)
                    val dy = abs(e.y - startY)

                    // 判断是否是横向滑动
                    if (dx > dy && dx > scrollThreshold) {
                        isScrolling = true

                        // 确定滑动方向
                        val isLeftScroll = e.x < startX

                        // 检查RecyclerView是否能向该方向滑动
                        val canRecyclerViewScroll = if (isLeftScroll) {
                            // 向左滑动，检查是否能向右滑动（即是否在右边界）
                            recyclerView.canScrollHorizontally(1)
                        } else {
                            // 向右滑动，检查是否能向左滑动（即是否在左边界）
                            recyclerView.canScrollHorizontally(-1)
                        }

                        if (canRecyclerViewScroll) {
                            // RecyclerView还可以滑动，自己处理
                            isRecyclerViewConsuming = true
                            recyclerView.parent.requestDisallowInterceptTouchEvent(true)
                        } else {
                            // RecyclerView已经滑动到边界，交给父View（ViewPager）
                            isRecyclerViewConsuming = false
                            recyclerView.parent.requestDisallowInterceptTouchEvent(false)
                        }
                    } else if (dy > dx && dy > scrollThreshold) {
                        // 纵向滑动，RecyclerView处理
                        isRecyclerViewConsuming = true
                        recyclerView.parent.requestDisallowInterceptTouchEvent(true)
                    }
                }

                // 如果RecyclerView正在处理滑动，继续检查边界条件
                if (isRecyclerViewConsuming && isScrolling) {
                    val currentX = e.x
                    val isLeftScroll = currentX < startX

                    // 实时检查是否滑动到边界
                    val canContinueScroll = if (isLeftScroll) {
                        recyclerView.canScrollHorizontally(1) // 检查是否能继续向右滑动
                    } else {
                        recyclerView.canScrollHorizontally(-1) // 检查是否能继续向左滑动
                    }

                    if (!canContinueScroll) {
                        // 已经滑动到边界，让ViewPager接管
                        isRecyclerViewConsuming = false
                        recyclerView.parent.requestDisallowInterceptTouchEvent(false)

                        // 创建一个新的事件给ViewPager
                        val newEvent = MotionEvent.obtain(e)
                        newEvent.action = MotionEvent.ACTION_DOWN
                        viewPager2.dispatchTouchEvent(newEvent)
                        newEvent.recycle()
                    }
                }

                startX = e.x
                startY = e.y
                return isRecyclerViewConsuming
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isScrolling = false
                isRecyclerViewConsuming = false
                recyclerView.parent.requestDisallowInterceptTouchEvent(false)
                return false
            }
        }
        return false
    }

    /**
     * 释放资源
     */
    fun release() {
        recyclerView.setOnTouchListener(null)
    }
}