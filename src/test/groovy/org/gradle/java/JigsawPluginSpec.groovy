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
package org.gradle.java

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Requires
import spock.lang.Specification

import static org.gradle.api.JavaVersion.VERSION_1_9
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.util.TextUtil.getWindowsLineSeparator

class JigsawPluginSpec extends Specification {

    private static final SUPPORTS_MODULES = JavaVersion.current() >= VERSION_1_9


    @Rule
    final TemporaryFolder tmpDir = new TemporaryFolder()


    def setup() {
        tmpDir.newFile('build.gradle.kts') << '''\
plugins {
  application
  id("org.gradle.java.experimental-jigsaw") version "0.1.1"
}

repositories.jcenter()

dependencies {
  testImplementation("junit", "junit", "4.12")
}

application.mainClassName = "test.module/com.example.AClass"
'''

        tmpDir.newFile('settings.gradle.kts') << '''\
rootProject.name = "modular"
'''

        tmpDir.newFolder('src', 'main', 'java', 'com', 'example')
        tmpDir.newFolder('src', 'test', 'java', 'com', 'example')

        tmpDir.newFile('src/main/java/module-info.java') << '''\
module test.module {
  exports com.example;
}
'''

        tmpDir.newFile('src/main/java/com/example/AClass.java') << '''\
package com.example;

public class AClass {

  public void aMethod(String aString) {
    System.out.println(aString);
  }

  public static void main(String... args) {
    new AClass().aMethod("Hello World!");
  }
}
'''

        tmpDir.newFile('src/test/java/com/example/AClassTest.java') << '''\
package com.example;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AClassTest {

  @Test
  public void isAnInstanceOfAClass() {
    assertTrue(new AImplementation() instanceof AClass);
  }

  static class AImplementation extends AClass {
    @Override
    public void aMethod(String aString) {
      // Do nothing
    }
  }
}
'''
    }

    private BuildResult build(final String taskName) {
        GradleRunner.create()
        .withProjectDir(tmpDir.root)
        .withArguments('--debug', '--stacktrace', '--warning-mode', 'all', taskName)
        .withPluginClasspath()
        .build()
    }

    @Requires({SUPPORTS_MODULES})
    def 'can assemble a module'() {
        when:
        final BuildResult result = build('assemble')

        then:
        result.task(':compileJava').outcome == SUCCESS
        result.task(':jar').outcome == SUCCESS
        new File(tmpDir.root, 'build/libs/modular.jar').exists()
        new File(tmpDir.root, 'build/classes/java/main/module-info.class').exists()
        new File(tmpDir.root, 'build/classes/java/main/com/example/AClass.class').exists()
    }

    @Requires({SUPPORTS_MODULES})
    def 'can check a module'() {
        when:
        final BuildResult result = build('check')

        then:
        result.task(':test').outcome == SUCCESS
    }

    @Requires({SUPPORTS_MODULES})
    def 'can run with a module'() {
        when:
        final BuildResult result = build('run')

        then:
        result.output.contains('Hello World!')
    }

    @Requires({SUPPORTS_MODULES})
    def 'can create start scripts with a module'() {
        when:
        build('startScripts')

        then:
        new File(tmpDir.root, 'build/scripts/modular').text == '''\
#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  modular start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \\(.*\\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \\"$PRG\\"`/.." >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="modular"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and MODULAR_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"--module-path" "$APP_HOME/lib" "--module" "test.module/com.example.AClass"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

CLASSPATH=

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" -a "$darwin" = "false" -a "$nonstop" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \\"-Xdock:name=$APP_NAME\\" \\"-Xdock:icon=$APP_HOME/media/gradle.icns\\""
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = "true" -o "$msys" = "true" ] ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
    JAVACMD=`cygpath --unix "$JAVACMD"`

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 1 -mindepth 1 -type d 2>/dev/null`
    SEP=""
    for dir in $ROOTDIRSRAW ; do
        ROOTDIRS="$ROOTDIRS$SEP$dir"
        SEP="|"
    done
    OURCYGPATTERN="(^($ROOTDIRS))"
    # Add a user-defined pattern to the cygpath arguments
    if [ "$GRADLE_CYGPATTERN" != "" ] ; then
        OURCYGPATTERN="$OURCYGPATTERN|($GRADLE_CYGPATTERN)"
    fi
    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    i=0
    for arg in "$@" ; do
        CHECK=`echo "$arg"|egrep -c "$OURCYGPATTERN" -`
        CHECK2=`echo "$arg"|egrep -c "^-"`                                 ### Determine if an option

        if [ $CHECK -ne 0 ] && [ $CHECK2 -eq 0 ] ; then                    ### Added a condition
            eval `echo args$i`=`cygpath --path --ignore --mixed "$arg"`
        else
            eval `echo args$i`="\\"$arg\\""
        fi
        i=`expr $i + 1`
    done
    case $i in
        0) set -- ;;
        1) set -- "$args0" ;;
        2) set -- "$args0" "$args1" ;;
        3) set -- "$args0" "$args1" "$args2" ;;
        4) set -- "$args0" "$args1" "$args2" "$args3" ;;
        5) set -- "$args0" "$args1" "$args2" "$args3" "$args4" ;;
        6) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" ;;
        7) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" ;;
        8) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" ;;
        9) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" "$args8" ;;
    esac
fi

# Escape application args
save () {
    for i do printf %s\\\\n "$i" | sed "s/'/'\\\\\\\\''/g;1s/^/'/;\\$s/\\$/' \\\\\\\\/" ; done
    echo " "
}
APP_ARGS=`save "$@"`

# Collect all arguments for the java command, following the shell quoting and substitution rules
eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $MODULAR_OPTS -classpath "\\"$CLASSPATH\\""  "$APP_ARGS"

exec "$JAVACMD" "$@"
'''
        new File(tmpDir.root, 'build/scripts/modular.bat').text == '''\
@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  modular startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and MODULAR_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="--module-path" "%APP_HOME%\\lib" "--module" "test.module/com.example.AClass"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=

@rem Execute modular
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %MODULAR_OPTS%  -classpath "%CLASSPATH%"  %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable MODULAR_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%MODULAR_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
'''
.replace('\n', windowsLineSeparator)
    }
}
