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
package org.gradle.java.taskconfigurer;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.application.CreateStartScripts;
import org.gradle.java.JigsawPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.addAll;
import static com.google.common.io.MoreFiles.asCharSink;
import static org.gradle.java.GradleUtils.doAfterAllOtherDoFirstActions;
import static org.gradle.java.GradleUtils.setModuleNamesInputProperty;
import static org.gradle.java.jdk.Java.OPTION_MODULE;
import static org.gradle.java.jdk.Java.OPTION_MODULE_PATH;
import static org.gradle.util.TextUtil.getUnixLineSeparator;
import static org.gradle.util.TextUtil.getWindowsLineSeparator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllLines;
import static java.util.regex.Pattern.compile;

public class CreateStartScriptsTaskConfigurer implements TaskConfigurer<CreateStartScripts> {

    private static final String  LIB_DIR_PLACEHOLDER         = "LIB_DIR_PLACEHOLDER";
    private static final Pattern LIB_DIR_PLACEHOLDER_PATTERN = compile(LIB_DIR_PLACEHOLDER);


    public CreateStartScriptsTaskConfigurer() {}


    @Override
    public Class<CreateStartScripts> getTaskClass() {
        return CreateStartScripts.class;
    }

    @Override
    public void configureTask(final CreateStartScripts createStartScripts, final JigsawPlugin jigsawPlugin) {
        final String main       = createStartScripts.getMainClassName();
        final String moduleName = jigsawPlugin.getModuleName(main);

        if (moduleName != null) {
            setModuleNamesInputProperty(createStartScripts, moduleName);

            doAfterAllOtherDoFirstActions(createStartScripts, task -> {
                final List<String> args = new ArrayList<>();

                addAll(args, createStartScripts.getDefaultJvmOpts());

                args.add(OPTION_MODULE_PATH);
                args.add(LIB_DIR_PLACEHOLDER);

                args.add(OPTION_MODULE);
                args.add(main);

                createStartScripts.setDefaultJvmOpts(args);
                createStartScripts.setMainClassName("");
                createStartScripts.setClasspath(createStartScripts.getProject().files());
            });

            createStartScripts.doLast(task -> {
                replaceLibDirectoryPlaceholder(createStartScripts.getUnixScript()   .toPath(), "\\$APP_HOME/lib",   getUnixLineSeparator());
                replaceLibDirectoryPlaceholder(createStartScripts.getWindowsScript().toPath(), "%APP_HOME%\\\\lib", getWindowsLineSeparator());
            });
        }
    }

    private void replaceLibDirectoryPlaceholder(final Path path, final String libDirReplacement, final String lineSeparator) {
        try {
            try (Stream<String> lineStream = readAllLines(path).stream().map(line -> LIB_DIR_PLACEHOLDER_PATTERN.matcher(line).replaceAll(libDirReplacement))) {
                asCharSink(path, UTF_8).writeLines(lineStream, lineSeparator);
            }
        }
        catch (final IOException ex) {
            throw new GradleException("Couldn't replace placeholder in " + path, ex);
        }
    }
}
