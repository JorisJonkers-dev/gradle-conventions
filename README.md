# gradle-conventions

Gradle convention plugins for JorisJonkers-dev JVM repositories, published to
GitHub Packages.

## What It Is

`gradle-conventions` provides versioned build conventions for Kotlin, Spring,
detekt, ktlint, testing, test logging, and jOOQ code generation. The public
plugin IDs use the `dev.jorisjonkers.*` namespace while configuration
properties and extension names keep the `extratoast` prefix during the
compatibility window.

## Local Use

```bash
./gradlew test
./gradlew build jacocoTestCoverageVerification
```

## Plugins

| Plugin ID | Implementation module |
| --- | --- |
| `dev.jorisjonkers.kotlin` | `dev.jorisjonkers:gradle-conventions-kotlin:<version>` |
| `dev.jorisjonkers.detekt` | `dev.jorisjonkers:gradle-conventions-detekt:<version>` |
| `dev.jorisjonkers.ktlint` | `dev.jorisjonkers:gradle-conventions-ktlint:<version>` |
| `dev.jorisjonkers.spring` | `dev.jorisjonkers:gradle-conventions-spring:<version>` |
| `dev.jorisjonkers.testing` | `dev.jorisjonkers:gradle-conventions-testing:<version>` |
| `dev.jorisjonkers.test-logging` | `dev.jorisjonkers:gradle-conventions-test-logging:<version>` |
| `dev.jorisjonkers.jooq-codegen` | `dev.jorisjonkers:gradle-conventions-jooq-codegen:<version>` |

The aggregate module `dev.jorisjonkers:gradle-conventions:<version>` is also
published for projects that intentionally consume the complete convention set.

## Coverage

`dev.jorisjonkers.testing` gates line coverage with JaCoCo. The gate does not count code no test
should exist for:

- **Generated code.** Anything under a `generated` or `jooq` package, and every class or method
  carrying an annotation whose simple name contains `Generated`.
- **What the Kotlin compiler writes for a data class.** `equals`, `hashCode`, `toString`, `copy`
  and `componentN` have no source lines, and JaCoCo ignores them. A data class is covered by
  constructing it and reading its properties, as any test using it already does.

So never write a test that only calls a data class's generated members: it cannot raise the gate.
The estate testing standards say the same
([docs/testing-standards.md](https://github.com/JorisJonkers-dev/workspace/blob/main/docs/testing-standards.md)).
`ConventionPluginSmokeTest` proves both: a data class passes a 100% gate without such tests, and
one untested line beside it still fails that gate.

## Consumption

Add the GitHub Packages Maven repository to the consuming repository's
`settings.gradle.kts`, then apply the required plugins with an exact released
version:

```kotlin
plugins {
    id("dev.jorisjonkers.kotlin") version "0.5.0"
    id("dev.jorisjonkers.testing") version "0.5.0"
}
```

GitHub Packages access requires credentials with package read access.

## Links

- [Organization profile](https://github.com/JorisJonkers-dev)
- [Security policy](https://github.com/JorisJonkers-dev/.github/security/policy)
- [Changelog](./CHANGELOG.md)
- [License](./LICENSE)

Copyright (c) Joris Jonkers. Source available for viewing only; use, copying,
modification, redistribution, deployment, or reuse is not licensed. See
[LICENSE](./LICENSE).
