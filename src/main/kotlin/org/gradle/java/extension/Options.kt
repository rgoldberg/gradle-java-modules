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


import org.gradle.api.reflect.HasPublicType
import org.gradle.api.reflect.TypeOf


const val TASK_OPTIONS_EXTENSION_NAME = "options"


interface Options

abstract class OptionsInternal: Options, HasPublicType {

    abstract override fun getPublicType(): TypeOf<out Options>


    protected val configureMlist: MutableList<() -> Unit> = mutableListOf()
    protected val     resetMlist: MutableList<() -> Unit> = mutableListOf()

    protected abstract val args: ArgAppendable


    protected abstract fun config()

    fun configure() {
        config()

        for (action in configureMlist) {
            action()
        }
    }

    fun reset() {
        for (action in resetMlist) {
            action()
        }
    }
}
