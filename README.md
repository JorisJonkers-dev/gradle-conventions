# gradle-conventions

Gradle convention plugins for ExtraToast projects, published to GitHub Packages.

## Plugin IDs

| Plugin ID | Configures |
| --- | --- |
| `dev.extratoast.kotlin` | Kotlin JVM, Java toolchain defaulting to 21, strict JSR-305 handling, warnings as errors, and JUnit Platform for tests. |
| `dev.extratoast.detekt` | Detekt with defaults layered on, `allRules = false`, and a configurable config path defaulting to `config/detekt/detekt.yml`. |
| `dev.extratoast.ktlint` | Ktlint with Android mode disabled and generated/build output excluded. |
| `dev.extratoast.spring` | Kotlin conventions, Kotlin Spring, Spring Boot, Spring dependency management, shared BOMs, and standard Spring service dependencies. |
| `dev.extratoast.testing` | Java, Jacoco, test logging, an `integrationTest` source set and task, aggregate reports, and an 80% coverage verification rule. |
| `dev.extratoast.test-logging` | Verbose test logging and a suite-level result summary. |
| `dev.extratoast.jooq-codegen` | A `generateJooq` task using jOOQ DDLDatabase against Flyway SQL migrations, plus generated source wiring. |

## Consumption

Add the GitHub Packages Maven repository to `pluginManagement` in the consuming
repo's `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = "ExtraToastGradleConventions"
            url = uri("https://maven.pkg.github.com/ExtraToast/gradle-conventions")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orNull
            }
        }
    }
}
```

Then apply the needed plugins in `build.gradle.kts`:

```kotlin
plugins {
    id("dev.extratoast.kotlin") version "x"
    id("dev.extratoast.detekt") version "x"
    id("dev.extratoast.ktlint") version "x"
}
```

GitHub Packages access requires credentials with package read access.

## Configuration

Defaults can be overridden with Gradle properties:

```properties
extratoast.java.toolchain=21
extratoast.detekt.config=config/detekt/detekt.yml
extratoast.jooq.schema=public
extratoast.jooq.package=dev.extratoast.jooq.generated
extratoast.jooq.migrationLocations=filesystem:src/main/resources/db/migration
extratoast.jooq.outputDirectory=generated/jooq
```

The jOOQ plugin also exposes a `jooqCodegen` extension for per-project build
script configuration:

```kotlin
jooqCodegen {
    schemaName.set("public")
    packageName.set("com.example.generated.jooq")
    migrationLocations.set(listOf("filesystem:src/main/resources/db/migration"))
}
```
