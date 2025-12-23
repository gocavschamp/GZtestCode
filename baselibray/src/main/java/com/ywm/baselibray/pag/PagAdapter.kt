package com.ywm.baselibray.pag

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ywm.baselibray.R
import com.ywm.baselibray.pag.RecyclablePagView
class PagAdapter() :
    RecyclerView.Adapter<PagAdapter.PagViewHolder>() {

    private val items = mutableListOf<PagItem>()

    inner class PagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pagView: RecyclablePagView = itemView.findViewById(R.id.pagView)

        fun pause(){
            pagView.pauseAnimation()
        }
        fun isPlaying(): Boolean{
            return pagView.isPlaying
        }
        fun resume(){
            pagView.resumeAnimation()
        }
        fun bind(item: PagItem) {
            pagView.setPagUrl(
                url = item.pagUrl,
                autoPlay = true,
                repeatCount = -1,
                listener = object : PagPlayerManager.PagLoadListener {
                    override fun onLoading() {
                        // 显示加载状态
                        Log.e("PagAdapter", "onLoading")
                    }

                    override fun onSuccess() {
                        // 隐藏加载状态
                        Log.e("PagAdapter", "onSuccess")

                    }

                    override fun onError(error: String) {
                        // 显示错误状态
                        Log.e("PagAdapter", "onError $error")

                    }
                }
            )

            // 绑定生命周期
//            pagView.bindLifecycle(lifecycleOwner)
        }

        fun recycle() {
            pagView.pause()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pag, parent, false)
        return PagViewHolder(view)
    }

//    override fun onCreateViewHolder(
//        parent: ViewGroup,
//        viewType: Int
//    ): PagViewHolder {
//        TODO("Not yet implemented")
//    }

    override fun onBindViewHolder(holder: PagViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onViewRecycled(holder: PagViewHolder) {
        super.onViewRecycled(holder)
        holder.pagView.pauseAnimation() // 使用新的方法名
    }

    override fun getItemCount(): Int = items.size

    fun setData(newItems: List<PagItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}

data class PagItem(
    val pagUrl: String,
    val autoPlay: Boolean = true,
    val repeatCount: Int = -1
)