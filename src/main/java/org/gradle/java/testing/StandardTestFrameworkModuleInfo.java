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
package org.gradle.java.testing;

import com.google.common.collect.ImmutableMap;
import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework;
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework;
import org.gradle.api.internal.tasks.testing.testng.TestNGTestFramework;
import org.gradle.api.tasks.testing.Test;

import static com.google.common.collect.Maps.uniqueIndex;

import static java.util.Arrays.asList;

public enum StandardTestFrameworkModuleInfo implements TestFrameworkModuleInfo {
    JUNIT4( JUnitTestFramework        .class.getName(), "junit"),
    JUNIT5( JUnitPlatformTestFramework.class.getName(), "org.junit.jupiter.api"),
    TESTNG( TestNGTestFramework       .class.getName(), "testng"),
    UNKNOWN("",                                         "");


    private static final ImmutableMap<String, StandardTestFrameworkModuleInfo> TEST_FRAMEWORK_MODULE_INFO_IBY_CLASS_NAME =
        uniqueIndex(asList(values()), testFrameworkModuleInfo -> testFrameworkModuleInfo.testFrameworkClassName)
    ;


    public static StandardTestFrameworkModuleInfo from(final Test test) {
        return TEST_FRAMEWORK_MODULE_INFO_IBY_CLASS_NAME.getOrDefault(test.getTestFramework().getClass().getName(), UNKNOWN);
    }

    public static String getTestModuleNameCommaDelimitedString(final Test test) {
        return from(test).getTestModuleNameCommaDelimitedString();
    }


    private final String testFrameworkClassName;
    private final String testModuleNameCommaDelimitedString;


    StandardTestFrameworkModuleInfo(final String testFrameworkClassName, final String testModuleNameCommaDelimitedString) {
        this.testFrameworkClassName             = testFrameworkClassName;
        this.testModuleNameCommaDelimitedString = testModuleNameCommaDelimitedString;
    }


    @Override
    public String getTestModuleNameCommaDelimitedString() {
        return testModuleNameCommaDelimitedString;
    }
}
