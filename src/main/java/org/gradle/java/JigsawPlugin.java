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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.java.taskconfigurer.CreateStartScriptsTaskConfigurer;
import org.gradle.java.taskconfigurer.JavaCompileTaskConfigurer;
import org.gradle.java.taskconfigurer.JavaExecTaskConfigurer;
import org.gradle.java.taskconfigurer.JavadocTaskConfigurer;
import org.gradle.java.taskconfigurer.KotlinCompileTaskConfigurer;
import org.gradle.java.taskconfigurer.TaskConfigurer;
import org.gradle.java.taskconfigurer.TestTaskConfigurer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_10;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_11;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_12;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_13;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_14;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_1_0;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_1_1;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_1_2;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_1_3;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_1_4;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_5;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_6;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_7;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_8;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_9;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.collect.Streams.stream;
import static org.gradle.api.logging.Logging.getLogger;
import static org.gradle.java.GradleUtils.getJavaCompile;
import static org.gradle.java.GradleUtils.getSourceSets;
import static org.gradle.java.jdk.Javac.FILE_NAME_MODULE_INFO_JAVA;
import static org.gradle.java.jdk.Javac.OPTION_RELEASE;
import static org.gradle.java.jdk.Javac.OPTION_SOURCE;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;

public class JigsawPlugin implements Plugin<Project> {

    //<editor-fold desc="Constants">
    private static final Logger LOGGER = getLogger(JigsawPlugin.class);
    //</editor-fold>


    //<editor-fold desc="Fields">
    private ImmutableMap<String, ImmutableMap<Path, String>> moduleNameIbyModuleInfoJavaPath_IbySourceSetName;

    private ImmutableSortedSet<String> moduleNameIsset;

    private final Set<TaskConfigurer<? extends Task>> taskConfigurerSet = new LinkedHashSet<>();
    //</editor-fold>


    //<editor-fold desc="Constructors">
    public JigsawPlugin() {}
    //</editor-fold>


    //<editor-fold desc="Accessors">
    public ImmutableMap<Path, String> getModuleNameIbyModuleInfoJavaPath(final String sourceSetName) {
        return moduleNameIbyModuleInfoJavaPath_IbySourceSetName.getOrDefault(sourceSetName, ImmutableMap.of());
    }

    public ImmutableSortedSet<String> getModuleNameIsset() {
        return moduleNameIsset;
    }

    public String getModuleName(final String main) {
        final int slashIndex = main.indexOf('/');
        return
            slashIndex >= 0
                // build script specified module/class
                ? main.substring(0, slashIndex)
                : moduleNameIsset.contains(main)
                    // build script specified module that is built in this build
                    ? main
                    // couldn't find module/class or module, so use non-modular command line
                    : null
        ;

        //TODO: check jars in classpath for modules, possibly from:
        //    module-info.class
        //    Automatic-Module-Name in META-INF/MANIFEST.MF
        //    jar file name
        //TODO: check directories in classpath for modules, possibly from:
        //    Automatic-Module-Name in META-INF/MANIFEST.MF
        //    directory name
    }

    public void setModuleNamesInputProperty(final Task task) {
        GradleUtils.setModuleNamesInputProperty(task, join(",", moduleNameIsset));
    }

    public void register(final TaskConfigurer<? extends Task> taskConfigurer) {
        taskConfigurerSet.add(taskConfigurer);
    }
    //</editor-fold>


    //<editor-fold desc="Plugin methods">
    @Override
    public void apply(final Project project) {
        LOGGER.debug("Applying JigsawPlugin to {}", project.getName());

        project.getPlugins().apply(JavaPlugin.class);

        register(new CreateStartScriptsTaskConfigurer());
        register(new JavaCompileTaskConfigurer());
        register(new JavaExecTaskConfigurer());
        register(new JavadocTaskConfigurer());
        register(new TestTaskConfigurer());

        project.getPlugins().withId("org.jetbrains.kotlin.jvm", plugin -> register(new KotlinCompileTaskConfigurer()));

        project.getGradle().getTaskGraph().whenReady(taskExecutionGraph -> {
            final List<Task> taskList = taskExecutionGraph.getAllTasks();

            if (
                taskList.stream().noneMatch(task ->
                    project.equals(task.getProject()) &&
                    taskConfigurerSet.stream().anyMatch(taskConfigurer -> taskConfigurer.getTaskClass().isInstance(task))
                )
            ) {
                return;
            }

            parseModuleInfoJavas(project);

            if (! moduleNameIbyModuleInfoJavaPath_IbySourceSetName.isEmpty()) {
                moduleNameIsset =
                    moduleNameIbyModuleInfoJavaPath_IbySourceSetName.values().stream()
                    .flatMap(entry -> entry.values().stream())
                    .collect(toImmutableSortedSet(naturalOrder()))
                ;

                for (final TaskConfigurer<? extends Task> taskConfigurer : taskConfigurerSet) {
                    configure(taskList, taskConfigurer);
                }
            }
        });
    }

    private <T extends Task> void configure(final List<? extends Task> taskList, final TaskConfigurer<T> taskConfigurer) {
        final Class<T> supportedClass = taskConfigurer.getTaskClass();

        for (final Task task : taskList) {
            if (supportedClass.isInstance(task)) {
                taskConfigurer.configureTask(supportedClass.cast(task), this);
            }
        }
    }
    //</editor-fold>


