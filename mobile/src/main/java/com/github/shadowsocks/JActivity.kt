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
//import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.lifecycle.observe
import com.github.shadowsocks.acl.Acl
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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.android.synthetic.main.activity_j.*
import kotlinx.android.synthetic.main.activity_store.*
import kotlinx.android.synthetic.main.layout_apps.*
import kotlinx.android.synthetic.main.layout_apps.view.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.text.SimpleDateFormat
import java.util.*

class JActivity : AppCompatActivity(), Callback , RewardedVideoAdListener {

    lateinit var connectButton: Button
    lateinit var profile : Profile

    var state = BaseService.State.Idle

    private lateinit var mRewardedVideoAd: RewardedVideoAd

//    private val rewardID = "ca-app-pub-3940256099942544/5224354917"
//    private val rewardID = "ca-app-pub-7592917484201943/3235936578"
    private var rewardID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_j)

        connectButton = findViewById(R.id.connect_button)
        connectButton.setOnClickListener{
            didClickedConnectButton()
        }

        this.getProfile()

        connection.connect(this, this)
        connection.bandwidthTimeout = 1000

        login()
        updateZhi()

        zhinengshangwang_switch.setOnClickListener{

            if (this.profile.route == Acl.GFWLIST){
//                全局模式
                this.profile.route = Acl.ALL
                ProfileManager.updateProfile(this.profile)
            }else{
//                智能模式
                this.profile.route = Acl.GFWLIST
                ProfileManager.updateProfile(this.profile)
            }
            updateZhi()
        }
        MobileAds.initialize(this) {}
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        mRewardedVideoAd.rewardedVideoAdListener = this

        show_ad_j.onClick {
            if (mRewardedVideoAd.isLoaded) {
                mRewardedVideoAd.show()
                show_ad_j.isEnabled = false
            }
        }
    }

    private fun loadRewardedVideoAd() {
        mRewardedVideoAd.loadAd(rewardID, AdRequest.Builder().build())
        Log.v("J","loadRewardedVideoAd:${rewardID}")
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
        if (msg != null){
            Log.v("J","msg:${msg},animate:${animate}")
        }
        updateZhi()
    }

    override fun trafficUpdated(profileId: Long, stats: TrafficStats) {
        Log.e("J","trafficUpdated")
        if (profileId == 0L){
            findViewById<TextView>(R.id.netflow_total_textView).text = "▲   ${Formatter.formatFileSize(this, stats.txTotal)}\n▼   ${Formatter.formatFileSize(this, stats.rxTotal)}"
            findViewById<TextView>(R.id.netflow_rate_textView).text = " ${Formatter.formatFileSize(this, stats.txRate)}/s\n${Formatter.formatFileSize(this, stats.rxRate)}/s"
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
    private fun didClickedConnectButton() {
        when {
            state.canStop -> Core.stopService()
            else -> getVPNConfig()
        }
    }

    data class ErrorData (
        val error : String = ""
    )

    data class LoginData (
        val token : String = "",
        val expiresDate : String = "0",
        val total : String = "0",
        val used : String = "0"
    )

    private var debug = 0

    private fun updateUI(expiresDate :Long,total :Long,used:Long) {
        Log.v("J","${expiresDate},${total},${used}")
        if (expiresDate == 0L){
            findViewById<TextView>(R.id.expireDateTextview).text = "欢迎光临！"
            findViewById<TextView>(R.id.trafficTextView).text = "大爷来玩啊~"
            findViewById<ProgressBar>(R.id.trafficProgressBar).visibility = View.INVISIBLE
            findViewById<Button>(R.id.connect_button).visibility = View.INVISIBLE
            findViewById<Button>(R.id.purshaseButton).text = "新用户点击领取使用大礼包"
            findViewById<Button>(R.id.purshaseButton).setOnClickListener{
                val post = ViewModelProvider(this).get<HttpPost>()
                post.post("https://frp.u03013112.win:18022/v1/android/buyTest","""
                    {"token":"${DataStore.token}","prodectionID":7}
                """.trimIndent(),
                        {str ->
                            Log.v("J",str)
                            toast("领取成功!")
                            val d = Gson().fromJson(str, LoginData::class.java)
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
            Log.v("J",calendar.time.toString())
            findViewById<TextView>(R.id.expireDateTextview).text = "有效期至：${SimpleDateFormat("yyyy年MM月dd日HH:mm:ss").format(calendar.time)}"
            findViewById<TextView>(R.id.trafficTextView).text = "已用流量/共有流量:${Formatter.formatFileSize(this, used)}/${Formatter.formatFileSize(this, total)}"
            findViewById<ProgressBar>(R.id.trafficProgressBar).progress = if (total > 0) {
                (used * 100 / total).toInt()
            } else {
                0
            }
            findViewById<Button>(R.id.connect_button).visibility = View.VISIBLE
            findViewById<Button>(R.id.purshaseButton).text = "购买流量"
            findViewById<ProgressBar>(R.id.trafficProgressBar).visibility = View.VISIBLE
            findViewById<Button>(R.id.purshaseButton).setOnClickListener{
                debug ++
                if (debug > 10) {
                    longToast("测试模式:${DataStore.token}")
                } else {
//                    var intent : Intent = Intent(this,StoreActivity::class.java)
//                    startActivity(intent)
                    toast("内测阶段，暂未开放")
                }

            }
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
                val d = Gson().fromJson(str, LoginData::class.java)
                if (d == null) {
                    toast("d==null")
                    return@post
                }
                Log.e("J",d.toString())
                val token = d.token
                Log.v("J","token:${token}")
                DataStore.token = token
                updateUI(d.expiresDate.toLong(),d.total.toLong(),d.used.toLong())
                getGoogleAd()
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
        val expiresDate : String = "",
        val error: String = ""
    )
    private  fun getVPNConfig() {
        var post = ViewModelProvider(this).get<HttpPost>()
        post.post("https://frp.u03013112.win:18022/v1/android/config","{\"token\":\"${DataStore.token}\"}",
            {str ->
                Log.v("J",str)
                val d = Gson().fromJson(str, VPNConfig::class.java)

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

    private fun updateZhi(){

        zhinengshangwang_switch.isEnabled = (state == BaseService.State.Idle || state == BaseService.State.Stopped)

        if (this.profile.route == Acl.ALL){
            zhinengshangwang_text.setText("全局模式")
            zhinengshangwang_help.setText("所有流量都将通过伟大的M神，包括国内流量，并计费。")
        }else if (this.profile.route == Acl.GFWLIST){
            zhinengshangwang_text.setText("智能模式")
            zhinengshangwang_help.setText("只有海外流量会通过伟大的M神，并计费。")
        }else{
            zhinengshangwang_text.setText("")
            zhinengshangwang_help.setText("")
        }
    }

    data class GetGoogleAdData (
        val id:String = ""
    )
    private fun getGoogleAd() {
        var post = ViewModelProvider(this).get<HttpPost>()
        post.post("https://frp.u03013112.win:18022/v1/android/getGoogleAd","{\"token\":\"${DataStore.token}\"}",
                {str ->
                    Log.v("J",str)
                    val d = Gson().fromJson(str, GetGoogleAdData::class.java)
                    rewardID = d.id
                    loadRewardedVideoAd()
                    return@post
                },
                {err ->
                    Log.e("J", err)
                    toast("广告ID未找到。")
                }
        )
    }

    override fun onRewarded(reward: RewardItem) {
//        longToast( "onRewarded! currency: ${reward.type} amount: ${reward.amount}")
        // Reward the user.
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

    override fun onRewardedVideoAdLeftApplication() {
//        toast("onRewardedVideoAdLeftApplication")
    }

    override fun onRewardedVideoAdClosed() {
//        toast("onRewardedVideoAdClosed")
        loadRewardedVideoAd()
    }

    override fun onRewardedVideoAdFailedToLoad(errorCode: Int) {
        toast("onRewardedVideoAdFailedToLoad${errorCode}")
        Log.v("J","onRewardedVideoAdFailedToLoad${errorCode}")
        loadRewardedVideoAd()
    }

    override fun onRewardedVideoAdLoaded() {
//        toast("onRewardedVideoAdLoaded")
        Log.v("J","onRewardedVideoAdLoaded")
        show_ad_j.isEnabled = true
    }

    override fun onRewardedVideoAdOpened() {
//        toast("onRewardedVideoAdOpened")
    }

    override fun onRewardedVideoStarted() {
//        toast("onRewardedVideoStarted")
    }

    override fun onRewardedVideoCompleted() {
//        toast("onRewardedVideoCompleted")
    }
}
