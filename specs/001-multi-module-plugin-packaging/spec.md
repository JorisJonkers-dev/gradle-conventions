# Feature Specification: Multi-module per-plugin packaging

## Overview
`gradle-conventions` currently publishes the convention plugins as a single
consolidated jar (`dev.jorisjonkers:gradle-conventions`). Consumers must take the
whole bundle, and an earlier attempt that relied on Gradle plugin-marker
artifacts produced ugly doubled package names
(`dev.jorisjonkers.kotlin.dev.jorisjonkers.kotlin.gradle.plugin`). This feature
restructures the project so each convention plugin is published as its own small
Maven module with a short, clean coordinate, individually consumable, while
still offering a one-line way to pull the full set — and without publishing any
plugin-marker artifacts.

## User Scenarios
- As a consuming repo, I want to depend on only the conventions I use (e.g. just
  `kotlin` + `ktlint`), so my build doesn't drag in jOOQ/Spring plugin code I
  don't need.
- As a consuming repo, I want one aggregate coordinate to pull every convention
  plugin at once, so a simple project can opt into all of them with a single
  entry.
- As a maintainer, I want the published package list to read cleanly
  (`gradle-conventions-kotlin`, `gradle-conventions-detekt`, …), so the org
  Packages page is not cluttered with doubled `*.gradle.plugin` names.

## Functional Requirements
- FR-1: Each convention plugin is published as a separate Maven module with a
  short coordinate of the form `dev.jorisjonkers:gradle-conventions-<name>` for
  names: `kotlin`, `detekt`, `ktlint`, `spring`, `testing`, `test-logging`,
  `jooq-codegen`.
- FR-2: An aggregate module `dev.jorisjonkers:gradle-conventions` is published that
  transitively brings in all per-plugin modules, so a consumer can opt into the
  full set with one coordinate.
- FR-3: No Gradle plugin-marker artifacts (`*.gradle.plugin`) are published from
  any module.
- FR-4: Each per-plugin module continues to expose the same plugin id
  (`dev.jorisjonkers.<name>`) and the same behaviour/opt-in extensions it has
  today.
- FR-5: Consumers resolve plugin ids to the per-plugin modules via
  `pluginManagement.resolutionStrategy.eachPlugin` (documented in the README),
  not via markers.
- FR-6: A single version applies across all modules, sourced from the release
  manifest, and all modules publish at that one version.

## Success Criteria
- SC-1: After a release, the org Packages page shows exactly the per-plugin
  modules plus the aggregate — and zero `*.gradle.plugin` entries.
- SC-2: A consumer can apply `id("dev.jorisjonkers.kotlin")` having declared only
  the `eachPlugin` mapping (no marker repo, no full-bundle dependency) and the
  build resolves.
- SC-3: A consumer can depend on a strict subset of plugins and the unused
  plugin code is not on its classpath.

## Assumptions
- Consumers configure GitHub Packages credentials in `pluginManagement` (already
  the case for the single-jar approach).
- The aggregate module carries no code, only dependencies on the per-plugin
  modules.

## Edge Cases
- `jooq-codegen` and `spring` carry heavier transitive dependencies; pulling
  only `kotlin` must not transitively drag those in.
- A consumer applying the aggregate then also pinning one plugin individually
  must resolve to a single consistent version (FR-6).

## Key Entities
- Per-plugin module: groupId `dev.jorisjonkers`, artifactId
  `gradle-conventions-<name>`, contains one precompiled script plugin.
- Aggregate module: `dev.jorisjonkers:gradle-conventions`, depends on all
  per-plugin modules.

## Out of Scope
- Personal-stack adoption of these modules (separate effort).
- Changing plugin behaviour or version bumps of the underlying tools.
- Publishing to any registry other than GitHub Packages.
