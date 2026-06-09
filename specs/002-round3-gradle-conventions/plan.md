# Implementation Plan: Round 3 configurable Gradle conventions

**Branch**: `002-round3-gradle-conventions` | **Date**: 2026-06-09 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-round3-gradle-conventions/spec.md`

## Summary

Expose configuration on the existing published convention plugins instead of
copying website build-logic into this repo verbatim. Keep current defaults for
Java 21, Spring Boot 4.0.6 plugin compatibility, 80% aggregate JaCoCo coverage,
Detekt/Ktlint filtering, and plugin ids while adding extension properties for
the Java 25/Spring Boot 4/testing split use case.

## Technical Context

**Language/Version**: Kotlin DSL precompiled script plugins, Java 21 CI runtime  
**Primary Dependencies**: Gradle Kotlin DSL, Kotlin Gradle plugin, Spring Boot
Gradle plugin, io.spring.dependency-management, JaCoCo, Detekt, Ktlint  
**Storage**: N/A  
**Testing**: JUnit 5 + Gradle TestKit in `aggregate/src/test/java`  
**Target Platform**: JVM Gradle consumer builds  
**Project Type**: Gradle plugin multi-module build  
**Performance Goals**: No extra tasks or dependency resolution unless opted in  
**Constraints**: No networked builds in this sandbox; CI validates externally  
**Scale/Scope**: Existing seven convention plugin modules plus aggregate tests

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] No attribution is introduced in files, comments, commit text, or PR text
- [x] Claude/Codex parity is preserved for any agent-facing behavior
- [x] Rendered artifacts are updated by the owning renderer when source changes require it
- [x] Small stacked PR boundary is clear and unrelated cleanup is excluded
- [x] Verification command is identified for each touched area

## Project Structure

### Documentation

```text
specs/002-round3-gradle-conventions/
|-- plan.md
|-- spec.md
`-- tasks.md
```

### Source Code

```text
plugins/kotlin/src/main/kotlin/dev.extratoast.kotlin.gradle.kts
plugins/spring/src/main/kotlin/dev.extratoast.spring.gradle.kts
plugins/testing/src/main/kotlin/dev.extratoast.testing.gradle.kts
plugins/detekt/src/main/kotlin/dev.extratoast.detekt.gradle.kts
plugins/ktlint/src/main/kotlin/dev.extratoast.ktlint.gradle.kts
aggregate/src/test/java/ConventionPluginSmokeTest.java
README.md
```

**Structure Decision**: Keep each feature in its existing plugin module and
cover cross-plugin behavior from the aggregate TestKit suite, matching the
current repository layout.

## Phase 0: Outline & Research

1. Compare current plugin defaults with website build-logic source paths.
2. Identify defaults that must remain stable for current consumers.
3. Identify new extension and Gradle-property inputs needed for Java 25,
   Spring Boot 4 BOMs, integration coverage, lint config paths, and
   `resolveAllDependencies`.

**Output**: Research is embedded in this plan and tasks because the brief is
implementation-oriented and the target surface is small.

## Phase 1: Design & Contracts

1. Add convention extension properties using Gradle `Property` and
   `ListProperty` APIs.
2. Preserve existing Gradle property names and add namespaced properties for new
   options.
3. Add TestKit assertions for task registration and generated build model
   behavior.
4. Re-run Constitution Check.

**Output**: Config contracts are the extension names and Gradle properties
documented in `README.md`.

## Phase 2: Task Planning Approach

Use `tasks.md` as the ordered checklist for spec creation, implementation,
tests, documentation, and verification.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| --- | --- | --- |
| N/A | N/A | N/A |

## Progress Tracking

**Phase Status**:

- [x] Phase 0: Research complete
- [x] Phase 1: Design complete
- [x] Phase 2: Task planning approach complete

**Gate Status**:

- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
