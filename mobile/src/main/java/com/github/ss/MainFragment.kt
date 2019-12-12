package com.github.ss

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.ss.acl.Acl
import com.github.ss.bg.BaseService
import com.github.ss.database.ProfileManager
import com.github.ss.net.HttpPost
import com.github.ss.preference.DataStore
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_j.*
import kotlinx.android.synthetic.main.activity_j.zhinengshangwang_switch
import kotlinx.android.synthetic.main.activity_store.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.layout_line.view.*
import kotlinx.android.synthetic.main.layout_production.view.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.longToast
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast

class MainFragment : Fragment() {
    lateinit var act : JNewActivity
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        act = activity as JNewActivity
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        act.mainFragment = this
        updateZhi()
        super.onViewCreated(view, savedInstanceState)

        zhinengshangwang_new_switch.setOnClickListener{
            val act = activity as JNewActivity
            if (act.profile.route == Acl.GFWLIST){
//                全局模式
                act.profile.route = Acl.ALL
                ProfileManager.updateProfile(act.profile)
            }else{
//                智能模式
                act.profile.route = Acl.GFWLIST
                ProfileManager.updateProfile(act.profile)
            }
            updateZhi()
        }
    }

    override fun onDestroyView() {
        act.mainFragment = null
        super.onDestroyView()
    }
    fun updateZhi(){
        Log.v("J","act=${act},status=${act.state}")
        zhinengshangwang_new_switch.isEnabled = (act.state == BaseService.State.Idle || act.state == BaseService.State.Stopped)

        if (act.profile.route == Acl.ALL){
            zhinengshangwang_new_text.setText(getString(R.string.all_proxy))
            zhinengshangwang_new_help.setText(getString(R.string.all_proxy_help))
        }else if (act.profile.route == Acl.GFWLIST){
            zhinengshangwang_new_text.setText(getString(R.string.gfw_proxy))
            zhinengshangwang_new_help.setText(getString(R.string.gfw_proxy_help))
        }else{
            zhinengshangwang_new_text.setText("")
            zhinengshangwang_new_help.setText("")
        }
    }
    data class getLineData(
            val id : String = "",
            val name : String = "",
            val address : String = "",
            val description : String = ""
    )
    data class getLineListData(
            val error : String = "",
            val list:ArrayList<getLineData>
    )
    private val prodectionList:ArrayList<getLineData> = ArrayList()
    fun getLineList(){
        val post = ViewModelProvider(this).get<HttpPost>()
        post.post("https://frp.u03013112.win:18022/v1/config/get-line-list","{\"token\":\"${DataStore.token}\"}",
                {str ->
                    Log.e("J",str)
                    val d = Gson().fromJson(str, getLineListData::class.java)
                    if (d == null) {
                        toast("d==null")
                        Log.e("J", "d==null")
                        return@post
                    }
                    if (d.error != null && d.error != ""){
                        longToast(d.error)
                        Log.e("J", "error:${d.error}")
                        updateLineButton()
                        return@post
                    }
                    for (line in d.list) {
                        prodectionList.add(line)
                    }
                    initRecyclerView()
                    updateLineButton()
                    return@post
                }, {
            err -> Log.e("J", err)
            toast("获取线路列表失败")}
        )
    }

    fun updateLineButton(){
        if (prodectionList.count() <= 0) {
            line_button.text = "无线路"
            line_button.onClick {
                getLineList()
            }
        }else{
            line_button.text = "有线路"
            line_button.onClick {
                line_list.visibility = View.VISIBLE
            }
        }
    }

    private lateinit var lineAdapter : LineAdapter
    private fun initRecyclerView(){
        line_list.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            val topSpacingDecorator = TopSpacingItemDecoration(30)
            addItemDecoration(topSpacingDecorator)
            lineAdapter = LineAdapter()
            adapter = lineAdapter
        }
    }
    inner class TopSpacingItemDecoration(private val padding: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            super.getItemOffsets(outRect, view, parent, state)
            outRect.top = padding
        }
    }
    inner class LineAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return Holder(
                    LayoutInflater.from(parent.context).inflate(R.layout.layout_line, parent, false)
            )
        }

        override fun getItemCount(): Int {
            return prodectionList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when(holder) {
                is Holder -> {
                    holder.bind(prodectionList.get(position))
                }
            }
        }

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView),View.OnClickListener {
            var id = 0
            fun bind(line : getLineData) {
                itemView.line_name.setText(line.name)
                itemView.line_address.setText(line.address)
                itemView.line_desc.setText(line.description)
                id = line.id.toInt()
                itemView.setOnClickListener(this)
            }
            override fun onClick(v: View?) {
                longToast("内测阶段，暂不支持支付")
                Log.v("J",id.toString())
            }
        }
    }
}
