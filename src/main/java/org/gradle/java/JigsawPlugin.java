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
import com.google.common.collect.ImmutableSet;
import org.gradle.api.Action;
import org.gradle.api.Describable;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.jvm.application.tasks.CreateStartScripts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;
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
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Streams.stream;
import static com.google.common.io.MoreFiles.asCharSink;
import static org.gradle.api.logging.Logging.getLogger;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;
import static org.gradle.java.jdk.Javac.ALL_MODULE_PATH;
import static org.gradle.java.jdk.Javac.FILE_NAME_MODULE_INFO_JAVA;
import static org.gradle.java.jdk.Javac.OPTION_ADD_MODULES;
import static org.gradle.java.jdk.Javac.OPTION_ADD_READS;
import static org.gradle.java.jdk.Javac.OPTION_MODULE;
import static org.gradle.java.jdk.Javac.OPTION_MODULE_PATH;
import static org.gradle.java.jdk.Javac.OPTION_PATCH_MODULE;
import static org.gradle.java.jdk.Javac.OPTION_RELEASE;
import static org.gradle.java.jdk.Javac.OPTION_SOURCE;
import static org.gradle.java.testing.StandardTestFrameworkModuleInfo.getTestModuleNameCommaDelimitedString;
import static org.gradle.util.TextUtil.getUnixLineSeparator;
import static org.gradle.util.TextUtil.getWindowsLineSeparator;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllLines;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;

public class JigsawPlugin implements Plugin<Project> {

    private static final Logger LOGGER = getLogger(JigsawPlugin.class);

    private static final String  LIB_DIR_PLACEHOLDER         = "LIB_DIR_PLACEHOLDER";
    private static final Pattern LIB_DIR_PLACEHOLDER_PATTERN = compile(LIB_DIR_PLACEHOLDER);

    private static final String PROPERTY_NAME_MODULE_NAME = "moduleName";

    private static final String JAVADOC_TASK_OPTION_MODULE_PATH = org.gradle.java.jdk.Javadoc.OPTION_MODULE_PATH.substring(1);

    private static final String DO_FIRST_ACTION_DISPLAY_NAME = "Execute doFirst {} action";


    private String mainModuleName;


    private void setModuleNameInputProperty(final Task task) {
        task.getInputs().property(PROPERTY_NAME_MODULE_NAME, mainModuleName);
    }


    @Override
    public void apply(final Project project) {
        LOGGER.debug("Applying JigsawPlugin to {}", project.getName());
        project.getPlugins().apply(JavaPlugin.class);
        configureJavaTasks(project);
    }


    private void configureJavaTasks(final Project project) {
        project.getGradle().getTaskGraph().whenReady(taskExecutionGraph -> {
            if (taskExecutionGraph.getAllTasks().stream().noneMatch(task -> project.equals(task.getProject()) && isSupportedTask(task))) {
                return;
            }

            mainModuleName = getMainModuleName(project);

            if (mainModuleName != null) {
                configureJavaCompileTasks(       project);
                configureTestTasks(              project);
                configureJavadocTasks(           project);
                configureJavaExecTasks(          project);
                configureCreateStartScriptsTasks(project);
            }
        });
    }

    private boolean isSupportedTask(final Task task) {
        return
            task instanceof JavaCompile        ||
            task instanceof Test               ||
            task instanceof Javadoc            ||
            task instanceof JavaExec           ||
            task instanceof CreateStartScripts
        ;
    }


    private String getMainModuleName(final Project project) {
        final JavaParser parser =
            new JavaParser(new ParserConfiguration().setLanguageLevel(getLanguageLevel((JavaCompile) project.getTasks().getByName(COMPILE_JAVA_TASK_NAME))))
        ;

        for (final File sourceDir : getSourceSets(project).getByName(MAIN_SOURCE_SET_NAME).getJava().getSrcDirs()) {
            final String moduleName = parseModuleName(sourceDir.toPath().resolve(FILE_NAME_MODULE_INFO_JAVA), parser);
            if (moduleName != null) {
                return moduleName;
            }
        }

        return null;
    }

