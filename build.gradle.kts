import org.gradle.api.JavaVersion.VERSION_1_9
import org.gradle.api.artifacts.dsl.LockMode.STRICT

group   = "org.gradle.java"
version = "0.1.1"

buildscript {
    configurations.configureEach {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    groovy
    `java-gradle-plugin`
    `java-library`
    id("com.gradle.plugin-publish") version "0.10.1"
}

java {
    targetCompatibility = VERSION_1_9
    sourceCompatibility = VERSION_1_9
}

tasks.withType<JavaCompile>().configureEach {
    val compilerArgList = options.compilerArgs
    compilerArgList += "--release"
    compilerArgList += VERSION_1_9.majorVersion
    compilerArgList += "-parameters"
    compilerArgList += "-Xdoclint:all,-missing"
    compilerArgList += "-Xlint:all,-requires-automatic,-requires-transitive-automatic"
}

// make the publishing plugin skip checks that disallow publishing to com.gradle / org.gradle groups
System.setProperty("gradle.publish.skip.namespace.check", "true")

repositories.jcenter()

dependencies {
    implementation(    "com.github.javaparser", "javaparser-core", "3.15.11")
    implementation(    "com.google.guava",      "guava",           "28.1-jre")
    testImplementation("org.spockframework",    "spock-core",      "1.3-groovy-2.5") {
        exclude("org.codehaus.groovy", "groovy-all")
    }
}

dependencyLocking {
    lockAllConfigurations()
    lockMode = STRICT
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
