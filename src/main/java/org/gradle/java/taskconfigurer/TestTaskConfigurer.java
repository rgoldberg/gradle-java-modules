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

import com.google.common.collect.ImmutableSortedSet;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.RelativeFile;
import org.gradle.api.internal.tasks.testing.detection.TestFrameworkDetector;
import org.gradle.api.tasks.testing.Test;
import org.gradle.java.JigsawPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.gradle.java.GradleUtils.doAfterAllOtherDoFirstActions;
import static org.gradle.java.GradleUtils.doBeforeAllOtherDoLastActions;
import static org.gradle.java.jdk.Java.ALL_MODULE_PATH;
import static org.gradle.java.jdk.Java.OPTION_ADD_MODULES;
import static org.gradle.java.jdk.Java.OPTION_ADD_READS;
import static org.gradle.java.jdk.JavaCommonTool.addModuleArguments;
import static org.gradle.java.testing.StandardTestFrameworkModuleInfo.getTestModuleNameCommaDelimitedString;

import static java.io.File.createTempFile;
import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class TestTaskConfigurer implements TaskConfigurer<Test> {

    public TestTaskConfigurer() {}


    @Override
    public Class<Test> getTaskClass() {
        return Test.class;
    }

    @Override
    public void configureTask(final Test test, final JigsawPlugin jigsawPlugin) {
        jigsawPlugin.setModuleNamesInputProperty(test);

        final FileCollection[] classpathHolder = new FileCollection[1];

        doAfterAllOtherDoFirstActions(test, task -> {
            final FileCollection classpath = test.getClasspath();

            classpathHolder[0] = classpath;

            final Set<File> classpathFileSet = classpath.getFiles();

            //HACK:
            // Test#classpath is used by the @TaskAction of Test to set the classpath, which is used to:
            //
            // 1) find superclasses of classes in the test source set that don't themselves directly contain tests to determine if any of the superclasses turn
            // the leaf classes into test classes. (Applies only to JUnit 4 & TestNG, whose respective TestFramework#getDetector() each returns an
            // AbstractTestFrameworkDetector subclass; JUnit 5 does not use any TestFrameworkDetector, as JUnitPlatformTestFramework#getDetector() always
            // returns null)
            //
            // 2) set the classpath for the JVM that runs tests. (Applies to all test frameworks)
            //
            // Test#classpath must be empty when this Action<? super Task> finishes, however, to prevent classpath from being set & used by the test JVM, so
            // that this TestTaskConfigurer can setup the --module-path instead.
            //
            // The hack in the following try-block in the if-block tries to force the detection of tests created by superclasses before the @TaskAction of Test
            // runs, so that this Action<? super Task> can safely set Test#classpath to empty.
            //
            // The @TaskAction of Test at some point causes AbstractTestFrameworkDetector#prepareClasspath() to be called, which, the first time it is called,
            // uses TestFrameworkDetector#testClasses & TestFrameworkDetector#testClasspath to construct a classpath to search for test superclasses. This
            // classpath is cached & then used for all the detector's subsequent detections.
            //
            // This Action<? super Task> must therefore set #testClasses & #testClasspath to the correct values, and then cause #prepareClasspath() to be called
            // to cache the correct test-superclass-search classpath.
            //
            // #prepareClasspath() is private, so some other public method (with the correct arguments) that causes #prepareClasspath() to be called must be
            // called instead.
            //
            // AbstractTestFrameworkDetector#processTestClass(RelativeFile testClassFile), which is public, will cause #prepareClasspath() to be called if
            // its argument, testClassFile, points to a class file that does not contain any tests.
            //
            // Therefore, the hack sets #testClasses & #testClasspath correctly, then calls #processTestClass(RelativeFile) on a RelativeFile for this class,
            // since it doesn't contain any tests.
            final TestFrameworkDetector detector = test.getTestFramework().getDetector();

            if (detector != null) {
                try {
                    // copy class file for this class to be used in the hack as described above
                    final String classSimpleName = TestTaskConfigurer.class.getSimpleName();

                    final File extractedClassFile = createTempFile(classSimpleName, ".class", test.getTemporaryDir());
                    extractedClassFile.deleteOnExit();

                    try (InputStream extractedClassIs = TestTaskConfigurer.class.getResourceAsStream(classSimpleName + ".class")) {
                        copy(extractedClassIs, extractedClassFile.toPath(), REPLACE_EXISTING);
                    }

                    // setup #testClasses & #testClasspath of detector to find tests, then find tests
                    detector.setTestClasses(test.getTestClassesDirs().getFiles());
                    detector.setTestClasspath(classpathFileSet);
                    detector.processTestClass(new RelativeFile(extractedClassFile, RelativePath.parse(true, extractedClassFile.getPath())));
                }
                catch (final IOException ex) {
                    throw new GradleException("Could not write non-test class file to setup test-superclass-search classpath", ex);
                }
            }

            final Project project = test.getProject();

            final List<String> args = new ArrayList<>();

            final ImmutableSortedSet<String> moduleNameIsset = jigsawPlugin.getModuleNameIsset();

            addModuleArguments(args, moduleNameIsset, classpathFileSet);

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

        doBeforeAllOtherDoLastActions(test, task -> test.setClasspath(classpathHolder[0]));
    }
}
