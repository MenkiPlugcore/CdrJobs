# Changelog

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

## 0.9.0 — Adventurer API
- Public CdrJobsAPI
- Profession action / XP / level-up Bukkit events
- Generic all-job PlaceholderAPI progression placeholders
- SQLite schema marker
- Config validation
- Admin diagnostics
- Production test matrix and API docs
- CI `clean verify` and version-independent JAR artifact upload

## 0.5.0 — Call of the Deep
- Tidebound Angler (Fisher), anti-AFK, trials, Ocean's Call

## 0.4.0 — Oath of the Ancient Grove
- Ironbark Warden (Lumberjack), placed-log protection, trials, Grove Rhythm

## 0.3.0 — Blood Moon
- Bloodfang Stalker (Hunter), spawn-origin protection, trials, Crimson Hunt

## 0.2.0 — Verdant Awakening
- Verdant Keeper (Farmer), harvest anti-spam, Rootbound, trials, Verdant Bloom

## 0.1.x — Runebound Chapters
- Runebound Delver, Fate Essence, skill tree, trials, Runic Surge, persistent exploit hardening
