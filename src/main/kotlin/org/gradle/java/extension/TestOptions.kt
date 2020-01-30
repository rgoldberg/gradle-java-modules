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
package org.gradle.java.extension


import java.io.IOException
import java.nio.file.Files.copy
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import org.gradle.api.GradleException
import org.gradle.api.file.RelativePath
import org.gradle.api.internal.file.RelativeFile
import org.gradle.api.reflect.TypeOf.typeOf
import org.gradle.api.tasks.testing.Test
import org.gradle.java.JigsawPlugin
import org.gradle.java.jdk.JAVA
import org.gradle.java.testing.moduleNameIset


interface TestOptions: RuntimeJavaOptions, AutoGenerateSettableCascading {

    override val addModules:  AutoGeneratableSeparableValueVarargOption<String>
    override val modulePath:  AutoGeneratableSeparableValueVarargOption<String>
    override val addReads:    AutoGeneratableSeparableValueKeyedVarargOption<String, String>
    override val patchModule: AutoGeneratableSeparableValueKeyedVarargOption<String, String>
}


open class TestOptionsInternal(
                 autoGenerateParent:  AutoGenerateGettable,
                separateValueParent: SeparateValueGettable,
    private val                test: Test
):
TestOptions,
AutoGeneratableCascading by DefaultAutoGeneratableCascading( autoGenerateParent),
 SeparableValueCascading by  DefaultSeparableValueCascading(separateValueParent),
RuntimeJavaOptionsInternal() {

    override fun getPublicType() =
        PUBLIC_TYPE


    override val args = JavaForkOptionsArgAppendable(test)


    override val addModules: AutoGeneratableSeparableValueSetOptionInternal<String> by lazy {
        object:
        AutoGeneratableSeparableValueSetOptionInternal<String>,
        AddModules(),
        AutoGeneratableCascading by DefaultAutoGeneratableCascading(this) {
            override val value: Set<String>
            get() =
                super.value.appendAutoGeneratedElement(autoGenerate.isEnabled) {
                    JAVA.ALL_MODULE_PATH
                }
        }
    }

    override val addReads: AutoGeneratableSeparableValueLinkedHashMultimapOptionInternal<String, String> by lazy {
        object:
        AutoGeneratableSeparableValueLinkedHashMultimapOptionInternal<String, String>,
        AddReads(),
        AutoGeneratableCascading by DefaultAutoGeneratableCascading(this) {
            override val value: Map<String, Set<String>>
            get() {
                val testModuleNameIset by lazy {
                    test.moduleNameIset
                }

                return valueMutable.appendAutoGeneratedMapFromPairIterator(autoGenerate.isEnabled && testModuleNameIset.isNotEmpty()) {
                    test.project.plugins.getPlugin(JigsawPlugin::class.java).moduleNameIset.asSequence()
                    .map {it to testModuleNameIset}
                    .iterator()
                }
            }
        }
    }

    override val modulePath: AutoGeneratableSeparableValueSetOptionInternal<String> by lazy {
        object:
        AutoGeneratableSeparableValueSetOptionInternal<String>,
        ModulePath(),
        AutoGeneratableCascading by DefaultAutoGeneratableCascading(this) {
            override val value: Set<String>
            get() =
                super.value.appendAutoGeneratedIterator(autoGenerate.isEnabled) {
                    val classpath = test.classpath

                    //HACK:
                    // Test.classpath is used by the @TaskAction of Test:
                    //
                    // 1) to set the classpath for the JVM that runs tests. (Applies to all test frameworks)
                    //
                    // 2) for classes in the test source set that don't themselves directly contain tests, to determine if any of their superclasses provide
                    // tests to run. (Applies to JUnit 4 & TestNG, whose respective TestFramework.detector each returns an AbstractTestFrameworkDetector
                    // subclass; JUnit 5 does not use any TestFrameworkDetector, as JUnitPlatformTestFramework.detector always returns null)
                    //
                    // This lambda clears Test.classpath to prevent it from being used to provide a classpath for the test JVM, so that it can be
                    // used to provide the module path, instead, for the test JVM.  Clearing Test.classpath, however, prevents the @TaskAction of Test from
                    // finding superclass-provided tests under normal circumstances.
                    //
                    // The hack in the following try-block in the let-block runs before Test.classpath is cleared.
                    //
                    // Under normal circumstances, if superclasses must be searched for tests, the @TaskAction of Test at some point causes
                    // AbstractTestFrameworkDetector.prepareClasspath() to be called.
                    //
                    // The first time AbstractTestFrameworkDetector.prepareClasspath() is called, it constructs a classpath to search for superclass-
                    // provided tests from TestFrameworkDetector.testClasses & TestFrameworkDetector.testClasspath. This superclass-provided-test classpath
                    // is cached & used for all the detector's subsequent searches for superclass-provided tests.
                    //
                    // This lambda must therefore set testClasses & testClasspath to the correct values, and then cause prepareClasspath() to be called to
                    // cache the correct superclass-provided-test classpath.
                    //
                    // prepareClasspath() is private, however, so some other public method that causes prepareClasspath() to be called must be called (with
                    // the correct arguments).
                    //
                    // AbstractTestFrameworkDetector.processTestClass(RelativeFile), which is public, will cause prepareClasspath() to be called if its
                    // argument, testClassFile, points to a class file that does not contain any tests.
                    //
                    // Therefore, the hack sets testClasses & testClasspath correctly, then calls processTestClass(RelativeFile) on a RelativeFile for this
                    // class, since it doesn't contain any tests.
                    test.testFramework.detector?.let {detector ->
                        try {
                            // copy class file for this class to be used in the hack as described above
                            val classSimpleName = TestOptions::class.java.simpleName

                            val extractedClassFile = createTempFile(classSimpleName, ".class", test.temporaryDir)
                            extractedClassFile.deleteOnExit()

                            TestOptions::class.java.getResourceAsStream(classSimpleName + ".class").use {
                                copy(it, extractedClassFile.toPath(), REPLACE_EXISTING)
                            }

                            // setup testClasses & testClasspath of detector to find tests, then find tests
                            detector.setTestClasses(test.testClassesDirs.files)
                            detector.setTestClasspath(classpath.files)
                            detector.processTestClass(RelativeFile(extractedClassFile, RelativePath.parse(true, extractedClassFile.path)))
                        }
                        catch (ex: IOException) {
                            throw GradleException("Could not write non-test class file to setup superclass-provided-test classpath", ex)
                        }
                    }

                    autoGenerateModulePath(classpath, {test.classpath = test.project.files()}, {test.classpath = classpath})
                }
        }
    }

    override val patchModule: AutoGeneratableSeparableValueLinkedHashMultimapOptionInternal<String, String> by lazy {
        object:
        AutoGeneratableSeparableValueLinkedHashMultimapOptionInternal<String, String>,
        PatchModule(),
        AutoGeneratableCascading by DefaultAutoGeneratableCascading(this) {
            override val value: Map<String, Set<String>>
            get() =
                valueMutable.appendAutoGeneratedMapFromPair(autoGenerate.isEnabled) {
                    autoGeneratePatchModule(test.project.plugins.getPlugin(JigsawPlugin::class.java).moduleNameIset, test.classpath)
                }
        }
    }


    companion object {
        private val PUBLIC_TYPE = typeOf(TestOptions::class.java)
    }
}
