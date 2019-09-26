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


val JAR = object: Jar() {}

abstract class Jar protected constructor() {

    // module options: create | update
    val OPTION_HASH_MODULES   = "--hash-modules"
    val OPTION_MODULE_PATH    = "--module-path"
    val OPTION_MODULE_VERSION = "--module-version"

    // module options: describe
    val OPTION_DESCRIBE_MODULE = "--describe-module"
}
