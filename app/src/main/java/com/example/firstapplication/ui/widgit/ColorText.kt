package com.example.firstapplication.ui.widgit

import android.content.Context
import android.widget.TextView

class ColorText(context: Context) : androidx.appcompat.widget.AppCompatTextView(context) {
    fun setTextColor(text: String) {
        this.text = text
    }
    fun setTextColor(text: String, color: Int) {
        this.text = text
        this.setTextColor(color)
    }

    fun setTextColor(text: String, color: Int, size: Int) {
        this.text = text
        this.setTextColor(color)
        this.textSize = size.toFloat()
    }
    var onclick:(()->Unit)? = null
    var onclickone:(ColorText.()->Unit)? = null
    var onclicktwo:(ColorText.(String)->Unit)? = null
    var onclickthree:(ColorText.(String)->Int)? = null
    fun setOnClick(listener: OnClickListener) {
        this.setOnClickListener{

            onclick?.invoke()
        }
    }
    fun setOnClickListenerone(listener: OnClickListener) {
        this.setOnLongClickListener{
            onclickone?.invoke(this@ColorText)
         return@setOnLongClickListener true
        }
    }
    fun setOnClickListenertwo(listener: OnClickListener) {
        this.setOnClickListener{
            this.setOnLongClickListener{
                onclicktwo?.invoke(this@ColorText, text.toString())
                return@setOnLongClickListener true
            }
        }
    }
    fun setOnClickListenerthree(listener: OnClickListener) {
        this.setOnClickListener{
            val length = onclickthree?.invoke(this@ColorText, text.toString())

        }
    }
}