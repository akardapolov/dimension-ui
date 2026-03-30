---
name: release-checklist
description: Verifies release readiness before cutting a release. Use this skill when preparing a release, checking pre-release conditions, or when asked about release preparation.
---

# Release Checklist

## Pre-flight (All must pass)

- [ ] `mvn clean package` succeeds
- [ ] `mvn test` passes on all modules
- [ ] Version `${revision}` updated in root `pom.xml`
- [ ] README.md and README-RU.md updated with changes
- [ ] Executable jar created in `desktop/target/`
- [ ] Dimension-DI, Dimension-DB, and Dimension-TT dependencies installed