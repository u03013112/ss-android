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
import android.util.Log
import android.view.View
import android.widget.Button
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.aidl.ShadowsocksConnection.Callback
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
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
            val intent = Intent()
            intent.setClass(this,MainActivity::class.java)
            startActivity(intent)
        }

        connectButton = findViewById(R.id.connect_button)
        connectButton.setOnClickListener{
            didClickedConnectButton()
        }

        this.getProfile()

        connection.connect(this, this)
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

    private val handler = Handler()
    private val connection = ShadowsocksConnection(handler, true)
    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) = changeState(state, msg, true)
    override fun onServiceConnected(service: IShadowsocksService) = changeState(try {
        BaseService.State.values()[service.state]
    } catch (_: RemoteException) {
        BaseService.State.Idle
    })
    fun changeState(state: BaseService.State, msg: String? = null, animate: Boolean = false) {
        this.state = state

        when(state) {
            BaseService.State.Idle,BaseService.State.Stopped -> {
                this.connectButton.text = "连  接"
                this.connectButton.setTextColor(Color.LTGRAY)
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

    fun didClickedConnectButton() {
        when {
            state.canStop -> Core.stopService()
            else -> Core.startService()
        }
    }
}
