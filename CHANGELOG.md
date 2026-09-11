# Changelog

## 1.0.4 — Hunter QoL & Safety
- Replaced ambiguous unlisted-mob behavior with `hunter.mob-filter.mode: ALL|WHITELIST`.
- Added optional Hunter mob blacklist; blacklist always overrides whitelist/ALL mode.
- Added separate rewarded `mob_kills` and `pvp_kills` counters.
- Added PlaceholderAPI: `%cdrjobs_hunter_mob_kills%` and `%cdrjobs_hunter_pvp_kills%`.
- Added `/cdrjobsadmin hunterdebug <player>` with the latest Hunter reward decision and safety state.
- Added PvP same-IP block option, minimum victim online time, minimum victim total playtime, same-victim cooldown and rolling kill-streak reward limit.
- Mob and PvP actionbars are now clearly distinguished.
- Existing spawner/breeding exclusions remain active.
- No database schema change; schema remains `9`.

## 1.0.3 — Hunter Configurability Hotfix
- Added configurable PvP Hunter XP and Trial participation.
- Added persistent same-victim PvP reward cooldown.
- Added configurable allow-unlisted/fallback mob behavior.

## 1.0.2 — Hunter Progression Hotfix
- Hunter mob kills can progress even when the mob is not explicitly listed, using fallback XP.
- Player kills were separated from mob handling.

## 1.0.1 — Stability Patch
- Added `/cdrjobsadmin inspect <player>`.
- Hardened admin numeric validation, reload behavior and config checks.
- Added XP overflow protection and schema health diagnostics.

## 1.0.0 — Five Paths
- First standalone production baseline.
- Five professions: Miner, Farmer, Hunter, Lumberjack and Fisher.
- Global Fate Essence, profession skill trees, trials, active abilities and awakened paths.
- SQLite persistence, anti-exploit protections, public API/events and PlaceholderAPI integration.

## 0.9.0 — Adventurer API
- Public CdrJobsAPI, profession events, schema marker, diagnostics and production test matrix.

## 0.5.0 — Call of the Deep
- Tidebound Angler (Fisher), anti-AFK, trials, Ocean's Call.

## 0.4.0 — Oath of the Ancient Grove
- Ironbark Warden (Lumberjack), placed-log protection, trials, Grove Rhythm.

## 0.3.0 — Blood Moon
- Bloodfang Stalker (Hunter), spawn-origin protection, trials, Crimson Hunt.

## 0.2.0 — Verdant Awakening
- Verdant Keeper (Farmer), harvest anti-spam, Rootbound, trials, Verdant Bloom.

## 0.1.x — Runebound Chapters
- Runebound Delver, Fate Essence, skill tree, trials, Runic Surge and exploit hardening.
