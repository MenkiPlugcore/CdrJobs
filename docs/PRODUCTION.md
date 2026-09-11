# Production Notes

## Requirements
- Java 21
- Paper-compatible server using the 1.21 API family
- PlaceholderAPI is optional

## Install
1. Stop the server.
2. Put `CdrJobs-1.0.0.jar` in `plugins/`.
3. Start the server.
4. Run `/cdrjobsadmin diagnose`.
5. Run the smoke-test matrix in `docs/TESTING.md`.

## Upgrading from development versions
Do not delete `plugins/CdrJobs/cdrjobs.db`. CdrJobs creates missing tables/metadata without intentionally wiping existing progression. Back up the plugin folder before any production upgrade.

## Performance note
CdrJobs uses SQLite WAL and keeps its data model lightweight. Profession progression writes happen during valid gameplay activities; large public servers should still profile the plugin under their real player count and plugin stack before declaring capacity targets.

## Rollback
Back up the entire `plugins/CdrJobs/` directory before update. To roll back safely, restore both the previous JAR and its matching database/config backup.
