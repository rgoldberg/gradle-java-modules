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


import org.gradle.api.Action
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.CoreJavadocOptions
import org.gradle.java.GradleUtils.doAfterAllOtherDoFirstActions
import org.gradle.java.GradleUtils.doBeforeAllOtherDoLastActions
import org.gradle.java.JigsawPlugin
import org.gradle.java.jdk.JavaCommonTool.Companion.OPTION_MODULE_PATH


class JavadocTaskConfigurer: TaskConfigurer<Javadoc> {

    override val taskClass
    get() = Javadoc::class.java

    override fun configureTask(javadoc: Javadoc, jigsawPlugin: JigsawPlugin) {
        jigsawPlugin.setModuleNamesInputProperty(javadoc)

        val classpath by lazy {javadoc.classpath}

        javadoc.doAfterAllOtherDoFirstActions(Action {
            if (! classpath.isEmpty) {
                (javadoc.options as CoreJavadocOptions).addStringOption(JAVADOC_TASK_OPTION_MODULE_PATH, classpath.asPath)

                javadoc.classpath = javadoc.project.files()
            }
        })

        javadoc.doBeforeAllOtherDoLastActions(Action {javadoc.classpath = classpath})
    }


    companion object {
        private val JAVADOC_TASK_OPTION_MODULE_PATH = OPTION_MODULE_PATH.substring(1)
    }
}
