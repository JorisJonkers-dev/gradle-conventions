import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "dev.extratoast"
version = releasePleaseVersion()

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    // Detekt and Kotlin must bump together: Detekt's task init does a strict
    // binary-compat check on the Kotlin compiler version it was built against.
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.3.21")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.0.6")
    implementation("dev.detekt:detekt-gradle-plugin:2.0.0-alpha.3")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:14.2.0")
    implementation("io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:1.1.7")
    // jOOQ codegen, meta-extensions, and runtime overrides should stay aligned.
    implementation("org.jooq:jooq-codegen:3.21.4")
    implementation("org.jooq:jooq-meta-extensions:3.21.4")

    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ExtraToast/gradle-conventions")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
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
