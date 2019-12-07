package com.github.ss.net

import android.util.Log
import androidx.lifecycle.ViewModel
import com.github.kittinunf.fuel.Fuel

class HttpPost : ViewModel() {

    fun post(url:String,data:String,callback:(String)->(Unit),failed:(String)->(Unit)) {
        Log.v("J","post")

        val header = mutableMapOf<String, String>()
        header["Content-Type"] = "application/json"

        Fuel.post(url)
        .body(data)
        .header(header)
        .responseString { _, _, result ->
            result.fold({
//                    Log.e("J", it)
                callback(it)
            }, {
//                Log.e("J", it.message)
                it.message?.let { it1 -> failed(it1) }
            })
        }
    }

    fun login() {



    }
}