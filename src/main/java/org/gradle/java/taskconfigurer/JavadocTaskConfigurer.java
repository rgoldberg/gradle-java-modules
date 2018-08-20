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

import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.java.JigsawPlugin;

import static org.gradle.java.GradleUtils.doAfterAllOtherDoFirstActions;
import static org.gradle.java.GradleUtils.doBeforeAllOtherDoLastActions;
import static org.gradle.java.jdk.Javadoc.OPTION_MODULE_PATH;

public class JavadocTaskConfigurer implements TaskConfigurer<Javadoc> {

    private static final String JAVADOC_TASK_OPTION_MODULE_PATH = OPTION_MODULE_PATH.substring(1);


    public JavadocTaskConfigurer() {}


    @Override
    public Class<Javadoc> getTaskClass() {
        return Javadoc.class;
    }

    @Override
    public void configureTask(final Javadoc javadoc, final JigsawPlugin jigsawPlugin) {
        jigsawPlugin.setModuleNamesInputProperty(javadoc);

        final FileCollection[] classpathHolder = new FileCollection[1];

        doAfterAllOtherDoFirstActions(javadoc, task -> {
            final FileCollection classpath = javadoc.getClasspath();

            classpathHolder[0] = classpath;

            if (! classpath.isEmpty()) {
                ((CoreJavadocOptions) javadoc.getOptions()).addStringOption(JAVADOC_TASK_OPTION_MODULE_PATH, classpath.getAsPath());

                javadoc.setClasspath(javadoc.getProject().files());
            }
        });

        doBeforeAllOtherDoLastActions(javadoc, task -> javadoc.setClasspath(classpathHolder[0]));
    }
}
