# Changelog

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
