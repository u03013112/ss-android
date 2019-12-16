package com.github.ss

import android.content.Context
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
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.longToast
import org.jetbrains.anko.support.v4.toast
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import kotlinx.android.synthetic.main.activity_j_new.*
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.alert


class MainFragment : Fragment() {
    lateinit var act : JNewActivity
    lateinit var sharedPreferences : SharedPreferences
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        act = activity as JNewActivity
        sharedPreferences = act.getSharedPreferences("defalut", MODE_PRIVATE)
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    private val lineIDKey = "lineIDKey"
    private var lineId = 0
    private var lineName = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        act.mainFragment = this
        updateZhi()
        super.onViewCreated(view, savedInstanceState)

        zhinengshangwang_new_switch.setOnClickListener{
            val act = activity as JNewActivity
            if (act.profile.route == Acl.BYPASS_LAN_CHN){
//                全局模式
                act.profile.route = Acl.ALL
                ProfileManager.updateProfile(act.profile)
            }else{
//                智能模式
                act.profile.route = Acl.BYPASS_LAN_CHN
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
//        Log.v("J","act=${act},status=${act.state}")
        zhinengshangwang_new_switch.isEnabled = (act.state == BaseService.State.Idle || act.state == BaseService.State.Stopped)

        if (act.profile.route == Acl.ALL){
            zhinengshangwang_new_text.setText(getString(R.string.all_proxy))
            zhinengshangwang_new_help.setText(getString(R.string.all_proxy_help))
        }else if (act.profile.route == Acl.BYPASS_LAN_CHN){
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
                        if (d.error == "expired"){
                            act.status_button.text = "状态：已过期，请及时续费"
                        }else if (d.error == "outOfTraffic"){
                            act.status_button.text = "状态：流量不足，请及时续费"
                        }
                        updateLineButton()
                        return@post
                    }else{
                        when(act.state) {
                            BaseService.State.Idle,BaseService.State.Stopped -> {
                                act.status_button.text = getString(R.string.status_stop)
                            }
                            BaseService.State.Connected -> {
                                act.status_button.text = getString(R.string.status_start)
                            }
                            else -> {
                                act.status_button.text = getString(R.string.status_doing)
                            }
                        }
                    }

                    var id = sharedPreferences.getString(lineIDKey,"")
                    var lineIdOk = false
                    for (line in d.list) {
                        prodectionList.add(line)
                        if (line.id == id){
                            lineIdOk = true
                            lineName = line.name
                            lineId = line.id.toInt()
                        }
                    }
                    if (lineIdOk == false && prodectionList.size > 0) {
                        sharedPreferences.edit().putString(lineIDKey,prodectionList[0].id)
                        lineName = prodectionList[0].name
                        lineId = prodectionList[0].id.toInt()
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
        val enable = (act.state == BaseService.State.Idle || act.state == BaseService.State.Stopped)

        if (enable == false) {
            line_button.textColor = resources.getColor(R.color.gray_drak)
            line_button.onClick {
                longToast("已连接状态不能切换线路")
            }
        }else {
            line_button.textColor = resources.getColor(R.color.black)
            if (prodectionList.count() <= 0) {
                line_button.text = "无线路"
                line_button.onClick {
                    getLineList()
                }
            }else{
                line_button.text = lineName
                line_button.onClick {
                    if (line_list.visibility == View.VISIBLE){
                        line_list.visibility = View.GONE
                    }else {
                        line_list.visibility = View.VISIBLE
                    }
                }
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
            var id = ""
            fun bind(line : getLineData) {
                itemView.line_name.setText(line.name)
                itemView.line_address.setText(line.address)
                itemView.line_desc.setText(line.description)
                id = line.id
                itemView.setOnClickListener(this)
            }
            override fun onClick(v: View?) {
                Log.v("J","线路被选择:${id}")
                sharedPreferences.edit().putString(lineIDKey,id).apply()
                lineId = id.toInt()
                lineName = itemView.line_name.text.toString()
                line_list.visibility = View.GONE
                updateLineButton()
            }
        }
    }
    fun startWithLineConfig() {
        var post = ViewModelProvider(this).get<HttpPost>()
        post.post("https://frp.u03013112.win:18022/v1/config/get-line-config","{\"token\":\"${DataStore.token}\",\"id\":${lineId}}",
            {str ->
                Log.v("J",str)
                val d = Gson().fromJson(str, JActivity.VPNConfig::class.java)

                if (d.error != ""){
                    longToast(d.error)
                    return@post
                }

                act.profile.host=d.IP
                act.profile.remotePort=d.port.toInt()
                act.profile.password=d.passwd
                act.profile.method=d.method
                ProfileManager.updateProfile(act.profile)

                Core.startService()
                return@post
            },
            {err ->
                Log.e("J", err)
                alert("连接服务器失败", "尊敬的用户") {
                    positiveButton("重试") { startWithLineConfig() }
                }.show()
            }
        )
    }
}
