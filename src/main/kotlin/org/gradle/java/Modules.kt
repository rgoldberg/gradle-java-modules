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
package org.gradle.java;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableCollection;
import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.lang.System.lineSeparator;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newDirectoryStream;

public class Modules {

    private static final String LS          = lineSeparator();
    private static final Joiner LINE_JOINER = Joiner.on(LS);


    @Deprecated
    private Modules() {
        throw new AssertionError("Should never execute");
    }


    public static void splitIntoModulePathAndPatchModule(
        final Set<File>                    classpathFileSet,
        final ImmutableCollection<String>  moduleNameIcoll,
        final Consumer<? super List<File>> modulePathConsumer,
        final Consumer<? super List<File>> patchModuleConsumer
    ) {
        // determine which classpath elements will be in --module-path, and which in --patch-module
        final int classpathFileCount = classpathFileSet.size();

        final List<File>  modulePathFileList = new ArrayList<>(classpathFileCount);
        final List<File> patchModuleFileList = new ArrayList<>(classpathFileCount);

        for (final File classpathFile : classpathFileSet) {
            if (containsModules(classpathFile.toPath())) {
                // directories that contain a module-info.class or at least one *.jar file; files (e.g., jars); nonexistent paths
                modulePathFileList.add(classpathFile);
            }
            else {
                // directories that don't contain module-info.class or *.jar files
                patchModuleFileList.add(classpathFile);
            }
        }

        // add module arguments
        if (! modulePathFileList.isEmpty()) {
            modulePathConsumer.accept(modulePathFileList);
        }

        if (
            ! patchModuleFileList.isEmpty() &&
            !     moduleNameIcoll.isEmpty()
        ) {
            if (moduleNameIcoll.size() > 1) {
                throw new GradleException(
                    "Cannot determine into which of the multiple modules to patch the non-module directories."                             + LS + LS
                    + "To avoid this problem, either only have one module per source set, or modularize the currently non-modular source." + LS + LS
                    + "Modules:"                                                    + LS + LS + LINE_JOINER.join(moduleNameIcoll)          + LS + LS
                    + "Directories containing non-modular source and/or resources:" + LS + LS + LINE_JOINER.join(patchModuleFileList)
                );
            }

            patchModuleConsumer.accept(patchModuleFileList);
        }
    }

    // directories that contain a module-info.class or at least one *.jar file; files (e.g., jars); nonexistent paths
    private static boolean containsModules(final Path dirPath) {
        if (! isDirectory(dirPath)) {
            return true;
        }

        try (DirectoryStream<Path> ds = newDirectoryStream(dirPath, "{module-info.class,*.jar}")) {
            return ds.iterator().hasNext();
        }
        catch (final IOException ex) {
            throw new GradleException("Could not determine if directory contains modules: " + dirPath, ex);
        }
    }
}
