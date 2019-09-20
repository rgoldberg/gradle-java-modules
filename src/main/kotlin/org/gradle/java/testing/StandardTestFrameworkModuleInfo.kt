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
package org.gradle.java.testing


import com.google.common.collect.Maps.uniqueIndex
import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.api.internal.tasks.testing.testng.TestNGTestFramework
import org.gradle.api.tasks.testing.Test
import org.gradle.java.testing.StandardTestFrameworkModuleInfo.Companion.from


fun getTestModuleNameCommaDelimitedString(test: Test) =
    from(test).testModuleNameCommaDelimitedString


enum class StandardTestFrameworkModuleInfo(
    private  val testFrameworkClassName:             String,
    override val testModuleNameCommaDelimitedString: String
):
TestFrameworkModuleInfo {
    JUNIT4( JUnitTestFramework        ::class.java.name, "junit"),
    JUNIT5( JUnitPlatformTestFramework::class.java.name, "org.junit.jupiter.api"),
    TESTNG( TestNGTestFramework       ::class.java.name, "testng"),
    UNKNOWN("",                                          "");


    companion object {
        private val TEST_FRAMEWORK_MODULE_INFO_IBY_CLASS_NAME =
            uniqueIndex(listOf(*values())) {it!!.testFrameworkClassName}

        fun from(test: Test) =
            TEST_FRAMEWORK_MODULE_INFO_IBY_CLASS_NAME.getOrDefault(test.testFramework.javaClass.name, UNKNOWN)
    }
}
