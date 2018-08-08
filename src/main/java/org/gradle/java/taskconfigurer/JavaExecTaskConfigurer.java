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

import com.google.common.collect.ImmutableSet;
import org.gradle.api.tasks.JavaExec;
import org.gradle.java.JigsawPlugin;

import java.util.ArrayList;
import java.util.List;

import static org.gradle.java.GradleUtils.doAfterAllOtherDoFirstActions;
import static org.gradle.java.GradleUtils.setModuleNamesInputProperty;
import static org.gradle.java.jdk.Java.OPTION_MODULE;
import static org.gradle.java.jdk.JavaCommonTool.addModuleArguments;

public class JavaExecTaskConfigurer implements TaskConfigurer<JavaExec> {

    public JavaExecTaskConfigurer() {}


    @Override
    public Class<JavaExec> getTaskClass() {
        return JavaExec.class;
    }

    @Override
    public void configureTask(final JavaExec javaExec, final JigsawPlugin jigsawPlugin) {
        final String main       = javaExec.getMain();
        final String moduleName = jigsawPlugin.getModuleName(main);

        if (moduleName != null) {
            setModuleNamesInputProperty(javaExec, moduleName);

            doAfterAllOtherDoFirstActions(javaExec, task -> {
                final List<String> args = new ArrayList<>();

                addModuleArguments(args, ImmutableSet.of(moduleName), javaExec.getClasspath().getFiles());

                args.add(OPTION_MODULE);
                args.add(main);

                javaExec.jvmArgs(args);
                javaExec.setMain("");
                javaExec.setClasspath(javaExec.getProject().files());
            });
        }
    }
}
