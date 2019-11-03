import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import kotlin.text.RegexOption.IGNORE_CASE
import org.gradle.api.JavaVersion.VERSION_1_9
import org.gradle.api.artifacts.dsl.LockMode.STRICT
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    kotlin("jvm")                       version "1.3.61"
    id("com.github.ben-manes.versions") version "0.27.0"
    id("com.gradle.plugin-publish")     version "0.10.1"
//    id("org.ysb33r.gradletest")         version "2.0"
}

java {
    targetCompatibility = VERSION_1_9
    sourceCompatibility = VERSION_1_9
}

internal val unstableVersionRegex = "alpha|beta|latest|m(?!\\p{Alpha})|pr|rc|snap".toRegex(IGNORE_CASE)

tasks.withType<DependencyUpdatesTask> {
    checkConstraints = true
    rejectVersionIf {
        ! unstableVersionRegex.containsMatchIn(currentVersion) && unstableVersionRegex.containsMatchIn(candidate.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    val compilerArgList = options.compilerArgs
    compilerArgList += "--release"
    compilerArgList += VERSION_1_9.majorVersion
    compilerArgList += "-parameters"
    compilerArgList += "-Xdoclint:all,-missing"
    compilerArgList += "-Xlint:all,-requires-automatic,-requires-transitive-automatic"
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        javaParameters = true
        jvmTarget      = VERSION_1_9.majorVersion
        verbose        = true
    }
}

/*
tasks.gradleTest.configure {
    kotlinDsl = true
    versions(
        "6.2-rc-1",
        "6.1.1",
        "6.1",
        "6.1-rc-3",
        "6.1-rc-2",
        "6.1-rc-1",
        "6.1-milestone-3",
        "6.1-milestone-2",
        "6.1-milestone-1",
        "6.0.1",
        "6.0",
        "6.0-rc-3",
        "6.0-rc-2",
        "6.0-rc-1",
        "5.6.4",
        "5.6.3",
        "5.6.2",
        "5.6.1",
        "5.6",
        "5.6-rc-2",
        "5.6-rc-1",
        "5.5.1",
        "5.5",
        "5.5-rc-4",
        "5.5-rc-3",
        "5.5-rc-2",
        "5.5-rc-1",
        "5.4.1",
        "5.4",
        "5.4-rc-1",
        "5.3.1",
        "5.3",
        "5.3-rc-3",
        "5.3-rc-2",
        "5.3-rc-1",
        "5.2.1",
        "5.2",
        "5.2-rc-1",
        "5.1.1",
        "5.1",
        "5.1-rc-3",
        "5.1-rc-2",
        "5.1-rc-1",
        "5.1-milestone-1",
        "5.0",
        "5.0-rc-5",
        "5.0-rc-4",
        "5.0-rc-3",
        "5.0-rc-2",
        "5.0-rc-1",
        "5.0-milestone-1",
        "4.10.3",
        "4.10.2",
        "4.10.1",
        "4.10",
        "4.10-rc-3",
        "4.10-rc-2",
        "4.10-rc-1"
    )
}
*/

// make the publishing plugin skip checks that disallow publishing to com.gradle / org.gradle groups
System.setProperty("gradle.publish.skip.namespace.check", "true")

repositories.jcenter()

dependencies {
    implementation(    "com.github.javaparser", "javaparser-core",                   "3.15.11")
    api(               "org.jetbrains.kotlinx", "kotlinx-collections-immutable-jvm", "0.3")
    testImplementation("org.spockframework",    "spock-core",                        "1.3-groovy-2.5") {
        exclude("org.codehaus.groovy", "groovy-all")
    }
    implementation(    gradleKotlinDsl())
    compileOnly(       kotlin("gradle-plugin", "1.3.61"))
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
