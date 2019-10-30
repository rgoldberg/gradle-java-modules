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


import com.google.common.collect.ImmutableSet
import org.gradle.api.internal.tasks.testing.TestFramework
import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.api.internal.tasks.testing.testng.TestNGTestFramework
import org.gradle.api.tasks.testing.Test


val Test.moduleNameCommaDelimitedString
get() = testFramework.moduleNameCommaDelimitedString

val TestFramework.moduleNameCommaDelimitedString
get() = moduleNameIset.firstOrNull()


val Test.moduleNameIset
get() = testFramework.moduleNameIset

val TestFramework.moduleNameIset
get() =
    when (this) {
        is         JUnitTestFramework -> moduleNameIset
        is JUnitPlatformTestFramework -> moduleNameIset
        is        TestNGTestFramework -> moduleNameIset
        else                          -> ImmutableSet.of()
    }

val JUnitTestFramework.moduleNameIset
get() = JU4

val JUnitPlatformTestFramework.moduleNameIset
get() = JU5

val TestNGTestFramework.moduleNameIset
get() = TNG

private val JU4: ImmutableSet<String> = ImmutableSet.of("junit")
private val JU5: ImmutableSet<String> = ImmutableSet.of("org.junit.jupiter.api")
private val TNG: ImmutableSet<String> = ImmutableSet.of("testng")
