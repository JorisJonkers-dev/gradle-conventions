import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.provider.Property

plugins {
    id("dev.detekt")
}

interface ExtratoastDetektConventionExtension {
    val configPath: Property<String>
    val requiredConfigFile: Property<Boolean>
}

val extratoastDetekt =
    extensions.create<ExtratoastDetektConventionExtension>("extratoastDetekt").apply {
        // Keeps the org config layout as the default while letting consumers opt out.
        configPath.convention(providers.gradleProperty("extratoast.detekt.config").orElse("config/detekt/detekt.yml"))
        requiredConfigFile.convention(
            providers.gradleProperty("extratoast.detekt.requiredConfigFile")
                .map(String::toBoolean)
                .orElse(false),
        )
    }

configure<DetektExtension> {
    buildUponDefaultConfig = true
    allRules = false
}

afterEvaluate {
    configure<DetektExtension> {
        val configFile = rootProject.layout.projectDirectory.file(extratoastDetekt.configPath.get()).asFile
        if (configFile.isFile || extratoastDetekt.requiredConfigFile.get()) {
            config.setFrom(configFile)
        }
    }
}
