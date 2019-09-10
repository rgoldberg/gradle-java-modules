plugins {
    id("org.gradle.java.experimental-jigsaw") version "0.1.1"
}

group   = "com.example"
version = "1.0"

tasks.register("runGradleTest") {
    dependsOn("build")
}
