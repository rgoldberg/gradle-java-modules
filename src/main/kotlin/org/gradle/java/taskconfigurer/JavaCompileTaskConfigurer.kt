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
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSet.toImmutableSet
import com.google.common.collect.Streams.stream
import java.io.File
import java.util.stream.Collectors.joining
import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.java.JigsawPlugin
import org.gradle.java.jdk.JavaCommonTool.Companion.OPTION_ADD_MODULES
import org.gradle.java.jdk.JavaCommonTool.Companion.OPTION_ADD_READS
import org.gradle.java.jdk.JavaCommonTool.Companion.addModuleArguments
import org.gradle.java.jdk.JavaSourceTool.Companion.OPTION_MODULE_SOURCE_PATH
import org.gradle.java.testing.getTestModuleNameCommaDelimitedString
import org.gradle.java.util.doAfterAllOtherDoFirstActions
import org.gradle.java.util.doBeforeAllOtherDoLastActions
import org.gradle.java.util.getCompileSourceSet
import org.gradle.java.util.getCompileSourceSetName
import org.gradle.java.util.sourceSets


class JavaCompileTaskConfigurer: TaskConfigurer<JavaCompile> {

    override val taskClass
    get() = JavaCompile::class.java


    override fun configureTask(javaCompile: JavaCompile, jigsawPlugin: JigsawPlugin) {
        val sourceSetName = javaCompile.getCompileSourceSetName(TARGET)

        val moduleNameIbyModuleInfoJavaPath = jigsawPlugin.getModuleNameIbyModuleInfoJavaPath(sourceSetName)

        if (moduleNameIbyModuleInfoJavaPath.isEmpty()) {
            //TODO: use better heuristic to determine if javaCompile is for test code
            if (TEST_SOURCE_SET_NAME == sourceSetName) {
                // when source set doesn't contain any module-info.java, only enable modules if compiling a test source set

                jigsawPlugin.setModuleNamesInputProperty(javaCompile)

                val classpath by lazy {javaCompile.classpath}

                javaCompile.doAfterAllOtherDoFirstActions(Action {
                    val project = javaCompile.project

                    val moduleNameIsset = jigsawPlugin.moduleNameIsset

                    val args =
                        configureTask(
                            javaCompile,
                            moduleNameIsset,
                            classpath + project.sourceSets.getByName(TEST_SOURCE_SET_NAME).allJava.sourceDirectories
                        )

                    project.tasks.withType(Test::class.java).configureEach {test ->
                        val testModuleNameCommaDelimitedString = getTestModuleNameCommaDelimitedString(test)

                        if (testModuleNameCommaDelimitedString.isNotEmpty()) {
                            args += OPTION_ADD_MODULES
                            args += testModuleNameCommaDelimitedString

                            moduleNameIsset.forEach {moduleName ->
                                args += OPTION_ADD_READS
                                args += moduleName + '=' + testModuleNameCommaDelimitedString
                            }
                        }
                    }
                })

                javaCompile.doBeforeAllOtherDoLastActions(Action {javaCompile.classpath = classpath})
            }
        }
        else {
            // source set contains at least one module-info.java
            val classpath by lazy {javaCompile.classpath}

            javaCompile.doAfterAllOtherDoFirstActions(Action {
                val moduleNameIcoll = moduleNameIbyModuleInfoJavaPath.values

                if (moduleNameIbyModuleInfoJavaPath.size > 1) {
                    // generate --module-source-path

                    //TODO: determine the packages for each module, and include root dir for all sources in that package

                    val args = javaCompile.options.compilerArgs

                    args += OPTION_MODULE_SOURCE_PATH
                    args +=
                        getModuleSourcePath(
                            moduleNameIbyModuleInfoJavaPath.entries.stream()
                            .map {moduleNameIforModuleInfoJavaPath ->
                                val moduleInfoJavaPath          = moduleNameIforModuleInfoJavaPath.key
                                val moduleName                  = moduleNameIforModuleInfoJavaPath.value
                                val separator                   = moduleInfoJavaPath.fileSystem.separator
                                val moduleInfoJavaDirPathString = moduleInfoJavaPath.parent.toString()

                                val i = moduleInfoJavaDirPathString.lastIndexOf(separator + moduleName + separator)

                                if (i == -1)
                                    if (moduleInfoJavaDirPathString.endsWith(separator + moduleName))
                                        moduleInfoJavaDirPathString.substring(0, moduleInfoJavaDirPathString.length - separator.length - moduleName.length)
                                    else
                                        moduleInfoJavaDirPathString
                                else
                                    StringBuilder(moduleInfoJavaDirPathString.length - moduleName.length + 1)
                                    .append(moduleInfoJavaDirPathString, 0, i + separator.length)
                                    .append('*')
                                    .append(
                                        moduleInfoJavaDirPathString,
                                        i + separator.length + moduleName.length,
                                        moduleInfoJavaDirPathString.length
                                    )
                                    .toString()
                            }
                            .collect(toImmutableSet())
                        )

                    // must change the classes output directories for the SourceSet:
                    // for each existing output directory, d, replace with subdirectories of d, one for each compile module name

                    //TODO: only works if SourceSet#output#classesDirs is a ConfigurableFileCollection
                    val outputClassesDirs = javaCompile.getCompileSourceSet(TARGET).output.classesDirs as ConfigurableFileCollection

                    //TODO: ensure it is OK to change SourceSet#output#classesDirs during execution phase
                    outputClassesDirs.setFrom(
                        *stream(outputClassesDirs)
                        .flatMap {dirFile -> moduleNameIcoll.stream().map {moduleName -> File(dirFile, moduleName)}}
                        .toArray()
                    )
                }

                configureTask(javaCompile, moduleNameIcoll, classpath)
            })

            javaCompile.doBeforeAllOtherDoLastActions(Action {javaCompile.classpath = classpath})
        }
    }

