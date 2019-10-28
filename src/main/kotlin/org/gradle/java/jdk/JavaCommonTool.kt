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


import java.io.File
import java.io.File.pathSeparator
import kotlinx.collections.immutable.ImmutableCollection
import org.gradle.java.util.splitIntoModulePathAndPatchModule


internal val JAVA_COMMON_TOOL = object: JavaCommonTool() {}

abstract class JavaCommonTool protected constructor() {

    val OPTION_ADD_EXPORTS         = "--add-exports"
    val OPTION_ADD_MODULES         = "--add-modules"
    val OPTION_ADD_READS           = "--add-reads"
    val OPTION_CLASS_PATH          = "--class-path"
    val OPTION_LIMIT_MODULES       = "--limit-modules"
    val OPTION_MODULE              = "--module"
    val OPTION_MODULE_PATH         = "--module-path"
    val OPTION_PATCH_MODULE        = "--patch-module"
    val OPTION_UPGRADE_MODULE_PATH = "--upgrade-module-path"

    val ALL_MODULE_PATH = "ALL-MODULE-PATH"
    val ALL_SYSTEM      = "ALL-SYSTEM"

    val ALL_UNNAMED = "ALL-UNNAMED"


    fun addModuleArguments(args: MutableList<String>, moduleNameIcoll: ImmutableCollection<String>, classpathFileSet: Set<File>) {
        classpathFileSet.splitIntoModulePathAndPatchModule(
            moduleNameIcoll,
            {modulePathFileList ->
                args += OPTION_MODULE_PATH
                args += modulePathFileList.joinToString(pathSeparator)
            },
            {patchModuleFileList ->
                // moduleNameIcoll is guaranteed to have exactly one element
                val moduleName = moduleNameIcoll.iterator().next()

                args += OPTION_PATCH_MODULE
                args +=
                    patchModuleFileList.joinTo(
                        StringBuilder(moduleName.length + patchModuleFileList.size + patchModuleFileList.sumBy {it.toString().length})
                        .append(moduleName)
                        .append('='),
                        pathSeparator
                    )
                    .toString()
            }
        )
    }
}
