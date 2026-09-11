# Changelog

## 1.0.3 — Hunter Configurability Hotfix
- Added `hunter.allow-unlisted-mobs` to switch between fallback mode and strict mob whitelist mode.
- Added configurable Hunter PvP progression with `hunter.pvp.enabled` and `hunter.pvp.xp`.
- Added `hunter.pvp.count-toward-trials`.
- Added persistent same-victim PvP cooldown to reduce kill farming.
- Player kills can now grant Hunter XP when PvP progression is enabled.
- Existing mob XP table, fallback XP, spawner exclusion and breeding exclusion remain supported.
- No database schema change; existing cooldown storage is reused.

## 1.0.2 — Hunter Progression Hotfix
- Hunter progression is now mob-based by default.
- Any non-player living mob can grant Hunter XP.
- Mobs listed in `hunter.xp` keep their custom XP values.
- Unlisted mobs use configurable `hunter.fallback-mob-xp` (default `3`).
- Existing spawner and breeding anti-exploit exclusions remain active.
- Armor Stands are excluded from Hunter progression.
- Fixed the practical issue where killing an unlisted mob resulted in no Hunter XP at all.
- No database schema change; schema remains `9`.

## 1.0.1 — Stability Patch
- Added `/cdrjobsadmin inspect <player>` for five-profession progression inspection.
- Hardened admin command syntax and numeric validation.
- `addxp` now rejects zero/negative values.
- `setlevel` now rejects values outside the configured level range.
- Added XP overflow protection and max-level no-op behavior.
- `/cdrjobsadmin diagnose` now reports expected schema and schema health.
- `/cdrjobsadmin reload` now persists merged config defaults before refreshing XP maps.
- Expanded config validation warnings for invalid curves, multipliers, Fate milestones and anti-exploit thresholds.
- Synced production docs and roadmap.
- No database schema change; schema remains `9`.

## 1.0.0 — Five Paths
- First standalone production baseline.
- Five professions: Miner, Farmer, Hunter, Lumberjack and Fisher.
- Global Fate Essence, profession skill trees, trials, active abilities and awakened paths.
- SQLite WAL persistence and profession-specific anti-exploit protections.
- Public CdrJobs API v1 and Bukkit integration events.
- PlaceholderAPI integration, admin diagnostics and production test matrix.
- CI release gate with `mvn clean verify`.
