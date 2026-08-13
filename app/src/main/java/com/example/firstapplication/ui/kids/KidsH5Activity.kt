package com.example.firstapplication.ui.kids

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.firstapplication.databinding.ActivityKidsH5Binding

/**
 * H5 小游戏列表页：6 个游戏卡片，点击进入二级页（KidsH5GameActivity）全屏游玩
 * - 数字翻牌（数字记忆配对）
 * - 汉字连连看（经典连通消除）
 * - 拼图（3x3 emoji 滑动拼图）
 * - 打飞机（拖动飞机自动开火）
 * - 赛车（左右变道躲避来车）
 * - 跳跳闯关（横向跳跃过关）
 * - 我的世界（体素世界挖方块、盖房子）
 */
class KidsH5Activity : AppCompatActivity() {

    private lateinit var binding: ActivityKidsH5Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKidsH5Binding.inflate(layoutInflater)
        setContentView(binding.root)
        KidsStatusBar.immersive(this, binding.root)

        binding.cardGameNumber.setOnClickListener { open(GAME_NUMBER, "数字翻牌") }
        binding.cardGameHanzi.setOnClickListener { open(GAME_HANZI, "汉字连连看") }
        binding.cardGamePuzzle.setOnClickListener { open(GAME_PUZZLE, "拼图") }
        binding.cardGamePlane.setOnClickListener { open(GAME_PLANE, "打飞机") }
        binding.cardGameRacing.setOnClickListener { open(GAME_RACING, "赛车") }
        binding.cardGameJump.setOnClickListener { open(GAME_JUMP, "跳跳闯关") }
        binding.cardGameMinecraft.setOnClickListener { open(GAME_MC, "我的世界") }
    }

    private fun open(file: String, title: String) {
        KidsH5GameActivity.start(this, file, title)
    }

    companion object {
        private const val GAME_NUMBER = "number_match.html"
        private const val GAME_HANZI = "hanzi_link.html"
        private const val GAME_PUZZLE = "puzzle.html"
        private const val GAME_PLANE = "plane_shoot.html"
        private const val GAME_RACING = "car_racing.html"
        private const val GAME_JUMP = "jump_levels.html"
        private const val GAME_MC = "minecraft.html"

        fun start(context: Context) {
            context.startActivity(Intent(context, KidsH5Activity::class.java))
        }
    }
}
