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


val JAVA = object: Java() {}

abstract class Java protected constructor(): JavaCommonTool() {

    // module options
    val OPTION_ADD_OPENS              = "--add-opens"
    val OPTION_DESCRIBE_MODULE        = "--describe-module"
    val OPTION_ILLEGAL_ACCESS         = "--illegal-access="
    val OPTION_LIST_MODULES           = "--list-modules"
    val OPTION_SHOW_MODULE_RESOLUTION = "--show-module-resolution"
    val OPTION_VALIDATE_MODULES       = "--validate-modules"

    // OPTION_ADD_MODULES values
    val ALL_DEFAULT = "ALL-DEFAULT"

    // OPTION_ILLEGAL_ACCESS values
    val PERMIT = "permit"
    val WARN   = "warn"
    val DEBUG  = "debug"
    val DENY   = "deny"
}
