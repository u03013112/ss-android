package com.github.kxu03013112

import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kxu03013112.net.HttpPost
import com.github.kxu03013112.preference.DataStore
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_store.*
import kotlinx.android.synthetic.main.layout_production.view.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.toast

class StoreActivity : AppCompatActivity(), RewardedVideoAdListener {

    private lateinit var mRewardedVideoAd: RewardedVideoAd

    private val rewardIDTest = "ca-app-pub-3940256099942544/5224354917"
    private val rewardID = "ca-app-pub-7592917484201943/3235936578"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        initRecyclerView()

        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        mRewardedVideoAd.rewardedVideoAdListener = this

        loadRewardedVideoAd()
        show_ad.onClick {
            if (mRewardedVideoAd.isLoaded) {
                mRewardedVideoAd.show()
                show_ad.isEnabled = false
            }
        }
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        getProductionList()
    }

    private fun loadRewardedVideoAd() {
        mRewardedVideoAd.loadAd(rewardIDTest, AdRequest.Builder().build())
    }

    private lateinit var storeAdapter: StoreAdapter
    private fun initRecyclerView(){
        list.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            val topSpacingDecorator = TopSpacingItemDecoration(30)
            addItemDecoration(topSpacingDecorator)
            storeAdapter = StoreAdapter()
            adapter = storeAdapter
        }
    }

    data class Production(
        val ID : String,
        val description : String,
        val total : String,
        val time : String,
        val price : String
    )
    data class ProductionList(
            val prodectionList:ArrayList<Production>
    )
    private fun getProductionList() {
        val post = ViewModelProvider(this).get<HttpPost>()
        post.post("https://frp.u03013112.win:18022/v1/android/getProdectionList","{}",
            {str ->
                Log.e("J",str)
                val d = Gson().fromJson(str, ProductionList::class.java)
                for ( p in d.prodectionList){
                    val traffic = "${Formatter.formatFileSize(this, p.total.toLong())}"
                    val time = "${p.time}天"
                    val price = "￥${p.price}元"
                    productionList.add(Production(p.ID,p.description,traffic,time,price))
                }
                wait.textSize=0f
                return@post
            }, {
                err -> Log.e("J", err)
                longToast("获取产品失败!")
            }
        )
    }

    private val productionList : ArrayList<Production> = ArrayList()

    inner class StoreAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return Holder(
                    LayoutInflater.from(parent.context).inflate(R.layout.layout_production, parent, false)
            )
        }

        override fun getItemCount(): Int {
            return productionList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when(holder) {
                is Holder -> {
                    holder.bind(productionList.get(position))
                }
            }
        }

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView),View.OnClickListener {
            var id = 0
            fun bind(production : Production) {
                itemView.title.setText(production.description)
                itemView.traffic.setText(production.total)
                itemView.time.setText(production.time)
                itemView.price.setText(production.price)
                id = production.ID.toInt()
                itemView.setOnClickListener(this)
            }
            override fun onClick(v: View?) {
                longToast("内测阶段，暂不支持支付")
                Log.v("J",id.toString())
            }
        }
    }

    inner class TopSpacingItemDecoration(private val padding: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            super.getItemOffsets(outRect, view, parent, state)
            outRect.top = padding
        }
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
//                    val d = Gson().fromJson(str, JActivity.LoginData::class.java)
//                    updateUI(d.expiresDate.toLong(),d.total.toLong(),d.used.toLong())
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
        toast("onRewardedVideoAdFailedToLoad")
        loadRewardedVideoAd()
    }

    override fun onRewardedVideoAdLoaded() {
//        toast("onRewardedVideoAdLoaded")
        show_ad.isEnabled = true
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
