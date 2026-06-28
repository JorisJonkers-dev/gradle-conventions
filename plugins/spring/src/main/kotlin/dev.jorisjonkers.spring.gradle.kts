import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

plugins {
    id("dev.jorisjonkers.kotlin")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

interface ExtratoastSpringConventionExtension {
    val springBootBomVersion: Property<String>
    val springModulithBomVersion: Property<String>
    val testcontainersBomVersion: Property<String>
    val jacksonBomVersion: Property<String>
    val additionalBoms: ListProperty<String>
    val jooqVersion: Property<String>
    val springCloudVaultVersion: Property<String>
    val cracVersion: Property<String>
    val logstashLogbackEncoderVersion: Property<String>
    val mockkVersion: Property<String>
    val archunitVersion: Property<String>
    val standardDependenciesEnabled: Property<Boolean>
    val testDependenciesEnabled: Property<Boolean>
}

val extratoastSpring =
    extensions.create<ExtratoastSpringConventionExtension>("extratoastSpring").apply {
        springBootBomVersion.convention(providers.gradleProperty("extratoast.spring.bootBomVersion").orElse(""))
        springModulithBomVersion.convention(
            providers.gradleProperty("extratoast.spring.modulithBomVersion").orElse("1.3.2"),
        )
        testcontainersBomVersion.convention(
            providers.gradleProperty("extratoast.spring.testcontainersBomVersion").orElse("2.0.5"),
        )
        jacksonBomVersion.convention(providers.gradleProperty("extratoast.spring.jacksonBomVersion").orElse(""))
        additionalBoms.convention(
            providers
                .gradleProperty("extratoast.spring.additionalBoms")
                .map { value -> value.split(",").map(String::trim).filter(String::isNotEmpty) }
                .orElse(emptyList()),
        )
        jooqVersion.convention(providers.gradleProperty("extratoast.spring.jooqVersion").orElse("3.21.4"))
        springCloudVaultVersion.convention(
            providers.gradleProperty("extratoast.spring.cloudVaultVersion").orElse("5.0.1"),
        )
        cracVersion.convention(providers.gradleProperty("extratoast.spring.cracVersion").orElse("1.5.0"))
        logstashLogbackEncoderVersion.convention(
            providers.gradleProperty("extratoast.spring.logstashLogbackEncoderVersion").orElse("8.0"),
        )
        mockkVersion.convention(providers.gradleProperty("extratoast.spring.mockkVersion").orElse("1.13.16"))
        archunitVersion.convention(providers.gradleProperty("extratoast.spring.archunitVersion").orElse("1.4.0"))
        standardDependenciesEnabled.convention(
            providers
                .gradleProperty("extratoast.spring.standardDependenciesEnabled")
                .map(String::toBoolean)
                .orElse(true),
        )
        testDependenciesEnabled.convention(
            providers
                .gradleProperty("extratoast.spring.testDependenciesEnabled")
                .map(String::toBoolean)
                .orElse(true),
        )
    }

afterEvaluate {
    dependencyManagement {
        imports {
            extratoastSpring.springBootBomVersion.orNull
                ?.takeIf(String::isNotBlank)
                ?.let { mavenBom("org.springframework.boot:spring-boot-dependencies:$it") }
            extratoastSpring.springModulithBomVersion.orNull
                ?.takeIf(String::isNotBlank)
                ?.let { mavenBom("org.springframework.modulith:spring-modulith-bom:$it") }
            extratoastSpring.testcontainersBomVersion.orNull
                ?.takeIf(String::isNotBlank)
                ?.let { mavenBom("org.testcontainers:testcontainers-bom:$it") }
            extratoastSpring.jacksonBomVersion.orNull
                ?.takeIf(String::isNotBlank)
                ?.let { mavenBom("tools.jackson:jackson-bom:$it") }
            extratoastSpring.additionalBoms.get().forEach { mavenBom(it) }
        }
        dependencies {
            // Spring Boot can lag jOOQ; keep runtime aligned with the codegen dependency.
            extratoastSpring.jooqVersion.orNull
                ?.takeIf(String::isNotBlank)
                ?.let { dependency("org.jooq:jooq:$it") }
        }
    }

    if (extratoastSpring.standardDependenciesEnabled.get()) {
        dependencies {
            implementation("org.springframework.boot:spring-boot-starter-web")
            implementation("org.springframework.boot:spring-boot-starter-actuator")
            implementation("org.springframework.boot:spring-boot-starter-validation")
            implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
            implementation("org.jetbrains.kotlin:kotlin-reflect")
            implementation("org.crac:crac:${extratoastSpring.cracVersion.get()}")
            runtimeOnly("io.micrometer:micrometer-registry-prometheus")
            implementation(
                "net.logstash.logback:logstash-logback-encoder:${extratoastSpring.logstashLogbackEncoderVersion.get()}",
            )
            implementation(
                "org.springframework.cloud:spring-cloud-starter-vault-config:${extratoastSpring.springCloudVaultVersion.get()}",
            )
        }
    }

    if (extratoastSpring.testDependenciesEnabled.get()) {
        dependencies {
            testImplementation("org.springframework.boot:spring-boot-starter-test") {
                exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
            }
            testImplementation("io.mockk:mockk:${extratoastSpring.mockkVersion.get()}")
            testImplementation("org.assertj:assertj-core")
            testImplementation("com.tngtech.archunit:archunit-junit5:${extratoastSpring.archunitVersion.get()}")
        }
    }
}
