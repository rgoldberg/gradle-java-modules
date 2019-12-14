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


import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_1_0
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_1_1
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_1_2
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_1_3
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_1_4
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_5
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_6
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_7
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_8
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_9
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_10
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_11
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_12
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_13
import com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_14
import java.io.IOException
import java.lang.System.lineSeparator
import java.nio.file.Path
import java.util.TreeMap
import java.util.TreeSet
import java.util.stream.Collectors.joining
import java.util.stream.Collectors.toCollection
import java.util.stream.Collectors.toMap
import java.util.stream.Stream
import java.util.stream.Stream.concat
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logging.getLogger
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.java.jdk.JAVAC
import org.gradle.java.taskconfigurer.CreateStartScriptsTaskConfigurer
import org.gradle.java.taskconfigurer.JavaCompileTaskConfigurer
import org.gradle.java.taskconfigurer.JavaExecTaskConfigurer
import org.gradle.java.taskconfigurer.JavadocTaskConfigurer
import org.gradle.java.taskconfigurer.KotlinCompileTaskConfigurer
import org.gradle.java.taskconfigurer.TaskConfigurer
import org.gradle.java.taskconfigurer.TestTaskConfigurer
import org.gradle.java.util.setModuleNamesInputProperty
import org.gradle.java.util.sourceSets
import org.gradle.java.util.stream


class JigsawPlugin: Plugin<Project> {

    //<editor-fold desc="Fields">
    private lateinit var project: Project

    val moduleNameIset by lazy {
        moduleNameIbyModuleInfoJavaPath_IbySourceSetName.values.stream()
        .flatMap {it.values.stream()}
        .collect(toCollection {TreeSet<String>()})
        .toImmutableSet()
    }

    private val taskConfigurerSet = mutableSetOf<TaskConfigurer<out Task>>()
    //</editor-fold>


    //<editor-fold desc="Accessors">
    fun getModuleNameIbyModuleInfoJavaPath(sourceSetName: String) =
        moduleNameIbyModuleInfoJavaPath_IbySourceSetName.getOrDefault(sourceSetName, persistentMapOf())

    fun getModuleName(main: String?): String? {
        main ?: return null

        val slashIndex = main.indexOf('/')
        return when {
            // build script specified module/class
            slashIndex >= 0               -> main.substring(0, slashIndex)
            // build script specified module that is built in this build
            moduleNameIset.contains(main) -> main
            // couldn't find module/class or module, so use non-modular command line
            else                          -> null

            //TODO: check jars in classpath for modules, possibly from:
            //    module-info.class
            //    Automatic-Module-Name in META-INF/MANIFEST.MF
            //    jar file name
            //TODO: check directories in classpath for modules, possibly from:
            //    Automatic-Module-Name in META-INF/MANIFEST.MF
            //    directory name
        }
    }

    fun setModuleNamesInputProperty(task: Task) =
        task.setModuleNamesInputProperty(moduleNameIset.joinToString(","))

    fun register(taskConfigurer: TaskConfigurer<out Task>) {
        taskConfigurerSet += taskConfigurer
    }
    //</editor-fold>


    //<editor-fold desc="Plugin methods">
    override fun apply(project: Project) {
        LOGGER.debug("Applying JigsawPlugin to {}", project.name)

        this.project = project

        project.plugins.apply(JavaPlugin::class.java)

        register(CreateStartScriptsTaskConfigurer())
        register(JavaCompileTaskConfigurer())
        register(JavaExecTaskConfigurer())
        register(JavadocTaskConfigurer())
        register(TestTaskConfigurer())

        project.plugins.withId("org.jetbrains.kotlin.jvm") {register(KotlinCompileTaskConfigurer())}

        project.gradle.taskGraph.whenReady {taskExecutionGraph ->
            val taskList = taskExecutionGraph.allTasks

            if (
                taskList.stream().anyMatch {task ->
                    project == task.project &&
                    taskConfigurerSet.stream().anyMatch {taskConfigurer -> taskConfigurer.taskClass.isInstance(task)}
                } &&
                moduleNameIbyModuleInfoJavaPath_IbySourceSetName.isNotEmpty()
            ) {
                for (taskConfigurer in taskConfigurerSet) {
                    configure(taskList, taskConfigurer)
                }
            }
        }
    }

    private fun <T: Task> configure(taskList: List<Task>, taskConfigurer: TaskConfigurer<T>) {
        val supportedClass = taskConfigurer.taskClass

        for (task in taskList) {
            if (supportedClass.isInstance(task)) {
                taskConfigurer.configureTask(supportedClass.cast(task), this)
            }
        }
    }
    //</editor-fold>


