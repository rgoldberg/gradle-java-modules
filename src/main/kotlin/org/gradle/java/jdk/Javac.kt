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


val JAVAC = object: Javac() {}

abstract class Javac protected constructor(): JavaSourceTool() {

    val OPTION_TARGET = "-target" //TODO? --target for Java 13+

    // module options
    val OPTION_DEFAULT_MODULE_FOR_CREATED_FILES = "--default-module-for-created-files"
    val OPTION_MODULE_VERSION                   = "--module-version"
    val OPTION_PREFER                           = "-Xprefer:"
    val OPTION_PROCESSOR_MODULE_PATH            = "--processor-module-path"

    // OPTION_PREFER values
    val SOURCE = "source"
}
