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
package org.gradle.java.taskconfigurer


import com.google.common.collect.ImmutableCollection
import java.io.File.pathSeparator
import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME
import org.gradle.api.tasks.testing.Test
import org.gradle.java.JigsawPlugin
import org.gradle.java.testing.getTestModuleNameCommaDelimitedString
import org.gradle.java.util.doAfterAllOtherDoFirstActions
import org.gradle.java.util.doBeforeAllOtherDoLastActions
import org.gradle.java.util.getCompileSourceSetName
import org.gradle.java.util.sourceSets
import org.gradle.java.util.splitIntoModulePathAndPatchModule
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


class KotlinCompileTaskConfigurer: TaskConfigurer<KotlinCompile> {

    override val taskClass
    get() = KotlinCompile::class.java

    override fun configureTask(kotlinCompile: KotlinCompile, jigsawPlugin: JigsawPlugin) {
        val sourceSetName = kotlinCompile.getCompileSourceSetName(TARGET)

        val moduleNameIbyModuleInfoJavaPath = jigsawPlugin.getModuleNameIbyModuleInfoJavaPath(sourceSetName)

        if (moduleNameIbyModuleInfoJavaPath.isEmpty()) {
            //TODO: use better heuristic to determine if kotlinCompile is for test code
            if (TEST_SOURCE_SET_NAME == sourceSetName) {
                // when source set doesn't contain any module-info.java, only enable modules if compiling a test source set
                jigsawPlugin.setModuleNamesInputProperty(kotlinCompile)

                val classpath by lazy {kotlinCompile.classpath}

                kotlinCompile.doAfterAllOtherDoFirstActions(Action {
                    val project = kotlinCompile.project

                    //TODO: .getCompileTaskName(LANGUAGE_NAME_KOTLIN)
                    val args =
                        configureTask(
                            kotlinCompile,
                            jigsawPlugin.moduleNameIsset,
                            classpath + project.sourceSets.getByName(TEST_SOURCE_SET_NAME).allSource.sourceDirectories //TODO? allSource
                        )

                    //TODO: ensure works
                    project.tasks.withType(Test::class.java).configureEach {test ->
                        val testModuleNameCommaDelimitedString = getTestModuleNameCommaDelimitedString(test)

                        if (testModuleNameCommaDelimitedString.isNotEmpty()) {
                            args += OPTION_ADD_MODULES + testModuleNameCommaDelimitedString
                        }
                    }
                })

                kotlinCompile.doBeforeAllOtherDoLastActions(Action {kotlinCompile.classpath = classpath})
            }
        }
        else {
            // source set contains at least one module-info.java
            val classpath by lazy {kotlinCompile.classpath}

            kotlinCompile.doAfterAllOtherDoFirstActions(Action {
                val moduleNameIcoll = moduleNameIbyModuleInfoJavaPath.values

                //TODO: FILTER BASED ON PRESENCE OF MODULE
                configureTask(kotlinCompile, moduleNameIcoll, classpath)
            })

            kotlinCompile.doBeforeAllOtherDoLastActions(Action {kotlinCompile.classpath = classpath})
        }
    }

    private fun configureTask(kotlinCompile: KotlinCompile, moduleNameIcoll: ImmutableCollection<String>, classpath: FileCollection): MutableList<String> {
        val kotlinJvmOptions = kotlinCompile.kotlinOptions

        val args = ArrayList(kotlinJvmOptions.freeCompilerArgs)

        kotlinJvmOptions.freeCompilerArgs = args

        classpath.files.splitIntoModulePathAndPatchModule(
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

        kotlinCompile.classpath = kotlinCompile.project.files()

        return args
    }


    companion object {
        private const val TARGET = "Kotlin"

        private const val OPTION_ADD_MODULES = "-Xadd-modules="
        private const val OPTION_MODULE_PATH = "-Xmodule-path="
    }
}
