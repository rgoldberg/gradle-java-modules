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
package org.gradle.java.util


import org.gradle.api.Action
import org.gradle.api.Describable
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.the


private class DoFirstAction<T> internal constructor(private val delegate: Action<in T>): Action<T>, Describable {

    override fun execute(t: T) =
        delegate.execute(t)

    override fun getDisplayName() =
        DO_FIRST_ACTION_DISPLAY_NAME
}


private const val VERB_COMPILE = "compile"

private const val DO_FIRST_ACTION_DISPLAY_NAME = "Execute doFirst {} action"


//<editor-fold desc="Task helper methods">
fun Task.doAfterAllOtherDoFirstActions(action: Action<in Task>) {
    val actionList = actions

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

    doFirst(action)
}

fun Task.doBeforeAllOtherDoLastActions(action: Action<in Task>) {
    val actionList = actions

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

    doLast(action)
}
//</editor-fold>


//<editor-fold desc="SourceSet helper methods">
val Project.sourceSets
get() = the<SourceSetContainer>()

fun Task.getCompileSourceSet(target: String) =
    getSourceSet(VERB_COMPILE, target)

fun Task.getSourceSet(verb: String, target: String) =
    project.getSourceSet(name, verb, target)

fun Project.getSourceSet(taskName: String, verb: String, target: String) =
    sourceSets.getByName(getSourceSetName(taskName, verb, target))


fun Task.getCompileSourceSetName(target: String) =
    getSourceSetName(VERB_COMPILE, target)

fun Task.getSourceSetName(verb: String, target: String) =
    getSourceSetName(name, verb, target)

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
//</editor-fold>
