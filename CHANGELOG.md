# Changelog

## 0.2.0 — Verdant Awakening

Second standalone profession: Verdant Keeper (Farmer).

### Added
- Farmer Lv.1–100 and fantasy rank titles
- Mature crop XP table with global Farmer multiplier
- Persistent per-location harvest reward cooldown to reduce bone-meal spam
- Farmer Path of Ascension
- Greenblood, Rootbound, Sunpetal, Verdant Bloom, Blessing of Gaia, Spirit of the Grove, Verdant Dominion
- Rootbound auto-replant for supported crops
- Trial of Seed and Trial of Gaia
- Verdant Bloom active ability with persistent cooldown
- Generic profession counters and flags stored in the existing SQLite database
- Generic admin XP/level commands with a job selector
- Farmer level/rank PlaceholderAPI values

### Changed
- Main profession menu now exposes both Miner and Farmer
- Level-up message now uses the actual profession name instead of hardcoded Miner text
- Admin testing commands can target any job id

## 0.1.2 — Runebound Tempering

Miner stability and balance pass before the next profession chapter.

### Added
- Persistent one-time Fate Essence milestone claim ledger
- Automatic migration/backfill for existing Miner levels
- Configurable profession XP curve (`base`, `linear`, `quadratic`)
- Global Miner XP multiplier for easy server balancing
- Piston-aware placed-ore exploit tracking
- Explosion cleanup for tracked placed ores
- Next Fate Essence milestone shown in `/cdrjobs stats`

### Fixed
- Fate Essence milestones can no longer be awarded repeatedly by lowering/re-raising a job level
- Player-placed ores remain tagged after piston movement
- Exploded placed ores no longer leave stale anti-exploit coordinates behind
- Max-level actionbar now respects configured `settings.max-level` instead of displaying a hardcoded 100

## 0.1.1 — Trials of the Deep

### Added
- Trial of Stone and Trial of the Deep
- Runic Surge active ability
- Persistent cooldowns and trial progress
- Trial-gated high-tier Miner skills

## 0.1.0 — The Runeborn

### Added
- Runebound Delver (Miner)
- Levels 1–100, Fate Essence and Path of Ascension
- SQLite persistence
- Persistent placed-ore exploit tracking
- PlaceholderAPI
- GitHub Actions build workflow
