# Agents

Guidance for any coding agent working in this repository.

- Agent contract: [CLAUDE.md](CLAUDE.md), which points at [the estate contract](https://github.com/JorisJonkers-dev/workspace/blob/main/CLAUDE.md).
- What this repository holds: Gradle convention plugins for JVM repositories (`plugins/`, `aggregate/`), published to GitHub Packages. See [README.md](README.md) and [VERSIONING.md](VERSIONING.md).

## Build and test

```bash
./gradlew test
./gradlew build jacocoTestCoverageVerification
./gradlew assemble detektMain detektTest ktlintCheck validatePlugins
```

The first two are in the README; the last is the CI lint step (`.github/workflows/ci.yml`).
