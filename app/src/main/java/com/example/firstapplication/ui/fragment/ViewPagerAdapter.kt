package com.example.firstapplication.ui.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter


class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    private val fragments = listOf(
        Fragment1(),
        Fragment2()
        // 可以添加更多 Fragment
    )

    private val titles = listOf("Tab 1", "Tab 2")

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun getTabTitle(position: Int): String = titles.getOrElse(position) { "Tab" }
}