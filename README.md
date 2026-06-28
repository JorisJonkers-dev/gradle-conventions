# gradle-conventions

Gradle convention plugins for JorisJonkers-dev projects, published to GitHub Packages.

## Plugin IDs

| Plugin ID | Configures |
| --- | --- |
| `dev.jorisjonkers.kotlin` | Kotlin JVM, Java toolchain defaulting to 21, strict JSR-305 handling, configurable warning policy, optional dependency cache warming, and JUnit Platform for tests. |
| `dev.jorisjonkers.detekt` | Detekt with defaults layered on, `allRules = false`, and a configurable config path defaulting to `config/detekt/detekt.yml` when present. |
| `dev.jorisjonkers.ktlint` | Ktlint with Android mode disabled and configurable generated/build output filters. |
| `dev.jorisjonkers.spring` | Kotlin conventions, Kotlin Spring, Spring Boot, Spring dependency management, configurable BOMs, and standard Spring service dependencies. |
| `dev.jorisjonkers.testing` | Java, Jacoco, test logging, a configurable integration-test source set/task, aggregate or split coverage reports, and configurable LINE coverage gates. |
| `dev.jorisjonkers.test-logging` | Verbose test logging and a suite-level result summary. |
| `dev.jorisjonkers.jooq-codegen` | A `generateJooq` task using jOOQ DDLDatabase against Flyway SQL migrations, plus generated source wiring. |

## Consumption

Each convention plugin publishes a Gradle plugin marker and an implementation
Maven module:

| Plugin ID | Implementation Maven coordinate |
| --- | --- |
| `dev.jorisjonkers.kotlin` | `dev.jorisjonkers:gradle-conventions-kotlin:<version>` |
| `dev.jorisjonkers.detekt` | `dev.jorisjonkers:gradle-conventions-detekt:<version>` |
| `dev.jorisjonkers.ktlint` | `dev.jorisjonkers:gradle-conventions-ktlint:<version>` |
| `dev.jorisjonkers.spring` | `dev.jorisjonkers:gradle-conventions-spring:<version>` |
| `dev.jorisjonkers.testing` | `dev.jorisjonkers:gradle-conventions-testing:<version>` |
| `dev.jorisjonkers.test-logging` | `dev.jorisjonkers:gradle-conventions-test-logging:<version>` |
| `dev.jorisjonkers.jooq-codegen` | `dev.jorisjonkers:gradle-conventions-jooq-codegen:<version>` |

The aggregate module `dev.jorisjonkers:gradle-conventions:<version>` is also
published for projects that intentionally want the full set.

Add the GitHub Packages Maven repository to the consuming repo's
`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = "JorisJonkersDevGradleConventions"
            url = uri("https://maven.pkg.github.com/JorisJonkers-dev/gradle-conventions")
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

Then apply the needed plugins in `build.gradle.kts` (version is the
`gradle-conventions` release, e.g. `0.4.0`):

```kotlin
plugins {
    id("dev.jorisjonkers.kotlin") version "0.4.0"
    id("dev.jorisjonkers.detekt") version "0.4.0"
    id("dev.jorisjonkers.ktlint") version "0.4.0"
}
```

GitHub Packages access requires credentials with package read access.

## Configuration

Defaults can be overridden with Gradle properties:

The configuration property and extension names retain the `extratoast` prefix
during the compatibility window.

```properties
extratoast.java.toolchain=21
extratoast.kotlin.allWarningsAsErrors=true
extratoast.kotlin.resolveAllDependencies=false
extratoast.detekt.config=config/detekt/detekt.yml
extratoast.detekt.requiredConfigFile=false
extratoast.ktlint.includes=
extratoast.ktlint.excludes=**/generated/**,**/build/**
extratoast.spring.bootBomVersion=
extratoast.spring.modulithBomVersion=1.3.2
extratoast.spring.testcontainersBomVersion=2.0.5
extratoast.spring.jacksonBomVersion=
extratoast.spring.additionalBoms=
extratoast.spring.jooqVersion=3.21.4
extratoast.spring.standardDependenciesEnabled=true
extratoast.spring.testDependenciesEnabled=true
extratoast.testing.integrationTestSourceSet=integrationTest
extratoast.testing.integrationTestTask=integrationTest
extratoast.testing.testExcludedTags=
extratoast.testing.integrationTestIncludedTags=integration
extratoast.testing.integrationTestExcludedTags=
extratoast.testing.coverageExclusions=
extratoast.testing.aggregateCoverageMinimum=0.80
extratoast.testing.unitCoverageMinimum=0.80
extratoast.testing.integrationCoverageMinimum=0.80
extratoast.testing.separateIntegrationCoverage=false
extratoast.testing.checkDependsOnCoverage=true
extratoast.jooq.schema=public
extratoast.jooq.package=dev.jorisjonkers.jooq.generated
extratoast.jooq.migrationLocations=filesystem:src/main/resources/db/migration
extratoast.jooq.outputDirectory=generated/jooq
```

The Kotlin, Spring, Testing, Detekt, Ktlint, and jOOQ plugins also expose
matching extensions for per-project build script configuration. Source-set and
task names for the testing convention are read during plugin application, so
non-default names should be supplied as Gradle properties.

```kotlin
extratoastKotlin {
    javaToolchain.set(25)
    allWarningsAsErrors.set(false)
    registerResolveAllDependencies.set(true)
}

extratoastSpring {
    springBootBomVersion.set("4.0.3")
    jacksonBomVersion.set("3.1.0")
    additionalBoms.set(listOf("org.junit:junit-bom:5.11.4"))
}

extratoastTesting {
    integrationTestIncludedTags.set(emptyList())
    integrationTestExcludedTags.set(listOf("system", "brevo-live", "discord-live"))
    separateIntegrationCoverage.set(true)
    unitCoverageMinimum.set("0.40".toBigDecimal())
    integrationCoverageMinimum.set("0.40".toBigDecimal())
}

extratoastDetekt {
    configPath.set("config/detekt/detekt.yml")
    requiredConfigFile.set(false)
}

extratoastKtlint {
    excludePatterns.add("**/generated-src/**")
}
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
