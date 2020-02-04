# gradle-java-modules

A place for experimenting with Java 9's module system.

Gradle doesn't currently support building Java 9 modules in a first-class way. For now, this repository acts as a place
for us to develop a shared knowledge of what we need to do to bring that first-class support to a reality in the future.

## The Experimental Jigsaw Plugin
Please feel free to use the
[`rgoldberg.experimental-jigsaw` Plugin](https://plugins.gradle.org/plugin/rgoldberg.experimental-jigsaw)
developed in this repository, but understand that it is not very sophisticated or particularly well-tested at
this point in its development.

If you already have a working Java project, applying the plugin is just one step:

 1. Apply the plugin:
    ```groovy
    plugins {
      id 'rgoldberg.experimental-jigsaw' version '0.5-SNAPSHOT'
    }
    ```
