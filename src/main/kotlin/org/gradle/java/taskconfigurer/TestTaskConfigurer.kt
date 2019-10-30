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
package org.gradle.java.taskconfigurer


import java.io.IOException
import java.nio.file.Files.copy
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.file.RelativePath
import org.gradle.api.internal.file.RelativeFile
import org.gradle.api.tasks.testing.Test
import org.gradle.java.JigsawPlugin
import org.gradle.java.jdk.JAVA
import org.gradle.java.testing.moduleNameCommaDelimitedString
import org.gradle.java.util.doAfterAllOtherDoFirstActions
import org.gradle.java.util.doBeforeAllOtherDoLastActions


class TestTaskConfigurer: TaskConfigurer<Test> {

    override val taskClass
    get() = Test::class.java

    override fun configureTask(test: Test, jigsawPlugin: JigsawPlugin) {
        jigsawPlugin.setModuleNamesInputProperty(test)

        val classpath by lazy {test.classpath}

        test.doAfterAllOtherDoFirstActions(Action {
            val classpathFileSet = classpath.files

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
            test.testFramework.detector?.let {detector ->
                try {
                    // copy class file for this class to be used in the hack as described above
                    val classSimpleName = TestTaskConfigurer::class.java.simpleName

                    val extractedClassFile = createTempFile(classSimpleName, ".class", test.temporaryDir)
                    extractedClassFile.deleteOnExit()

                    TestTaskConfigurer::class.java.getResourceAsStream(classSimpleName + ".class").use {extractedClassIs ->
                        copy(extractedClassIs, extractedClassFile.toPath(), REPLACE_EXISTING)
                    }

                    // setup #testClasses & #testClasspath of detector to find tests, then find tests
                    detector.setTestClasses(test.testClassesDirs.files)
                    detector.setTestClasspath(classpathFileSet)
                    detector.processTestClass(RelativeFile(extractedClassFile, RelativePath.parse(true, extractedClassFile.path)))
                }
                catch (ex: IOException) {
                    throw GradleException("Could not write non-test class file to setup test-superclass-search classpath", ex)
                }
            }

            val args = mutableListOf<String>()

            val moduleNameIsset = jigsawPlugin.moduleNameIsset

            JAVA.addModuleArguments(args, moduleNameIsset, classpathFileSet)

            args += JAVA.OPTION_ADD_MODULES
            args += JAVA.ALL_MODULE_PATH

            test.moduleNameCommaDelimitedString?.let {testModuleNameCommaDelimitedString ->
                moduleNameIsset.forEach {moduleName ->
                    args += JAVA.OPTION_ADD_READS
                    args += moduleName + '=' + testModuleNameCommaDelimitedString
                }
            }

            test.jvmArgs(args)

            test.classpath = test.project.files()
        })

        test.doBeforeAllOtherDoLastActions(Action {test.classpath = classpath})
    }
}
