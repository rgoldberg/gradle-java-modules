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
package org.gradle.kotlin.tool


import com.google.common.collect.ImmutableCollection
import java.io.File
import java.io.File.pathSeparator
import org.gradle.java.util.splitIntoModulePathAndPatchModule


val KOTLINC = object: Kotlinc() {}

abstract class Kotlinc protected constructor() {

    //TODO: correct kotlinc equivalents
    // module options
    val OPTION_ADD_MODULES = "-Xadd-modules="
    val OPTION_MODULE_PATH = "-Xmodule-path="

    val ALL_MODULE_PATH = "ALL-MODULE-PATH"


    fun addModuleArguments(args: MutableList<String>, moduleNameIcoll: ImmutableCollection<String>, classpathFileSet: Set<File>) {
        classpathFileSet.splitIntoModulePathAndPatchModule(
            moduleNameIcoll,
            {modulePathFileList ->
                args +=
                    modulePathFileList.joinTo(
                        StringBuilder(
                            OPTION_MODULE_PATH.length
                            + modulePathFileList.size
                            - 1
                            + modulePathFileList.stream().mapToInt {patchModuleFile -> patchModuleFile.toString().length}.sum()
                        )
                        .append(OPTION_MODULE_PATH),
                        pathSeparator
                    )
                    .toString()
            },
            {patchModuleFileList ->
                //TODO: find kotlinc equivalent for javac's --patch-module
            }
        )
    }
}
