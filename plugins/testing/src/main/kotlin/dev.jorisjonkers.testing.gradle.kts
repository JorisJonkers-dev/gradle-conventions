import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.testing.Test
import java.math.BigDecimal

plugins {
    java
    jacoco
    id("dev.jorisjonkers.test-logging")
}

jacoco {
    toolVersion = "0.8.13"
}

fun csvProperty(name: String) =
    providers
        .gradleProperty(name)
        .map { value -> value.split(",").map(String::trim).filter(String::isNotEmpty) }

interface ExtratoastTestingConventionExtension {
    val testExcludedTags: ListProperty<String>
    val integrationTestIncludedTags: ListProperty<String>
    val integrationTestExcludedTags: ListProperty<String>
    val coverageExclusionPatterns: ListProperty<String>
    val aggregateCoverageMinimum: Property<BigDecimal>
    val unitCoverageMinimum: Property<BigDecimal>
    val integrationCoverageMinimum: Property<BigDecimal>
    val separateIntegrationCoverage: Property<Boolean>
    val checkDependsOnCoverage: Property<Boolean>
}

val jacocoExclusionPatterns: ListProperty<String> =
    objects.listProperty(String::class.java).convention(emptyList())
extensions.add("jacocoExclusionPatterns", jacocoExclusionPatterns)

val extratoastTesting =
    extensions.create<ExtratoastTestingConventionExtension>("extratoastTesting").apply {
        testExcludedTags.convention(csvProperty("extratoast.testing.testExcludedTags").orElse(emptyList()))
        integrationTestIncludedTags.convention(
            csvProperty("extratoast.testing.integrationTestIncludedTags").orElse(listOf("integration")),
        )
        integrationTestExcludedTags.convention(
            csvProperty("extratoast.testing.integrationTestExcludedTags").orElse(emptyList()),
        )
        coverageExclusionPatterns.convention(
            csvProperty("extratoast.testing.coverageExclusions").orElse(emptyList()),
        )
        aggregateCoverageMinimum.convention(
            providers
                .gradleProperty("extratoast.testing.aggregateCoverageMinimum")
                .map(String::toBigDecimal)
                .orElse(BigDecimal("0.80")),
        )
        unitCoverageMinimum.convention(
            providers
                .gradleProperty("extratoast.testing.unitCoverageMinimum")
                .map(String::toBigDecimal)
                .orElse(BigDecimal("0.80")),
        )
        integrationCoverageMinimum.convention(
            providers
                .gradleProperty("extratoast.testing.integrationCoverageMinimum")
                .map(String::toBigDecimal)
                .orElse(BigDecimal("0.80")),
        )
        separateIntegrationCoverage.convention(
            providers
                .gradleProperty("extratoast.testing.separateIntegrationCoverage")
                .map(String::toBoolean)
                .orElse(false),
        )
        checkDependsOnCoverage.convention(
            providers
                .gradleProperty("extratoast.testing.checkDependsOnCoverage")
                .map(String::toBoolean)
                .orElse(true),
        )
    }

val defaultJacocoExclusions =
    listOf(
        "**/jooq/**",
        "**/generated/**",
        "**/*Application.class",
        "**/*ApplicationKt.class",
    )

val integrationTestSourceSetName =
    providers
        .gradleProperty("extratoast.testing.integrationTestSourceSet")
        .orElse("integrationTest")
        .get()
val integrationTestTaskName =
    providers
        .gradleProperty("extratoast.testing.integrationTestTask")
        .orElse(integrationTestSourceSetName)
        .get()

