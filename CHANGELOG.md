# Changelog

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

### Compatibility
- Existing SQLite data is preserved
- Existing players at or above old Fate milestones are marked as already claimed during migration, preventing duplicate rewards
- Default XP curve remains numerically identical to v0.1.0/v0.1.1 unless the server changes the new tuning values

## 0.1.1 — Trials of the Deep

Second Runebound development chapter. Miner progression now includes proof-of-mastery objectives and its first active ability.

### Added
- `Trial of Stone` with persistent natural-ore progress
- `Trial of the Deep` with deep ore, rare ore and Ancient Debris objectives
- Trial progress GUI via `/cdrjobs trials`
- New fantasy skill: `Runic Surge`
- `/cdrjobs ability` to activate Runic Surge
- Persistent ability cooldowns in SQLite
- Runic Surge Haste + configurable temporary Miner XP bonus
- Trial requirements for Runic Surge and Heart of the Mountain
- Trial and cooldown PlaceholderAPI values
- Automatic merging of new config/message defaults during upgrades

### Changed
- Miner skill GUI expanded to include Profession Trials
- Heart of the Mountain now requires completion of Trial of the Deep
- Natural ore mining is now the source of both XP and trial progression
- Player-placed ore remains excluded from XP and trial progress, including across restarts

### PlaceholderAPI
- `%cdrjobs_miner_trial_stone%`
- `%cdrjobs_miner_trial_deep%`
- `%cdrjobs_miner_total_ores%`
- `%cdrjobs_miner_deep_ores%`
- `%cdrjobs_miner_rare_ores%`
- `%cdrjobs_miner_ancient_debris%`
- `%cdrjobs_runic_surge_cooldown%`

## 0.1.0 — The Runeborn

First development chapter of CdrJobs.

### Added
- Runebound Delver (Miner) profession
- Levels 1–100 and fantasy rank titles
- Configurable ore XP
- Global Fate Essence milestones
- Path of Ascension GUI
- Stonewhisper, Runebreaker, Deepborn, Gemseeker, Echo of the Depth, Heart of the Mountain
- Persistent SQLite player progress
- Persistent placed-ore anti XP exploit tracking
- `/cdrjobs`, `/cdrjobs stats`, `/cdrjobs skills`
- Admin XP, level, reset and reload commands
- PlaceholderAPI placeholders
- GitHub Actions Maven build workflow
