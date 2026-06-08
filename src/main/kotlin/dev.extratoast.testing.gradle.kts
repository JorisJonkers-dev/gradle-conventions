import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.testing.Test

plugins {
    java
    jacoco
    id("dev.extratoast.test-logging")
}

jacoco {
    toolVersion = "0.8.12"
}

val jacocoExclusionPatterns: ListProperty<String> =
    objects.listProperty(String::class.java).convention(emptyList())
extensions.add("jacocoExclusionPatterns", jacocoExclusionPatterns)

val defaultJacocoExclusions =
    listOf(
        "**/jooq/**",
        "**/generated/**",
        "**/*Application.class",
        "**/*ApplicationKt.class",
    )

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

configurations["integrationTestImplementation"].extendsFrom(configurations.testImplementation.get())
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
    shouldRunAfter(tasks.test)
}

fun filteredClassDirectories(): FileCollection =
    files(
        sourceSets.main
            .get()
            .output.classesDirs
            .map { dir ->
                fileTree(dir) {
                    exclude(defaultJacocoExclusions + jacocoExclusionPatterns.get())
                }
            },
    )

tasks.jacocoTestReport {
    dependsOn(tasks.test, integrationTest)
    executionData.setFrom(
        fileTree(layout.buildDirectory) { include("jacoco/*.exec") },
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(filteredClassDirectories())
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test, integrationTest)
    executionData.setFrom(
        fileTree(layout.buildDirectory) { include("jacoco/*.exec") },
    )
    classDirectories.setFrom(filteredClassDirectories())
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs(
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.util=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.time=ALL-UNNAMED",
    )
}

tasks.check {
    dependsOn(integrationTest)
}
