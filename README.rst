Sailor Plugin
=============

This is my base plugin that I use across my modding projects.

Usage
-----

In your ``settings.gradle.kts``:

.. code-block:: kotlin

    pluginManagement {
        repositories {
            gradlePluginPortal()
            mavenCentral()
            mavenLocal()
            maven("https://maven.veriny.tf/releases")
        }
    }

In your ``build.gradle.kts``:

.. code-block:: kotlin

    plugins {
        id("tf.veriny.gradle.base-plugin").version("0.7.0").apply(false)
    }

    subprojects {
        apply(plugin = "tf.veriny.gradle.base-plugin")
    }

What does it do?
----------------

- Configures Java to use a Java 17 toolchain, as well as producing source and javadoc jars.
- Configures Kotlin to use Explicit API, a few compiler options, and to target Kotlin 1.9/JVM 17.
- Adds Maven Central, Maven Local, Mojang, Fabric, and Quilt repositories, and limits their groups
  to prevent trying to look up dependencies in places that don't have them.
- Applies the Spotless plugin for LF line endings and UTF-8 encoding, and applies Kotlin styling
  of 4 spaces and a licence header.
- Adds a publishing rule to publish to my maven releases repository.