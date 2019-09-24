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
import org.gradle.api.reflect.TypeOf.typeOf
import org.gradle.api.tasks.bundling.Jar
import org.gradle.java.jdk.JAR


interface JarOptions: Options, SeparateValueSettableCascading {

    val describeModule: ToggleOption

    val hashModules:   SeparableValueScalarOption<String?> //TODO? <Regex> <Pattern>
    val moduleVersion: SeparableValueScalarOption<String?>

    val modulePath: SeparableValueVarargOption<String>
}


open class JarOptionsInternal(
                separateValueParent: SeparateValueGettable,
    private val                 jar: Jar
):
JarOptions,
OptionsInternal(),
SeparableValueCascading by DefaultSeparableValueCascading(separateValueParent) {

    override fun getPublicType() =
        PUBLIC_TYPE


    override val args: ArgAppendable
        get() = TODO("not implemented")

    override fun config() {
        args
        .append(describeModule)
        .append(hashModules)
        .append(moduleVersion)
        .append(modulePath)
    }


    override val describeModule = DefaultToggleOptionInternal(JAR.OPTION_DESCRIBE_MODULE)

    override val hashModules:   SeparableValueScalarOptionInternal<String?> by lazy {
        DefaultSeparableValueScalarOptionInternal<String?>(JAR.OPTION_HASH_MODULES,   "=", null, this)
    }

    override val moduleVersion: SeparableValueScalarOptionInternal<String?> by lazy {
        DefaultSeparableValueScalarOptionInternal<String?>(JAR.OPTION_MODULE_VERSION, "=", null, this)
    }

    override val modulePath: SeparableValueSetOptionInternal<String> by lazy {
        DefaultSeparableValueSetOptionInternal<String>(JAR.OPTION_MODULE_PATH, "=", pathSeparator, this)
    }


    companion object {
        private val PUBLIC_TYPE = typeOf(JarOptions::class.java)
    }
}
