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
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.Button
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

class JActivity : AppCompatActivity(), Callback {

    lateinit var testButton : Button
    lateinit var connectButton: Button
    lateinit var profile : Profile

    var state = BaseService.State.Idle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_j)

        testButton = findViewById(R.id.test_button)
        testButton.setOnClickListener{
//            val intent = Intent()
//            intent.setClass(this,MainActivity::class.java)
//            startActivity(intent)
//            val tester = ViewModelProvider(this).get<HttpsTest>()
//            tester.testConnection()
//            tester.status.observe(this, Observer { status -> Log.v("J",status.toString()) })
            var post = ViewModelProvider(this).get<HttpPost>()

            post.post("https://frp.u03013112.win:18022/v1/ios/login","{\"uuid\":\"123456789\"}",{str -> Log.v("J","cb:"+str)},{err -> Log.e("J",err)})
        }
        connectButton = findViewById(R.id.connect_button)
        connectButton.setOnClickListener{
            didClickedConnectButton()
        }

        this.getProfile()

        connection.connect(this, this)
        connection.bandwidthTimeout = 1000
    }

    private fun setStatus(text: CharSequence) {
        Log.v("J",text.toString())
    }

    private fun setProfile() {
        this.profile.name = "J"
        this.profile.host="c9s1.jamjams.net"
        this.profile.remotePort=58700
        this.profile.password="xKpQV8wUVe"
        this.profile.method="aes-256-gcm"

        ProfileManager.updateProfile(this.profile)
    }
    private fun getProfile() {
        var profileList = ProfileManager.getAllProfiles()
        if (profileList != null && profileList.isNotEmpty()) {
            this.profile = profileList[0]
        }else{
            this.profile = ProfileManager.createProfile(Profile())
        }
        this.setProfile()
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
            else -> Core.startService()
        }
    }
}
