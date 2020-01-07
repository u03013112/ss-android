package com.github.kxu03013112free

import android.graphics.Rect
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.github.kxu03013112free.net.HttpPost
import com.github.kxu03013112free.preference.DataStore
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_store.*
import kotlinx.android.synthetic.main.fragment_store.*
import kotlinx.android.synthetic.main.layout_production.view.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.longToast
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast


class StoreFragment : Fragment(),PurchasesUpdatedListener{

    private val p1 = "com.github.kxu03013112free.p1"
    private val p2 = "com.github.kxu03013112free.p2"
    private val p3 = "com.github.kxu03013112free.p3"
    private val p4 = "com.github.kxu03013112free.p4"
    private val p5 = "com.github.kxu03013112free.p5"


    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
                Log.v("J","purchase:${purchase}")
            }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Log.v("J","USER_CANCELED")
        } else {
            // Handle any other error codes.
            Log.v("J","other error codes.${billingResult.responseCode}")
        }
    }

    private fun handlePurchase(purchase:Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Grant entitlement to the user.
            Log.v("J","purchase${purchase}")

            var consumeParams = ConsumeParams.newBuilder().
                    setPurchaseToken(purchase.purchaseToken).
                    build()
            if (!purchase.isAcknowledged) {
                billingClient.consumeAsync(consumeParams) { billingResult, outToken ->
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        Log.v("J","consumeAsync ok,outToken = ${outToken}")
                        getProduction("${purchase.sku}")
                    }else{
                        Log.v("J","consumeAsync ${billingResult.responseCode}")
                    }
                }
            }
        }
    }
    lateinit var act : JNewActivity
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        act = activity as JNewActivity
        return inflater.inflate(R.layout.fragment_store, container, false)
    }

    lateinit private var billingClient: BillingClient

    private fun startConnection(){
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    Log.v("J","BillingClient is ready")
                    reflush_store_button.visibility = View.GONE
                    reflush_store_button.onClick { }
                    getSkuList()

                    billingClient.queryPurchases(BillingClient.SkuType.INAPP)//这个应该是类似restore，暂时只有消耗类

                }else{
                    Log.v("J","billingResult.responseCode:${billingResult.responseCode}")
                    if (billingResult.responseCode == BillingResponseCode.BILLING_UNAVAILABLE) {
                        reflush_store_button.visibility = View.VISIBLE
                        reflush_store_button.text = "无法连接到Google商店，请确保科学网络和Google账号已登录后点击 重试。"
                        reflush_store_button.onClick {
                            startConnection()
                        }
                    }
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.v("J","onBillingServiceDisconnected")
            }
        })
    }
    private fun getSkuList() {
        val skuList = ArrayList<String>()
        skuList.add(p1)
        skuList.add(p2)
        skuList.add(p3)
        skuList.add(p4)
        skuList.add(p5)
//        skuList.add("android.test.purchased")

//        skuList.add("android.test.item_unavailable")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            Log.v("J","billingResult:${billingResult.responseCode} ${billingResult.debugMessage}")

            if (billingResult.responseCode != BillingResponseCode.OK) {
                reflush_store_button.visibility = View.VISIBLE
                reflush_store_button.text = "获取商品失败,请确保科学网络 点击重试。"
                reflush_store_button.onClick {
                    getSkuList()
                }
            }else {
                Log.v("J","skuDetailsList:${skuDetailsList}")
                skuList.clear()
                if (skuDetailsList.size == 0) {
                    reflush_store_button.visibility = View.VISIBLE
                    reflush_store_button.text = "暂无商品列表 点击重试。"
                    reflush_store_button.onClick {
                        getSkuList()
                    }
                }else{
                    reflush_store_button.visibility = View.GONE
                    for (skuDetail in skuDetailsList) {
                        this.skuList.add(skuDetail)
                    }
                }
                initRecyclerView()
            }
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        billingClient = BillingClient.newBuilder(act).enablePendingPurchases().setListener(this).build()
        startConnection()
    }

    private var skuList : ArrayList<SkuDetails> = ArrayList()

    inner class StoreAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return Holder(
                    LayoutInflater.from(parent.context).inflate(R.layout.layout_production, parent, false)
            )
        }

        override fun getItemCount(): Int {
            return skuList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when(holder) {
                is Holder -> {
                    holder.bind(skuList.get(position))
                }
            }
        }

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView),View.OnClickListener {
            var id = ""
            private var skuDetails:SkuDetails? = null
            fun bind(production : SkuDetails) {
                itemView.title.text = production.description
                itemView.price.text = production.price
                itemView.traffic.text = ""
                itemView.time.text = ""
                id = production.getSku()
                itemView.setOnClickListener(this)
                skuDetails = production
            }
            override fun onClick(v: View?) {
                Log.v("J","onClick:${skuDetails}")
                val flowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetails)
                        .build()
                val responseCode = billingClient.launchBillingFlow(activity, flowParams)
                Log.v("J","responseCode:${responseCode}")
            }
        }
    }

    inner class TopSpacingItemDecoration(private val padding: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            super.getItemOffsets(outRect, view, parent, state)
            outRect.top = padding
        }
    }

    private lateinit var storeAdapter:StoreAdapter
    private fun initRecyclerView(){
        store_list.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            val topSpacingDecorator = TopSpacingItemDecoration(30)
            addItemDecoration(topSpacingDecorator)
            storeAdapter = StoreAdapter()
            adapter = storeAdapter
        }
    }

    private fun getProduction(pid : String) {
        Log.v("J","getProduction ${pid}")
        var prodectionID : Int

        when (pid) {
            p1 -> prodectionID = 6
            p2 -> prodectionID = 2
            p3 -> prodectionID = 3
            p4 -> prodectionID = 4
            p5 -> prodectionID = 5
            else -> {
                Log.e("J","not support this pid")
                return
            }
        }

        val post = ViewModelProvider(this).get<HttpPost>()
        post.post("https://frp.u03013112.win:18022/v1/android/buyTest","""
                    {"token":"${DataStore.token}","prodectionID":${prodectionID}}
                """.trimIndent(),
                {str ->
                    Log.v("J",str)
                    toast("购买成功!")
                    val d = Gson().fromJson(str, JActivity.LoginData::class.java)
                    act.updateUI(d.expiresDate.toLong(),d.total.toLong(),d.used.toLong())
                    return@post
                }, {
            err -> Log.e("J", err)
            toast("登陆失败，正在重试!")
        }
        )
    }
}
