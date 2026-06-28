import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-library`
    `maven-publish`
}

base {
    archivesName.set("gradle-conventions")
}

dependencies {
    api(project(":plugins:kotlin"))
    api(project(":plugins:detekt"))
    api(project(":plugins:ktlint"))
    api(project(":plugins:spring"))
    api(project(":plugins:testing"))
    api(project(":plugins:test-logging"))
    api(project(":plugins:jooq-codegen"))

    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val testKitJacocoAgent by configurations.creating
val testKitJacocoDestFile = layout.buildDirectory.file("jacoco/testkit.exec")

dependencies {
    testKitJacocoAgent("org.jacoco:org.jacoco.agent:0.8.12:runtime")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.test {
    dependsOn(testKitJacocoAgent)
    outputs.file(testKitJacocoDestFile)
    doFirst {
        val jacocoDestFile = testKitJacocoDestFile.get().asFile
        jacocoDestFile.parentFile.mkdirs()
        jacocoDestFile.delete()

        systemProperty(
            "pluginClasspath",
            sourceSets.test
                .get()
                .runtimeClasspath.asPath,
        )
        systemProperty("jacocoAgentJar", testKitJacocoAgent.singleFile.absolutePath)
        systemProperty("jacocoDestFile", jacocoDestFile.absolutePath)
    }
}
