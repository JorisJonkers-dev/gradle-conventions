import org.gradle.api.provider.Property
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
}

interface ExtratoastKotlinConventionExtension {
    val javaToolchain: Property<Int>
}

val extratoastKotlin =
    extensions.create<ExtratoastKotlinConventionExtension>("extratoastKotlin").apply {
        // Consumers can override the default org toolchain with -Pextratoast.java.toolchain=17.
        javaToolchain.convention(
            providers.gradleProperty("extratoast.java.toolchain")
                .map(String::toInt)
                .orElse(21),
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
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Werror")
        jvmTarget.set(extratoastKotlin.javaToolchain.map { JvmTarget.fromTarget(it.toString()) })
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
