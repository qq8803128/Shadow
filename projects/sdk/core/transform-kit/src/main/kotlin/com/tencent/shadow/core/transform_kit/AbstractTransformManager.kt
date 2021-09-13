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

package com.tencent.shadow.core.transform_kit

import javassist.ClassPool
import javassist.CtClass
import java.lang.StringBuilder

abstract class AbstractTransformManager(ctClassInputMap: Map<CtClass, InputClass>,
                                        private val classPool: ClassPool,
                                        private val disableTransformClasses: Array<String>,
) {
    private val allInputClass = ctClassInputMap.keys
    private lateinit var filterInputClass: Set<CtClass>

    abstract val mTransformList: List<SpecificTransform>


    fun setupAll() {
        mTransformList.forEach {
            it.mClassPool = classPool
            filterInputClass = allInputClass.wrapper()
            it.setup(filterInputClass)

        }
    }

    fun fireAll() {
        mTransformList.flatMap { it.list }.forEach { transform ->
            transform.filter(filterInputClass).forEach {
                transform.transform(it)
            }
        }
    }

    fun Set<CtClass>.wrapper():Set<CtClass>{
        val classes = fillDisableTransformClasses()
        return this.filter {
            it.name !in classes.toStringArray()
        }.toSet()
    }

    fun Set<CtClass>.toStringArray(): Array<String>{
        val list = ArrayList<String>()
        forEach {
            list.add(it.name)
        }
        return list.toTypedArray()
    }

    fun Set<CtClass>.fillDisableTransformClasses(): Set<CtClass> {
        return this.filter {
            var result = it.name !in disableTransformClasses
            result
        }.toSet().fillDisableTransformOneChildPackage().toSet().fillDisableTransformAllChildPackage().toSet()
    }

    /**
     * xxx.xxx.xxx.*类型的包名
     */
    fun Set<CtClass>.fillDisableTransformOneChildPackage(): Set<CtClass> {
        return this.filter {
            var result = it.packageName !in disableTransformClasses.getOneChildPackage()
            result
        }.toSet()
    }

    /**
     * xxx.xxx.xxx.**类型的包名
     */
    fun Set<CtClass>.fillDisableTransformAllChildPackage(): Set<CtClass> {
        return this.filter { ctClass ->
            val packageName = ctClass.packageName
            var result = false
            disableTransformClasses.getAllChildPackage().forEach {
                if (packageName.startsWith(it)){
                    result = true
                    return@forEach
                }
            }
            result
        }.toSet()
    }

    private var mOneChildPackage: ArrayList<String> ?= null
    private var mAllChildPackage: ArrayList<String> ?= null

    fun Array<String>.getOneChildPackage():List<String>{
        if (mOneChildPackage != null){
            return mOneChildPackage!!
        }
        val list = ArrayList<String>()
        disableTransformClasses.forEach {
            if (it.endsWith(".*")){
                list.add(it.substring(0,it.lastIndexOf(".*")))
            }
        }
        mOneChildPackage = list
        return mOneChildPackage!!
    }

    fun Array<String>.getAllChildPackage():List<String>{
        if (mAllChildPackage != null){
            return mAllChildPackage!!
        }
        val list = ArrayList<String>()
        disableTransformClasses.forEach {
            if (it.endsWith(".**")){
                list.add(it.substring(0,it.lastIndexOf(".**")))
            }
        }
        mAllChildPackage = list
        return mAllChildPackage!!
    }

    class LogList : ArrayList<String>() {
        override fun add(element: String): Boolean {
            if (!contains(element)) {
                return super.add(element)
            }
            return false
        }

        override fun toString(): String {
            val builder = StringBuilder()
            forEach {
                builder.append(it + "\n")
            }
            return builder.toString()
        }
    }
}