package com.github.ssu03013112

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
import com.github.ssu03013112.net.HttpPost
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_store.*
import kotlinx.android.synthetic.main.fragment_store.*
import kotlinx.android.synthetic.main.layout_production.view.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.longToast


class StoreFragment : Fragment(),PurchasesUpdatedListener {
    override fun onPurchasesUpdated(p0: BillingResult?, p1: MutableList<Purchase>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        skuList.add("android.test.purchased")
        skuList.add("android.test.canceled")
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
                for (skuDetail in skuDetailsList) {
                    this.skuList.add(skuDetail)
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
            fun bind(production : SkuDetails) {
                itemView.title.text = production.description
                itemView.price.text = production.price
                itemView.traffic.text = ""
                itemView.time.text = ""
                id = production.getSku()
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
}