    //<editor-fold desc="module-info.java parsing methods">
    private void parseModuleInfoJavas(final Project project) {
        final SortedMap<String, SortedMap<Path, String>> moduleNameSbyModuleInfoJavaPath_SbySourceSetName = new TreeMap<>();

        getSourceSets(project).stream()
        .flatMap(sourceSet ->
            stream(sourceSet.getAllJava().matching(pattern -> pattern.include("**/" + FILE_NAME_MODULE_INFO_JAVA)))
            .map(moduleInfoJavaFile -> immutableEntry(sourceSet, moduleInfoJavaFile.toPath()))
        )
        .forEach(moduleInfoJavaPathIforSourceSet -> {
            final Path moduleInfoJavaPath = moduleInfoJavaPathIforSourceSet.getValue();
            try {
                final SourceSet sourceSet = moduleInfoJavaPathIforSourceSet.getKey();

                final ParseResult<CompilationUnit> parseResult =
                    new JavaParser(new ParserConfiguration().setLanguageLevel(getLanguageLevel(getJavaCompile(project.getTasks(), sourceSet))))
                    .parse(moduleInfoJavaPath)
                ;

                if (! parseResult.isSuccessful()) {
                    throw new GradleException(
                        concat(
                            Stream.of(
                                "Couldn't parse Java module name from:",
                                "",
                                moduleInfoJavaPath.toString(),
                                "",
                                "Because of the following parse problems:",
                                ""
                            ),
                            parseResult.getProblems().stream().map(Object::toString)
                        )
                        .collect(joining(lineSeparator()))
                    );
                }

                moduleNameSbyModuleInfoJavaPath_SbySourceSetName.computeIfAbsent(sourceSet.getName(), k -> new TreeMap<>()).put(
                    moduleInfoJavaPath,
                    parseResult.getResult().get().getModule().orElseThrow(GradleException::new).getName().asString()
                );
            }
            catch (final IOException ex) {
                throw new GradleException("Couldn't parse Java module name from " + moduleInfoJavaPath, ex);
            }
        });

        moduleNameIbyModuleInfoJavaPath_IbySourceSetName =
            moduleNameSbyModuleInfoJavaPath_SbySourceSetName.entrySet().stream()
            .collect(toImmutableMap(Entry::getKey, e -> ImmutableMap.copyOf(e.getValue())))
        ;
    }

    private static LanguageLevel getLanguageLevel(final JavaCompile javaCompile) {
        switch (getSourceCompatibility(javaCompile)) {
        case "0":
        case "1.0":
            return JAVA_1_0;
        case "1":
        case "1.1":
            return JAVA_1_1;
        case "2":
        case "1.2":
            return JAVA_1_2;
        case "3":
        case "1.3":
            return JAVA_1_3;
        case "4":
        case "1.4":
            return JAVA_1_4;
        case "5":
        case "1.5":
            return JAVA_5;
        case "6":
        case "1.6":
            return JAVA_6;
        case "7":
        case "1.7":
            return JAVA_7;
        case "8":
        case "1.8":
            return JAVA_8;
        case "9":
        case "1.9":
            return JAVA_9;
        case "10":
        case "1.10":
            return JAVA_10;
        case "11":
        case "1.11":
            return JAVA_11;
        case "12":
        case "1.12":
            return JAVA_12;
        case "13":
        case "1.13":
            return JAVA_13;
        case "14":
        case "1.14":
            return JAVA_14;
        default:
            return null;
        }
    }

    private static String getSourceCompatibility(final JavaCompile javaCompile) {
        String sourceCompatibility = "";
        final Iterator<String> argItr = javaCompile.getOptions().getAllCompilerArgs().iterator();

        while (argItr.hasNext()) {
            final String arg = argItr.next();

            final String source = getOptionValueWhitespaceSeparator(OPTION_SOURCE, arg, argItr);
            if (source != null) {
                sourceCompatibility = source;
            }
            else {
                final String release = getOptionValueWhitespaceOrOtherSeparator(OPTION_RELEASE, '=', arg, argItr);
                if (release != null) {
                    sourceCompatibility = release;
                }
            }
        }

        return
            sourceCompatibility.isEmpty()
                ? javaCompile.getSourceCompatibility()
                : sourceCompatibility
        ;
    }

    private static String getOptionValueWhitespaceSeparator(final String option, final String arg, final Iterator<String> argItr) {
        if (option.equals(arg)) {
            if (argItr.hasNext()) {
                return argItr.next();
            }
            else {
                throw new GradleException("Missing value for option " + option);
            }
        }

        return null;
    }

    private static String getOptionValueWhitespaceOrOtherSeparator(
        final String           option,
        final char             otherSeparator,
        final String           arg,
        final Iterator<String> argItr
    ) {
        if (arg.startsWith(option)) {
            if (arg.length() == option.length()) {
                if (argItr.hasNext()) {
                    return argItr.next();
                }
                else {
                    throw new GradleException("Missing value for option " + option);
                }
            }
            else {
                final int optionLength = option.length();
                if (arg.charAt(optionLength) == otherSeparator) {
                    return arg.substring(optionLength + 1);
                }
            }
        }

        return null;
    }
    //</editor-fold>
}
