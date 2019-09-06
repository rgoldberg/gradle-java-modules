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
package org.gradle.java.taskconfigurer;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.testing.Test;
import org.gradle.java.JigsawPlugin;
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;
import static org.gradle.java.GradleUtils.doAfterAllOtherDoFirstActions;
import static org.gradle.java.GradleUtils.doBeforeAllOtherDoLastActions;
import static org.gradle.java.GradleUtils.getCompileSourceSetName;
import static org.gradle.java.GradleUtils.getSourceSet;
import static org.gradle.java.Modules.splitIntoModulePathAndPatchModule;
import static org.gradle.java.testing.StandardTestFrameworkModuleInfo.getTestModuleNameCommaDelimitedString;

import static java.io.File.pathSeparator;

public class KotlinCompileTaskConfigurer implements TaskConfigurer<KotlinCompile> {

    private static final Joiner PATH_JOINER = Joiner.on(pathSeparator);

    private static final String TARGET = "Kotlin";

    private static final String OPTION_ADD_MODULES = "-Xadd-modules=";
    private static final String OPTION_MODULE_PATH = "-Xmodule-path=";


    public KotlinCompileTaskConfigurer() {}


    @Override
    public Class<KotlinCompile> getTaskClass() {
        return KotlinCompile.class;
    }

    @Override
    public void configureTask(final KotlinCompile kotlinCompile, final JigsawPlugin jigsawPlugin) {
        final String sourceSetName = getCompileSourceSetName(kotlinCompile, TARGET);

        final ImmutableMap<Path, String> moduleNameIbyModuleInfoJavaPath = jigsawPlugin.getModuleNameIbyModuleInfoJavaPath(sourceSetName);

        if (moduleNameIbyModuleInfoJavaPath.isEmpty()) {
            //TODO: use better heuristic to determine if kotlinCompile is for test code
            if (TEST_SOURCE_SET_NAME.equals(sourceSetName)) {
                // when source set doesn't contain any module-info.java, only enable modules if compiling a test source set
                jigsawPlugin.setModuleNamesInputProperty(kotlinCompile);

                final FileCollection[] classpathHolder = new FileCollection[1];

                doAfterAllOtherDoFirstActions(kotlinCompile, task -> {
                    final FileCollection classpath = kotlinCompile.getClasspath();

                    classpathHolder[0] = classpath;

                    final Project project = kotlinCompile.getProject();

                    //TODO: .getCompileTaskName(LANGUAGE_NAME_KOTLIN)
                    final List<String> args =
                        configureTask(
                            kotlinCompile,
                            jigsawPlugin.getModuleNameIsset(),
                            classpath.plus(getSourceSet(project, TEST_SOURCE_SET_NAME).getAllSource().getSourceDirectories()) //TODO? getAllSource()
                        )
                    ;

                    //TODO: ensure works
                    project.getTasks().withType(Test.class).configureEach(test -> {
                        final String testModuleNameCommaDelimitedString = getTestModuleNameCommaDelimitedString(test);

                        if (! testModuleNameCommaDelimitedString.isEmpty()) {
                            args.add(OPTION_ADD_MODULES + testModuleNameCommaDelimitedString);
                        }
                    });
                });

                doBeforeAllOtherDoLastActions(kotlinCompile, task -> kotlinCompile.setClasspath(classpathHolder[0]));
            }
        }
        else {
            // source set contains at least one module-info.java
            final FileCollection[] classpathHolder = new FileCollection[1];

            doAfterAllOtherDoFirstActions(kotlinCompile, task -> {
                final FileCollection classpath = kotlinCompile.getClasspath();

                classpathHolder[0] = classpath;

                final ImmutableCollection<String> moduleNameIcoll = moduleNameIbyModuleInfoJavaPath.values();

                //TODO: FILTER BASED ON PRESENCE OF MODULE
                configureTask(kotlinCompile, moduleNameIcoll, classpath);
            });

            doBeforeAllOtherDoLastActions(kotlinCompile, task -> kotlinCompile.setClasspath(classpathHolder[0]));
        }
    }

    private List<String> configureTask(final KotlinCompile kotlinCompile, final ImmutableCollection<String> moduleNameIcoll, final FileCollection classpath) {
        final KotlinJvmOptions kotlinJvmOptions = kotlinCompile.getKotlinOptions();

        final List<String> args = new ArrayList<>(kotlinJvmOptions.getFreeCompilerArgs());

        kotlinJvmOptions.setFreeCompilerArgs(args);

        splitIntoModulePathAndPatchModule(
            classpath.getFiles(),
            moduleNameIcoll,
            modulePathFileList ->
                args.add(
                    PATH_JOINER.appendTo(
                        new StringBuilder(
                            OPTION_MODULE_PATH.length()
                            + modulePathFileList.size()
                            - 1
                            + modulePathFileList.stream().mapToInt(patchModuleFile -> patchModuleFile.toString().length()).sum()
                        )
                        .append(OPTION_MODULE_PATH),
                        modulePathFileList
                    )
                    .toString()
                )
            ,
            patchModuleFileList -> {
                //TODO: find kotlinc equivalent for javac's --patch-module
            }
        );

        kotlinCompile.setClasspath(kotlinCompile.getProject().files());

        return args;
    }
}
