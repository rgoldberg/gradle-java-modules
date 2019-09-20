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


interface JavaSourceTool: JavaCommonTool {

    companion object {
        const val OPTION_RELEASE = "--release"
        const val OPTION_SOURCE  = "-source"

        // module options
        const val OPTION_MODULE_SOURCE_PATH = "--module-source-path"
        const val OPTION_SYSTEM             = "--system"

        // OPTION_SYSTEM values
        const val NONE = "none"

        const val FILE_NAME_MODULE_INFO_JAVA = "module-info.java"
    }
}
