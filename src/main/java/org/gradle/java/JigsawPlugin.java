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
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.UnmodifiableIterator;
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
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.jvm.application.tasks.CreateStartScripts;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
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
import static com.google.common.base.Strings.commonPrefix;
import static com.google.common.base.Strings.commonSuffix;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.collect.Streams.stream;
import static com.google.common.io.MoreFiles.asCharSink;
import static org.gradle.api.logging.Logging.getLogger;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;
import static org.gradle.java.jdk.Javac.ALL_MODULE_PATH;
import static org.gradle.java.jdk.Javac.FILE_NAME_MODULE_INFO_JAVA;
import static org.gradle.java.jdk.Javac.OPTION_ADD_MODULES;
import static org.gradle.java.jdk.Javac.OPTION_ADD_READS;
import static org.gradle.java.jdk.Javac.OPTION_MODULE;
import static org.gradle.java.jdk.Javac.OPTION_MODULE_PATH;
import static org.gradle.java.jdk.Javac.OPTION_MODULE_SOURCE_PATH;
import static org.gradle.java.jdk.Javac.OPTION_PATCH_MODULE;
import static org.gradle.java.jdk.Javac.OPTION_RELEASE;
import static org.gradle.java.jdk.Javac.OPTION_SOURCE;
import static org.gradle.java.testing.StandardTestFrameworkModuleInfo.getTestModuleNameCommaDelimitedString;
import static org.gradle.util.TextUtil.getUnixLineSeparator;
import static org.gradle.util.TextUtil.getWindowsLineSeparator;

import static java.io.File.pathSeparator;
import static java.lang.Character.toLowerCase;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.readAllLines;
import static java.util.Comparator.naturalOrder;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;

public class JigsawPlugin implements Plugin<Project> {

    //<editor-fold desc="Constants">
    private static final Logger LOGGER = getLogger(JigsawPlugin.class);

    private static final String LS = lineSeparator();

    private static final Joiner LINE_JOINER = Joiner.on(LS);
    private static final Joiner PATH_JOINER = Joiner.on(pathSeparator);

    private static final String  LIB_DIR_PLACEHOLDER         = "LIB_DIR_PLACEHOLDER";
    private static final Pattern LIB_DIR_PLACEHOLDER_PATTERN = compile(LIB_DIR_PLACEHOLDER);

    private static final String PROPERTY_NAME_MODULE_NAMES = "moduleNames";

    private static final String JAVADOC_TASK_OPTION_MODULE_PATH = org.gradle.java.jdk.Javadoc.OPTION_MODULE_PATH.substring(1);

    private static final String VERB_COMPILE = "compile";

    private static final String TARGET_JAVA = "Java";

    private static final String DO_FIRST_ACTION_DISPLAY_NAME = "Execute doFirst {} action";
    //</editor-fold>


    //<editor-fold desc="Fields">
    private ImmutableMap<String, ImmutableMap<Path, String>> moduleNameIbyModuleInfoJavaPath_IbySourceSetName;

    private ImmutableSortedSet<String> moduleNameIsset;
    //</editor-fold>


    //<editor-fold desc="Accessors">
    private void setModuleNamesInputProperty(final Task task) {
        setModuleNamesInputProperty(task, join(",", moduleNameIsset));
    }
    //</editor-fold>


