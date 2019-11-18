package com.github.shadowsocks

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.VpnService
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.provider.Settings
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.lifecycle.observe
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.aidl.ShadowsocksConnection.Callback
import com.github.shadowsocks.aidl.TrafficStats
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.net.HttpPost
import com.github.shadowsocks.net.HttpsTest
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.utils.Key
import com.google.gson.Gson
import kotlinx.android.synthetic.main.layout_apps.*
import kotlinx.android.synthetic.main.layout_apps.view.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import java.text.SimpleDateFormat

class JActivity : AppCompatActivity(), Callback {

    lateinit var testButton : Button
    lateinit var connectButton: Button
    lateinit var profile : Profile

    var ready = false
    var state = BaseService.State.Idle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_j)

        testButton = findViewById(R.id.test_button)
        testButton.setOnClickListener{
//             v ->
//                alert("真的卸载吗?", "尊敬的用户") {
//                    positiveButton("残忍卸载") {  }
//                    negativeButton("我再想想") {toast("login success!") }
//                    cancelButton { Log.v("J","cancel") }
//                }.show()
        }

        connectButton = findViewById(R.id.connect_button)
        connectButton.setOnClickListener{
            didClickedConnectButton()
        }

        this.getProfile()

        connection.connect(this, this)
        connection.bandwidthTimeout = 1000

        login()
    }

    private fun setStatus(text: CharSequence) {
        Log.v("J",text.toString())
    }

    private fun getProfile() {
        var profileList = ProfileManager.getAllProfiles()
        if (profileList != null && profileList.isNotEmpty()) {
            this.profile = profileList[0]
        }else{
            this.profile = ProfileManager.createProfile(Profile())
        }
        Core.switchProfile(this.profile.id)
    }

    private val connection = ShadowsocksConnection(Handler(), true)
    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) = changeState(state, msg, true)
    override fun onServiceConnected(service: IShadowsocksService) = changeState(try {
        BaseService.State.values()[service.state]
    } catch (_: RemoteException) {
        BaseService.State.Idle
    })
    private fun changeState(state: BaseService.State, msg: String? = null, animate: Boolean = false) {
        this.state = state
        when(state) {
            BaseService.State.Idle,BaseService.State.Stopped -> {
                this.connectButton.text = "连  接"
                this.connectButton.setTextColor(Color.WHITE)
                this.connectButton.background.setTint(Color.GREEN)
            }
            BaseService.State.Connected -> {
                this.connectButton.text = "断  开"
                this.connectButton.setTextColor(Color.WHITE)
                this.connectButton.background.setTint(Color.RED)
            }
            else -> {
                this.connectButton.text = "..."
                this.connectButton.setTextColor(Color.RED)
                this.connectButton.background.setTint(Color.YELLOW)
            }
        }
    }

    override fun trafficUpdated(profileId: Long, stats: TrafficStats) {
        Log.e("J","trafficUpdated")
        if (profileId == 0L){
            findViewById<TextView>(R.id.netflow_total_textView).text = "▲   ${Formatter.formatFileSize(this, stats.txTotal)}\n▼   ${Formatter.formatFileSize(this, stats.rxTotal)}"
            findViewById<TextView>(R.id.netflow_rate_textView).text = " ${Formatter.formatFileSize(this, stats.txRate)}/s\n${Formatter.formatFileSize(this, stats.rxRate)}/s"
        }
    }

    private fun didClickedConnectButton() {
        when {
            state.canStop -> Core.stopService()
            else ->
                if ( false === ready ){
//                    longToast("需要连接服务器")
                    alert("连接服务器失败", "尊敬的用户") {
                        positiveButton("重试") { getVPNConfig() }
                    }.show()
                }else{
                    Core.startService()
                }

        }
    }

    data class ErrorData (
        val error : String = ""
    )

    data class LoginData (
        val token : String = "",
        val expiresDate : Int = 0,
        val total : Long = 0,
        val used : Long = 0
    )

    private fun updateUI(expiresDate :Int,total :Long,used:Long) {
        findViewById<TextView>(R.id.expireDateTextview).text = "有效期至："+ SimpleDateFormat("YYYY年MM月DD日hh:mm:ss").format(expiresDate)
        findViewById<TextView>(R.id.trafficTextView).text = "已用流量/共有流量:${Formatter.formatFileSize(this, used)}/${Formatter.formatFileSize(this, total)}"
        findViewById<ProgressBar>(R.id.trafficProgressBar).progress = if(total>0){(used*100/total).toInt()}else{0}
    }

    private fun login() {
        val androidID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Log.v("J",androidID)
        var post = ViewModelProvider(this).get<HttpPost>()
        post.post("https://frp.u03013112.win:18022/v1/android/login","{\"uuid\":\"${androidID}\"}",
            {str ->
                Log.v("J",str)
                toast("登陆成功!")
                val d = Gson().fromJson(str, LoginData::class.java)

                val token = d.token

                Log.v("J","token:"+token)
                DataStore.token = token
                updateUI(d.expiresDate,d.total,d.used)
                getVPNConfig()
                return@post
            }, {
                err -> Log.e("J", err)
                toast("登陆失败，正在重试!")
                login()
            }
        )
    }

    data class VPNConfig (
        val IP : String = "",
        val port : String = "",
        val method : String = "",
        val passwd : String = "",
        val expiresDate : String = ""
    )
    private  fun getVPNConfig() {
        var post = ViewModelProvider(this).get<HttpPost>()
        post.post("https://frp.u03013112.win:18022/v1/android/config","{\"token\":\"${DataStore.token}\"}",
            {str ->
                Log.v("J",str)
                val d = Gson().fromJson(str, VPNConfig::class.java)
                this.profile.host=d.IP
                this.profile.remotePort=d.port.toInt()
                this.profile.password=d.passwd
                this.profile.method=d.method
                ProfileManager.updateProfile(this.profile)
                ready = true
                return@post
            },
            {err ->
                ready = false
                Log.e("J", err)
                alert("连接服务器失败", "尊敬的用户") {
                    positiveButton("重试") { getVPNConfig() }
                }.show()
            }
        )
    }
}
