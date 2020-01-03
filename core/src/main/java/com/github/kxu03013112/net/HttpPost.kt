package com.github.kxu03013112.net

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.github.kittinunf.fuel.Fuel
import com.github.kxu03013112.Core
import com.github.kxu03013112.database.Profile
import com.github.kxu03013112.database.ProfileManager
import com.github.kxu03013112.preference.DataStore
import com.google.gson.Gson
import java.nio.charset.Charset

class HttpPost : ViewModel() {

    fun post(url:String,data:String,callback:(String)->(Unit),failed:(String)->(Unit)) {
        Log.v("J","post:${url} data:${data}")

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

    data class VPNConfig (
        val config : String = ""
    )
    data class VPNConfigDecode (
            val IP : String = "",
            val port : String = "",
            val method : String = "",
            val passwd : String = "",
            val expiresDate : String = "",
            val error: String = ""
    )
    fun getVPNConfig(profile : Profile,callback: () -> Unit,failed: (String) -> Unit){
        post("https://frp.u03013112.win:18022/v1/android/get-config","{\"token\":\"${DataStore.token}\"}",
                {str ->
                    val d = Gson().fromJson(str, VPNConfig::class.java)
                    val dataStr = decode(d.config)
                    val config = Gson().fromJson(dataStr, VPNConfigDecode::class.java)
                    if (config.error != ""){
                        failed(config.error)
                        return@post
                    }

                    profile.host=config.IP
                    profile.remotePort=config.port.toInt()
                    profile.password=config.passwd
                    profile.method=config.method

                    callback()
                    return@post
                },
                {err ->
                    failed("连接服务器失败")
                }
        )
    }

    private fun decode(str:String) : String {
        var ret = str
        for (i in 'a'..'z') {
            ret = ret.replace(".${i-1}","${i}")
        }
        var retByte = Base64.decode(ret,Base64.DEFAULT)
        return String(retByte, Charset.defaultCharset())
    }

    private fun encode(str:String) : String {
        var ret = str
        for (i in 'a'..'z') {
            ret = ret.replace("${i}",".${i-1}")
        }
        return ret
    }
}