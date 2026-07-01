plugins {
    `kotlin-dsl`
    `maven-publish`
}

base {
    archivesName.set("gradle-conventions-spring")
}

dependencies {
    implementation(project(":plugins:kotlin"))
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.3.20")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.0.6")
    implementation("io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:1.1.7")
}
