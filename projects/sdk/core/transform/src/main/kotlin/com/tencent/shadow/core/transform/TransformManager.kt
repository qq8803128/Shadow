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

package com.tencent.shadow.core.transform

import com.tencent.shadow.core.transform.specific.*
import com.tencent.shadow.core.transform_kit.AbstractTransformManager
import com.tencent.shadow.core.transform_kit.InputClass
import com.tencent.shadow.core.transform_kit.SpecificTransform
import javassist.ClassPool
import javassist.CtClass

class TransformManager(ctClassInputMap: Map<CtClass, InputClass>,
                       classPool: ClassPool,
                       useHostContext: () -> Array<String>,
                       private val disableTransformClasses: () -> Array<String>
) : AbstractTransformManager(ctClassInputMap, classPool,disableTransformClasses.invoke()) {

    override val mTransformList: List<SpecificTransform> = listOf(
            ApplicationTransform(),
            ActivityTransform(),
            ServiceTransform(),
            IntentServiceTransform(),
            InstrumentationTransform(),
            FragmentSupportTransform(),
            DialogSupportTransform(),
            WebViewTransform(),
            ContentProviderTransform(),
            PackageManagerTransform(),
            PackageItemInfoTransform(),
            AppComponentFactoryTransform(),
            LayoutInflaterTransform(),
            KeepHostContextTransform(useHostContext())
    )
}