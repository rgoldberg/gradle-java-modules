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
import org.gradle.api.reflect.TypeOf.typeOf


interface EnabledGettable {

    val isEnabled: Boolean
}


interface EnabledSettable {

    fun setIsEnabled(isEnabled: Boolean)
    fun enable() {
        setIsEnabled(true)
    }
    fun disable() {
        setIsEnabled(false)
    }
}


interface EnabledSettableCascading: EnabledSettable {

    override fun setIsEnabled(isEnabled: Boolean) {
        setIsEnabled(isEnabled as Boolean?)
    }

    fun setIsEnabled(isEnabled: Boolean?)
}


interface Enableable: EnabledGettable, EnabledSettable

open class DefaultEnableable(override var isEnabled: Boolean = true): Enableable, HasPublicType {

    override fun getPublicType() =
        PUBLIC_TYPE


    override fun setIsEnabled(isEnabled: Boolean) {
        this.isEnabled = isEnabled
    }


    companion object {
        private val PUBLIC_TYPE = typeOf(Enableable::class.java)
    }
}


interface EnableableCascading: Enableable, EnabledSettableCascading

open class DefaultEnableableCascading(private val parent: EnabledGettable, private var isEnabledInternal: Boolean? = null): EnableableCascading, HasPublicType {

    override fun getPublicType() =
        PUBLIC_TYPE


    override val isEnabled: Boolean
    get() = isEnabledInternal ?: parent.isEnabled

    override fun setIsEnabled(isEnabled: Boolean?) {
        isEnabledInternal = isEnabled
    }


    companion object {
        private val PUBLIC_TYPE = typeOf(EnableableCascading::class.java)
    }
}
