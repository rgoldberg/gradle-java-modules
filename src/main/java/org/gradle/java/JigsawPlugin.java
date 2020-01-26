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
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.AppliedPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.application.tasks.CreateStartScripts;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.gradle.api.logging.Logging.getLogger;
import static org.gradle.api.plugins.ApplicationPlugin.APPLICATION_PLUGIN_NAME;
import static org.gradle.api.plugins.ApplicationPlugin.TASK_RUN_NAME;
import static org.gradle.api.plugins.ApplicationPlugin.TASK_START_SCRIPTS_NAME;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.TEST_TASK_NAME;

public class JigsawPlugin implements Plugin<Project> {
    private static final Logger LOGGER = getLogger(JigsawPlugin.class);

    private static final String EXTENSION_NAME = "javaModule";

    private static final String LIBS_PLACEHOLDER = "APP_HOME_LIBS_PLACEHOLDER";

    @Override
    public void apply(final Project project) {
        LOGGER.debug("Applying JigsawPlugin to " + project.getName());
        project.getPlugins().apply(JavaPlugin.class);
        project.getExtensions().create(EXTENSION_NAME, JavaModule.class);

        configureJavaTasks(project);
    }


    private void configureJavaTasks(final Project project) {
        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(final Project project) {
                configureCompileJavaTask(project);
                configureCompileTestJavaTask(project);
                configureTestTask(project);
                project.getPluginManager().withPlugin(APPLICATION_PLUGIN_NAME, new Action<AppliedPlugin>() {
                    @Override
                    public void execute(final AppliedPlugin appliedPlugin) {
                        configureRunTask(project);
                        configureStartScriptsTask(project);
                    }
                });
            }
        });
    }


    private void configureCompileJavaTask(final Project project) {
        final JavaCompile compileJava = (JavaCompile) project.getTasks().findByName(COMPILE_JAVA_TASK_NAME);
        compileJava.doFirst(new Action<Task>() {
            @Override
            public void execute(final Task task) {
                final List<String> args = new ArrayList<>();
                args.add("--module-path");
                args.add(compileJava.getClasspath().getAsPath());
                compileJava.getOptions().setCompilerArgs(args);
                compileJava.setClasspath(project.files());
            }
        });
    }


    private void configureCompileTestJavaTask(final Project project) {
        final JavaCompile compileTestJava = (JavaCompile) project.getTasks().findByName(COMPILE_TEST_JAVA_TASK_NAME);
        final SourceSet test = ((SourceSetContainer) project.getProperties().get("sourceSets")).getByName("test");
        final String moduleName = getJavaModuleName(project);
        compileTestJava.getInputs().property("moduleName", moduleName);
        compileTestJava.doFirst(new Action<Task>() {
            @Override
            public void execute(final Task task) {
                final List<String> args = new ArrayList<>();
                args.add("--module-path");
                args.add(compileTestJava.getClasspath().getAsPath());
                args.add("--add-modules");
                args.add("junit");
                args.add("--add-reads");
                args.add(moduleName + "=junit");
                args.add("--patch-module");
                args.add(moduleName + '=' + test.getJava().getSourceDirectories().getAsPath());
                compileTestJava.getOptions().setCompilerArgs(args);
                compileTestJava.setClasspath(project.files());
            }
        });
    }


    private void configureTestTask(final Project project) {
        final Test testTask = (Test) project.getTasks().findByName(TEST_TASK_NAME);
        final SourceSet test = ((SourceSetContainer) project.getProperties().get("sourceSets")).getByName("test");
        final String moduleName = getJavaModuleName(project);
        testTask.getInputs().property("moduleName", moduleName);
        testTask.doFirst(new Action<Task>() {
            @Override
            public void execute(final Task task) {
                final List<String> args = new ArrayList<>();
                args.add("--module-path");
                args.add(testTask.getClasspath().getAsPath());
                args.add("--add-modules");
                args.add("ALL-MODULE-PATH");
                args.add("--add-reads");
                args.add(moduleName + "=junit");
                args.add("--patch-module");
                args.add(moduleName + '=' + test.getJava().getOutputDir());
                testTask.setJvmArgs(args);
                testTask.setClasspath(project.files());
            }
        });
    }


    private void configureRunTask(final Project project) {
        final JavaExec run = (JavaExec) project.getTasks().findByName(TASK_RUN_NAME);
        final String moduleName = getJavaModuleName(project);
        run.getInputs().property("moduleName", moduleName);
        run.doFirst(new Action<Task>() {
            @Override
            public void execute(final Task task) {
                final List<String> args = new ArrayList<>();
                args.add("--module-path");
                args.add(run.getClasspath().getAsPath());
                args.add("--module");
                args.add(moduleName + '/' + run.getMain());
                run.setJvmArgs(args);
                run.setMain("");
                run.setClasspath(project.files());
            }
        });
    }


    private void configureStartScriptsTask(final Project project) {
        final CreateStartScripts startScripts = (CreateStartScripts) project.getTasks().findByName(TASK_START_SCRIPTS_NAME);
        final String moduleName = getJavaModuleName(project);
        startScripts.getInputs().property("moduleName", moduleName);
        startScripts.doFirst(new Action<Task>() {
            @Override
            public void execute(final Task task) {
                startScripts.setClasspath(project.files());
                final List<String> args = new ArrayList<>();
                args.add("--module-path");
                args.add(LIBS_PLACEHOLDER);
                args.add("--module");
                args.add(moduleName + '/' + startScripts.getMainClassName());
                startScripts.setDefaultJvmOpts(args);
                startScripts.setMainClassName("");
            }
        });
        startScripts.doLast(new Action<Task>() {
            @Override
            public void execute(final Task task) {
                final File bashScript = new File(startScripts.getOutputDir(), startScripts.getApplicationName());
                replaceLibsPlaceHolder(bashScript.toPath(), "\\$APP_HOME/lib");
                final File batFile = new File(startScripts.getOutputDir(), startScripts.getApplicationName() + ".bat");
                replaceLibsPlaceHolder(batFile.toPath(), "%APP_HOME%\\lib");
            }
        });
    }

    private void replaceLibsPlaceHolder(final Path path, final String newText) {
        try {
            final List<String> bashContent = new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
            for (int i=0; i < bashContent.size(); i++) {
                bashContent.set(i, bashContent.get(i).replaceFirst(LIBS_PLACEHOLDER, newText));
            }
            Files.write(path, bashContent, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new GradleException("Couldn't replace placeholder in " + path);
        }
    }


    private String getJavaModuleName(final Project project) {
        return ((JavaModule) project.getExtensions().getByName(EXTENSION_NAME)).getName();
    }
}
