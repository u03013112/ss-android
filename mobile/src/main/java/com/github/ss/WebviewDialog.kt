package com.github.ss

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlinx.android.synthetic.main.layout_webview_dialog.*

class WebviewDialog : Dialog {
    var mContext: Context

    constructor(context: Context) : super(context) { this.mContext = context}
    private var height = 0f

    override fun onStart() {
        super.onStart()
        window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun show() {
        this.window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        this.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        var window = this.window
        if(height == 0f){
            window?.attributes?.height = DisplayUtil.dip2px(mContext, 440f)
        }else {
            window?.attributes?.height = DisplayUtil.dip2px(mContext, height)
        }
        window?.attributes?.width = DisplayUtil.dip2px(mContext, 300f)
        window?.setBackgroundDrawable(mContext.resources.getDrawable(R.color.color_transparent))
        window?.setGravity(Gravity.CENTER)
        super.show()
    }

    fun setDialogHeight(width : Float) : WebviewDialog {
        this.height = width
        return this
    }

    fun setUrl(url : String) {
        webview_dialog.settings.javaScriptEnabled = true
        webview_dialog.loadUrl(url)
    }
}