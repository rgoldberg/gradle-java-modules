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


import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf


interface ScalarOption<T>: Option {

    operator fun invoke(value: T)
}

interface ScalarOptionInternal<T>: ScalarOption<T>, ValueOptionInternal<T, T> {

    override var value: T
}

open class DefaultScalarOptionInternal<T>(
    override val flag:  String,
    override var value: T
):
ScalarOptionInternal<T> {

    override val valueIterable: ImmutableSet<T>
    get() =
        when (val v = value) {
            null -> persistentSetOf()
            else -> persistentSetOf(v)
        }

    override fun valueString(i: T) =
        i?.toString()

    override operator fun invoke(value: T) {
        this.value = value
    }
}
