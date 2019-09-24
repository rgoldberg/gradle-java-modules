/*
 * Copyright 2017 - 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.java.extension


import java.io.File.pathSeparator
import org.gradle.api.reflect.TypeOf
import org.gradle.java.jdk.JAVA_COMMON_TOOL


interface CommonJavaOptions: Options, SeparateValueSettableCascading {

    val addModules:        SeparableValueVarargOption<String>
    val limitModules:      SeparableValueVarargOption<String>
    val modulePath:        SeparableValueVarargOption<String>
    val upgradeModulePath: SeparableValueVarargOption<String>

    val patchModule: SeparableValueKeyedVarargOption<String, String>
    val addReads:    SeparableValueKeyedVarargOption<String, String>
    val addExports:  SeparableValueKeyedVarargOption<String, String> //TODO? moduleName: String, packageName: String

    // addModules targets
    val ALL_MODULE_PATH: String
    val ALL_SYSTEM:      String

    // addExports, addReads & addOpens targets
    val ALL_UNNAMED: String
}


abstract class CommonJavaOptionsInternal: CommonJavaOptions, OptionsInternal(), AutoGeneratableCascading, SeparableValueCascading {

    abstract override fun getPublicType(): TypeOf<out CommonJavaOptions>


    override fun config() {
        args
        .append(addModules)
        .append(limitModules)
        .append(modulePath)
        .append(upgradeModulePath)
        .append(patchModule)
        .append(addReads)
        .append(addExports)
    }


    override val addModules:        SeparableValueSetOptionInternal<String> by lazy {
        DefaultSeparableValueSetOptionInternal<String>(JAVA_COMMON_TOOL.OPTION_ADD_MODULES,         "=", ",",           this)
    }
    override val limitModules:      SeparableValueSetOptionInternal<String> by lazy {
        DefaultSeparableValueSetOptionInternal<String>(JAVA_COMMON_TOOL.OPTION_LIMIT_MODULES,       "=", ",",           this)
    }
    override val modulePath:        SeparableValueSetOptionInternal<String> by lazy {
        DefaultSeparableValueSetOptionInternal<String>(JAVA_COMMON_TOOL.OPTION_MODULE_PATH,         "=", pathSeparator, this)
    }
    override val upgradeModulePath: SeparableValueSetOptionInternal<String> by lazy {
        DefaultSeparableValueSetOptionInternal<String>(JAVA_COMMON_TOOL.OPTION_UPGRADE_MODULE_PATH, "=", pathSeparator, this)
    }

    override val patchModule: SeparableValueLinkedHashMultimapOptionInternal<String, String> by lazy {
        DefaultSeparableValueLinkedHashMultimapOptionInternal<String, String>(JAVA_COMMON_TOOL.OPTION_PATCH_MODULE, "=", "=", pathSeparator, this)
    }
    override val addReads:    SeparableValueLinkedHashMultimapOptionInternal<String, String> by lazy {
        DefaultSeparableValueLinkedHashMultimapOptionInternal<String, String>(JAVA_COMMON_TOOL.OPTION_ADD_READS,    "=", "=", ",",           this)
    }
    override val addExports:  SeparableValueLinkedHashMultimapOptionInternal<String, String> by lazy {
        DefaultSeparableValueLinkedHashMultimapOptionInternal<String, String>(JAVA_COMMON_TOOL.OPTION_ADD_EXPORTS,  "=", "=", ",",           this)
    }

    // addModules targets
    override val ALL_MODULE_PATH = JAVA_COMMON_TOOL.ALL_MODULE_PATH
    override val ALL_SYSTEM      = JAVA_COMMON_TOOL.ALL_SYSTEM

    // addExports, addReads & addOpens targets
    override val ALL_UNNAMED = JAVA_COMMON_TOOL.ALL_UNNAMED
}
