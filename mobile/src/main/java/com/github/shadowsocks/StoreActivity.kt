package com.github.shadowsocks

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
import com.github.shadowsocks.net.HttpPost
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_store.*
import kotlinx.android.synthetic.main.layout_production.view.*
import org.jetbrains.anko.longToast

class StoreActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        getProductionList()
        initRecyclerView()
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
}
