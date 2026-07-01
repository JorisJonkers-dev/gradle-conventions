plugins {
    `kotlin-dsl`
    `maven-publish`
}

base {
    archivesName.set("gradle-conventions-kotlin")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
}
