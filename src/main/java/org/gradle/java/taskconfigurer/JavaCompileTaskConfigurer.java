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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.UnmodifiableIterator;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.java.JigsawPlugin;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static com.google.common.base.Strings.commonPrefix;
import static com.google.common.base.Strings.commonSuffix;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Streams.stream;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;
import static org.gradle.java.GradleUtils.doAfterAllOtherDoFirstActions;
import static org.gradle.java.GradleUtils.getSourceSet;
import static org.gradle.java.GradleUtils.getSourceSetName;
import static org.gradle.java.jdk.JavaCommonTool.addModuleArguments;
import static org.gradle.java.jdk.Javac.OPTION_ADD_MODULES;
import static org.gradle.java.jdk.Javac.OPTION_ADD_READS;
import static org.gradle.java.jdk.Javac.OPTION_MODULE_SOURCE_PATH;
import static org.gradle.java.testing.StandardTestFrameworkModuleInfo.getTestModuleNameCommaDelimitedString;

import static java.util.stream.Collectors.joining;

public class JavaCompileTaskConfigurer implements TaskConfigurer<JavaCompile> {

    public JavaCompileTaskConfigurer() {}


    @Override
    public Class<JavaCompile> getTaskClass() {
        return JavaCompile.class;
    }

    @Override
    public void configureTask(final JavaCompile javaCompile, final JigsawPlugin jigsawPlugin) {
        final String sourceSetName = getSourceSetName(javaCompile);

        final ImmutableMap<Path, String> moduleNameIbyModuleInfoJavaPath = jigsawPlugin.getModuleNameIbyModuleInfoJavaPath(sourceSetName);

        if (moduleNameIbyModuleInfoJavaPath.isEmpty()) {
            //TODO: use better heuristic to determine if javaCompile is for test code
            if (TEST_SOURCE_SET_NAME.equals(sourceSetName)) {
                // when source set doesn't contain any module-info.java, only enable modules if compiling a test source set
                jigsawPlugin.setModuleNamesInputProperty(javaCompile);

                doAfterAllOtherDoFirstActions(javaCompile, task -> {
                    final Project project = javaCompile.getProject();

                    final ImmutableSortedSet<String> moduleNameIsset = jigsawPlugin.getModuleNameIsset();

                    final List<String> args =
                        configureTask(
                            javaCompile,
                            moduleNameIsset,
                            javaCompile.getClasspath().plus(getSourceSet(project, TEST_SOURCE_SET_NAME).getAllJava().getSourceDirectories())
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
        }
        else {
            // source set contains at least one module-info.java
            doAfterAllOtherDoFirstActions(javaCompile, task -> {
                final ImmutableCollection<String> moduleNameIcoll = moduleNameIbyModuleInfoJavaPath.values();

                if (moduleNameIbyModuleInfoJavaPath.size() > 1) {
                    // generate --module-source-path

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

                    // must change the classes output directories for the SourceSet:
                    // for each existing output directory, d, replace with subdirectories of d, one for each compile module name

                    //TODO: only works if SourceSet#output#classesDirs is a ConfigurableFileCollection
                    final ConfigurableFileCollection outputClassesDirs = (ConfigurableFileCollection) getSourceSet(javaCompile).getOutput().getClassesDirs();

                    //TODO: ensure it is OK to change SourceSet#output#classesDirs during execution phase
                    outputClassesDirs.setFrom(
                        stream(outputClassesDirs)
                        .flatMap(dirFile -> moduleNameIcoll.stream().map(moduleName -> new File(dirFile, moduleName)))
                        .toArray()
                    );
                }

                configureTask(javaCompile, moduleNameIcoll, javaCompile.getClasspath());
            });
        }
    }

    private List<String> configureTask(final JavaCompile javaCompile, final ImmutableCollection<String> moduleNameIcoll, final FileCollection classpath) {
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
}
