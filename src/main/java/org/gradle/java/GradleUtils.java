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
package org.gradle.java;

import org.gradle.api.Action;
import org.gradle.api.Describable;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.JavaCompile;

import java.util.List;
import java.util.ListIterator;

import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import static java.lang.Character.toLowerCase;

public class GradleUtils {

    private static final String PROPERTY_NAME_MODULE_NAMES = "moduleNames";

    private static final String VERB_COMPILE = "compile";

    private static final String TARGET_JAVA = "Java";

    private static final String DO_FIRST_ACTION_DISPLAY_NAME = "Execute doFirst {} action";


    @Deprecated
    private GradleUtils() {
        throw new AssertionError("Should never execute");
    }


    //<editor-fold desc="Task helper methods">
    public static void doAfterAllOtherDoFirstActions(final Task task, final Action<? super Task> action) {
        final List<Action<? super Task>> actionList = task.getActions();

        for (final ListIterator<Action<? super Task>> actionLitr = actionList.listIterator(actionList.size()); actionLitr.hasPrevious();) {
            final Action<? super Task> existingAction = actionLitr.previous();

            if (
                existingAction instanceof Describable &&
                DO_FIRST_ACTION_DISPLAY_NAME.equals(((Describable) existingAction).getDisplayName())
            ) {
                actionList.add(actionLitr.nextIndex() + 1, new DoFirstAction<>(action));
                return;
            }
        }

        task.doFirst(action);
    }

    private static class DoFirstAction<T> implements Action<T>, Describable {

        private final Action<? super T> delegate;


        DoFirstAction(final Action<? super T> delegate) {
            this.delegate = delegate;
        }


        @Override
        public void execute(final T t) {
            delegate.execute(t);
        }

        @Override
        public String getDisplayName() {
            return DO_FIRST_ACTION_DISPLAY_NAME;
        }
    }

    public static void doBeforeAllOtherDoLastActions(final Task task, final Action<? super Task> action) {
        final List<Action<? super Task>> actionList = task.getActions();

        for (final ListIterator<Action<? super Task>> actionLitr = actionList.listIterator(); actionLitr.hasNext();) {
            final Action<? super Task> existingAction = actionLitr.next();

            if (
                existingAction instanceof Describable &&
                "Execute doLast {} action".equals(((Describable) existingAction).getDisplayName())
            ) {
                actionList.add(actionLitr.previousIndex(), action);
                return;
            }
        }

        task.doLast(action);
    }
    //</editor-fold>


    //<editor-fold desc="SourceSet helper methods">
    public static SourceSetContainer getSourceSets(final Project project) {
        return project.getExtensions().getByType(SourceSetContainer.class);
    }

    public static SourceSet getSourceSet(final JavaCompile javaCompile) {
        return getCompileSourceSet(javaCompile, TARGET_JAVA);
    }

    public static SourceSet getCompileSourceSet(final Task task, final String target) {
        return getSourceSet(task, VERB_COMPILE, target);
    }

    public static SourceSet getSourceSet(final Task task, final String verb, final String target) {
        return getSourceSet(task.getProject(), task.getName(), verb, target);
    }

    public static SourceSet getSourceSet(final Project project, final String taskName, final String verb, final String target) {
        return getSourceSet(project, getSourceSetName(taskName, verb, target));
    }

    public static SourceSet getSourceSet(final Project project, final String sourceSetName) {
        return getSourceSets(project).getByName(sourceSetName);
    }


    public static String getSourceSetName(final JavaCompile javaCompile) {
        return getCompileSourceSetName(javaCompile, TARGET_JAVA);
    }

    public static String getCompileSourceSetName(final Task task, final String target) {
        return getSourceSetName(task, VERB_COMPILE, target);
    }

    public static String getSourceSetName(final Task task, final String verb, final String target) {
        return getSourceSetName(task.getName(), verb, target);
    }

    public static String getSourceSetName(final String taskName, final String verb, final String target) {
        final int taskNameLength   = taskName.length();
        final int verbLength       = verb.length();
        final int targetLength     = target.length();
        final int verbTargetLength = verbLength + targetLength;

        if (taskNameLength == verbTargetLength) {
            return MAIN_SOURCE_SET_NAME;
        }
        else {
            final StringBuilder sb = new StringBuilder(taskNameLength - verbTargetLength);
            sb.append(toLowerCase(taskName.charAt(verbLength)));
            sb.append(taskName, verbLength + 1, taskNameLength - targetLength);
            return sb.toString();
        }
    }


    public static JavaCompile getJavaCompile(final TaskContainer tasks, final SourceSet sourceSet) {
        return (JavaCompile) tasks.getByName(sourceSet.getCompileJavaTaskName());
    }
    //</editor-fold>


    //<editor-fold desc="moduleNames input property helper methods">
    public static void setModuleNamesInputProperty(final Task task, final String moduleNamesCommaDelimited) {
        task.getInputs().property(PROPERTY_NAME_MODULE_NAMES, moduleNamesCommaDelimited);
    }
    //</editor-fold>
}
