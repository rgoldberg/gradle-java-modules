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
package org.gradle.java


import org.gradle.api.Action
import org.gradle.api.Describable
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.compile.JavaCompile


object GradleUtils {

    private class DoFirstAction<T> internal constructor(private val delegate: Action<in T>): Action<T>, Describable {

        override fun execute(t: T) =
            delegate.execute(t)

        override fun getDisplayName() =
            DO_FIRST_ACTION_DISPLAY_NAME
    }


    private const val PROPERTY_NAME_MODULE_NAMES = "moduleNames"

    private const val VERB_COMPILE = "compile"

    private const val TARGET_JAVA = "Java"

    private const val DO_FIRST_ACTION_DISPLAY_NAME = "Execute doFirst {} action"


    //<editor-fold desc="Task helper methods">
    @JvmStatic
    fun doAfterAllOtherDoFirstActions(task: Task, action: Action<in Task>) {
        val actionList = task.actions

        val actionLitr = actionList.listIterator(actionList.size)
        while (actionLitr.hasPrevious()) {
            val existingAction = actionLitr.previous()

            if (
                existingAction is Describable &&
                DO_FIRST_ACTION_DISPLAY_NAME == (existingAction as Describable).displayName
            ) {
                actionList.add(actionLitr.nextIndex() + 1, DoFirstAction(action))
                return
            }
        }

        task.doFirst(action)
    }

    @JvmStatic
    fun doBeforeAllOtherDoLastActions(task: Task, action: Action<in Task>) {
        val actionList = task.actions

        val actionLitr = actionList.listIterator()
        while (actionLitr.hasNext()) {
            val existingAction = actionLitr.next()

            if (
                existingAction is Describable &&
                "Execute doLast {} action" == (existingAction as Describable).displayName
            ) {
                actionList.add(actionLitr.previousIndex(), action)
                return
            }
        }

        task.doLast(action)
    }
    //</editor-fold>


    //<editor-fold desc="SourceSet helper methods">
    @JvmStatic
    fun getSourceSets(project: Project) =
        project.extensions.getByType(SourceSetContainer::class.java)

    @JvmStatic
    fun getSourceSet(javaCompile: JavaCompile) =
        getCompileSourceSet(javaCompile, TARGET_JAVA)

    @JvmStatic
    fun getCompileSourceSet(task: Task, target: String) =
        getSourceSet(task, VERB_COMPILE, target)

    @JvmStatic
    fun getSourceSet(task: Task, verb: String, target: String) =
        getSourceSet(task.project, task.name, verb, target)

    @JvmStatic
    fun getSourceSet(project: Project, taskName: String, verb: String, target: String) =
        getSourceSet(project, getSourceSetName(taskName, verb, target))

    @JvmStatic
    fun getSourceSet(project: Project, sourceSetName: String) =
        getSourceSets(project).getByName(sourceSetName)


    @JvmStatic
    fun getSourceSetName(javaCompile: JavaCompile) =
        getCompileSourceSetName(javaCompile, TARGET_JAVA)

    @JvmStatic
    fun getCompileSourceSetName(task: Task, target: String) =
        getSourceSetName(task, VERB_COMPILE, target)

    @JvmStatic
    fun getSourceSetName(task: Task, verb: String, target: String) =
        getSourceSetName(task.name, verb, target)

    @JvmStatic
    fun getSourceSetName(taskName: String, verb: String, target: String): String {
        val taskNameLength   = taskName.length
        val verbLength       = verb.length
        val targetLength     = target.length
        val verbTargetLength = verbLength + targetLength

        return if (taskNameLength == verbTargetLength) {
            MAIN_SOURCE_SET_NAME
        }
        else {
            val sb = StringBuilder(taskNameLength - verbTargetLength)
            sb.append(taskName[verbLength].toLowerCase())
            sb.append(taskName, verbLength + 1, taskNameLength - targetLength)
            sb.toString()
        }
    }


    @JvmStatic
    fun getJavaCompile(tasks: TaskContainer, sourceSet: SourceSet) =
        tasks.getByName(sourceSet.compileJavaTaskName) as JavaCompile
    //</editor-fold>


    //<editor-fold desc="moduleNames input property helper methods">
    @JvmStatic
    fun setModuleNamesInputProperty(task: Task, moduleNamesCommaDelimited: String) =
        task.inputs.property(PROPERTY_NAME_MODULE_NAMES, moduleNamesCommaDelimited)
    //</editor-fold>
}
