/*
 * Tencent is pleased to support the open source community by making Tencent Shadow available.
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.tencent.shadow.core.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension

import com.tencent.shadow.core.transform.ShadowTransform
import com.tencent.shadow.core.transform_kit.AndroidClassPoolBuilder
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionContainer
import java.io.File
import java.io.FileWriter
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

class ShadowPlugin : Plugin<Project> {
    companion object{
        val mergeAssetsOutput: ArrayList<String> = ArrayList()
    }
    override fun apply(project: Project) {
        System.err.println("ShadowPlugin project.name==" + project.name)

        val baseExtension = getBaseExtension(project)
        val sdkDirectory = baseExtension.sdkDirectory
        val androidJarPath = "platforms/${baseExtension.compileSdkVersion}/android.jar"
        val androidJar = File(sdkDirectory, androidJarPath)

        //在这里取到的contextClassLoader包含运行时库(classpath方式引入的)shadow-runtime
        val contextClassLoader = Thread.currentThread().contextClassLoader

        val classPoolBuilder = AndroidClassPoolBuilder(project, contextClassLoader, androidJar)

        val shadowExtension = project.extensions.create("shadow", ShadowExtension::class.java)
        shadowExtension.assetsDir = project.file("src/main/assets/")
        if (!project.hasProperty("disable_shadow_transform")) {
            baseExtension.registerTransform(ShadowTransform(
                    project,
                    classPoolBuilder,
                    { shadowExtension.transformConfig.useHostContext },
                    { shadowExtension.transformConfig.disableTransformClasses }
            ))
        }

        //var mergeAssetsOutput: ArrayList<String> = ArrayList()

        val task = project.tasks.create("createShadowPropsAssetsTask"){
            it.group = "plugin"

        }
        task.outputs.upToDateWhen{false}

        project.extensions[AppExtension::class].run {
            applicationVariants.all {
                val file = it.mergeAssetsProvider.get().outputDir.asFile.get()
                if (!mergeAssetsOutput.contains(file.absolutePath)){
                    mergeAssetsOutput.add(file.absolutePath)
                }
                it.mergeAssetsProvider.get().dependsOn(task)
            }
        }




        /*
        project.extensions.create("packagePlugin", PackagePluginExtension::class.java, project)

        project.afterEvaluate {
            val packagePlugin = project.extensions.findByName("packagePlugin")
            val extension = packagePlugin as PackagePluginExtension
            val buildTypes = extension.buildTypes

            val tasks = mutableListOf<Task>()
            for (i in buildTypes) {
                println("buildTypes = " + i.name)
                val task = createPackagePluginTask(project, i)
                tasks.add(task)
            }
            if (tasks.isNotEmpty()) {
                project.tasks.create("packageAllPlugin") {
                    it.group = "plugin"
                    it.description = "打包所有插件"
                }.dependsOn(tasks)
            }
        }

         */
    }

    open class ShadowExtension {
        var transformConfig = TransformConfig()
        var pluginInfo = PluginInfo()
        lateinit var assetsDir: File


        fun transform(action: Action<in TransformConfig>) {
            action.execute(transformConfig)
        }

        fun plugin(action: Action<in PluginInfo>) {
            action.execute(pluginInfo)
            val file = File(assetsDir,"shadow.properties")
            assetsDir.mkdirs()
            if (file.exists() && file.isFile){
                file.delete()
            }
            if (pluginInfo.canWrite()) {
                writeShadowPropsFile(file, pluginInfo)
            }
        }

        private fun writeShadowPropsFile(file: File, pluginInfo: PluginInfo) {
            var fileWrite = FileWriter(file)
            var content = "# Shadow Plugin Config File\n" +
                    "# the settings of the plugin type(type=loader,runtime,manager,plugin)\n" +
                    "type=${pluginInfo.type}\n\n" +
                    "# part key name\n" +
                    "partKey=${pluginInfo.partKey}\n\n" +
                    "# business name\n" +
                    "businessName=${pluginInfo.businessName}\n\n" +
                    "# the plugin can use host class list(only type = plugin)\n# hostWhiteList=package1;package2\n" +
                    "hostWhiteList=${pluginInfo.hostWhiteList.newString()}\n\n" +
                    "# the plugin depends on other plugin part key name list(only type = plugin)\n# dependsOn=partKey1;partKey2\n" +
                    "dependsOn=${pluginInfo.dependsOn.newString()}"
            fileWrite.write(content)
            fileWrite.flush()
            fileWrite.close()
        }

        private fun Array<String>.newString(): String {
            var content = ""
            forEach {
                var end = if (it == get(size - 1)) {
                    ""
                } else {
                    ";"
                }
                content += (it + end)
            }
            return content
        }

        public fun PluginInfo.canWrite(): Boolean{
            var typeArray = arrayOf("runtime","manager","loader","plugin")

            type = type.toLowerCase()
            println(type)
            if (!typeArray.contains(type)){
                System.err.println("shadow config file the key type is error!")
                return false
            }
            if (type == "plugin"){
                if (partKey.length <= 0){
                    System.err.println("shadow config file the key partKey is null!")
                    return false
                }

                if (businessName.length <= 0){
                    System.err.println("shadow config file the key businessName is null!")
                    return false
                }
            }

            return true
        }
    }

    class PluginInfo {
        var type: String = ""
        var partKey: String = ""
        var businessName: String = ""
        var hostWhiteList: Array<String> = emptyArray()
        var dependsOn: Array<String> = emptyArray()
    }

    class TransformConfig {
        var useHostContext: Array<String> = emptyArray()
        var disableTransformClasses: Array<String> = emptyArray()
    }

    fun getBaseExtension(project: Project): BaseExtension {
        val plugin = project.plugins.getPlugin(AppPlugin::class.java)
        if (com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION == "3.0.0") {
            val method = BasePlugin::class.declaredFunctions.first { it.name == "getExtension" }
            method.isAccessible = true
            return method.call(plugin) as BaseExtension
        } else {
            return project.extensions.getByName("android") as BaseExtension
        }
    }


    private operator fun <T : Any> ExtensionContainer.get(type: KClass<T>): T {
        return getByType(type.java)!!
    }


}