    private String parseModuleName(final Path moduleInfoJavaPath, final JavaParser parser) {
        try {
            final ParseResult<CompilationUnit> parseResult = parser.parse(moduleInfoJavaPath);
            if (parseResult.isSuccessful()) {
                return parseResult.getResult().get().getModule().map(module -> module.getName().asString()).orElseThrow(GradleException::new);
            }

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
        catch (final IOException ex) {
            throw new GradleException("Couldn't parse Java module name from " + moduleInfoJavaPath, ex);
        }
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


    private void configureJavaCompileTasks(final Project project) {
        final TaskCollection<JavaCompile> javaCompileTaskCollection = project.getTasks().withType(JavaCompile.class);

        if (! javaCompileTaskCollection.isEmpty()) {
            final ImmutableSet<File> outputDirFileIset =
                getSourceSets(project).stream().flatMap(sourceSet -> stream(sourceSet.getOutput())).collect(toImmutableSet())
            ;

            javaCompileTaskCollection.forEach(javaCompile -> configureJavaCompileTask(javaCompile, outputDirFileIset));
        }
    }

    private void configureJavaCompileTask(final JavaCompile javaCompile, final ImmutableSet<File> outputDirFileIset) {
        if (COMPILE_TEST_JAVA_TASK_NAME.equals(javaCompile.getName())) {
            configureCompileTestJavaTask(javaCompile);
        }
        else {
            doAfterAllOtherDoFirstActions(javaCompile, task ->
                configureJavaCompileTask(
                    javaCompile,
                    javaCompile.getClasspath().filter(f -> ! outputDirFileIset.contains(f)),
                    javaCompile.getClasspath().filter(outputDirFileIset::contains)
                )
            );
        }
    }

    private void configureCompileTestJavaTask(final JavaCompile compileTestJava) {
        setModuleNameInputProperty(compileTestJava);

        doAfterAllOtherDoFirstActions(compileTestJava, task -> {
            final Project project = compileTestJava.getProject();

            final List<String> args =
                configureJavaCompileTask(
                    compileTestJava,
                    compileTestJava.getClasspath(),
                    getSourceSets(project).getByName(TEST_SOURCE_SET_NAME).getJava().getSourceDirectories()
                )
            ;

            project.getTasks().withType(Test.class).configureEach(test -> {
                final String testModuleNameCommaDelimitedString = getTestModuleNameCommaDelimitedString(test);

                if (! testModuleNameCommaDelimitedString.isEmpty()) {
                    args.add(OPTION_ADD_MODULES);
                    args.add(testModuleNameCommaDelimitedString);

                    args.add(OPTION_ADD_READS);
                    args.add(mainModuleName + '=' + testModuleNameCommaDelimitedString);
                }
            });
        });
    }

    private List<String> configureJavaCompileTask(
        final JavaCompile    javaCompile,
        final FileCollection modulePathFileCollection,
        final FileCollection patchModuleFileCollection
    ) {
        final List<String> args = javaCompile.getOptions().getCompilerArgs();

        addModulePathArgument(args, modulePathFileCollection);

        addPatchModuleArgument(args, mainModuleName, patchModuleFileCollection);

        javaCompile.setClasspath(javaCompile.getProject().files());

        return args;
    }


    private void configureTestTasks(final Project project) {
        project.getTasks().withType(Test.class).forEach(test -> configureTestTask(test));
    }

    private void configureTestTask(final Test test) {
        setModuleNameInputProperty(test);

        doAfterAllOtherDoFirstActions(test, task -> {
            final Project project = test.getProject();

            final List<String> args = new ArrayList<>();

            addModulePathArgument(args, test.getClasspath());

            args.add(OPTION_ADD_MODULES);
            args.add(ALL_MODULE_PATH);

            final String testModuleNameCommaDelimitedString = getTestModuleNameCommaDelimitedString(test);

            if (! testModuleNameCommaDelimitedString.isEmpty()) {
                args.add(OPTION_ADD_READS);
                args.add(mainModuleName + '=' + testModuleNameCommaDelimitedString);
            }

            addPatchModuleArgument(args, mainModuleName, getSourceSets(project).getByName(TEST_SOURCE_SET_NAME).getJava().getOutputDir());

            test.jvmArgs(args);

            test.setClasspath(project.files());
        });
    }


    private void configureJavadocTasks(final Project project) {
        project.getTasks().withType(Javadoc.class).forEach(javadoc -> configureJavadocTask(javadoc));
    }

    private void configureJavadocTask(final Javadoc javadoc) {
        setModuleNameInputProperty(javadoc);

        doAfterAllOtherDoFirstActions(javadoc, task -> {
            final FileCollection classpath = javadoc.getClasspath();

            if (! classpath.isEmpty()) {
                ((CoreJavadocOptions) javadoc.getOptions()).addStringOption(JAVADOC_TASK_OPTION_MODULE_PATH, classpath.getAsPath());

                javadoc.setClasspath(javadoc.getProject().files());
            }
        });
    }


    private void configureJavaExecTasks(final Project project) {
        project.getTasks().withType(JavaExec.class).forEach(javaExec -> configureJavaExecTask(javaExec));
    }

    private void configureJavaExecTask(final JavaExec javaExec) {
        setModuleNameInputProperty(javaExec);

        doAfterAllOtherDoFirstActions(javaExec, task -> {
            final List<String> args = new ArrayList<>();

            addModulePathArgument(args, javaExec.getClasspath());

            args.add(OPTION_MODULE);
            args.add(mainModuleName + '/' + javaExec.getMain());

            javaExec.jvmArgs(args);
            javaExec.setMain("");
            javaExec.setClasspath(javaExec.getProject().files());
        });
    }


    private void configureCreateStartScriptsTasks(final Project project) {
        project.getTasks().withType(CreateStartScripts.class).forEach(css -> configureCreateStartScriptsTask(css));
    }

    private void configureCreateStartScriptsTask(final CreateStartScripts createStartScripts) {
        setModuleNameInputProperty(createStartScripts);

        doAfterAllOtherDoFirstActions(createStartScripts, task -> {
            final List<String> args = new ArrayList<>();

            addAll(args, createStartScripts.getDefaultJvmOpts());

            args.add(OPTION_MODULE_PATH);
            args.add(LIB_DIR_PLACEHOLDER);

            args.add(OPTION_MODULE);
            args.add(mainModuleName + '/' + createStartScripts.getMainClassName());

            createStartScripts.setDefaultJvmOpts(args);
            createStartScripts.setMainClassName("");
            createStartScripts.setClasspath(createStartScripts.getProject().files());
        });

        createStartScripts.doLast(task -> {
            replaceLibDirectoryPlaceholder(createStartScripts.getUnixScript()   .toPath(), "\\$APP_HOME/lib",   getUnixLineSeparator());
            replaceLibDirectoryPlaceholder(createStartScripts.getWindowsScript().toPath(), "%APP_HOME%\\\\lib", getWindowsLineSeparator());
        });
    }

    private void replaceLibDirectoryPlaceholder(final Path path, final String libDirReplacement, final String lineSeparator) {
        try {
            try (Stream<String> lineStream = readAllLines(path).stream().map(line -> LIB_DIR_PLACEHOLDER_PATTERN.matcher(line).replaceAll(libDirReplacement))) {
                asCharSink(path, UTF_8).writeLines(lineStream, lineSeparator);
            }
        }
        catch (final IOException ex) {
            throw new GradleException("Couldn't replace placeholder in " + path, ex);
        }
    }


    private static void doAfterAllOtherDoFirstActions(final Task task, final Action<? super Task> action) {
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


    private static SourceSetContainer getSourceSets(final Project project) {
        return project.getExtensions().getByType(SourceSetContainer.class);
    }


    private void addModulePathArgument(final List<String> args, final FileCollection modulePathFileCollection) {
        if (! modulePathFileCollection.isEmpty()) {
            args.add(OPTION_MODULE_PATH);
            args.add(modulePathFileCollection.getAsPath());
        }
    }

    private void addPatchModuleArgument(final List<String> args, final String moduleName, final File file) {
        args.add(OPTION_PATCH_MODULE);
        args.add(moduleName + '=' + file);
    }

    private void addPatchModuleArgument(final List<String> args, final String moduleName, final FileCollection patchModuleFileCollection) {
        if (! patchModuleFileCollection.isEmpty()) {
            args.add(OPTION_PATCH_MODULE);
            args.add(moduleName + '=' + patchModuleFileCollection.getAsPath());
        }
    }
}
