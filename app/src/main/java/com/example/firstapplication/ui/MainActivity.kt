package com.example.firstapplication.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.baseapi.floatview.FloatViewRouter
import com.example.firstapplication.R
import com.example.firstapplication.databinding.ActivityMainBinding
import com.ethanhua.skeleton.Skeleton
import com.ethanhua.skeleton.SkeletonScreen
import com.ywm.baselibray.utils.CountryFlagUtil
import com.ywm.baselibray.utils.dp
import com.ywm.baselibray.utils.setDrawableWithSize
import com.ywm.baselibray.utils.setRichText
import com.ywm.baselibray.weiget.ShimmerColorTextView
import com.ywm.baselibray.weiget.ShineEffect


@Route(path = "/module/main")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var editText: EditText
//    private lateinit var sendButton: Button
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private var skeletonScreen: SkeletonScreen? = null
    private lateinit var pollingHandler: Handler
    private var pollingRunnable: Runnable? = null
    private val pollingInterval = 10000L // 轮询间隔，单位：毫秒 (例如 5秒)
    var onclick :(()-> Unit)?=null //空参函数 无返回值
    var ontextclick :(TextView.()-> Unit)?=null //带接收者textview 的函数，相当于第一个
    var ontextclickTwo :(TextView.(s: String)-> Int)?=null //带接收者textview和string 的函数，相当于第一个textview 第二个参数String

    var testIndex = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val root = binding.root
        setContentView(root)
        initViews()

        setupRecyclerView()
        binding.shimmer.post {
            binding.shimmer.setShimmerEnabled(true)
            binding.shimmer.gradientColors = intArrayOf(
                "#ff0101".toColorInt(),
                "#ff9400".toColorInt(),
                "#eaff00".toColorInt(),
                "#00ff7b".toColorInt(),
                "#bd01ff".toColorInt(),
            )
            binding.shimmer.startShimmerAnimation()

        }
//        binding.luckCount.getTextView().setDrawableWithSize(com.example.basetools.R.drawable.ic_icon,16.dp,16.dp)
        binding.luckCount.setText(System.currentTimeMillis().toString())
        binding.luckCountMin.setText("100")
        binding.luckCount.isVisible = true
        binding.luckCountMin.isVisible = true
        //todo shine effect text
//        binding.shineTextView.post {
//            ShineEffect(binding.shineTextView).apply {
//                setAutoStart(true)
//            }
//        }
        for (i in 1..30){
            testIndex++
            messages.add(Message("testIndex=${testIndex}", System.currentTimeMillis()))
        }
        val drawable =
            CountryFlagUtil.getFlagDrawableSafe(this, "CN", com.ywm.baselibray.R.drawable.flag_tr)
        binding.iv1.setImageDrawable(drawable)
        val drawable1 =
            CountryFlagUtil.getFlagDrawableSafe(this, "UY", com.ywm.baselibray.R.drawable.flag_tr)
        binding.iv2.setImageDrawable(drawable1)
        val drawable3 =
            CountryFlagUtil.getFlagDrawableSafe(this, "TW", com.ywm.baselibray.R.drawable.flag_tr)
        binding.txt1.setCompoundDrawablesRelative(drawable3, null, null, null)
        binding.txt1.setDrawableWithSize(drawable3,20.dp,20.dp)

 val drawable4 =
            CountryFlagUtil.getFlagDrawableSafe(this, "CC", com.ywm.baselibray.R.drawable.flag_tr)
        binding.txt2.setCompoundDrawablesRelative(drawable4, null, null, null)
        binding.txt2.setDrawableWithSize(drawable4,20.dp,20.dp)
        messageAdapter.notifyDataSetChanged()
        setupSendButton()
        // 初始化Handler，绑定到主线程Looper
        pollingHandler = Handler(Looper.getMainLooper())
        // 方法1：使用 GestureDetector 版本
        binding.testFrameLayout.setOnDismissListener {
//            binding.testFrameLayout.isVisible = true
//            Toast.makeText(this, "dismiss", Toast.LENGTH_SHORT).show()
        }
        binding.testButton.setOnClickListener {
            Toast.makeText(this, "click", Toast.LENGTH_SHORT).show()
            binding.testFrameLayout.isVisible = true
        }
        // 在 Kotlin 代码中调用
        binding.shineTextView.setRichText {
            // 1. 拼接蓝色文字
            text(
                text = "点击这里",
                color = Color.BLUE,
                underline = true,
                onClick = {
                    Toast.makeText(this@MainActivity, "文字被点击", Toast.LENGTH_SHORT).show()
                }
            )

            // 2. 拼接一个星星图标 (24x24 px)
            icon(
                drawable = ContextCompat.getDrawable(this@MainActivity, com.example.basetools.R.drawable.ic_icon)!!,
                width = 24.dp,
                height = 24.dp,
                onClick = {
                    Toast.makeText(this@MainActivity, "星星被点击", Toast.LENGTH_SHORT).show()
                }
            )

            // 3. 拼接普通红色文字（无下划线，无点击）
            text(
                text = " 普通红色文字",
                color = Color.RED
            )
            // 3. 拼接普通红色文字（无下划线，无点击）
            text(
                text = " 普通红色文字",
                color = Color.GREEN
            )
 // 3. 拼接普通红色文字（无下划线，无点击）
            text(
                text = " 普通红色文字",
                color = Color.YELLOW
            )

            // 4. 拼接一个箭头图标 + 点击
            icon(
                drawable = ContextCompat.getDrawable(this@MainActivity, com.huantansheng.easyphotos.R.drawable.ic_album_item_choose_easy_photos)!!,
                width = 30.dp,
                height = 30.dp,
                onClick = {
                    Toast.makeText(this@MainActivity, "箭头被点击", Toast.LENGTH_SHORT).show()
                }
            )
        }
