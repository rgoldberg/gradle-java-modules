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


import org.gradle.api.reflect.TypeOf.typeOf
import org.gradle.api.tasks.JavaExec


interface JavaExecOptions: RuntimeJavaOptions, AutoGenerateSettableCascading


open class JavaExecOptionsInternal(
                 autoGenerateParent:  AutoGenerateGettable,
                separateValueParent: SeparateValueGettable,
    private val            javaExec: JavaExec
):
JavaExecOptions,
AutoGeneratableCascading by DefaultAutoGeneratableCascading( autoGenerateParent),
 SeparableValueCascading by  DefaultSeparableValueCascading(separateValueParent),
RuntimeJavaOptionsInternal() {

    override fun getPublicType() =
        PUBLIC_TYPE


    override val args = JavaForkOptionsArgAppendable(javaExec)


    companion object {
        private val PUBLIC_TYPE = typeOf(JavaExecOptions::class.java)
    }
}
