import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner

class ConventionPluginSmokeTest {
    @Test
    fun `kotlin detekt and ktlint plugins register their tasks`() {
        val projectDir = Files.createTempDirectory("gradle-conventions-smoke")

        try {
            projectDir.resolve("settings.gradle.kts").writeText(
                """
                rootProject.name = "smoke"
                """.trimIndent(),
            )
            projectDir.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    id("dev.extratoast.kotlin")
                    id("dev.extratoast.detekt")
                    id("dev.extratoast.ktlint")
                }

                repositories {
                    mavenCentral()
                }
                """.trimIndent(),
            )

            val sourceDirectory = projectDir.resolve("src/main/kotlin/dev/extratoast/smoke")
            Files.createDirectories(sourceDirectory)
            sourceDirectory.resolve("Smoke.kt").writeText(
                """
                package dev.extratoast.smoke

                class Smoke
                """.trimIndent(),
            )

            val result =
                GradleRunner.create()
                    .withProjectDir(projectDir.toFile())
                    .withArguments("tasks", "--all", "--stacktrace")
                    .withPluginClasspath()
                    .build()

            assertTrue(result.output.contains("compileKotlin"), "Kotlin tasks should be registered.")
            assertTrue(result.output.contains("detekt"), "Detekt tasks should be registered.")
            assertTrue(result.output.contains("ktlintCheck"), "Ktlint tasks should be registered.")
        } finally {
            projectDir.toFile().deleteRecursively()
        }
    }
}
