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


import java.io.File
import java.io.IOException
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.java.JigsawPlugin
import org.gradle.java.jdk.JAVA
import org.gradle.java.util.doAfterAllOtherDoFirstActions
import org.gradle.java.util.doBeforeAllOtherDoLastActions
import org.gradle.java.util.setModuleNamesInputProperty
import org.gradle.util.TextUtil.getUnixLineSeparator
import org.gradle.util.TextUtil.getWindowsLineSeparator


class CreateStartScriptsTaskConfigurer: TaskConfigurer<CreateStartScripts> {

    override val taskClass
    get() = CreateStartScripts::class.java

    override fun configureTask(createStartScripts: CreateStartScripts, jigsawPlugin: JigsawPlugin) {
        val main = createStartScripts.mainClassName ?: return

        jigsawPlugin.getModuleName(main)?.let {moduleName ->
            createStartScripts.setModuleNamesInputProperty(moduleName)

            val classpath by lazy {createStartScripts.classpath}

            createStartScripts.doAfterAllOtherDoFirstActions(Action {
                classpath

                val args = mutableListOf<String>()

                createStartScripts.defaultJvmOpts?.let {
                    args += it
                }

                args += JAVA.OPTION_MODULE_PATH
                args += LIB_DIR_PLACEHOLDER

                args += JAVA.OPTION_MODULE
                args += main

                createStartScripts.defaultJvmOpts = args
                createStartScripts.mainClassName  = ""
                createStartScripts.classpath      = createStartScripts.project.files()
            })

            createStartScripts.doBeforeAllOtherDoLastActions(Action {
                replaceLibDirectoryPlaceholder(createStartScripts.unixScript,    "\\\$APP_HOME/lib",  getUnixLineSeparator())
                replaceLibDirectoryPlaceholder(createStartScripts.windowsScript, "%APP_HOME%\\\\lib", getWindowsLineSeparator())

                createStartScripts.mainClassName = main
                createStartScripts.classpath     = classpath
            })
        }
    }

    private fun replaceLibDirectoryPlaceholder(file: File, libDirReplacement: String, lineSeparator: String) =
        try {
            file.readLines().stream().map {line -> LIB_DIR_PLACEHOLDER_REGEX.replace(line, libDirReplacement)}.use {lineStream ->
                file.bufferedWriter().use {
                    Iterable(lineStream::iterator).joinTo(it, lineSeparator, "", lineSeparator)
                }
            }
        }
        catch (ex: IOException) {
            throw GradleException("Couldn't replace placeholder in " + file, ex)
        }


    companion object {
        private const val LIB_DIR_PLACEHOLDER = "LIB_DIR_PLACEHOLDER"

        private val LIB_DIR_PLACEHOLDER_REGEX = LIB_DIR_PLACEHOLDER.toRegex()
    }
}
