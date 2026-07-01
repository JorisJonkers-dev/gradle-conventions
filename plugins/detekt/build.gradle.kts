plugins {
    `kotlin-dsl`
    `maven-publish`
}

base {
    archivesName.set("gradle-conventions-detekt")
}

dependencies {
    implementation("dev.detekt:detekt-gradle-plugin:2.0.0-alpha.3")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
}
