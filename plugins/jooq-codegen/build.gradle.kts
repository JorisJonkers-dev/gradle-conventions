plugins {
    `kotlin-dsl`
    `maven-publish`
}

base {
    archivesName.set("gradle-conventions-jooq-codegen")
}

dependencies {
    implementation("org.jooq:jooq-codegen:3.21.9")
    implementation("org.jooq:jooq-meta-extensions:3.21.9")
}
