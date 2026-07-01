import groovy.json.JsonSlurper
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask

plugins {
    base
    jacoco
    id("org.jetbrains.kotlin.jvm") version "2.3.21" apply false
    id("dev.detekt") version "2.0.0-alpha.3" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}

group = "dev.jorisjonkers"
version = releasePleaseVersion()

jacoco {
    toolVersion = "0.8.13"
}

repositories {
    mavenCentral()
}

val pluginProjectPaths =
    setOf(
        ":plugins:kotlin",
        ":plugins:detekt",
        ":plugins:ktlint",
        ":plugins:spring",
        ":plugins:testing",
        ":plugins:test-logging",
        ":plugins:jooq-codegen",
    )

val jacocoClassExclusions =
    listOf(
        "META-INF/**",
        "gradle/kotlin/dsl/accessors/**",
        "gradle/kotlin/dsl/plugins/**",
        "**/*Plugin.class",
        "**/*Plugin$*.class",
    )

subprojects {
    group = rootProject.group
    version = rootProject.version

    val moduleArtifactId =
        when {
            path == ":aggregate" -> "gradle-conventions"
            path in pluginProjectPaths -> "gradle-conventions-$name"
            else -> null
        }

    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }

    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension>("publishing") {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/JorisJonkers-dev/gradle-conventions")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }

            publications.withType(MavenPublication::class.java).configureEach {
                pom {
                    licenses {
                        license {
                            name.set("Joris Jonkers Proprietary Source-Available License 1.0")
                            url.set("https://github.com/JorisJonkers-dev/gradle-conventions/blob/main/LICENSE")
                            distribution.set("repo")
                            comments.set("SPDX-License-Identifier: LicenseRef-JorisJonkers-Proprietary-1.0")
                        }
                    }
                }

                if (moduleArtifactId != null && (project.path == ":aggregate" || name == "pluginMaven")) {
                    artifactId = moduleArtifactId
                }
            }
        }
    }

    tasks.withType(Test::class.java).configureEach {
        useJUnitPlatform()
    }

    if (path in pluginProjectPaths) {
        apply(plugin = "dev.detekt")
        apply(plugin = "org.jlleitschuh.gradle.ktlint")

        extensions.configure<DetektExtension>("detekt") {
            buildUponDefaultConfig = true
            allRules = false
            source.setFrom("src")
        }

        extensions.configure<KtlintExtension>("ktlint") {
            android.set(false)
            filter {
                include("src/**/*.kt", "src/**/*.kts")
                exclude("**/build/**", "**/generated/**")
            }
        }

        tasks.withType(Detekt::class.java).configureEach {
            setSource(files("src"))
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", "**/generated/**", "**/generated-sources/**")
            exclude { fileTreeElement ->
                fileTreeElement.file
                    .invariantSeparatorsPath
                    .contains("/build/")
            }
        }

        tasks.withType(BaseKtLintCheckTask::class.java).configureEach {
            setSource(files("src"))
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", "**/generated/**", "**/generated-sources/**")
            exclude { fileTreeElement ->
                fileTreeElement.file
                    .invariantSeparatorsPath
                    .contains("/build/")
            }
        }

        afterEvaluate {
            tasks.withType(Detekt::class.java).configureEach {
                setSource(files("src"))
            }

            tasks.withType(BaseKtLintCheckTask::class.java).configureEach {
                setSource(files("src"))
            }
        }
    }
}

val jacocoExecutionData =
    fileTree(layout.projectDirectory) {
        include("aggregate/build/jacoco/*.exec")
    }

val jacocoTestReport by tasks.registering(JacocoReport::class) {
    group = "verification"
    description = "Generates Jacoco coverage reports for the convention plugins."
    dependsOn(":aggregate:test")
    executionData.setFrom(jacocoExecutionData)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val jacocoTestCoverageVerification by tasks.registering(JacocoCoverageVerification::class) {
    group = "verification"
    description = "Verifies convention plugin line coverage is at least 80%."
    dependsOn(":aggregate:test")
    executionData.setFrom(jacocoExecutionData)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(jacocoTestCoverageVerification)
}

gradle.projectsEvaluated {
    val pluginMainSourceSets =
        pluginProjectPaths
            .map {
                project(it)
                    .extensions
                    .getByType(SourceSetContainer::class.java)
                    .named("main")
                    .get()
            }

    val pluginClassDirectories =
        files(
            pluginMainSourceSets.map { sourceSet ->
                sourceSet.output.classesDirs.map { classDirectory ->
                    fileTree(classDirectory) {
                        exclude(jacocoClassExclusions)
                    }
                }
            },
        )
    val pluginSourceDirectories = files(pluginMainSourceSets.map { it.allSource.srcDirs })

    tasks.named<JacocoReport>("jacocoTestReport") {
        classDirectories.setFrom(pluginClassDirectories)
        sourceDirectories.setFrom(pluginSourceDirectories)
        additionalSourceDirs.setFrom(pluginSourceDirectories)
    }
    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        classDirectories.setFrom(pluginClassDirectories)
        sourceDirectories.setFrom(pluginSourceDirectories)
    }
}

fun Project.releasePleaseVersion(): String {
    val manifest = layout.projectDirectory.file(".release-please-manifest.json").asFile
    if (!manifest.isFile) {
        return "0.1.0"
    }

    val versions = JsonSlurper().parse(manifest) as Map<*, *>
    return versions["."]?.toString()?.takeIf(String::isNotBlank) ?: "0.1.0"
}
