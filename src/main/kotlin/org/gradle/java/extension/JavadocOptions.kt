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


import org.gradle.api.JavaVersion
import org.gradle.api.reflect.TypeOf.typeOf
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.CoreJavadocOptions
import org.gradle.java.JigsawPlugin
import org.gradle.java.jdk.JAVADOC


interface JavadocOptions: SourceJavaOptions, AutoGenerateSettableCascading {

    override val modulePath: AutoGeneratableSeparableValueVarargOption<String>

    override val patchModule: AutoGeneratableSeparableValueKeyedVarargOption<String, String>

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
        object:
        DefaultSeparableValueScalarOptionInternal<String?>(JAVADOC.OPTION_EXPAND_REQUIRES, "=", null, this) {
            override val flag
            get() = super.flag.substring(1)
        }
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
        object:
        DefaultSeparableValueScalarOptionInternal<String?>(JAVADOC.OPTION_SHOW_MODULE_CONTENTS, "=", null, this) {
            override val flag
            get() = super.flag.substring(1)
        }
    }
    override fun showModuleContentsApi() {
        showModuleContents(null)
    }
    override fun showModuleContentsAll() {
        showModuleContents(JAVADOC.ALL)
    }

    private val showPackages by lazy {
        object:
        DefaultSeparableValueScalarOptionInternal<String?>(JAVADOC.OPTION_SHOW_PACKAGES, "=", null, this) {
            override val flag
            get() = super.flag.substring(1)
        }
    }
    override fun showPackagesExported() {
        showPackages(null)
    }
    override fun showPackagesAll() {
        showPackages(JAVADOC.ALL)
    }

    private val showTypes by lazy {
        object:
        DefaultSeparableValueScalarOptionInternal<String?>(JAVADOC.OPTION_SHOW_TYPES, "=", null, this) {
            override val flag
            get() = super.flag.substring(1)
        }
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
        object:
        DefaultSeparableValueScalarOptionInternal<String?>(JAVADOC.OPTION_SHOW_MEMBERS, "=", null, this) {
            override val flag
            get() = super.flag.substring(1)
        }
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


    override val source: ScalarOptionInternal<JavaVersion?> =
        object:
        Source() {
            override val flag
            get() = super.flag.substring(1)
        }

    override val release: SeparableValueScalarOptionInternal<JavaVersion?> =
        object:
        Release() {
            override val flag
            get() = super.flag.substring(1)
        }

    override val system: SeparableValueScalarOptionInternal<String?> =
        object:
        System() {
            override val flag
            get() = super.flag.substring(1)
        }

    override val module: SeparableValueSetOptionInternal<String> =
        object:
        Module() {
            override val flag
            get() = super.flag.substring(1)
        }

    override val moduleSourcePath: SeparableValueSetOptionInternal<String> =
        object:
        ModuleSourcePath() {
            override val flag
            get() = super.flag.substring(1)
        }


    override val addModules: SeparableValueSetOptionInternal<String> =
        object:
        AddModules() {
            override val flag
            get() = super.flag.substring(1)
        }

    override val limitModules: SeparableValueSetOptionInternal<String> =
        object:
        LimitModules() {
            override val flag
            get() = super.flag.substring(1)
        }

    override val modulePath: AutoGeneratableSeparableValueSetOptionInternal<String> by lazy {
        object:
        AutoGeneratableSeparableValueSetOptionInternal<String>,
        ModulePath(),
        AutoGeneratableCascading by DefaultAutoGeneratableCascading(this) {
            override val flag
            get() = super.flag.substring(1)

            override val value: Set<String>
            get() =
                super.value.appendAutoGeneratedIterator(autoGenerate.isEnabled) {
                    val classpath = javadoc.classpath

                    autoGenerateModulePath(classpath, {javadoc.classpath = javadoc.project.files()}, {javadoc.classpath = classpath})
                }
        }
    }

    override val upgradeModulePath: SeparableValueSetOptionInternal<String> =
        object:
        UpgradeModulePath() {
            override val flag
            get() = super.flag.substring(1)
        }

    override val patchModule: AutoGeneratableSeparableValueLinkedHashMultimapOptionInternal<String, String> by lazy {
        object:
        AutoGeneratableSeparableValueLinkedHashMultimapOptionInternal<String, String>,
        PatchModule(),
        AutoGeneratableCascading by DefaultAutoGeneratableCascading(this) {
            override val flag
            get() = super.flag.substring(1)

            override val value: Map<String, Set<String>>
            get() =
                valueMutable.appendAutoGeneratedMapFromPair(autoGenerate.isEnabled) {
                    autoGeneratePatchModule(javadoc.project.plugins.getPlugin(JigsawPlugin::class.java).moduleNameIset, javadoc.classpath)
                }
        }
    }

    override val addReads: SeparableValueLinkedHashMultimapOptionInternal<String, String> =
        object:
        AddReads() {
            override val flag
            get() = super.flag.substring(1)
        }

    override val addExports: SeparableValueLinkedHashMultimapOptionInternal<String, String> =
        object:
        AddExports() {
            override val flag
            get() = super.flag.substring(1)
        }


    companion object {
        private val PUBLIC_TYPE = typeOf(JavadocOptions::class.java)
    }
}
