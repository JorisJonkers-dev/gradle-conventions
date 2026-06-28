import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConventionPluginSmokeTest {
    @TempDir
    Path projectDir;

    @Test
    void kotlinDetektAndKtlintPluginsRegisterTheirTasks() throws IOException {
        writeSettings("smoke");
        writeBuild(
            """
            plugins {
                id("dev.jorisjonkers.kotlin")
                id("dev.jorisjonkers.detekt")
                id("dev.jorisjonkers.ktlint")
            }

            repositories {
                mavenCentral()
            }
            """
        );

        Path sourceDirectory = projectDir.resolve("src/main/kotlin/dev/jorisjonkers/smoke");
        Files.createDirectories(sourceDirectory);
        Files.writeString(
            sourceDirectory.resolve("Smoke.kt"),
            """
            package dev.jorisjonkers.smoke

            class Smoke
            """.stripIndent()
        );

        BuildResult result = gradle("tasks", "--all").build();

        assertTrue(result.getOutput().contains("compileKotlin"), "Kotlin tasks should be registered.");
        assertTrue(result.getOutput().contains("detekt"), "Detekt tasks should be registered.");
        assertTrue(result.getOutput().contains("ktlintCheck"), "Ktlint tasks should be registered.");
    }

    @Test
    void springPluginRegistersSpringBootTasks() throws IOException {
        writeSettings("spring-smoke");
        writeBuild(
            """
            plugins {
                id("dev.jorisjonkers.spring")
            }

            repositories {
                mavenCentral()
            }
            """
        );

        BuildResult result = gradle("tasks", "--all").build();

        assertTrue(result.getOutput().contains("bootRun"), "Spring Boot tasks should be registered.");
        assertTrue(result.getOutput().contains("bootJar"), "Spring Boot packaging tasks should be registered.");
        assertTrue(result.getOutput().contains("compileKotlin"), "Kotlin tasks should be registered.");
    }

    @Test
    void kotlinPluginCanRegisterResolveAllDependenciesForCacheWarming() throws IOException {
        writeSettings("kotlin-cache-smoke");
        writeBuild(
            """
            plugins {
                id("dev.jorisjonkers.kotlin")
            }

            repositories {
                mavenCentral()
            }

            extratoastKotlin {
                javaToolchain.set(25)
                allWarningsAsErrors.set(false)
                registerResolveAllDependencies.set(true)
            }
            """
        );

        BuildResult result = gradle("tasks", "--all").build();

        assertTrue(
            result.getOutput().contains("resolveAllDependencies"),
            "Kotlin convention should expose opt-in dependency cache warming."
        );
    }

    @Test
    void springPluginAcceptsConfigurableBomInputs() throws IOException {
        writeSettings("spring-bom-smoke");
        writeBuild(
            """
            plugins {
                id("dev.jorisjonkers.spring")
            }

            repositories {
                mavenCentral()
            }

            extratoastSpring {
                springBootBomVersion.set("4.0.3")
                springModulithBomVersion.set("")
                testcontainersBomVersion.set("1.21.4")
                jacksonBomVersion.set("3.1.0")
                additionalBoms.set(listOf("org.junit:junit-bom:5.11.4"))
                jooqVersion.set("")
                standardDependenciesEnabled.set(false)
                testDependenciesEnabled.set(false)
            }
            """
        );

        BuildResult result = gradle("dependencyManagement").build();

        assertDependencyManagementContainsVersion(
            result,
            "4.0.3",
            "Spring Boot BOM version should be configurable."
        );
        assertDependencyManagementContainsVersion(
            result,
            "3.1.0",
            "Jackson BOM should be opt-in and configurable."
        );
        assertDependencyManagementContainsVersion(
            result,
            "1.21.4",
            "Testcontainers BOM version should be configurable."
        );
        assertDependencyManagementContainsVersion(
            result,
            "5.11.4",
            "Additional BOM coordinates should be imported."
        );
    }

    @Test
    void jooqCodegenPluginGeneratesSourcesFromMigrations() throws IOException {
        writeSettings("jooq-smoke");
        writeBuild(
            """
            plugins {
                id("dev.jorisjonkers.jooq-codegen")
            }

            repositories {
                mavenCentral()
            }
            """
        );
        Path migrations = projectDir.resolve("src/main/resources/db/migration");
        Files.createDirectories(migrations);
        Files.writeString(
            migrations.resolve("V1__create_sample_entity.sql"),
            """
            create schema public;

            create table public.sample_entity (
                id integer not null primary key,
                name varchar(255) not null
            );
            """.stripIndent()
        );

        BuildResult result = gradle("generateJooq").build();

        assertTrue(result.getOutput().contains(":generateJooq"), "jOOQ generation should run.");
        assertTrue(hasGeneratedJava(projectDir.resolve("build/generated/jooq")), "jOOQ should generate Java sources.");
    }

    @Test
    void testingPluginRunsCoverageVerificationFromCheck() throws IOException {
        writeSettings("testing-smoke");
        writeBuild(
            """
            plugins {
                id("dev.jorisjonkers.testing")
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                testImplementation(platform("org.junit:junit-bom:5.11.4"))
                testImplementation("org.junit.jupiter:junit-jupiter-api")
                testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            }
            """
        );
        Path sourceDirectory = projectDir.resolve("src/main/java/dev/jorisjonkers/smoke");
        Files.createDirectories(sourceDirectory);
        Files.writeString(
            sourceDirectory.resolve("CoveredType.java"),
            """
            package dev.jorisjonkers.smoke;

            public class CoveredType {
                public String message() {
                    return "covered";
                }
            }
            """.stripIndent()
        );
        Path testDirectory = projectDir.resolve("src/test/java/dev/jorisjonkers/smoke");
        Files.createDirectories(testDirectory);
        Files.writeString(
            testDirectory.resolve("CoveredTypeTest.java"),
            """
            package dev.jorisjonkers.smoke;

            import static org.junit.jupiter.api.Assertions.assertEquals;

            import org.junit.jupiter.api.Test;

            class CoveredTypeTest {
                @Test
                void returnsMessage() {
                    assertEquals("covered", new CoveredType().message());
                }
            }
            """.stripIndent()
        );

        BuildResult result = gradle("check").build();

        assertTrue(result.getOutput().contains(":integrationTest"), "Check should include integration tests.");
        assertTrue(
            result.getOutput().contains(":jacocoTestCoverageVerification"),
            "Check should include coverage verification."
        );
    }

    @Test
    void testingPluginCanRegisterCustomIntegrationCoverageGates() throws IOException {
        writeSettings("testing-split-smoke");
        writeBuild(
            """
            plugins {
                id("dev.jorisjonkers.testing")
            }

            repositories {
                mavenCentral()
            }

            extratoastTesting {
                testExcludedTags.set(listOf("system"))
                integrationTestIncludedTags.set(emptyList())
                integrationTestExcludedTags.set(listOf("system", "brevo-live", "discord-live"))
                coverageExclusionPatterns.set(listOf("**/dto/**"))
                unitCoverageMinimum.set("0.40".toBigDecimal())
                integrationCoverageMinimum.set("0.40".toBigDecimal())
                separateIntegrationCoverage.set(true)
            }
            """
        );

        BuildResult result = gradle(
            "-Pextratoast.testing.integrationTestSourceSet=functionalTest",
            "-Pextratoast.testing.integrationTestTask=functionalTest",
            "tasks",
            "--all"
        ).build();

        assertTrue(result.getOutput().contains("functionalTest"), "Custom integration test task should exist.");
        assertTrue(
            result.getOutput().contains("jacocoIntegrationTestReport"),
            "Integration JaCoCo report task should be registered."
        );
        assertTrue(
            result.getOutput().contains("jacocoIntegrationTestCoverageVerification"),
            "Integration JaCoCo coverage gate should be registered."
        );
    }

    private void writeSettings(String rootProjectName) throws IOException {
        Files.writeString(
            projectDir.resolve("settings.gradle.kts"),
            """
            rootProject.name = "%s"
            """.formatted(rootProjectName).stripIndent()
        );
    }

    private void writeBuild(String buildScript) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle.kts"), buildScript.stripIndent());
    }

    private GradleRunner gradle(String... arguments) throws IOException {
        writeGradleProperties();
        var runnerArguments = new ArrayList<String>();
        runnerArguments.add("--stacktrace");
        runnerArguments.addAll(Arrays.asList(arguments));
        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments(runnerArguments)
            .withPluginClasspath(pluginClasspath());
    }

    private void writeGradleProperties() throws IOException {
        String jacocoAgentJar = System.getProperty("jacocoAgentJar", "");
        String jacocoDestFile = System.getProperty("jacocoDestFile", "");
        if (jacocoAgentJar.isBlank() || jacocoDestFile.isBlank()) {
            return;
        }

        Files.writeString(
            projectDir.resolve("gradle.properties"),
            """
            org.gradle.jvmargs=-javaagent:%s=destfile=%s,append=true,dumponexit=true
            org.gradle.daemon=false
            """.formatted(jacocoAgentJar, jacocoDestFile).stripIndent()
        );
    }

    private static boolean hasGeneratedJava(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.anyMatch(path -> path.toString().endsWith(".java"));
        }
    }

    private static void assertDependencyManagementContainsVersion(
        BuildResult result,
        String version,
        String message
    ) {
        assertTrue(result.getOutput().contains(version), message);
    }

    private static List<File> pluginClasspath() {
        String classpath = System.getProperty("pluginClasspath", "");
        assertFalse(classpath.isBlank(), "Plugin classpath should be configured.");
        return Arrays.stream(classpath.split(Pattern.quote(File.pathSeparator)))
            .filter(entry -> !entry.isBlank())
            .map(File::new)
            .toList();
    }
}