    //<editor-fold desc="module-info.java parsing methods">
    private val moduleNameIbyModuleInfoJavaPath_IbySourceSetName: ImmutableMap<String, ImmutableMap<Path, String>> by lazy {
        val moduleNameByModuleInfoJavaPath_BySourceSetName = mutableMapOf<String, TreeMap<Path, String>>()

        val tasks = project.tasks

        project.sourceSets.stream()
        .flatMap {sourceSet ->
            stream(sourceSet.allJava.matching {pattern -> pattern.include("**/" + JAVAC.FILE_NAME_MODULE_INFO_JAVA)})
            .map {sourceSet to it.toPath()}
        }
        .forEach {(sourceSet, moduleInfoJavaPath) ->
            try {
                val parseResult =
                    JavaParser(ParserConfiguration().setLanguageLevel(getLanguageLevel(tasks.getByName(sourceSet.compileJavaTaskName) as JavaCompile)))
                    .parse(moduleInfoJavaPath)

                if (! parseResult.isSuccessful) {
                    throw GradleException(
                        concat(
                            Stream.of(
                                "Couldn't parse Java module name from:",
                                "",
                                moduleInfoJavaPath.toString(),
                                "",
                                "Because of the following parse problems:",
                                ""
                            ),
                            parseResult.problems.stream().map(Any::toString)
                        )
                        .collect(joining(lineSeparator()))
                    )
                }

                moduleNameByModuleInfoJavaPath_BySourceSetName.computeIfAbsent(sourceSet.name) {TreeMap()}[moduleInfoJavaPath] =
                    parseResult.result.get().module.orElseThrow(::GradleException).name.asString()
            }
            catch (ex: IOException) {
                throw GradleException("Couldn't parse Java module name from " + moduleInfoJavaPath, ex)
            }
        }

        moduleNameByModuleInfoJavaPath_BySourceSetName.entries.stream()
        .collect(
            toMap<Map.Entry<String, TreeMap<Path, String>>, String, ImmutableMap<Path, String>, TreeMap<String, ImmutableMap<Path, String>>>(
                {it.key},
                {it.value.toImmutableMap()},
                {a, _ -> a},
                ::TreeMap
            )
        )
        .toImmutableMap()
    }

    private fun getLanguageLevel(javaCompile: JavaCompile) =
        when (getSourceCompatibility(javaCompile)) {
             "0",  "1.0" -> JAVA_1_0
             "1",  "1.1" -> JAVA_1_1
             "2",  "1.2" -> JAVA_1_2
             "3",  "1.3" -> JAVA_1_3
             "4",  "1.4" -> JAVA_1_4
             "5",  "1.5" -> JAVA_5
             "6",  "1.6" -> JAVA_6
             "7",  "1.7" -> JAVA_7
             "8",  "1.8" -> JAVA_8
             "9",  "1.9" -> JAVA_9
            "10", "1.10" -> JAVA_10
            "11", "1.11" -> JAVA_11
            "12", "1.12" -> JAVA_12
            "13", "1.13" -> JAVA_13
            "14", "1.14" -> JAVA_14
            else         -> null
        }

    private fun getSourceCompatibility(javaCompile: JavaCompile): String {
        var sourceCompatibility = ""

        val argItr = javaCompile.options.allCompilerArgs.iterator()

        while (argItr.hasNext()) {
            val arg = argItr.next()

            val source = getOptionValueWhitespaceSeparator(JAVAC.OPTION_SOURCE, arg, argItr)
            if (source.isNotEmpty()) {
                sourceCompatibility = source
            }
            else {
                val release = getOptionValueWhitespaceOrOtherSeparator(JAVAC.OPTION_RELEASE, '=', arg, argItr)
                if (release.isNotEmpty()) {
                    sourceCompatibility = release
                }
            }
        }

        return if (sourceCompatibility.isEmpty()) {
            javaCompile.sourceCompatibility
        }
        else {
            sourceCompatibility
        }
    }

    private fun getOptionValueWhitespaceSeparator(option: String, arg: String, argItr: Iterator<String>) =
        if (option == arg) {
            if (argItr.hasNext()) {
                argItr.next()
            }
            else {
                throw GradleException("Missing value for option " + option)
            }
        }
        else {
            ""
        }

    private fun getOptionValueWhitespaceOrOtherSeparator(option: String, otherSeparator: Char, arg: String, argItr: Iterator<String>): String {
        if (arg.startsWith(option)) {
            if (arg.length == option.length) {
                if (argItr.hasNext()) {
                    return argItr.next()
                }
                else {
                    throw GradleException("Missing value for option " + option)
                }
            }
            else {
                val optionLength = option.length
                if (arg[optionLength] == otherSeparator) {
                    return arg.substring(optionLength + 1)
                }
            }
        }

        return ""
    }
    //</editor-fold>


    companion object {
        private val LOGGER = getLogger(JigsawPlugin::class.java)
    }
}
