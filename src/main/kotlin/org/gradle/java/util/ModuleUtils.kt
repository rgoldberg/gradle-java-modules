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


import java.io.IOException
import java.nio.file.Files.isDirectory
import java.nio.file.Files.newDirectoryStream
import java.nio.file.Path
import org.gradle.api.GradleException


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
