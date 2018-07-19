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

import com.google.common.collect.ImmutableSet;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.application.tasks.CreateStartScripts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Streams.stream;
import static com.google.common.io.MoreFiles.asCharSink;
import static org.gradle.api.logging.Logging.getLogger;
import static org.gradle.api.plugins.ApplicationPlugin.APPLICATION_PLUGIN_NAME;
import static org.gradle.api.plugins.ApplicationPlugin.TASK_RUN_NAME;
import static org.gradle.api.plugins.ApplicationPlugin.TASK_START_SCRIPTS_NAME;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.TEST_TASK_NAME;
import static org.gradle.util.TextUtil.getUnixLineSeparator;
import static org.gradle.util.TextUtil.getWindowsLineSeparator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllLines;
import static java.util.regex.Pattern.compile;

public class JigsawPlugin implements Plugin<Project> {

    private static final Logger LOGGER = getLogger(JigsawPlugin.class);

    private static final String EXTENSION_NAME = "javaModule";

    private static final String  LIB_DIR_PLACEHOLDER         = "LIB_DIR_PLACEHOLDER";
    private static final Pattern LIB_DIR_PLACEHOLDER_PATTERN = compile(LIB_DIR_PLACEHOLDER);


    @Override
    public void apply(final Project project) {
        LOGGER.debug("Applying JigsawPlugin to {}", project.getName());
        project.getPlugins().apply(JavaPlugin.class);
        project.getExtensions().create(EXTENSION_NAME, JavaModule.class);

        configureJavaTasks(project);
    }


    private void configureJavaTasks(final Project project) {
        project.afterEvaluate(p -> {
            configureCompileJavaTask(p);
            configureCompileTestJavaTask(p);
            configureTestTask(p);
            p.getPluginManager().withPlugin(APPLICATION_PLUGIN_NAME, appliedPlugin -> {
                configureRunTask(p);
                configureStartScriptsTask(p);
            });
        });
    }


    private void configureCompileJavaTask(final Project project) {
        final JavaCompile compileJava = (JavaCompile) project.getTasks().findByName(COMPILE_JAVA_TASK_NAME);
        final String moduleName = getJavaModuleName(project);
        final ImmutableSet<File> outputDirFileIset =
            getSourceSets(project).stream().flatMap(sourceSet -> stream(sourceSet.getOutput())).collect(toImmutableSet())
        ;
        compileJava.doFirst(task -> {
            final List<String> args = compileJava.getOptions().getCompilerArgs();

            addModulePathArgument(args, compileJava.getClasspath().filter(f -> ! outputDirFileIset.contains(f)));

            addPatchModuleArgument(args, moduleName, compileJava.getClasspath().filter(outputDirFileIset::contains));

            compileJava.setClasspath(project.files());
        });
    }


    private void configureCompileTestJavaTask(final Project project) {
        final JavaCompile compileTestJava = (JavaCompile) project.getTasks().findByName(COMPILE_TEST_JAVA_TASK_NAME);
        final SourceSet test = getSourceSets(project).getByName("test");
        final String moduleName = getJavaModuleName(project);
        compileTestJava.getInputs().property("moduleName", moduleName);
        compileTestJava.doFirst(task -> {
            final List<String> args = compileTestJava.getOptions().getCompilerArgs();

            addModulePathArgument(args, compileTestJava.getClasspath());

            args.add("--add-modules");
            args.add("junit");

            args.add("--add-reads");
            args.add(moduleName + "=junit");

            addPatchModuleArgument(args, moduleName, test.getJava().getSourceDirectories());

            compileTestJava.setClasspath(project.files());
        });
    }


    private void configureTestTask(final Project project) {
        final Test testTask = (Test) project.getTasks().findByName(TEST_TASK_NAME);
        final SourceSet test = getSourceSets(project).getByName("test");
        final String moduleName = getJavaModuleName(project);
        testTask.getInputs().property("moduleName", moduleName);
        testTask.doFirst(task -> {
            final List<String> args = new ArrayList<>();

            addModulePathArgument(args, testTask.getClasspath());

            args.add("--add-modules");
            args.add("ALL-MODULE-PATH");

            args.add("--add-reads");
            args.add(moduleName + "=junit");

            addPatchModuleArgument(args, moduleName, test.getJava().getOutputDir());

            testTask.jvmArgs(args);

            testTask.setClasspath(project.files());
        });
    }


    private void configureRunTask(final Project project) {
        final JavaExec run = (JavaExec) project.getTasks().findByName(TASK_RUN_NAME);
        final String moduleName = getJavaModuleName(project);
        run.getInputs().property("moduleName", moduleName);
        run.doFirst(task -> {
            final List<String> args = new ArrayList<>();

            addModulePathArgument(args, run.getClasspath());

            args.add("--module");
            args.add(moduleName + '/' + run.getMain());

            run.jvmArgs(args);
            run.setMain("");
            run.setClasspath(project.files());
        });
    }


    private void configureStartScriptsTask(final Project project) {
        final CreateStartScripts startScripts = (CreateStartScripts) project.getTasks().findByName(TASK_START_SCRIPTS_NAME);
        final String moduleName = getJavaModuleName(project);
        startScripts.getInputs().property("moduleName", moduleName);
        startScripts.doFirst(task -> {
            final List<String> args = new ArrayList<>();

            addAll(args, startScripts.getDefaultJvmOpts());

            args.add("--module-path");
            args.add(LIB_DIR_PLACEHOLDER);

            args.add("--module");
            args.add(moduleName + '/' + startScripts.getMainClassName());

            startScripts.setDefaultJvmOpts(args);
            startScripts.setMainClassName("");
            startScripts.setClasspath(project.files());
        });
        startScripts.doLast(task -> {
            replaceLibDirectoryPlaceholder(startScripts.getUnixScript()   .toPath(), "\\$APP_HOME/lib",   getUnixLineSeparator());
            replaceLibDirectoryPlaceholder(startScripts.getWindowsScript().toPath(), "%APP_HOME%\\\\lib", getWindowsLineSeparator());
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


    private String getJavaModuleName(final Project project) {
        return ((JavaModule) project.getExtensions().getByName(EXTENSION_NAME)).getName();
    }


    private static SourceSetContainer getSourceSets(final Project project) {
        return (SourceSetContainer) project.getProperties().get("sourceSets");
    }


    private void addModulePathArgument(final List<String> args, final FileCollection modulePathFileCollection) {
        if (! modulePathFileCollection.isEmpty()) {
            args.add("--module-path");
            args.add(modulePathFileCollection.getAsPath());
        }
    }

    private void addPatchModuleArgument(final List<String> args, final String moduleName, final File file) {
        args.add("--patch-module");
        args.add(moduleName + '=' + file);
    }

    private void addPatchModuleArgument(final List<String> args, final String moduleName, final FileCollection patchModuleFileCollection) {
        if (! patchModuleFileCollection.isEmpty()) {
            args.add("--patch-module");
            args.add(moduleName + '=' + patchModuleFileCollection.getAsPath());
        }
    }
}
