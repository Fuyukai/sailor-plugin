package tf.veriny.gradle

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import com.diffplug.spotless.LineEnding
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

/**
 * The base helper plugin for managing settings.
 */
@Suppress("UnstableApiUsage")
public class BasePlugin : Plugin<Project> {
    private class Repo(
        uri: String,
        val name: String,
        val groups: List<String> = emptyList(),
        val releasesOnly: Boolean = true
    ) {
        val uri = URI.create(uri)
    }

    // don't add repositories here. i'll mirror them.
    private companion object {
        private val REPOS = listOf(
            Repo(
                "https://maven.veriny.tf/releases",
                "Veriny Maven",
                listOf("tf.veriny")
            ),
            Repo(
                "https://maven.veriny.tf/mirror",
                "Veriny Mirror",
                releasesOnly = true
            ),

            Repo(
                "https://maven.fabricmc.net/",
                "FabricMC Maven",
                listOf("net.fabricmc", "fabric-loom", "teamreborn", "me.zeroeightsix" /* ??? */),
                releasesOnly = true,
            ),

            Repo(
                "https://libraries.minecraft.net/",
                "Mojang Maven",
                listOf("com.mojang"),
                releasesOnly = true
            ),

            Repo(
                "https://maven.quiltmc.org/repository/release",
                "QuiltMC Maven",
                listOf("org.quiltmc"),
            ),
        )
    }

    private fun Project.applyJavaSettings() {
        val javaExt = extensions.getByType(JavaPluginExtension::class.java)
        javaExt.apply {
            withSourcesJar()
            withJavadocJar()

            toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    private fun Project.applyKotlinSettings() {
        val kotlinExt = extensions.findByType(KotlinJvmProjectExtension::class.java)
            ?: error("Project is missing KotlinJvmProjectExtension?")

        kotlinExt.explicitApi = ExplicitApiMode.Strict

        dependencies.add("implementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        dependencies.add("implementation", "org.jetbrains.kotlin:kotlin-reflect")

        tasks.withType(KotlinCompile::class.java).configureEach { task ->
            task.compilerOptions.apply {
                jvmTarget.set(JvmTarget.JVM_17)
                languageVersion.set(KotlinVersion.KOTLIN_1_9)

                val compilerArgs = listOf(
                    "-Xjvm-default=all",  // Forcibly enable Java 8+ default interface methods
                    "-Xassertions=always-enable",  // Forcibly enable assertions
                    "-Xlambdas=indy",  // Forcibly use invokedynamic for all lambdas.
                    "-Xexplicit-api=strict"  // explicit api doesn't get applied for some reason.
                )
                freeCompilerArgs.set(freeCompilerArgs.get() + compilerArgs)
            }
        }
    }

    private fun Project.applySpotlessSettings() {
        val spotlessExt = extensions.getByType(SpotlessExtension::class.java)
        spotlessExt.lineEndings = LineEnding.UNIX
        spotlessExt.encoding = Charsets.UTF_8

        spotlessExt.kotlin { k ->
            k.indentWithSpaces(4)
            k.targetExclude("build/generated/**")
            k.trimTrailingWhitespace()

            k.licenseHeaderFile(rootProject.file("gradle/LICENCE-HEADER.txt"))
        }
    }

    private fun Project.applyPublishingSettings() {
        val publishingExt = extensions.getByType(PublishingExtension::class.java)
        publishingExt.repositories { repos ->
            repos.maven { mv ->
                mv.url = URI.create("https://maven.veriny.tf/releases")

                mv.credentials.username = properties["verinyUsername"]!! as String
                mv.credentials.password = properties["verinyPassword"]!! as String
            }
        }
    }

    override fun apply(target: Project) {
        // make sure that we have the appropriate plugins applied
        // https://stackoverflow.com/questions/74177878/use-kotlin-plugins-from-buildsrc
        target.pluginManager.apply("org.jetbrains.kotlin.jvm")
        target.pluginManager.apply(SpotlessPlugin::class.java)
        target.pluginManager.apply(MavenPublishPlugin::class.java)

        target.repositories.mavenLocal()
        target.repositories.mavenCentral { it.name = "Maven Central" }
            .mavenContent { content ->
                content.excludeGroupAndSubgroups("net.fabricmc")
                content.excludeGroupAndSubgroups("tf.veriny")
                content.excludeGroupAndSubgroups("org.quiltmc")
                content.excludeGroupAndSubgroups("com.mojang")
            }

        // just add all repositories to all groups here.
        // this rather complex setup ensures we don't end up hitting random repos for deps that
        // will NEVER be there, saving quite a bit of time importing - esp on slow connections.

        for (repo in REPOS) {
            target.repositories.maven { m ->
                m.url = repo.uri
                m.name = repo.name

                if (repo.groups.isNotEmpty()) {
                    m.mavenContent { content ->
                        repo.groups.forEach(content::includeGroupAndSubgroups)
                    }
                }

                if (repo.releasesOnly) m.mavenContent { c -> c.releasesOnly() }
            }
        }

        target.applyJavaSettings()
        target.applyKotlinSettings()
        target.applyPublishingSettings()

        target.afterEvaluate { it ->
            it.applySpotlessSettings()

            // move out spotless tasks into their own group rather than verification
            it.tasks.filter { it.name.startsWith("spotless") }.forEach { it.group = "lint" }

            // remove added loom repositories
            val repoIterator = it.repositories.listIterator()
            for (repo in repoIterator) {
                if (repo.name == "Fabric" || repo.name == "Mojang") {
                    repoIterator.remove()
                } else if (  // duplicate maven central added by loom
                    repo is MavenArtifactRepository &&
                    repo.url.toString() == "https://repo.maven.apache.org/maven2/" &&
                    repo.name != "Maven Central"
                ) {
                    repoIterator.remove()
                }
            }
        }
    }
}
