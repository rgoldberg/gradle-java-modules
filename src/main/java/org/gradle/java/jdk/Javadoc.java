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
package org.gradle.java.jdk;

public interface Javadoc extends JavaSourceTool {

    String OPTION_EXPAND_REQUIRES      = "--expand-requires";
    String OPTION_SHOW_MEMBERS         = "--show-members";
    String OPTION_SHOW_MODULE_CONTENTS = "--show-module-contents";
    String OPTION_SHOW_PACKAGES        = "--show-packages";
    String OPTION_SHOW_TYPES           = "--show-types";

    // OPTION_EXPAND_REQUIRES values
    String TRANSITIVE = "transitive";

    // OPTION_EXPAND_REQUIRES, OPTION_SHOW_MODULE_CONTENTS & OPTION_SHOW_PACKAGES values
    String ALL = "all";

    // OPTION_SHOW_MEMBERS & OPTION_SHOW_TYPES values
    String PUBLIC  = "public";
    String PACKAGE = "package";
    String PRIVATE = "private";
}