    //<editor-fold desc="Plugin methods">
    @Override
    public void apply(final Project project) {
        LOGGER.debug("Applying JigsawPlugin to {}", project.getName());

        project.getPlugins().apply(JavaPlugin.class);

        project.getGradle().getTaskGraph().whenReady(taskExecutionGraph -> {
            if (taskExecutionGraph.getAllTasks().stream().noneMatch(task -> project.equals(task.getProject()) && isSupportedTask(task))) {
                return;
            }

            parseModuleInfoJavas(project);

            if (! moduleNameIbyModuleInfoJavaPath_IbySourceSetName.isEmpty()) {
                moduleNameIsset =
                    moduleNameIbyModuleInfoJavaPath_IbySourceSetName.values().stream()
                    .flatMap(entry -> entry.values().stream())
                    .collect(toImmutableSortedSet(naturalOrder()))
                ;

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


    //<editor-fold desc="JavaCompile configuration methods">
    private void configureJavaCompileTasks(final Project project) {
        project.getTasks().withType(JavaCompile.class).forEach(this::configureJavaCompileTask);
    }

    private void configureJavaCompileTask(final JavaCompile javaCompile) {
        final String sourceSetName = getSourceSetName(javaCompile);

        final ImmutableMap<Path, String> moduleNameIbyModuleInfoJavaPath =
            moduleNameIbyModuleInfoJavaPath_IbySourceSetName.getOrDefault(sourceSetName, ImmutableMap.of())
        ;

        if (moduleNameIbyModuleInfoJavaPath.isEmpty()) {
            //TODO: use better heuristic to determine if javaCompile is for test code
            if (TEST_SOURCE_SET_NAME.equals(sourceSetName)) {
                // when source set doesn't contain any module-info.java, only enable modules if compiling a test source set
                configureCompileTestJavaTask(javaCompile);
            }
        }
        else {
            // source set contains at least one module-info.java
            doAfterAllOtherDoFirstActions(javaCompile, task -> {
                if (moduleNameIbyModuleInfoJavaPath.size() > 1) {
                    // generate --module-source-path

                    //TODO: fix failing .class output check at ValidateTaskProperties$1.visitFile(ValidateTaskProperties.java:162)
                    //TODO: determine the packages for each module, and include root dir for all sources in that package

                    final List<String> args = javaCompile.getOptions().getCompilerArgs();

                    args.add(OPTION_MODULE_SOURCE_PATH);
                    args.add(
                        getModuleSourcePath(
                            moduleNameIbyModuleInfoJavaPath.entrySet().stream()
                            .map(moduleNameIforModuleInfoJavaPath -> {
                                final Path   moduleInfoJavaPath          = moduleNameIforModuleInfoJavaPath.getKey();
                                final String moduleName                  = moduleNameIforModuleInfoJavaPath.getValue();
                                final String separator                   = moduleInfoJavaPath.getFileSystem().getSeparator();
                                final String moduleInfoJavaDirPathString = moduleInfoJavaPath.getParent().toString();

                                final int i = moduleInfoJavaDirPathString.lastIndexOf(separator + moduleName + separator);

                                return
                                    i == -1
                                        ? moduleInfoJavaDirPathString.endsWith(separator + moduleName)
                                            ? moduleInfoJavaDirPathString.substring(
                                                0,
                                                moduleInfoJavaDirPathString.length() - separator.length() - moduleName.length()
                                            )
                                            : moduleInfoJavaDirPathString
                                        : new StringBuilder(moduleInfoJavaDirPathString.length() - moduleName.length() + 1)
                                        .append(moduleInfoJavaDirPathString, 0, i + separator.length())
                                        .append('*')
                                        .append(
                                            moduleInfoJavaDirPathString,
                                            i + separator.length() + moduleName.length(),
                                            moduleInfoJavaDirPathString.length()
                                        )
                                        .toString()
                                ;
                            })
                            .collect(toImmutableSet())
                        )
                    );
                }

                configureJavaCompileTask(javaCompile, moduleNameIbyModuleInfoJavaPath.values(), javaCompile.getClasspath());
            });
        }
    }

    private void configureCompileTestJavaTask(final JavaCompile compileTestJava) {
        setModuleNamesInputProperty(compileTestJava);

        doAfterAllOtherDoFirstActions(compileTestJava, task -> {
            final Project project = compileTestJava.getProject();

            final List<String> args =
                configureJavaCompileTask(
                    compileTestJava,
                    moduleNameIsset,
                    compileTestJava.getClasspath().plus(getSourceSet(project, TEST_SOURCE_SET_NAME).getAllJava().getSourceDirectories())
                )
            ;

            project.getTasks().withType(Test.class).configureEach(test -> {
                final String testModuleNameCommaDelimitedString = getTestModuleNameCommaDelimitedString(test);

                if (! testModuleNameCommaDelimitedString.isEmpty()) {
                    args.add(OPTION_ADD_MODULES);
                    args.add(testModuleNameCommaDelimitedString);

                    moduleNameIsset.forEach(moduleName -> {
                        args.add(OPTION_ADD_READS);
                        args.add(moduleName + '=' + testModuleNameCommaDelimitedString);
                    });
                }
            });
        });
    }

    private List<String> configureJavaCompileTask(
        final JavaCompile                 javaCompile,
        final ImmutableCollection<String> moduleNameIcoll,
        final FileCollection              classpath
    ) {
        final List<String> args = javaCompile.getOptions().getCompilerArgs();

        addModuleArguments(args, moduleNameIcoll, classpath.getFiles());

        javaCompile.setClasspath(javaCompile.getProject().files());

        return args;
    }

    private String getModuleSourcePath(final ImmutableSet<String> moduleSourceIset) {
        if (moduleSourceIset.size() == 1) {
            return moduleSourceIset.iterator().next();
        }

        final UnmodifiableIterator<String> moduleSourceCommonUitr = moduleSourceIset.iterator();

        String commonPrefix = moduleSourceCommonUitr.next();
        String commonSuffix = commonPrefix;

        while (moduleSourceCommonUitr.hasNext()) {
            final String currModuleSource = moduleSourceCommonUitr.next();
            commonPrefix = commonPrefix(commonPrefix, currModuleSource);
            commonSuffix = commonSuffix(commonSuffix, currModuleSource);
        }

        if (commonPrefix.isEmpty() && commonSuffix.isEmpty()) {
            return moduleSourceIset.stream().collect(joining(",", "{", "}"));
        }

        final int commonPrefixLength = commonPrefix.length();
        final int commonSuffixLength = commonSuffix.length();

        final StringBuilder sb = new StringBuilder();
        sb.append(commonPrefix);
        sb.append('{');

        final UnmodifiableIterator<String> moduleSourceAlternateUitr = moduleSourceIset.iterator();

        appendModuleSourceAlternate(moduleSourceAlternateUitr.next(), commonPrefixLength, commonSuffixLength, sb);

        while (moduleSourceAlternateUitr.hasNext()) {
            sb.append(',');
            appendModuleSourceAlternate(moduleSourceAlternateUitr.next(), commonPrefixLength, commonSuffixLength, sb);
        }

        sb.append('}');
        sb.append(commonSuffix);

        return sb.toString();
    }

    private void appendModuleSourceAlternate(final String moduleSource, final int commonPrefixLength, final int commonSuffixLength, final StringBuilder sb) {
        sb.append(moduleSource, commonPrefixLength, moduleSource.length() - commonSuffixLength);
    }
    //</editor-fold>


    //<editor-fold desc="Test configuration methods">
    private void configureTestTasks(final Project project) {
        project.getTasks().withType(Test.class).forEach(this::configureTestTask);
    }

    private void configureTestTask(final Test test) {
        setModuleNamesInputProperty(test);

        doAfterAllOtherDoFirstActions(test, task -> {
            final Project project = test.getProject();

            final List<String> args = new ArrayList<>();

            addModuleArguments(args, moduleNameIsset, test.getClasspath().getFiles());

            args.add(OPTION_ADD_MODULES);
            args.add(ALL_MODULE_PATH);

            final String testModuleNameCommaDelimitedString = getTestModuleNameCommaDelimitedString(test);

            if (! testModuleNameCommaDelimitedString.isEmpty()) {
                moduleNameIsset.forEach(moduleName -> {
                    args.add(OPTION_ADD_READS);
                    args.add(moduleName + '=' + testModuleNameCommaDelimitedString);
                });
            }

            test.jvmArgs(args);

            test.setClasspath(project.files());
        });
    }
    //</editor-fold>


    //<editor-fold desc="Javadoc configuration methods">
    private void configureJavadocTasks(final Project project) {
        project.getTasks().withType(Javadoc.class).forEach(this::configureJavadocTask);
    }

    private void configureJavadocTask(final Javadoc javadoc) {
        setModuleNamesInputProperty(javadoc);

        doAfterAllOtherDoFirstActions(javadoc, task -> {
            final FileCollection classpath = javadoc.getClasspath();

            if (! classpath.isEmpty()) {
                ((CoreJavadocOptions) javadoc.getOptions()).addStringOption(JAVADOC_TASK_OPTION_MODULE_PATH, classpath.getAsPath());

                javadoc.setClasspath(javadoc.getProject().files());
            }
        });
    }
    //</editor-fold>


    //<editor-fold desc="JavaExec configuration methods">
    private void configureJavaExecTasks(final Project project) {
        project.getTasks().withType(JavaExec.class).forEach(this::configureJavaExecTask);
    }

    private void configureJavaExecTask(final JavaExec javaExec) {
        final String main       = javaExec.getMain();
        final String moduleName = getModuleName(main);

        if (moduleName != null) {
            setModuleNamesInputProperty(javaExec, moduleName);

            doAfterAllOtherDoFirstActions(javaExec, task -> {
                final List<String> args = new ArrayList<>();

                addModuleArguments(args, ImmutableSet.of(moduleName), javaExec.getClasspath().getFiles());

                args.add(OPTION_MODULE);
                args.add(main);

                javaExec.jvmArgs(args);
                javaExec.setMain("");
                javaExec.setClasspath(javaExec.getProject().files());
            });
        }
    }
    //</editor-fold>

    private String getModuleName(final String main) {
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


    //<editor-fold desc="CreateStartScripts configuration methods">
    private void configureCreateStartScriptsTasks(final Project project) {
        project.getTasks().withType(CreateStartScripts.class).forEach(this::configureCreateStartScriptsTask);
    }

    private void configureCreateStartScriptsTask(final CreateStartScripts createStartScripts) {
        final String main       = createStartScripts.getMainClassName();
        final String moduleName = getModuleName(main);

        if (moduleName != null) {
            setModuleNamesInputProperty(createStartScripts, moduleName);

            doAfterAllOtherDoFirstActions(createStartScripts, task -> {
                final List<String> args = new ArrayList<>();

                addAll(args, createStartScripts.getDefaultJvmOpts());

                args.add(OPTION_MODULE_PATH);
                args.add(LIB_DIR_PLACEHOLDER);

                args.add(OPTION_MODULE);
                args.add(main);

                createStartScripts.setDefaultJvmOpts(args);
                createStartScripts.setMainClassName("");
                createStartScripts.setClasspath(createStartScripts.getProject().files());
            });

            createStartScripts.doLast(task -> {
                replaceLibDirectoryPlaceholder(createStartScripts.getUnixScript()   .toPath(), "\\$APP_HOME/lib",   getUnixLineSeparator());
                replaceLibDirectoryPlaceholder(createStartScripts.getWindowsScript().toPath(), "%APP_HOME%\\\\lib", getWindowsLineSeparator());
            });
        }
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
    //</editor-fold>


    //<editor-fold desc="Task helper methods">
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
    //</editor-fold>


    //<editor-fold desc="SourceSet helper methods">
    private static SourceSetContainer getSourceSets(final Project project) {
        return project.getExtensions().getByType(SourceSetContainer.class);
    }

    private static SourceSet getSourceSet(final JavaCompile javaCompile) {
        return getCompileSourceSet(javaCompile, TARGET_JAVA);
    }

    private static SourceSet getCompileSourceSet(final Task task, final String target) {
        return getSourceSet(task, VERB_COMPILE, target);
    }

    private static SourceSet getSourceSet(final Task task, final String verb, final String target) {
        return getSourceSet(task.getProject(), task.getName(), verb, target);
    }

    private static SourceSet getSourceSet(final Project project, final String taskName, final String verb, final String target) {
        return getSourceSet(project, getSourceSetName(taskName, verb, target));
    }

    private static SourceSet getSourceSet(final Project project, final String sourceSetName) {
        return getSourceSets(project).getByName(sourceSetName);
    }


    private static String getSourceSetName(final JavaCompile javaCompile) {
        return getCompileSourceSetName(javaCompile, TARGET_JAVA);
    }

    private static String getCompileSourceSetName(final Task task, final String target) {
        return getSourceSetName(task, VERB_COMPILE, target);
    }

    private static String getSourceSetName(final Task task, final String verb, final String target) {
        return getSourceSetName(task.getName(), verb, target);
    }

    private static String getSourceSetName(final String taskName, final String verb, final String target) {
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


    private static JavaCompile getJavaCompile(final TaskContainer tasks, final SourceSet sourceSet) {
        return (JavaCompile) tasks.getByName(sourceSet.getCompileJavaTaskName());
    }
    //</editor-fold>


    //<editor-fold desc="moduleNames input property helper methods">
    private void setModuleNamesInputProperty(final Task task, final String moduleNamesCommaDelimited) {
        task.getInputs().property(PROPERTY_NAME_MODULE_NAMES, moduleNamesCommaDelimited);
    }
    //</editor-fold>


    //<editor-fold desc="JDK tool module options helper methods">
    private static void addModuleArguments(final List<String> args, final ImmutableCollection<String> moduleNameIcoll, final Set<File> classpathFileSet) {
        // determine which classpath elements will be in --module-path, and which in --patch-module
        final int classpathFileCount = classpathFileSet.size();

        final List<File>  modulePathFileList = new ArrayList<>(classpathFileCount);
        final List<File> patchModuleFileList = new ArrayList<>(classpathFileCount);

        for (final File classpathFile : classpathFileSet) {
            final Path classpathPath = classpathFile.toPath();
            if (
                isDirectory(classpathPath) &&
                ! containsModules(classpathPath)
            ) {
                // directories that don't contain module-info.class or *.jar files
                patchModuleFileList.add(classpathFile);
            }
            else {
                // directories that contain a module-info.class or at least one *.jar file; files (e.g., jars); nonexistent paths
                modulePathFileList.add(classpathFile);
            }
        }

        // add module arguments
        if (! modulePathFileList.isEmpty()) {
            args.add(OPTION_MODULE_PATH);
            args.add(PATH_JOINER.join(modulePathFileList));
        }

        if (! patchModuleFileList.isEmpty()) {
            if (moduleNameIcoll.size() > 1) {
                throw new GradleException(
                    "Cannot determine into which of the multiple modules to patch the non-module directories."                             + LS + LS
                    + "To avoid this problem, either only have one module per source set, or modularize the currently non-modular source." + LS + LS
                    + "Modules:"                                                    + LS + LS + LINE_JOINER.join(moduleNameIcoll)          + LS + LS
                    + "Directories containing non-modular source and/or resources:" + LS + LS + LINE_JOINER.join(patchModuleFileList)
                );
            }

            // moduleNameIcoll is guaranteed to have exactly one element
            final String moduleName = moduleNameIcoll.iterator().next();

            int sbLength = moduleName.length() + patchModuleFileList.size();

            for (final File patchModuleFile : patchModuleFileList) {
                sbLength += patchModuleFile.toString().length();
            }

            args.add(OPTION_PATCH_MODULE);
            args.add(PATH_JOINER.appendTo(new StringBuilder(sbLength).append(moduleName).append('='), patchModuleFileList).toString());
        }
    }

    private static boolean containsModules(final Path dirPath) {
        try (DirectoryStream<Path> ds = newDirectoryStream(dirPath, "{module-info.class,*.jar}")) {
            return ds.iterator().hasNext();
        }
        catch (final IOException ex) {
            throw new GradleException("Could not determine if directory contains modules: " + dirPath, ex);
        }
    }
    //</editor-fold>
}
