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
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.CoreJavadocOptions
import org.gradle.java.jdk.JAVADOC


interface JavadocOptions: SourceJavaOptions, AutoGenerateSettableCascading {

    fun expandRequiresDefault()
    fun expandRequiresTransitive()
    fun expandRequiresAll()

    fun showModuleContentsApi()
    fun showModuleContentsAll()

    fun showPackagesExported()
    fun showPackagesAll()

    fun showTypesPublic()
    fun showTypesProtected()
    fun showTypesPackage()
    fun showTypesPrivate()

    fun showMembersPublic()
    fun showMembersProtected()
    fun showMembersPackage()
    fun showMembersPrivate()
}


open class JavadocOptionsInternal(
                 autoGenerateParent:  AutoGenerateGettable,
                separateValueParent: SeparateValueGettable,
    private val             javadoc: Javadoc
):
JavadocOptions,
AutoGeneratableCascading by DefaultAutoGeneratableCascading( autoGenerateParent),
 SeparableValueCascading by  DefaultSeparableValueCascading(separateValueParent),
SourceJavaOptionsInternal() {

    override fun getPublicType() =
        PUBLIC_TYPE


    override val args =
        object: ArgAppendable {
            override fun append(arg: String) {
                (javadoc.options as CoreJavadocOptions).addBooleanOption(arg, true)
            }

            override fun append(flag: String, value: String) {
                (javadoc.options as CoreJavadocOptions).addStringOption(flag, value)
            }
        }

    override fun config() {
        super.config()

        args
        .append(expandRequires)
        .append(showMembers)
        .append(showModuleContents)
        .append(showPackages)
        .append(showTypes)
    }


    private val expandRequires by lazy {
        DefaultSeparableValueScalarOptionInternal<String?>(JAVADOC.OPTION_EXPAND_REQUIRES, "=", null, this)
    }
    override fun expandRequiresDefault() {
        expandRequires(null)
    }
    override fun expandRequiresTransitive() {
        expandRequires(JAVADOC.TRANSITIVE)
    }
    override fun expandRequiresAll() {
        expandRequires(JAVADOC.ALL)
    }

    private val showModuleContents by lazy {
        DefaultSeparableValueScalarOptionInternal<String?>(JAVADOC.OPTION_SHOW_MODULE_CONTENTS, "=", null, this)
    }
    override fun showModuleContentsApi() {
        showModuleContents(null)
    }
    override fun showModuleContentsAll() {
        showModuleContents(JAVADOC.ALL)
    }

    private val showPackages by lazy {
        DefaultSeparableValueScalarOptionInternal<String?>(JAVADOC.OPTION_SHOW_PACKAGES, "=", null, this)
    }
    override fun showPackagesExported() {
        showPackages(null)
    }
    override fun showPackagesAll() {
        showPackages(JAVADOC.ALL)
    }

    private val showTypes by lazy {
        DefaultSeparableValueScalarOptionInternal<String?>(JAVADOC.OPTION_SHOW_TYPES, "=", null, this)
    }
    override fun showTypesPublic() {
        showTypes(JAVADOC.PUBLIC)
    }
    override fun showTypesProtected() {
        showTypes(null)
    }
    override fun showTypesPackage() {
        showTypes(JAVADOC.PACKAGE)
    }
    override fun showTypesPrivate() {
        showTypes(JAVADOC.PRIVATE)
    }

    private val showMembers by lazy {
        DefaultSeparableValueScalarOptionInternal<String?>(JAVADOC.OPTION_SHOW_MEMBERS, "=", null, this)
    }
    override fun showMembersPublic() {
        showMembers(JAVADOC.PUBLIC)
    }
    override fun showMembersProtected() {
        showMembers(null)
    }
    override fun showMembersPackage() {
        showMembers(JAVADOC.PACKAGE)
    }
    override fun showMembersPrivate() {
        showMembers(JAVADOC.PRIVATE)
    }


    companion object {
        private val PUBLIC_TYPE = typeOf(JavadocOptions::class.java)
    }
}
