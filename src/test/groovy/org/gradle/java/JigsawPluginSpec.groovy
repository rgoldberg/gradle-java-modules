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
package org.gradle.java

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Requires
import spock.lang.Specification

import static org.gradle.api.JavaVersion.VERSION_1_9
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class JigsawPluginSpec extends Specification {

    private static final SUPPORTS_MODULES = JavaVersion.current() >= VERSION_1_9


    @Rule
    final TemporaryFolder tmpDir = new TemporaryFolder()


    def setup() {
        tmpDir.newFile('build.gradle.kts') << '''\
plugins {
  application
  id("org.gradle.java.experimental-jigsaw") version "0.1.1"
}

repositories.jcenter()

dependencies {
  testImplementation("junit", "junit", "4.12")
}

javaModule.name = "test.module"

application.mainClassName = "com.example.AClass"
'''

        tmpDir.newFile('settings.gradle.kts') << '''\
rootProject.name = "modular"
'''

        tmpDir.newFolder('src', 'main', 'java', 'com', 'example')
        tmpDir.newFolder('src', 'test', 'java', 'com', 'example')

        tmpDir.newFile('src/main/java/module-info.java') << '''\
module test.module {
  exports com.example;
}
'''

        tmpDir.newFile('src/main/java/com/example/AClass.java') << '''\
package com.example;

public class AClass {

  public void aMethod(String aString) {
    System.out.println(aString);
  }

  public static void main(String... args) {
    new AClass().aMethod("Hello World!");
  }
}
'''

        tmpDir.newFile('src/test/java/com/example/AClassTest.java') << '''\
package com.example;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AClassTest {

  @Test
  public void isAnInstanceOfAClass() {
    assertTrue(new AImplementation() instanceof AClass);
  }

  static class AImplementation extends AClass {
    @Override
    public void aMethod(String aString) {
      // Do nothing
    }
  }
}
'''
    }

    private BuildResult build(final String taskName) {
        GradleRunner.create()
        .withProjectDir(tmpDir.root)
        .withArguments('--debug', '--stacktrace', '--warning-mode', 'all', taskName)
        .withPluginClasspath()
        .build()
    }

    @Requires({SUPPORTS_MODULES})
    def 'can assemble a module'() {
        when:
        final BuildResult result = build('assemble')

        then:
        result.task(':compileJava').outcome == SUCCESS
        result.task(':jar').outcome == SUCCESS
        new File(tmpDir.root, 'build/libs/modular.jar').exists()
        new File(tmpDir.root, 'build/classes/java/main/module-info.class').exists()
        new File(tmpDir.root, 'build/classes/java/main/com/example/AClass.class').exists()
    }

    @Requires({SUPPORTS_MODULES})
    def 'can check a module'() {
        when:
        final BuildResult result = build('check')

        then:
        result.task(':test').outcome == SUCCESS
    }

    @Requires({SUPPORTS_MODULES})
    def 'can run with a module'() {
        when:
        final BuildResult result = build('run')

        then:
        result.output.contains('Hello World!')
    }
}
