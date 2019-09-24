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


interface ToggleOption: Option {

    operator fun invoke() {
        invoke(true)
    }

    operator fun invoke(isToggled: Boolean)
}

interface ToggleOptionInternal: ToggleOption, OptionInternal {

    val isToggled: Boolean
}

open class DefaultToggleOptionInternal(override val flag: String, isToggled: Boolean = false): ToggleOptionInternal {

    final override var isToggled: Boolean = isToggled
    private set


    override operator fun invoke(isToggled: Boolean) {
        this.isToggled = isToggled
    }
}

fun ArgAppendable.append(option: ToggleOptionInternal): ArgAppendable {
    if (option.isToggled) {
        append(option.flag)
    }

    return this
}
