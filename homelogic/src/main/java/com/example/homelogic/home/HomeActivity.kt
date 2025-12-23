package com.example.homelogic.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homelogic.R
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Route(path = "/type/home")
class HomeActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var editText: EditText
    private lateinit var sendButton: Button
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupRecyclerView()
        setupSendButton()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        editText = findViewById(R.id.editText)
        sendButton = findViewById(R.id.sendButton)
        editText.setText("home, home!")
    }
    
    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = messageAdapter
    }
    
    private fun setupSendButton() {
        sendButton.setOnClickListener {
            sendMessage()
        }
        
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }
    
    private fun sendMessage() {
        val content = editText.text.toString().trim()
        if (content.isNotEmpty()) {
            messages.add(Message(content, System.currentTimeMillis()))
            messageAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
            editText.setText("")
        }
    }
    
    data class Message(val content: String, val timestamp: Long)
    
    class MessageAdapter(private val messages: List<Message>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
        
        class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val messageText: TextView = itemView.findViewById(R.id.messageText)
            val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false)
            return MessageViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val message = messages[position]
            holder.messageText.text = message.content
            holder.timestampText.text = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(message.timestamp))
            holder.itemView.setOnClickListener {
                // Handle click event
                ARouter.getInstance()
                    .build("/other/mine") // 构建跳转
                    .navigation()
            }
        }
        
        override fun getItemCount(): Int = messages.size
    }
}