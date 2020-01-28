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


import kotlinx.collections.immutable.persistentSetOf
import org.gradle.api.Action
import org.gradle.api.tasks.JavaExec
import org.gradle.java.JigsawPlugin
import org.gradle.java.jdk.JAVA
import org.gradle.java.util.doAfterAllOtherDoFirstActions
import org.gradle.java.util.doBeforeAllOtherDoLastActions


class JavaExecTaskConfigurer: TaskConfigurer<JavaExec> {

    override val taskClass
    get() = JavaExec::class.java

    override fun configureTask(javaExec: JavaExec, jigsawPlugin: JigsawPlugin) {
        val main = javaExec.main

        jigsawPlugin.getModuleName(main)?.let {moduleName ->
            val classpath by lazy {javaExec.classpath}

            javaExec.doAfterAllOtherDoFirstActions(Action {
                val args = mutableListOf<String>()

                JAVA.addModuleArguments(args, persistentSetOf(moduleName), classpath.files)

                args += JAVA.OPTION_MODULE
                args += main!!

                javaExec.jvmArgs(args)
                javaExec.main      = ""
                javaExec.classpath = javaExec.project.files()
            })

            javaExec.doBeforeAllOtherDoLastActions(Action {
                javaExec.main      = main
                javaExec.classpath = classpath
            })
        }
    }
}
