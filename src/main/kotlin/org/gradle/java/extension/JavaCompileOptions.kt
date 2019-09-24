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
import org.gradle.api.reflect.TypeOf.typeOf
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.java.jdk.JAVAC


interface JavaCompileOptions: SourceJavaOptions, AutoGenerateSettableCascading {

    val target: ScalarOption<JavaVersion?>

    val moduleVersion:                SeparableValueScalarOption<String?>
    val defaultModuleForCreatedFiles: SeparableValueScalarOption<String?>

    val processorModulePath: SeparableValueVarargOption<String>

    fun preferNewer()
    fun preferSource()
}


open class JavaCompileOptionsInternal(
                 autoGenerateParent:  AutoGenerateGettable,
                separateValueParent: SeparateValueGettable,
    private val         javaCompile: JavaCompile
):
JavaCompileOptions,
AutoGeneratableCascading by DefaultAutoGeneratableCascading( autoGenerateParent),
 SeparableValueCascading by  DefaultSeparableValueCascading(separateValueParent),
SourceJavaOptionsInternal() {

    //TODO?
    // -Xlint:
    //    exports
    //    module
    //    opens
    //    requires-automatic
    //    requires-transitive-automatic

    override fun getPublicType() =
        PUBLIC_TYPE


    override val args = {
        val options = javaCompile.options

        val argMlist = options.compilerArgs.toMutableList()

        options.compilerArgs = argMlist

        ListArgAppendable(argMlist)
    }()

    override fun config() {
        super.config()

        args
        .appendSeparated(target)
        .append(moduleVersion)
        .append(defaultModuleForCreatedFiles)
        .appendJoined(prefer)
        .append(processorModulePath)
    }


    override val target = DefaultScalarOptionInternal<JavaVersion?>(JAVAC.OPTION_TARGET, null)

    override val moduleVersion by lazy {
        DefaultSeparableValueScalarOptionInternal<String?>(JAVAC.OPTION_MODULE_VERSION,                   "=", null, this)
    }

    override val defaultModuleForCreatedFiles by lazy {
        DefaultSeparableValueScalarOptionInternal<String?>(JAVAC.OPTION_DEFAULT_MODULE_FOR_CREATED_FILES, "=", null, this)
    }

    override val processorModulePath by lazy {
        DefaultSeparableValueSetOptionInternal<String>(JAVAC.OPTION_PROCESSOR_MODULE_PATH, "=", pathSeparator, this)
    }

    private val prefer = DefaultScalarOptionInternal<String?>(JAVAC.OPTION_PREFER, null)
    override fun preferNewer() {
        prefer(null)
    }
    override fun preferSource() {
        prefer(JAVAC.SOURCE)
    }


    companion object {
        private val PUBLIC_TYPE = typeOf(JavaCompileOptions::class.java)
    }
}
