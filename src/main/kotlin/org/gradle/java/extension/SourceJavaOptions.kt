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


import java.io.File.pathSeparator
import org.gradle.api.JavaVersion
import org.gradle.api.reflect.TypeOf
import org.gradle.java.jdk.JAVA_SOURCE_TOOL


interface SourceJavaOptions: CommonJavaOptions {

    val release: SeparableValueScalarOption<JavaVersion?>
    val source:  ScalarOption<JavaVersion?>

    val system: SeparableValueScalarOption<String?>
    fun systemDefault()
    fun systemNone()

    val module:           SeparableValueVarargOption<String>
    val moduleSourcePath: SeparableValueVarargOption<String>
}


abstract class SourceJavaOptionsInternal: SourceJavaOptions, CommonJavaOptionsInternal() {

    abstract override fun getPublicType(): TypeOf<out SourceJavaOptions>


    override fun config() {
        super.config()

        args
        .append(release)
        .appendSeparated(source)
        .append(system)
        .append(module)
        .append(moduleSourcePath)
    }


    override val source: ScalarOptionInternal<JavaVersion?> = DefaultScalarOptionInternal(JAVA_SOURCE_TOOL.OPTION_SOURCE, null)

    override val release: SeparableValueScalarOptionInternal<JavaVersion?> by lazy {
        DefaultSeparableValueScalarOptionInternal<JavaVersion?>(JAVA_SOURCE_TOOL.OPTION_RELEASE, "=", null, this)
    }

    override val system: SeparableValueScalarOptionInternal<String?> by lazy {
        DefaultSeparableValueScalarOptionInternal<String?>(JAVA_SOURCE_TOOL.OPTION_SYSTEM, "=", null, this)
    }
    override fun systemDefault() {
        system(null)
    }
    override fun systemNone() {
        system(JAVA_SOURCE_TOOL.NONE)
    }

    override val module: SeparableValueSetOptionInternal<String> by lazy {
        DefaultSeparableValueSetOptionInternal<String>(JAVA_SOURCE_TOOL.OPTION_MODULE, "=", ",", this)
    }

    override val moduleSourcePath: SeparableValueSetOptionInternal<String> by lazy {
        DefaultSeparableValueSetOptionInternal<String>(JAVA_SOURCE_TOOL.OPTION_MODULE_SOURCE_PATH, "=", pathSeparator, this)
    }
    //TODO: collapse moduleSourcePath patterns
}
