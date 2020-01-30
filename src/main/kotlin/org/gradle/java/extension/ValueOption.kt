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


import org.gradle.process.JavaForkOptions


interface ValueOptionInternal<T, I>: OptionInternal {

    val value:         T
    val valueIterable: Iterable<I>

    fun valueString(i: I): String?
}

interface ArgAppendable {

    fun append(arg: String)
    fun append(flag: String, value: String)


    fun <T, I> appendJoined(option: ValueOptionInternal<T, I>): ArgAppendable {
        option.valueIterable.asSequence()
        .mapNotNull {option.valueString(it)}
        .forEach {
            append(option.flag + it)
        }

        return this
    }

    fun <T, I> appendSeparated(option: ValueOptionInternal<T, I>): ArgAppendable {
        option.valueIterable.asSequence()
        .mapNotNull {option.valueString(it)}
        .forEach {
            append(option.flag, it)
        }

        return this
    }

    fun <T, I> append(option: SeparableValueOptionInternal<T, I>): ArgAppendable {
        val append: (String) -> Unit =
            if (option.separateValue.isEnabled) {
                {
                    append(option.flag, it)
                }
            }
            else {
                {
                    append(option.flag + option.flagValueSeparator + it)
                }
            }

        option.valueIterable.asSequence()
        .mapNotNull {option.valueString(it)}
        .forEach {
            append(it)
        }

        return this
    }
}

open class ListArgAppendable(val argMlist: MutableList<String>): ArgAppendable {

    override fun append(arg: String) {
        argMlist += arg
    }

    override fun append(flag: String, value: String) {
        argMlist += flag
        argMlist += value
    }
}

open class JavaForkOptionsArgAppendable(val forkOptions: JavaForkOptions): ArgAppendable {

    override fun append(arg: String) {
        forkOptions.jvmArgs(arg)
    }

    override fun append(flag: String, value: String) {
        forkOptions.jvmArgs(flag, value)
    }
}
