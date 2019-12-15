package com.github.ss

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_about_j.*
import kotlinx.android.synthetic.main.layout_webview_dialog.*

class AboutJFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about_j, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        about_webview.settings.javaScriptEnabled = true
        about_webview.loadUrl("https://frp.u03013112.win:18022/htmls/ss-html/#/about")
    }
}