    private fun configureTask(javaCompile: JavaCompile, moduleNameIcoll: ImmutableCollection<String>, classpath: FileCollection): MutableList<String> {
        val args = javaCompile.options.compilerArgs

        addModuleArguments(args, moduleNameIcoll, classpath.files)

        javaCompile.classpath = javaCompile.project.files()

        return args
    }

    private fun getModuleSourcePath(moduleSourceIset: ImmutableSet<String>): String {
        if (moduleSourceIset.size == 1) {
            return moduleSourceIset.iterator().next()
        }

        val moduleSourceCommonUitr = moduleSourceIset.iterator()

        var commonPrefix = moduleSourceCommonUitr.next()
        var commonSuffix = commonPrefix

        while (moduleSourceCommonUitr.hasNext()) {
            val currModuleSource = moduleSourceCommonUitr.next()
            commonPrefix = commonPrefix.commonPrefixWith(currModuleSource)
            commonSuffix = commonSuffix.commonSuffixWith(currModuleSource)
        }

        if (commonPrefix.isEmpty() && commonSuffix.isEmpty()) {
            return moduleSourceIset.stream().collect(joining(",", "{", "}"))
        }

        val commonPrefixLength = commonPrefix.length
        val commonSuffixLength = commonSuffix.length

        val sb = StringBuilder()
        sb.append(commonPrefix)
        sb.append('{')

        val moduleSourceAlternateUitr = moduleSourceIset.iterator()

        appendModuleSourceAlternate(moduleSourceAlternateUitr.next(), commonPrefixLength, commonSuffixLength, sb)

        while (moduleSourceAlternateUitr.hasNext()) {
            sb.append(',')
            appendModuleSourceAlternate(moduleSourceAlternateUitr.next(), commonPrefixLength, commonSuffixLength, sb)
        }

        sb.append('}')
        sb.append(commonSuffix)

        return sb.toString()
    }

    private fun appendModuleSourceAlternate(moduleSource: String, commonPrefixLength: Int, commonSuffixLength: Int, sb: StringBuilder) {
        sb.append(moduleSource, commonPrefixLength, moduleSource.length - commonSuffixLength)
    }


    companion object {
        private const val TARGET = "Java"
    }
}
