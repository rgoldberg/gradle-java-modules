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


import java.io.File.pathSeparator
import java.lang.System.lineSeparator
import kotlinx.collections.immutable.ImmutableCollection
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.reflect.TypeOf
import org.gradle.java.jdk.JAVA_COMMON_TOOL
import org.gradle.java.util.containsModules


interface CommonJavaOptions: Options, SeparateValueSettableCascading {

    val addModules:        SeparableValueVarargOption<String>
    val limitModules:      SeparableValueVarargOption<String>
    val modulePath:        SeparableValueVarargOption<String>
    val upgradeModulePath: SeparableValueVarargOption<String>

    val patchModule: SeparableValueKeyedVarargOption<String, String>
    val addReads:    SeparableValueKeyedVarargOption<String, String>
    val addExports:  SeparableValueKeyedVarargOption<String, String> //TODO? moduleName: String, packageName: String

    // addModules targets
    val ALL_MODULE_PATH: String
    val ALL_SYSTEM:      String

    // addExports, addReads & addOpens targets
    val ALL_UNNAMED: String
}


abstract class CommonJavaOptionsInternal: CommonJavaOptions, ModulePathOptionsInternal(), AutoGeneratableCascading, SeparableValueCascading {

    abstract override fun getPublicType(): TypeOf<out CommonJavaOptions>


    override fun config() {
        args
        .append(addModules)
        .append(limitModules)
        .append(modulePath)
        .append(upgradeModulePath)
        .append(patchModule)
        .append(addReads)
        .append(addExports)
    }


    protected open inner class AddModules:
    DefaultSeparableValueSetOptionInternal<String>(JAVA_COMMON_TOOL.OPTION_ADD_MODULES,         "=", ",",           this)
    override val addModules:        SeparableValueSetOptionInternal<String> by lazy {
        AddModules()
    }

    protected open inner class LimitModules:
    DefaultSeparableValueSetOptionInternal<String>(JAVA_COMMON_TOOL.OPTION_LIMIT_MODULES,       "=", ",",           this)
    override val limitModules:      SeparableValueSetOptionInternal<String> by lazy {
        LimitModules()
    }

    protected open inner class ModulePath:
    DefaultSeparableValueSetOptionInternal<String>(JAVA_COMMON_TOOL.OPTION_MODULE_PATH,         "=", pathSeparator, this)
    override val modulePath:        SeparableValueSetOptionInternal<String> by lazy {
        ModulePath()
    }

    protected open inner class UpgradeModulePath:
    DefaultSeparableValueSetOptionInternal<String>(JAVA_COMMON_TOOL.OPTION_UPGRADE_MODULE_PATH, "=", pathSeparator, this)
    override val upgradeModulePath: SeparableValueSetOptionInternal<String> by lazy {
        UpgradeModulePath()
    }

    protected open inner class PatchModule:
    DefaultSeparableValueLinkedHashMultimapOptionInternal<String, String>(JAVA_COMMON_TOOL.OPTION_PATCH_MODULE, "=", "=", pathSeparator, this)
    override val patchModule: SeparableValueLinkedHashMultimapOptionInternal<String, String> by lazy {
        PatchModule()
    }

    protected open inner class AddReads:
    DefaultSeparableValueLinkedHashMultimapOptionInternal<String, String>(JAVA_COMMON_TOOL.OPTION_ADD_READS,    "=", "=", ",",           this)
    override val addReads:    SeparableValueLinkedHashMultimapOptionInternal<String, String> by lazy {
        AddReads()
    }

    protected open inner class AddExports:
    DefaultSeparableValueLinkedHashMultimapOptionInternal<String, String>(JAVA_COMMON_TOOL.OPTION_ADD_EXPORTS,  "=", "=", ",",           this)
    override val addExports:  SeparableValueLinkedHashMultimapOptionInternal<String, String> by lazy {
        AddExports()
    }

    // addModules targets
    override val ALL_MODULE_PATH = JAVA_COMMON_TOOL.ALL_MODULE_PATH
    override val ALL_SYSTEM      = JAVA_COMMON_TOOL.ALL_SYSTEM

    // addExports, addReads & addOpens targets
    override val ALL_UNNAMED = JAVA_COMMON_TOOL.ALL_UNNAMED


    protected fun autoGeneratePatchModule(moduleNameIcoll: ImmutableCollection<String>, classpath: FileCollection) =
        when (moduleNameIcoll.size) {
            0 -> {
                null
            }
            1 -> {
                autoGeneratePatchModule(moduleNameIcoll.iterator().next(), classpath)
            }
            else -> {
                throw GradleException(
                    "Cannot determine into which of the multiple modules to patch the non-module directories." + LS + LS
                    + "To avoid this problem, either only have one module per source set, or modularize the currently non-modular source." + LS + LS
                    + "Modules:" + LS + LS + moduleNameIcoll.joinToString(LS)
                )
            }
        }

    protected fun autoGeneratePatchModule(moduleName: String, classpath: FileCollection) =
        classpath.files.stream().filter {! it.toPath().containsModules}.map(Any::toString).iterator().let {
            if (it.hasNext()) {
                moduleName to it
            }
            else {
                null
            }
        }


    companion object {
        private val LS = lineSeparator()
    }
}
