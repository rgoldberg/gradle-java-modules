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
import org.gradle.api.Task
import org.gradle.java.JigsawPlugin
import org.gradle.java.extension.OptionsInternal
import org.gradle.java.extension.TASK_OPTIONS_EXTENSION_NAME
import org.gradle.java.extension.ToolOptionDefaults
import org.gradle.java.util.doAfterAllOtherDoFirstActions
import org.gradle.java.util.doBeforeAllOtherDoLastActions


interface TaskConfigurer<T: Task> {

    val taskClass: Class<T>

    val optionsInternalClass: Class<out OptionsInternal>

    fun configureExtensions(task: T, jigsawPlugin: JigsawPlugin) {
        with(task.project.extensions.getByType(ToolOptionDefaults::class.java)) {
            task.extensions.create(TASK_OPTIONS_EXTENSION_NAME, optionsInternalClass, this, this, task)
        }
    }

    fun configureTask(task: T, jigsawPlugin: JigsawPlugin) {
        task.doAfterAllOtherDoFirstActions(Action {
            task.extensions.configure<OptionsInternal>(TASK_OPTIONS_EXTENSION_NAME) {it.configure()}
        })

        task.doBeforeAllOtherDoLastActions(Action {
            task.extensions.configure<OptionsInternal>(TASK_OPTIONS_EXTENSION_NAME) {it.reset()}
        })
    }
}
