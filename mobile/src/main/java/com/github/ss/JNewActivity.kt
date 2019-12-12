package com.github.ss

import android.graphics.Color
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.viewpager.widget.ViewPager
import com.github.ss.aidl.IShadowsocksService
import com.github.ss.aidl.ShadowsocksConnection
import com.github.ss.aidl.TrafficStats
import com.github.ss.bg.BaseService
import com.github.ss.database.Profile
import com.github.ss.database.ProfileManager
import com.github.ss.net.HttpPost
import com.github.ss.preference.DataStore
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
//import com.up.ads.UPAdsSdk
import kotlinx.android.synthetic.main.activity_j_new.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.image
import org.jetbrains.anko.longToast
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.toast
import java.text.SimpleDateFormat
import java.util.*
//import android.Manifest.permission.READ_PHONE_STATE
//import android.Manifest.permission.REQUEST_INSTALL_PACKAGES
//import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
//import androidx.core.app.ActivityCompat
//import android.content.pm.PackageManager
//import androidx.core.content.ContextCompat
//import android.os.Build



class JNewActivity : AppCompatActivity(), ShadowsocksConnection.Callback {
    var viewPager: ViewPager? = null
    private var tabLayout: TabLayout? = null

    var state = BaseService.State.Idle
    lateinit var profile : Profile

    var mainFragment : MainFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_j_new)
        viewPager = view_page
        setupViewPager(viewPager!!)

        tabLayout = tab_layout
        tabLayout!!.setupWithViewPager(viewPager)

        connect_button_new.setOnClickListener{
            didClickedConnectButton()
        }
        getProfile()

        connection.connect(this, this)
        connection.bandwidthTimeout = 1000
        login()
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = FragmentAdapter(supportFragmentManager)
        adapter.addFragment(MainFragment(), getString(R.string.setting))
        adapter.addFragment(AboutJFragment(), getString(R.string.about))

        viewPager.adapter = adapter
    }

    private fun didClickedConnectButton() {
        when {
            state.canStop -> {
                Core.stopService()
                netflow_speed_textView.visibility = View.INVISIBLE
            }
            else -> {
//                getVPNConfig()
//                netflow_speed_textView.visibility = View.VISIBLE
                mainFragment?.startWithLineConfig()
            }
        }
    }
    private  fun getVPNConfig() {
        var post = ViewModelProvider(this).get<HttpPost>()
        post.post("https://frp.u03013112.win:18022/v1/android/config","{\"token\":\"${DataStore.token}\"}",
                {str ->
                    Log.v("J",str)
                    val d = Gson().fromJson(str, JActivity.VPNConfig::class.java)

                    if (d.error != ""){
                        longToast(d.error)
                        return@post
                    }

                    this.profile.host=d.IP
                    this.profile.remotePort=d.port.toInt()
                    this.profile.password=d.passwd
                    this.profile.method=d.method
                    ProfileManager.updateProfile(this.profile)

                    Core.startService()
                    return@post
                },
                {err ->
                    Log.e("J", err)
                    alert("连接服务器失败", "尊敬的用户") {
                        positiveButton("重试") { getVPNConfig() }
                    }.show()
                }
        )
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
                status_button.text = getString(R.string.status_stop)
                connect_button_new.image = getDrawable(R.drawable.ic_close)
            }
            BaseService.State.Connected -> {
                status_button.text = getString(R.string.status_start)
                connect_button_new.image = getDrawable(R.drawable.ic_open)
            }
            else -> {
                status_button.text = getString(R.string.status_doing)
                connect_button_new.image = getDrawable(R.drawable.ic_doing)
            }
        }
        if (msg != null){
            Log.v("J","msg:${msg},animate:${animate}")
        }

        mainFragment?.updateZhi()
        mainFragment?.updateLineButton()
    }

    override fun trafficUpdated(profileId: Long, stats: TrafficStats) {
//        Log.e("J","trafficUpdated")
        netflow_speed_textView.visibility = View.VISIBLE
        if (profileId == 0L){
            netflow_speed_textView.text = "▲   ${Formatter.formatFileSize(this, stats.txRate)}/s\n▼   ${Formatter.formatFileSize(this, stats.rxRate)}/s"
        }
    }
    private data class keepaliveData(
            val needStop : Boolean = false,
            val expiresDate : String = "0",
            val total : String = "0",
            val used : String = "0"
    )
    override fun keepalive(str: String) {
        val d = Gson().fromJson(str, keepaliveData::class.java)
        updateUI(d.expiresDate.toLong(),d.total.toLong(),d.used.toLong())
    }
    private fun updateUI(expiresDate :Long,total :Long,used:Long) {
        Log.v("J","${expiresDate},${total},${used}")
        if (expiresDate == 0L){
            netflow_use_textView.text = "欢迎光临！\n大爷来玩啊~"
            op_button.text = "新用户点击领取使用大礼包"
            op_button.visibility = View.VISIBLE
            op_button.setOnClickListener{
                val post = ViewModelProvider(this).get<HttpPost>()
                post.post("https://frp.u03013112.win:18022/v1/android/buyTest","""
                    {"token":"${DataStore.token}","prodectionID":7}
                """.trimIndent(),
                        {str ->
                            Log.v("J",str)
                            toast("领取成功!")
                            val d = Gson().fromJson(str, JActivity.LoginData::class.java)
                            updateUI(d.expiresDate.toLong(),d.total.toLong(),d.used.toLong())
                            return@post
                        }, {
                    err -> Log.e("J", err)
                    toast("登陆失败，正在重试!")
                }
                )
            }
        }else {
            var calendar = Calendar.getInstance()
            calendar.timeInMillis = expiresDate * 1000
            op_button.visibility = View.GONE
            op_button.onClick { }
            Log.v("J",calendar.time.toString())
            netflow_use_textView.text = "有效期至：\n${SimpleDateFormat("yyyy-MM-dd\nHH:mm:ss").format(calendar.time)}\n拥有流量：${Formatter.formatFileSize(this, total)}\n已用：${Formatter.formatFileSize(this, used)}"
        }
    }

    private fun login() {
        val androidID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Log.v("J",androidID)
        val post = ViewModelProvider(this).get<HttpPost>()
        post.post("https://frp.u03013112.win:18022/v1/android/login","{\"uuid\":\"${androidID}\"}",
                {str ->
                    Log.e("J",str)
                    toast("登陆成功!")
                    val d = Gson().fromJson(str, JActivity.LoginData::class.java)
                    if (d == null) {
                        toast("d==null")
                        return@post
                    }
                    Log.e("J",d.toString())
                    val token = d.token
                    Log.v("J","token:${token}")
                    DataStore.token = token
                    updateUI(d.expiresDate.toLong(),d.total.toLong(),d.used.toLong())
                    mainFragment?.getLineList()
                    return@post
                }, {
            err -> Log.e("J", err)
            toast("登陆失败，正在重试!")
            login()
        }
        )
    }
}
