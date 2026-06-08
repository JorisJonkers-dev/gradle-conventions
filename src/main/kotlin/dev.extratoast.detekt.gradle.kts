import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.provider.Property

plugins {
    id("dev.detekt")
}

interface ExtratoastDetektConventionExtension {
    val configPath: Property<String>
}

val extratoastDetekt =
    extensions.create<ExtratoastDetektConventionExtension>("extratoastDetekt").apply {
        // Keeps the org config layout as the default while letting consumers opt out.
        configPath.convention(providers.gradleProperty("extratoast.detekt.config").orElse("config/detekt/detekt.yml"))
    }

configure<DetektExtension> {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(extratoastDetekt.configPath.map { rootProject.layout.projectDirectory.file(it) })
}
