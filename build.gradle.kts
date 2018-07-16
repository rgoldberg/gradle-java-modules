import org.gradle.api.JavaVersion.VERSION_1_7

plugins {
    groovy
    idea
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.10.1"
}

group = "org.gradle.java"

version = "0.1.1"

java {
    targetCompatibility = VERSION_1_7
    sourceCompatibility = VERSION_1_7
}

// Fix a bad interaction with IntelliJ and Gradle > 4.0
idea.module.inheritOutputDirs = true

// make the publishing plugin skip checks that disallow publishing to com.gradle / org.gradle groups
System.setProperty("gradle.publish.skip.namespace.check", "true")

repositories.jcenter()

dependencies {
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5") {
        exclude(module = "groovy-all")
    }
}

internal val pluginName = "experimentalJigsawPlugin"
internal val pluginId   = "org.gradle.java.experimental-jigsaw"

gradlePlugin {
    (plugins) {
        register(pluginName) {
            id                  = pluginId
            implementationClass = "org.gradle.java.JigsawPlugin"
        }
    }
}

pluginBundle {
    website = "https://guides.gradle.org/building-java-9-modules"
    vcsUrl  = "https://github.com/gradle/gradle-java-modules"
    (plugins) {
        pluginName {
            id          = pluginId
            displayName = "Experimental Jigsaw Plugin"
            description = "Experiment with Java 9 modules before they are officially supported."
            tags        = listOf("jigsaw", "modules", "java9")
            version     = project.version.toString()
        }
    }
}
