package com.tencent.shadow.core.loader.util

import android.content.ComponentName
import android.content.Intent
import com.tencent.shadow.core.loader.exceptions.ParsePluginApkException
import com.tencent.shadow.core.loader.infos.PluginComponentInfo

object AdapterPackage{
    val fixMultiPackageName = true
    val pluginClassComponentMap: MutableMap<String, ComponentName> = HashMap()
    @Throws(ParsePluginApkException::class)
    fun checkPackageName(hostPackageName: String,plugPackageName: String) {
        if (!fixMultiPackageName && hostPackageName != plugPackageName){
            throw ParsePluginApkException("插件和宿主包名不一致。宿主:${hostPackageName} 插件:${plugPackageName}")
        }
        android.util.Log.d("AdapterPackage","插件包名检测完成")
    }
    
    fun fixPluginComponentName(intent: Intent, pluginComponentInfoMap: MutableMap<ComponentName, PluginComponentInfo>){
        if (fixMultiPackageName){
            android.util.Log.d("AdapterPackage","修复插件ComponentName(${intent.component!!.className})")
            if (pluginClassComponentMap.size != pluginComponentInfoMap.size){
                pluginComponentInfoMap.forEach {
                    pluginClassComponentMap.put(it.key.className,it.key)
                }
            }
            val componentName: ComponentName? = pluginClassComponentMap[intent.component!!.className]
            if (componentName != null){
                intent.component = componentName
                android.util.Log.d("AdapterPackage","插件ComponentName修复成功")
            }else{
                android.util.Log.d("AdapterPackage","插件ComponentName修复失败")
            }
        }
    }
}