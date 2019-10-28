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
package org.gradle.java.util


import java.io.File
import java.io.IOException
import java.lang.System.lineSeparator
import java.nio.file.Files.isDirectory
import java.nio.file.Files.newDirectoryStream
import java.nio.file.Path
import kotlinx.collections.immutable.ImmutableCollection
import org.gradle.api.GradleException


private val LS = lineSeparator()


fun Set<File>.splitIntoModulePathAndPatchModule(
    moduleNameIcoll:     ImmutableCollection<String>,
    modulePathConsumer:  (List<File>) -> Unit,
    patchModuleConsumer: (List<File>) -> Unit
) {
    // determine which classpath elements will be in --module-path, and which in --patch-module
    val classpathFileCount  = size
    val modulePathFileList  = ArrayList<File>(classpathFileCount)
    val patchModuleFileList = ArrayList<File>(classpathFileCount)

    for (classpathFile in this) {
        if (classpathFile.toPath().containsModules) {
            // directories that contain a module-info.class or at least one *.jar file; files (e.g., jars); nonexistent paths
            modulePathFileList += classpathFile
        }
        else {
            // directories that don't contain module-info.class or *.jar files
            patchModuleFileList += classpathFile
        }
    }

    // add module arguments
    if (modulePathFileList.isNotEmpty()) {
        modulePathConsumer(modulePathFileList)
    }

    if (
        patchModuleFileList.isNotEmpty() &&
            moduleNameIcoll.isNotEmpty()
    ) {
        if (moduleNameIcoll.size > 1) {
            throw GradleException(
                "Cannot determine into which of the multiple modules to patch the non-module directories."                             + LS + LS
                + "To avoid this problem, either only have one module per source set, or modularize the currently non-modular source." + LS + LS
                + "Modules:"                                                    + LS + LS + moduleNameIcoll    .joinToString(LS)       + LS + LS
                + "Directories containing non-modular source and/or resources:" + LS + LS + patchModuleFileList.joinToString(LS)
            )
        }

        patchModuleConsumer(patchModuleFileList)
    }
}

// directories that contain a module-info.class or at least one *.jar file; files (e.g., jars); nonexistent paths
val Path.containsModules
get() =
    try {
        ! isDirectory(this)
        || newDirectoryStream(this, "{module-info.class,*.jar}").use {it.iterator().hasNext()}
    }
    catch (ex: IOException) {
        throw GradleException("Could not determine if directory contains modules: " + this, ex)
    }
