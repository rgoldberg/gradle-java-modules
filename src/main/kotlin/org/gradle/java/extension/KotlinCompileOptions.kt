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
import org.gradle.api.reflect.TypeOf.typeOf
import org.gradle.kotlin.tool.KOTLINC
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


interface KotlinCompileOptions: Options, AutoGenerateSettableCascading {

    val addModules: VarargOption<String>
    val modulePath: VarargOption<String>

    // modulePath targets
    val ALL_MODULE_PATH: String
}


open class KotlinCompileOptionsInternal(
                autoGenerateParent: AutoGenerateGettable,
    private val      kotlinCompile: KotlinCompile
):
KotlinCompileOptions,
ModulePathOptionsInternal(),
AutoGeneratableCascading by DefaultAutoGeneratableCascading(autoGenerateParent) {

    //TODO?
    // -Xfriend-paths=<path>
    // -Xsingle-module
    // -Xallow-kotlin-package

    override fun getPublicType() =
        PUBLIC_TYPE


    override val args = {
        val kotlinJvmOptions = kotlinCompile.kotlinOptions

        val argMlist = kotlinJvmOptions.freeCompilerArgs.toMutableList()

        kotlinJvmOptions.freeCompilerArgs = argMlist

        ListArgAppendable(argMlist)
    }()

    override fun config() {
        args
        .appendJoined(addModules)
        .appendJoined(modulePath)
    }


    override val addModules = DefaultSetOptionInternal<String>(KOTLINC.OPTION_ADD_MODULES, ",")
    override val modulePath = DefaultSetOptionInternal<String>(KOTLINC.OPTION_MODULE_PATH, pathSeparator)


    // modulePath targets
    override val ALL_MODULE_PATH = KOTLINC.ALL_MODULE_PATH


    companion object {
        private val PUBLIC_TYPE = typeOf(KotlinCompileOptions::class.java)
    }
}
