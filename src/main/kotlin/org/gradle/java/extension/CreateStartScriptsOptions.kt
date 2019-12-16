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


import java.io.File
import java.io.IOException
import org.gradle.api.GradleException
import org.gradle.api.reflect.TypeOf.typeOf
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.util.TextUtil.getUnixLineSeparator
import org.gradle.util.TextUtil.getWindowsLineSeparator


interface CreateStartScriptsOptions: RuntimeJavaOptions, AutoGenerateSettableCascading {

    override val module:     AutoGeneratableSeparableValueScalarOption<String?>
    override val modulePath: AutoGeneratableSeparableValueVarargOption<String>
}


open class CreateStartScriptsOptionsInternal(
                 autoGenerateParent:  AutoGenerateGettable,
                separateValueParent: SeparateValueGettable,
    private val  createStartScripts: CreateStartScripts
):
CreateStartScriptsOptions,
AutoGeneratableCascading by DefaultAutoGeneratableCascading( autoGenerateParent),
 SeparableValueCascading by  DefaultSeparableValueCascading(separateValueParent),
RuntimeJavaOptionsInternal() {

    override fun getPublicType() =
        PUBLIC_TYPE


    override val args = {
        val argMlist = mutableListOf<String>()

        createStartScripts.defaultJvmOpts?.let {
            argMlist += it
        }

        createStartScripts.defaultJvmOpts = argMlist

        ListArgAppendable(argMlist)
    }()


    override val module: AutoGeneratableSeparableValueScalarOptionInternal<String?> by lazy {
        object:
        AutoGeneratableSeparableValueScalarOptionInternal<String?>,
        Module(),
        AutoGeneratableCascading by DefaultAutoGeneratableCascading(this) {
            override var value: String?
            get() {
                if (autoGenerate.isEnabled) {
                    val mainClassName = createStartScripts.mainClassName

                    configureMlist += {createStartScripts.mainClassName = ""}
                        resetMlist += {createStartScripts.mainClassName = mainClassName}

                    if (super.value == null) {
                        return mainClassName
                    }
                }

                return super.value
            }
            set(module) {
                super.value = module
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
                super.value.appendAutoGeneratedElement(autoGenerate.isEnabled) {
                    val classpath = createStartScripts.classpath

                    configureMlist += {createStartScripts.classpath = createStartScripts.project.files()}
                        resetMlist += {
                            replaceLibDirectoryPlaceholder(createStartScripts.unixScript,    "\\\$APP_HOME/lib",  getUnixLineSeparator())
                            replaceLibDirectoryPlaceholder(createStartScripts.windowsScript, "%APP_HOME%\\\\lib", getWindowsLineSeparator())

                            createStartScripts.classpath = classpath
                        }


                    LIB_DIR_PLACEHOLDER
                }

            private fun replaceLibDirectoryPlaceholder(file: File, libDirReplacement: String, lineSeparator: String) =
                try {
                    file.readLines().stream().map {line -> LIB_DIR_PLACEHOLDER_REGEX.replace(line, libDirReplacement)}.use {lineStream ->
                        file.bufferedWriter().use {
                            Iterable(lineStream::iterator).joinTo(it, lineSeparator, "", lineSeparator)
                        }
                    }
                }
                catch (ex: IOException) {
                    throw GradleException("Couldn't replace placeholder in " + file, ex)
                }
        }
    }


    companion object {
        private val PUBLIC_TYPE = typeOf(CreateStartScriptsOptions::class.java)

        private const val LIB_DIR_PLACEHOLDER       = "LIB_DIR_PLACEHOLDER"
        private       val LIB_DIR_PLACEHOLDER_REGEX = LIB_DIR_PLACEHOLDER.toRegex()
    }
}
