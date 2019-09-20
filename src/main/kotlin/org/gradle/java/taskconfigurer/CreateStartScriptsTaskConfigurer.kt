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


import com.google.common.io.MoreFiles.asCharSink
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files.readAllLines
import java.nio.file.Path
import java.util.stream.Stream
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.java.GradleUtils.doAfterAllOtherDoFirstActions
import org.gradle.java.GradleUtils.doBeforeAllOtherDoLastActions
import org.gradle.java.GradleUtils.setModuleNamesInputProperty
import org.gradle.java.JigsawPlugin
import org.gradle.java.jdk.JavaCommonTool.Companion.OPTION_MODULE
import org.gradle.java.jdk.JavaCommonTool.Companion.OPTION_MODULE_PATH
import org.gradle.util.TextUtil.getUnixLineSeparator
import org.gradle.util.TextUtil.getWindowsLineSeparator


class CreateStartScriptsTaskConfigurer: TaskConfigurer<CreateStartScripts> {

    override val taskClass
    get() = CreateStartScripts::class.java

    override fun configureTask(createStartScripts: CreateStartScripts, jigsawPlugin: JigsawPlugin) {
        val main = createStartScripts.mainClassName ?: return

        jigsawPlugin.getModuleName(main)?.let {moduleName ->
            setModuleNamesInputProperty(createStartScripts, moduleName)

            val classpath by lazy {createStartScripts.classpath}

            doAfterAllOtherDoFirstActions(createStartScripts, Action {
                classpath

                val args = mutableListOf<String>()

                createStartScripts.defaultJvmOpts?.let {
                    args += it
                }

                args += OPTION_MODULE_PATH
                args += LIB_DIR_PLACEHOLDER

                args += OPTION_MODULE
                args += main

                createStartScripts.defaultJvmOpts = args
                createStartScripts.mainClassName  = ""
                createStartScripts.classpath      = createStartScripts.project.files()
            })

            doBeforeAllOtherDoLastActions(createStartScripts, Action {
                replaceLibDirectoryPlaceholder(createStartScripts.unixScript   .toPath(), "\\\$APP_HOME/lib",  getUnixLineSeparator())
                replaceLibDirectoryPlaceholder(createStartScripts.windowsScript.toPath(), "%APP_HOME%\\\\lib", getWindowsLineSeparator())

                createStartScripts.mainClassName = main
                createStartScripts.classpath     = classpath
            })
        }
    }

    private fun replaceLibDirectoryPlaceholder(path: Path, libDirReplacement: String, lineSeparator: String) =
        try {
            readAllLines(path).stream().map {line -> LIB_DIR_PLACEHOLDER_REGEX.replace(line, libDirReplacement)}.use {lineStream: Stream<String> ->
                asCharSink(path, UTF_8).writeLines(lineStream, lineSeparator)
            }
        }
        catch (ex: IOException) {
            throw GradleException("Couldn't replace placeholder in " + path, ex)
        }


    companion object {
        private const val LIB_DIR_PLACEHOLDER = "LIB_DIR_PLACEHOLDER"

        private val LIB_DIR_PLACEHOLDER_REGEX = LIB_DIR_PLACEHOLDER.toRegex()
    }
}
