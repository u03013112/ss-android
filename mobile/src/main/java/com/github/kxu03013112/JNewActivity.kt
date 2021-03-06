package com.github.kxu03013112

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.provider.Settings
import android.text.format.Formatter
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.viewpager.widget.ViewPager
import com.github.kxu03013112.aidl.IShadowsocksService
import com.github.kxu03013112.aidl.ShadowsocksConnection
import com.github.kxu03013112.aidl.TrafficStats
import com.github.kxu03013112.bg.BaseService
import com.github.kxu03013112.database.Profile
import com.github.kxu03013112.database.ProfileManager
import com.github.kxu03013112.net.HttpPost
import com.github.kxu03013112.preference.DataStore
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_j_new.*
import kotlinx.coroutines.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.text.SimpleDateFormat
import java.util.*
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure

class JNewActivity : AppCompatActivity(), ShadowsocksConnection.Callback, RewardedVideoAdListener {
    val version : String = "v0.0.13"

    var viewPager: ViewPager? = null
    private var tabLayout: TabLayout? = null

    var state = BaseService.State.Idle
    lateinit var profile : Profile

    var mainFragment : MainFragment? = null

    private var rewardID = ""
    private lateinit var mRewardedVideoAd: RewardedVideoAd

    lateinit var webviewDialog: WebviewDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UMConfigure.init(this,"5e031dde4ca357e9300007f5","GOOGLE_PLAY",UMConfigure.DEVICE_TYPE_PHONE,null)
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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

        MobileAds.initialize(this) {}
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        mRewardedVideoAd.rewardedVideoAdListener = this

        updateAdButton()

        initWebviewDialog()

        zanzhu_button.onClick {
            webviewDialog.setUrl("https://frp.u03013112.win:18022/htmls/ss-html/#/zan")
            webviewDialog.show()
        }
    }

    override fun onStart() {
        super.onStart()
        login()
    }

    private val dialogHeight = 460f
    private fun initWebviewDialog() {
        webviewDialog = WebviewDialog(this)
        webviewDialog.setContentView(R.layout.layout_webview_dialog)
        webviewDialog.setDialogHeight(dialogHeight)

        webviewDialog.setOnDismissListener(DialogInterface.OnDismissListener { dialog ->
            Log.i("TAG", "cancel Dialog dismiss")
        })
    }

    private fun loadRewardedVideoAd() {
        mRewardedVideoAd.loadAd(rewardID, AdRequest.Builder().build())
//        Log.v("J","loadRewardedVideoAd:${rewardID}")
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = FragmentAdapter(supportFragmentManager)
        adapter.addFragment(MainFragment(), getString(R.string.setting))
        adapter.addFragment(StoreFragment(),"购买")
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
                mainFragment?.startWithLineConfig()
            }
        }
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

        updateAdButton()
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
    fun updateUI(expiresDate :Long,total :Long,used:Long) {
        Log.v("J","${expiresDate},${total},${used}")
        if (expiresDate == 0L){
            netflow_use_textView.text = "欢迎光临！\n大爷来玩啊~"
            op_button.text = "新用户点击领取使用大礼包"
            op_button.visibility = View.VISIBLE
            status_button.visibility = View.GONE
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
            status_button.visibility = View.VISIBLE
            op_button.onClick { }
            Log.v("J",calendar.time.toString())
            netflow_use_textView.text = "有效期至：\n${SimpleDateFormat("yyyy-MM-dd\nHH:mm:ss").format(calendar.time)}\n拥有流量：\n${Formatter.formatFileSize(this, total)}\n已用：${Formatter.formatFileSize(this, used)}"
            mainFragment?.getLineList()
        }
    }
    val prodectionList:ArrayList<MainFragment.getLineData> = ArrayList()
    var lineId = 0
    var lineName = ""
    private fun login() {
        version_text.text = "版本：${version}"
        var androidID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        if (androidID == null || androidID == "") {
            if (DataStore.androidID == ""){
                androidID = UUID.randomUUID().toString()
                DataStore.androidID = androidID
            }else{
                androidID = DataStore.androidID
            }
        }
        Log.v("J",androidID)
        val post = ViewModelProvider(this).get<HttpPost>()
        post.post("https://frp.u03013112.win:18022/v1/android/login","{\"uuid\":\"${androidID}\",\"version\":\"${version}\"}",
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

                    getGoogleAd()
                    showHello()
                    return@post
                }, {
            err -> Log.e("J", err)
            toast("登陆失败，正在重试!")
            login()
        }
        )
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
        Log.v("J","onRewardedVideoAdFailedToLoad ${errorCode}")
        GlobalScope.launch{
            delay(1500L)
            withContext(Dispatchers.Main){
                loadRewardedVideoAd()
            }
        }
    }

    override fun onRewardedVideoAdLoaded() {
//        toast("onRewardedVideoAdLoaded")
        Log.v("J","onRewardedVideoAdLoaded")
        updateAdButton()
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

    fun updateAdButton() {
        if (mRewardedVideoAd.isLoaded && (state == BaseService.State.Idle || state == BaseService.State.Stopped)) {
            ad_button.setTextColor(resources.getColor(R.color.porn_orange))
            ad_button.background.setTint(Color.WHITE)
            ad_button.onClick {
                ad_button.setTextColor(resources.getColor(R.color.gray_drak))
                ad_button.background.setTint(Color.LTGRAY)
                ad_button.onClick { }
                if (mRewardedVideoAd.isLoaded) {
                    mRewardedVideoAd.show()
                }
            }
        }else{
            ad_button.setTextColor(resources.getColor(R.color.gray_drak))
            ad_button.background.setTint(Color.LTGRAY)
            status_button.onClick {
            }
        }
    }

    fun showHello() {
        webviewDialog.setUrl("https://frp.u03013112.win:18022/htmls/ss-html/#/hello")
        webviewDialog.show()
    }
}
