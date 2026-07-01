import org.gradle.api.provider.ListProperty
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

interface ExtratoastKtlintConventionExtension {
    val includePatterns: ListProperty<String>
    val excludePatterns: ListProperty<String>
}

val extratoastKtlint =
    extensions.create<ExtratoastKtlintConventionExtension>("extratoastKtlint").apply {
        includePatterns.convention(
            providers
                .gradleProperty("extratoast.ktlint.includes")
                .map { value -> value.split(",").map(String::trim).filter(String::isNotEmpty) }
                .orElse(emptyList()),
        )
        excludePatterns.convention(
            providers
                .gradleProperty("extratoast.ktlint.excludes")
                .map { value -> value.split(",").map(String::trim).filter(String::isNotEmpty) }
                // `.github-workflows` is a vendored copy of the shared CI actions repo that the
                // reusable workflows check out beside the sources; it is not the project's code.
                .orElse(listOf("**/generated/**", "**/build/**", "**/.github-workflows/**")),
        )
    }

configure<KtlintExtension> {
    android.set(false)
}

afterEvaluate {
    configure<KtlintExtension> {
        filter {
            extratoastKtlint.includePatterns.get().forEach { include(it) }
            extratoastKtlint.excludePatterns.get().forEach { exclude(it) }
        }
    }
}
