# Feature Specification: Round 3 configurable Kotlin, Spring, and test conventions

## Overview
Extend the published `gradle-conventions` plugins so repos that currently carry
local build logic can adopt the shared conventions without inheriting
hardcoded Java, Spring, testing, lint, or cache-warming assumptions. The feature
keeps the existing plugin ids and module packaging while exposing configuration
for Java 21/25, Spring Boot BOM inputs, integration-test source sets, coverage
gates, Detekt/Ktlint paths, and optional dependency resolution.

## User Scenarios
- As a JVM service repo, I want the Kotlin convention to support Java 21 or 25
  from a property or extension, so that current LTS services and early-adopter
  services can share the same plugin.
- As a Spring service repo, I want Spring Boot, Spring Modulith,
  Testcontainers, Jackson, and other BOMs to be configurable, so that Spring
  Boot 3 and Spring Boot 4 consumers do not need forked build logic.
- As a test-heavy repo, I want integration-test source sets and JaCoCo gates to
  be configurable, so that unit and integration coverage can be enforced
  independently where needed.
- As a repo-template maintainer, I want Detekt and Ktlint defaults to point at
  conventional configurable paths, so generated repos do not need local plugin
  forks.

## Functional Requirements
- FR-1: `dev.extratoast.kotlin` supports configurable Java toolchains,
  compiler warning policy, and an optional `resolveAllDependencies` task.
- FR-2: `dev.extratoast.spring` keeps existing default dependencies but exposes
  configurable Spring/Testcontainers/Jackson/Modulith/jOOQ BOM or dependency
  inputs.
- FR-3: `dev.extratoast.testing` supports configurable integration-test source
  set/task naming, JUnit tag filters, generated-code exclusions, and
  unit/integration/aggregate coverage thresholds.
- FR-4: `dev.extratoast.detekt` uses a conventional Detekt path only when the
  file exists unless a consumer explicitly requires it.
- FR-5: `dev.extratoast.ktlint` exposes include/exclude path filters while
  preserving generated/build output exclusions.
- FR-6: Existing plugin ids, Maven module coordinates, and default behavior
  remain backward-compatible for current consumers.

## Success Criteria
- SC-1: A consumer can configure Java 25 and register
  `resolveAllDependencies` through `dev.extratoast.kotlin`.
- SC-2: A consumer can override Spring BOM versions without editing shared
  plugin code.
- SC-3: A consumer can run separate unit and integration JaCoCo report/gate
  tasks when configured, while the existing aggregate 80% default remains
  available.
- SC-4: Detekt and Ktlint conventions can be adopted by repos with missing or
  alternate config paths.
- SC-5: The aggregate TestKit suite covers the new configuration surfaces.

## Assumptions
- Spring Boot 4 consumers can use the same plugin artifact as Spring Boot 3
  consumers by overriding BOM/dependency inputs.
- `resolveAllDependencies` is an opt-in cache-warming task because it resolves
  configurations at execution time.
- Repo-template will generate the same Detekt default path:
  `config/detekt/detekt.yml`.

## Edge Cases
- Java 25 requires a matching Kotlin `JvmTarget`; unsupported JVM target values
  should fail during build configuration rather than silently compiling to the
  wrong bytecode level.
- A missing Detekt config file should not break adopters unless they set
  `requiredConfigFile`.
- TestKit and integration tests may not contribute to this repo's JaCoCo gate,
  so new behavior should be covered by direct aggregate tests.

## Key Entities
- `extratoastKotlin`: Kotlin/JVM convention extension.
- `extratoastSpring`: Spring dependency and BOM convention extension.
- `extratoastTesting`: test source-set and coverage convention extension.
- `extratoastDetekt`: Detekt config path convention extension.
- `extratoastKtlint`: Ktlint path filter convention extension.

## Out of Scope
- Extracting OpenAPI client build logic; that belongs in `openapi-client-gradle`.
- Migrating `/workspace/website` or `/workspace/personal-stack`.
- Changing publishing coordinates or introducing Gradle plugin-marker artifacts.