//         或者方法2：使用自定义触摸处理版本
//        binding.testFrameLayout.enableRightSwipeToDismissV2()
//        startPolling()
//        binding.colorText.onclick = {
//            Toast.makeText(this, "dian ji", Toast.LENGTH_SHORT).show()
//        }
//        binding.colorText.onclickone = {
//            Toast.makeText(this@MainActivity, "${this.text}", Toast.LENGTH_SHORT).show()
//        }
//        binding.colorText.onclicktwo = { text ->
//            Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
//        }
//        binding.colorText.onclickthree = { text ->
//            Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
//            text.length
//        }


    }
    /**
     * 开始轮询
     */
    private fun startPolling() {
        // 先移除可能存在的旧任务，避免重复
        pollingRunnable?.let { pollingHandler.removeCallbacks(it) }

        pollingRunnable = object : Runnable {
            override fun run() {
                // 1. 这里是轮询到任务后要执行的核心操作
                simulateVipPurchaseNotification()

                // 2. 安排下一次执行，形成循环
                pollingHandler.postDelayed(this, pollingInterval)
            }
        }
        // 启动轮询
        pollingRunnable?.let { pollingHandler.postDelayed(it,pollingInterval) }
    }

    /**
     * 模拟轮询到VIP购买事件后的操作
     */
    private fun simulateVipPurchaseNotification() {
        // 这里模拟一个随机的用户名
        val randomUser = listOf("张三", "李四", "王五", "赵六").random()

        // 将消息添加到全局通知管理器
        FloatViewRouter.addNotice("用户${randomUser}成功升级为VIP！")
        Log.d("PollingDemo", "已发送通知: ${randomUser}")
    }

    /**
     * 停止轮询
     */
    private fun stopPolling() {
        pollingRunnable?.let { pollingHandler.removeCallbacks(it) }
        pollingRunnable = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // 当Activity销毁时，务必停止轮询，避免内存泄漏
        stopPolling()
    }
    var gameInt = 0
    private fun initViews() {
        binding.picker.setOnClickListener {
            Toast.makeText(this, "dian ji", Toast.LENGTH_SHORT).show()
            ARouter.getInstance()
                .build("/picker/pickerImage/image") // 构建跳转
                .navigation()
        }
        binding.pag.setOnClickListener {
            Toast.makeText(this, "dian ji", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, PagActivity::class.java))
        }
        binding.svga.setOnClickListener {
            Toast.makeText(this, "dian ji", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SvgaActivity::class.java))
        }
        binding.vap.setOnClickListener {
            Toast.makeText(this, "vap", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AnimSimpleDemoActivity::class.java))
        }
        binding.scale.setOnClickListener {
            Toast.makeText(this, "scale", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ScaleDemoActivity::class.java))
        }
        binding.dialogUtils.setOnClickListener {
            startActivity(Intent(this, DialogDemoActivity::class.java))
        }
        binding.scoreview.setOnClickListener {
            startActivity(Intent(this, ScoreViewActivity::class.java))
        }
        binding.countdown.setOnClickListener {
            ARouter.getInstance()
                .build("/module/countdown") // 构建跳转
                .navigation()
        }
        val circleCrop: Transformation<Bitmap?> = CircleCrop()
        Glide.with(this)
            .load(R.raw.ic_room_bottom_gift)
            .optionalTransform(circleCrop)
            .optionalTransform(WebpDrawable::class.java, WebpDrawableTransformation(circleCrop))
            .into(binding.image)
        binding.game.setOnClickListener {
//           when(gameInt){
//               0 -> {
//                   GameActivity.launchSimple(this)
//                   gameInt = 0
//               }
//               1 -> {
                   GameActivity.launchAsTransparentWindow(
                       context = this,
                       opaqueRatio = 0.8f,
                       position = GameActivity.POSITION_CENTER
                   )
                   gameInt = 1
//               }
        }

    }
    
    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = messageAdapter

        // 显示骨架屏，模拟加载
        skeletonScreen = Skeleton.bind(binding.recyclerView)
            .adapter(messageAdapter)
            .load(R.layout.layout_skeleton_item_message)
            .shimmer(false)
            .angle(20)
            .duration(1200)
            .count(10)
            .show()

        // 模拟加载完成，2s 后隐藏骨架屏
        binding.recyclerView.postDelayed({
            skeletonScreen?.hide()
            skeletonScreen = null
        }, 2000L)
    }
    
    private fun setupSendButton() {
        binding.newMsgCome.setOnClickListener {
//            messageAdapter.notifyDataSetChanged()
            binding.recyclerView.scrollToPosition(messages.size-1)
            binding.newMsgCome.isVisible = false
        }
        binding.sendButton.setOnClickListener {
//            binding.luckCount.getTextView().setDrawableWithSize(com.example.basetools.R.drawable.ic_icon,16.dp,16.dp)
            binding.luckCount.setTextWithAnimation(System.currentTimeMillis().toString())
            binding.luckCountMin.setText("+"+System.currentTimeMillis().toString())
            binding.luckCountMin.postDelayed({
                binding.luckCountMin.setTextWithAnimation("")
            },2000L)

            sendMessage()
//            ARouter.getInstance()
//                .build("/type/home") // 构建跳转
//                .navigation()
//            ARouter.getInstance()
//                .build("/other/mine") // 构建跳转
//                .navigation()
        }

        binding.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }
    
    private fun sendMessage() {
        val content = binding.editText.text.toString().trim()
        if (content.isNotEmpty()) {
            if (messages.size>15){
                messages.removeAt(0)
                messageAdapter.notifyItemRemoved(0)
                testIndex++
                messages.add(Message("testIndex=${testIndex}", System.currentTimeMillis()))
                messageAdapter.notifyItemInserted(messages.size - 1)
                binding.newMsgCome.isVisible = true
                return
            }
            testIndex++
            messages.add(Message("testIndex=${testIndex}", System.currentTimeMillis()))
            messageAdapter.notifyItemInserted(messages.size - 1)
            binding.recyclerView.scrollToPosition(messages.size - 1)
        }
    }
    
    data class Message(val content: String, val timestamp: Long)
    
    class MessageAdapter(private val messages: List<Message>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
        
        class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val messageText: TextView = itemView.findViewById(R.id.messageText)
            val timestampText: TextView = itemView.findViewById(R.id.timestampText)
            val shimmer: ShimmerColorTextView = itemView.findViewById(R.id.shimmer)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false)
            return MessageViewHolder(view)
        }

        override fun onViewRecycled(holder: MessageViewHolder) {
            super.onViewRecycled(holder)
            holder.shimmer.onRecycled()
        }
        
        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {

            val message = messages[position]
            holder.messageText.text ="index= $position ___" + message.content
            holder.timestampText.text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(message.timestamp))
            when (position) {
                in 0..1 -> {
                    holder.shimmer.setShimmerEnabled(true)
                    holder.shimmer.text = "index=index=index=$position"

                    holder.shimmer.gradientColors = intArrayOf(
                        "#ff0101".toColorInt(),
                        "#ff9400".toColorInt(),
                        "#eaff00".toColorInt(),
                        "#00ff7b".toColorInt(),
                        "#bd01ff".toColorInt(),
                    )
                }
                in 2..3 -> {
                    holder.shimmer.setShimmerEnabled(true)
                    holder.shimmer.text = "ind$position"
                    holder.shimmer.gradientColors = intArrayOf(
                        "#ff0101".toColorInt(),
                        "#ff9400".toColorInt(),
                        "#eaff00".toColorInt(),
                        "#00ff7b".toColorInt(),
                        "#bd01ff".toColorInt(),
                    )
                }
                in 4..7 -> {
                    holder.shimmer.gradientColors = intArrayOf(
                        ContextCompat.getColor(holder.itemView.context, R.color.blue),
                        ContextCompat.getColor(holder.itemView.context, R.color.blue)
                    )
                    holder.shimmer.setShimmerEnabled(true)
//                    holder.shimmer.startShimmerAnimation()
                }
                in 8..14 -> {
                    holder.shimmer.gradientColors = intArrayOf(
                        ContextCompat.getColor(holder.itemView.context, R.color.gold),
                        ContextCompat.getColor(holder.itemView.context, R.color.gold)
                    )
                    holder.shimmer.setShimmerEnabled(true)
//                    holder.shimmer.startShimmerAnimation()

                }
                in 14..18 -> {
                    holder.shimmer.gradientColors = intArrayOf(
                        ContextCompat.getColor(holder.itemView.context, R.color.green),
                        ContextCompat.getColor(holder.itemView.context, R.color.green)
                    )
                    holder.shimmer.setShimmerEnabled(false)
//                    holder.shimmer.startShimmerAnimation()

                }
                else -> {
                    holder.shimmer.setShimmerEnabled(false)

                    holder.shimmer.gradientColors = intArrayOf(
                        ContextCompat.getColor(holder.itemView.context, R.color.black),
                        ContextCompat.getColor(holder.itemView.context, R.color.black),
                        ContextCompat.getColor(holder.itemView.context, R.color.black),
                        ContextCompat.getColor(holder.itemView.context, R.color.black),
                        ContextCompat.getColor(holder.itemView.context, R.color.black)
                    )
                }
            }
            holder.shimmer.onBind()
            holder.itemView.setOnClickListener {
                // Handle click event
                ARouter.getInstance()
                    .build("/type/home") // 构建跳转
                    .navigation()
            }
        }
        
        override fun getItemCount(): Int = messages.size
    }
}