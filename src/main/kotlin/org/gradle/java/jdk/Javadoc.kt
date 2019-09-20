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
package org.gradle.java.jdk


interface Javadoc: JavaSourceTool {

    companion object {
        const val OPTION_EXPAND_REQUIRES      = "--expand-requires"
        const val OPTION_SHOW_MEMBERS         = "--show-members"
        const val OPTION_SHOW_MODULE_CONTENTS = "--show-module-contents"
        const val OPTION_SHOW_PACKAGES        = "--show-packages"
        const val OPTION_SHOW_TYPES           = "--show-types"

        // OPTION_EXPAND_REQUIRES values
        const val TRANSITIVE = "transitive"

        // OPTION_EXPAND_REQUIRES, OPTION_SHOW_MODULE_CONTENTS & OPTION_SHOW_PACKAGES values
        const val ALL = "all"

        // OPTION_SHOW_MEMBERS & OPTION_SHOW_TYPES values
        const val PUBLIC  = "public"
        const val PACKAGE = "package"
        const val PRIVATE = "private"
    }
}