sourceSets {
    create(integrationTestSourceSetName) {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

configurations["${integrationTestSourceSetName}Implementation"].extendsFrom(configurations.testImplementation.get())
configurations["${integrationTestSourceSetName}RuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

val integrationTest =
    tasks.register<Test>(integrationTestTaskName) {
        description = "Runs integration tests."
        group = "verification"
        testClassesDirs = sourceSets[integrationTestSourceSetName].output.classesDirs
        classpath = sourceSets[integrationTestSourceSetName].runtimeClasspath
        useJUnitPlatform()
        shouldRunAfter(tasks.test)
    }

fun filteredClassDirectories(exclusions: List<String>): FileCollection =
    files(
        sourceSets.main
            .get()
            .output.classesDirs
            .map { dir ->
                fileTree(dir) {
                    exclude(exclusions)
                }
            },
    )

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val jacocoIntegrationTestReport by tasks.registering(JacocoReport::class) {
    dependsOn(integrationTest)
    executionData.setFrom(layout.buildDirectory.file("jacoco/$integrationTestTaskName.exec"))
    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/jacocoIntegrationTestReport/jacocoIntegrationTestReport.xml"),
        )
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoIntegrationTestReport/html"))
    }
}

val jacocoIntegrationTestCoverageVerification by tasks.registering(JacocoCoverageVerification::class) {
    dependsOn(integrationTest)
    executionData.setFrom(layout.buildDirectory.file("jacoco/$integrationTestTaskName.exec"))
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs(
        "-XX:+EnableDynamicAgentLoading",
        "-Xshare:off",
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.util=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.time=ALL-UNNAMED",
    )
}

afterEvaluate {
    val jacocoExclusions =
        defaultJacocoExclusions +
            jacocoExclusionPatterns.get() +
            extratoastTesting.coverageExclusionPatterns.get()
    val filteredClasses = filteredClassDirectories(jacocoExclusions)

    tasks.named<Test>("test") {
        useJUnitPlatform {
            val excludedTags = extratoastTesting.testExcludedTags.get()
            if (excludedTags.isNotEmpty()) {
                excludeTags(*excludedTags.toTypedArray())
            }
        }
    }

    tasks.named<Test>(integrationTestTaskName) {
        useJUnitPlatform {
            val includedTags = extratoastTesting.integrationTestIncludedTags.get()
            val excludedTags = extratoastTesting.integrationTestExcludedTags.get()
            if (includedTags.isNotEmpty()) {
                includeTags(*includedTags.toTypedArray())
            }
            if (excludedTags.isNotEmpty()) {
                excludeTags(*excludedTags.toTypedArray())
            }
        }
    }

    tasks.jacocoTestReport {
        if (extratoastTesting.separateIntegrationCoverage.get()) {
            dependsOn(tasks.test)
            executionData.setFrom(layout.buildDirectory.file("jacoco/test.exec"))
        } else {
            dependsOn(tasks.test, integrationTest)
            executionData.setFrom(fileTree(layout.buildDirectory) { include("jacoco/*.exec") })
        }
        sourceDirectories.setFrom(
            sourceSets.main
                .get()
                .allSource.srcDirs,
        )
        classDirectories.setFrom(filteredClasses)
    }

    tasks.named<JacocoReport>("jacocoIntegrationTestReport") {
        sourceDirectories.setFrom(
            sourceSets.main
                .get()
                .allSource.srcDirs,
        )
        classDirectories.setFrom(filteredClasses)
    }

    tasks.jacocoTestCoverageVerification {
        if (extratoastTesting.separateIntegrationCoverage.get()) {
            executionData.setFrom(layout.buildDirectory.file("jacoco/test.exec"))
        } else {
            dependsOn(integrationTest)
            executionData.setFrom(fileTree(layout.buildDirectory) { include("jacoco/*.exec") })
        }
        sourceDirectories.setFrom(
            sourceSets.main
                .get()
                .allSource.srcDirs,
        )
        classDirectories.setFrom(filteredClasses)
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum =
                        if (extratoastTesting.separateIntegrationCoverage.get()) {
                            extratoastTesting.unitCoverageMinimum.get()
                        } else {
                            extratoastTesting.aggregateCoverageMinimum.get()
                        }
                }
            }
        }
    }

    tasks.named<JacocoCoverageVerification>("jacocoIntegrationTestCoverageVerification") {
        sourceDirectories.setFrom(
            sourceSets.main
                .get()
                .allSource.srcDirs,
        )
        classDirectories.setFrom(filteredClasses)
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = extratoastTesting.integrationCoverageMinimum.get()
                }
            }
        }
    }

    tasks.check {
        dependsOn(integrationTest)
        if (extratoastTesting.checkDependsOnCoverage.get()) {
            dependsOn(tasks.jacocoTestCoverageVerification)
            if (extratoastTesting.separateIntegrationCoverage.get()) {
                dependsOn(jacocoIntegrationTestCoverageVerification)
            }
        }
    }
}
