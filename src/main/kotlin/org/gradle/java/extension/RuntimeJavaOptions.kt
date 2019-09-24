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


import org.gradle.api.reflect.TypeOf
import org.gradle.java.jdk.JAVA


interface RuntimeJavaOptions: CommonJavaOptions {

    val listModules:          ToggleOption
    val validateModules:      ToggleOption
    val showModuleResolution: ToggleOption

    val describeModule: SeparableValueScalarOption<String?>
    val module:         SeparableValueScalarOption<String?>
    fun module(moduleName: String, mainClassName: String)

    fun illegalAccessDefault()
    fun illegalAccessPermit()
    fun illegalAccessWarn()
    fun illegalAccessDebug()
    fun illegalAccessDeny()

    val addOpens: SeparableValueKeyedVarargOption<String, String>

    // addModules targets
    val ALL_DEFAULT: String
}


abstract class RuntimeJavaOptionsInternal: RuntimeJavaOptions, CommonJavaOptionsInternal() {

    //TODO?
    // -verbose:module

    abstract override fun getPublicType(): TypeOf<out RuntimeJavaOptions>


    override fun config() {
        super.config()

        args
        .append(listModules)
        .append(validateModules)
        .append(showModuleResolution)
        .append(describeModule)
        .append(module)
        .appendJoined(illegalAccess)
        .append(addOpens)
    }


    override val listModules          = DefaultToggleOptionInternal(JAVA.OPTION_LIST_MODULES)
    override val validateModules      = DefaultToggleOptionInternal(JAVA.OPTION_VALIDATE_MODULES)
    override val showModuleResolution = DefaultToggleOptionInternal(JAVA.OPTION_SHOW_MODULE_RESOLUTION)

    override val describeModule by lazy {DefaultSeparableValueScalarOptionInternal<String?>(JAVA.OPTION_DESCRIBE_MODULE, "=", null, this)}
    override val module         by lazy {DefaultSeparableValueScalarOptionInternal<String?>(JAVA.OPTION_MODULE,          "=", null, this)}
    override fun module(moduleName: String, mainClassName: String) {
        module(moduleName + '/' + mainClassName)
    }

    private val illegalAccess = DefaultScalarOptionInternal<String?>(JAVA.OPTION_ILLEGAL_ACCESS, null)
    override fun illegalAccessDefault() {
        illegalAccess(null)
    }
    override fun illegalAccessPermit() {
        illegalAccess(JAVA.PERMIT)
    }
    override fun illegalAccessWarn() {
        illegalAccess(JAVA.WARN)
    }
    override fun illegalAccessDebug() {
        illegalAccess(JAVA.DEBUG)
    }
    override fun illegalAccessDeny() {
        illegalAccess(JAVA.DENY)
    }

    override val addOpens by lazy {DefaultSeparableValueLinkedHashMultimapOptionInternal<String, String>(JAVA.OPTION_ADD_OPENS, "=", "=", ",", this)}

    // addModules targets
    override val ALL_DEFAULT = JAVA.ALL_DEFAULT
}
