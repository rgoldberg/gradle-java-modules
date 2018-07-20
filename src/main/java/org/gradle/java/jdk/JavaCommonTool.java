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

public interface JavaCommonTool {

    String OPTION_ADD_EXPORTS         = "--add-exports";
    String OPTION_ADD_MODULES         = "--add-modules";
    String OPTION_ADD_READS           = "--add-reads";
    String OPTION_CLASS_PATH          = "--class-path";
    String OPTION_LIMIT_MODULES       = "--limit-modules";
    String OPTION_MODULE              = "--module";
    String OPTION_MODULE_PATH         = "--module-path";
    String OPTION_PATCH_MODULE        = "--patch-module";
    String OPTION_UPGRADE_MODULE_PATH = "--upgrade-module-path";

    String ALL_MODULE_PATH = "ALL-MODULE-PATH";
    String ALL_SYSTEM      = "ALL-SYSTEM";

    String ALL_UNNAMED = "ALL-UNNAMED";
}
