---
name: config-migration
description: Migrates configuration schema in the configuration module. Use this skill when explicitly asked to migrate config, update config schema, or move configuration files between versions.
---

# Config Migration

1. Backup configuration files in `configuration/` module
2. Review schema changes in affected config classes
3. Dry run: compile and verify no compilation errors
4. Apply after user confirmation
5. Verify: run application and test affected features
6. Rollback from backup if issues occur