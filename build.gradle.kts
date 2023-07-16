import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Gradle plugin project to get you started.
 * For more details take a look at the Writing Custom Plugins chapter in the Gradle
 * User Manual available at https://docs.gradle.org/8.1.1/userguide/custom_plugins.html
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    `java-gradle-plugin`
    kotlin("jvm").version("1.9.0")
    id("maven-publish")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://maven.veriny.tf/releases")
}

group = "tf.veriny.gradle"
version = "0.7.0"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.19.0")
}


gradlePlugin {
    plugins {
        create("base-plugin") {
            id = "tf.veriny.gradle.base-plugin"
            implementationClass = "tf.veriny.gradle.BasePlugin"
        }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "20"
        }
    }

    withType<JavaCompile> {
        targetCompatibility = "20"
    }
}

kotlin {
    explicitApi = ExplicitApiMode.Strict
}

publishing {
    repositories {
        maven {
            url = URI.create("https://maven.veriny.tf/releases")
            credentials.username = properties["verinyUsername"]!! as String
            credentials.password = properties["verinyPassword"]!! as String
        }
    }
}

