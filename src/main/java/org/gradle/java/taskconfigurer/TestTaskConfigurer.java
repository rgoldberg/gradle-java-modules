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

import com.google.common.collect.ImmutableSortedSet;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import org.gradle.java.JigsawPlugin;

import java.util.ArrayList;
import java.util.List;

import static org.gradle.java.GradleUtils.doAfterAllOtherDoFirstActions;
import static org.gradle.java.jdk.Java.ALL_MODULE_PATH;
import static org.gradle.java.jdk.Java.OPTION_ADD_MODULES;
import static org.gradle.java.jdk.Java.OPTION_ADD_READS;
import static org.gradle.java.jdk.JavaCommonTool.addModuleArguments;
import static org.gradle.java.testing.StandardTestFrameworkModuleInfo.getTestModuleNameCommaDelimitedString;

public class TestTaskConfigurer implements TaskConfigurer<Test> {

    public TestTaskConfigurer() {}


    @Override
    public Class<Test> getTaskClass() {
        return Test.class;
    }

    @Override
    public void configureTask(final Test test, final JigsawPlugin jigsawPlugin) {
        jigsawPlugin.setModuleNamesInputProperty(test);

        doAfterAllOtherDoFirstActions(test, task -> {
            final Project project = test.getProject();

            final List<String> args = new ArrayList<>();

            final ImmutableSortedSet<String> moduleNameIsset = jigsawPlugin.getModuleNameIsset();

            addModuleArguments(args, moduleNameIsset, test.getClasspath().getFiles());

            args.add(OPTION_ADD_MODULES);
            args.add(ALL_MODULE_PATH);

            final String testModuleNameCommaDelimitedString = getTestModuleNameCommaDelimitedString(test);

            if (! testModuleNameCommaDelimitedString.isEmpty()) {
                moduleNameIsset.forEach(moduleName -> {
                    args.add(OPTION_ADD_READS);
                    args.add(moduleName + '=' + testModuleNameCommaDelimitedString);
                });
            }

            test.jvmArgs(args);

            test.setClasspath(project.files());
        });
    }
}
