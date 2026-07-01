import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
}

interface ExtratoastKotlinConventionExtension {
    val javaToolchain: Property<Int>
    val allWarningsAsErrors: Property<Boolean>
    val registerResolveAllDependencies: Property<Boolean>
}

val extratoastKotlin =
    extensions.create<ExtratoastKotlinConventionExtension>("extratoastKotlin").apply {
        // Consumers can override the default org toolchain with -Pextratoast.java.toolchain=17.
        javaToolchain.convention(
            providers
                .gradleProperty("extratoast.java.toolchain")
                .map(String::toInt)
                .orElse(21),
        )
        allWarningsAsErrors.convention(
            providers
                .gradleProperty("extratoast.kotlin.allWarningsAsErrors")
                .map(String::toBoolean)
                .orElse(true),
        )
        registerResolveAllDependencies.convention(
            providers
                .gradleProperty("extratoast.kotlin.resolveAllDependencies")
                .map(String::toBoolean)
                .orElse(
                    providers
                        .gradleProperty("extratoast.resolveAllDependencies.enabled")
                        .map(String::toBoolean)
                        .orElse(false),
                ),
        )
    }

java {
    toolchain {
        languageVersion.set(extratoastKotlin.javaToolchain.map(JavaLanguageVersion::of))
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(extratoastKotlin.javaToolchain.map(JavaLanguageVersion::of))
    }

    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        allWarningsAsErrors.set(extratoastKotlin.allWarningsAsErrors)
        jvmTarget.set(extratoastKotlin.javaToolchain.map { JvmTarget.fromTarget(it.toString()) })
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgumentProviders.add(
        object : CommandLineArgumentProvider {
            @get:Input
            val warningsAsErrors: Provider<Boolean> = extratoastKotlin.allWarningsAsErrors

            override fun asArguments(): Iterable<String> =
                if (warningsAsErrors.get()) {
                    listOf("-Werror", "-Xlint:all,-processing")
                } else {
                    emptyList()
                }
        },
    )
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

afterEvaluate {
    if (extratoastKotlin.registerResolveAllDependencies.get() && tasks.findByName("resolveAllDependencies") == null) {
        tasks.register("resolveAllDependencies") {
            description = "Resolves every resolvable configuration to warm the Gradle cache."
            group = "build setup"
            notCompatibleWithConfigurationCache("Resolves configurations at execution time.")
            doLast {
                configurations
                    .matching { it.isCanBeResolved }
                    .forEach { configuration ->
                        runCatching { configuration.resolve() }
                            .onFailure { logger.warn("Skipping ${configuration.name}: ${it.message}") }
                    }
            }
        }
    }
}
