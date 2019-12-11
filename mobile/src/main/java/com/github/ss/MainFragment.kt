package com.github.ss

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.ss.acl.Acl
import com.github.ss.bg.BaseService
import com.github.ss.database.ProfileManager
import kotlinx.android.synthetic.main.activity_j.*
import kotlinx.android.synthetic.main.activity_j.zhinengshangwang_switch
import kotlinx.android.synthetic.main.fragment_main.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast

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

        line_button.onClick {
            toast("暂未开放")
        }
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
}
