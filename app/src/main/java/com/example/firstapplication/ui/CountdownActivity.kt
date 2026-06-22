package com.example.firstapplication.ui

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.LogUtils
import com.example.firstapplication.databinding.ActivityCountdownBinding

@Route(path = "/module/countdown")
class CountdownActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCountdownBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCountdownBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.btnStart.setOnClickListener {
            viewBinding.countdownView.setText("5s Countdown")
            viewBinding.countdownView.startCountdown(5000L)
        }

        viewBinding.btnReset.setOnClickListener {
            viewBinding.countdownView.resetCountdown()
            viewBinding.countdownView.setText("Reset")
        }

        viewBinding.btnStart2.setOnClickListener {
            viewBinding.countdownView2.setText("Running...")
            viewBinding.countdownView2.startCountdown(10000L)
        }

        viewBinding.countdownView.setOnCountdownFinish {
            LogUtils.e("CountdownView finished")
            viewBinding.countdownView.setText("Done!")
        }

        viewBinding.countdownView2.setOnCountdownFinish {
            LogUtils.e("CountdownView2 finished")
            viewBinding.countdownView2.setText("Finished!")
        }
    }
}
