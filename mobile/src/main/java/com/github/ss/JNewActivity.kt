package com.github.ss

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_j_new.*

class JNewActivity : AppCompatActivity() {
    var viewPager: ViewPager? = null
    private var tabLayout: TabLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_j_new)
        viewPager = view_page
        setupViewPager(viewPager!!)

        tabLayout = tab_layout
        tabLayout!!.setupWithViewPager(viewPager)
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = FragmentAdapter(supportFragmentManager)
        adapter.addFragment(MainFragment(), getString(R.string.setting))
        adapter.addFragment(AboutJFragment(), getString(R.string.about))

        viewPager.adapter = adapter
    }
}
