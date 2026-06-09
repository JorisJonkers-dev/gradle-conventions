# Tasks: Round 3 configurable Gradle conventions

**Input**: Design documents from `/specs/002-round3-gradle-conventions/`
**Prerequisites**: plan.md, spec.md

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with other tasks because it touches different files
- **[Story]**: User story label, for example US1, US2, US3
- Include exact file paths in descriptions

## Phase 1: Setup

- [x] T001 Create `/specs/002-round3-gradle-conventions/spec.md`, `plan.md`, and `tasks.md`
- [x] T002 Identify validation command: `./gradlew test jacocoTestCoverageVerification` for CI, with sandbox note that networked Gradle execution is not allowed here

## Phase 2: Foundational

- [x] T003 Add extension/property configuration to existing plugin modules
- [x] T004 Add aggregate TestKit helpers for new configurable behavior

## Phase 3: User Story 1 (Priority: P1)

**Goal**: Consumers can configure Java toolchain behavior and optional cache warming.

**Independent Test**: Apply `dev.extratoast.kotlin`, set Java 25 and
`registerResolveAllDependencies`, and assert task/toolchain configuration.

- [x] T005 [US1] Update `plugins/kotlin/src/main/kotlin/dev.extratoast.kotlin.gradle.kts`
- [x] T006 [US1] Update `aggregate/src/test/java/ConventionPluginSmokeTest.java`

## Phase 4: User Story 2 (Priority: P1)

**Goal**: Consumers can override Spring BOM/dependency inputs without forking.

**Independent Test**: Apply `dev.extratoast.spring`, configure BOM versions, and
assert dependency-management output contains the requested versions.

- [x] T007 [US2] Update `plugins/spring/src/main/kotlin/dev.extratoast.spring.gradle.kts`
- [x] T008 [US2] Update `aggregate/src/test/java/ConventionPluginSmokeTest.java`

## Phase 5: User Story 3 (Priority: P1)

**Goal**: Consumers can configure integration source sets, test tags, coverage
exclusions, and independent coverage gates.

**Independent Test**: Apply `dev.extratoast.testing`, enable separate
integration coverage, and assert task registration and check wiring.

- [x] T009 [US3] Update `plugins/testing/src/main/kotlin/dev.extratoast.testing.gradle.kts`
- [x] T010 [US3] Update `aggregate/src/test/java/ConventionPluginSmokeTest.java`

## Phase 6: User Story 4 (Priority: P2)

**Goal**: Detekt and Ktlint defaults can be adopted by repos with alternate
config paths and generated-code layouts.

**Independent Test**: Apply the lint plugins with alternate path filters and
assert tasks configure without requiring website/personal-stack files.

- [x] T011 [P] [US4] Update `plugins/detekt/src/main/kotlin/dev.extratoast.detekt.gradle.kts`
- [x] T012 [P] [US4] Update `plugins/ktlint/src/main/kotlin/dev.extratoast.ktlint.gradle.kts`
- [x] T013 [US4] Update `aggregate/src/test/java/ConventionPluginSmokeTest.java`

## Phase 7: Polish

- [x] T014 Update `README.md` configuration docs
- [x] T015 Run allowed local checks or document why Gradle verification was not run in this sandbox
- [x] T016 Inspect `git diff --check` and final file diff

## Dependencies

- Setup before implementation
- Kotlin/Spring/testing changes before TestKit assertions that depend on them
- README after final configuration contract is stable
- Validation after all source and test changes

## Parallel Example

```text
T011 [P] [US4] Update detekt convention
T012 [P] [US4] Update ktlint convention
```
