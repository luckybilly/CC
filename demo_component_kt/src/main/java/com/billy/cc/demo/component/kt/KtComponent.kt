package com.billy.cc.demo.component.kt

import android.app.Activity
import android.content.Intent
import com.billy.cc.core.component.CC
import com.billy.cc.core.component.CCResult
import com.billy.cc.core.component.IComponent

/**
 * kotlin component demo
 * @author billy.qi
 */
class KtComponent : IComponent {

    override fun getName(): String {
        return "demo.ktComponent"
    }

    override fun onCall(cc: CC): Boolean {
        when (cc.actionName){
           "showActivity" -> openActivity(cc)
            //确保每个逻辑分支上都会调用CC.sendCCResult将结果发送给调用方
            else -> CC.sendCCResult(cc.callId
                    , CCResult.error("actionName ${cc.actionName} does not support"))
        }
        return false
    }

    private fun openActivity(cc: CC) {
        val context = cc.context
        val intent = Intent(context, MainActivity::class.java)
        if (context !is Activity) {
            //调用方没有设置context或app间组件跳转，context为application
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        //需要确保每个逻辑分支都会调用CC.sendCCResult将结果发送给调用方
        CC.sendCCResult(cc.callId, CCResult.success())
    }
}