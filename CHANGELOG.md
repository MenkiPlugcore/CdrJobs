# Changelog

## 1.0.5 — Admin & Debug Tools
- Added `/cdrjobsadmin addessence <player> <amount>`.
- Added `/cdrjobsadmin setessence <player> <amount>`.
- Added `/cdrjobsadmin resetjob <player> <job>` to reset one profession without deleting global Fate Essence or other jobs.
- Added `/cdrjobsadmin resettrial <player> <job>`.
- Added `/cdrjobsadmin resetcooldown <player> <job>`.
- Expanded `/cdrjobsadmin inspect <player>` with counters, trial flags, Miner trial state, all skill ranks and cooldowns.
- Added `/cdrjobsadmin export <player> [file|console|both]` for support/debug snapshots.
- Debug exports include version, schema, all five profession levels/XP/ranks, Fate Essence, counters, flags, skills, cooldowns and latest Hunter decision.
- No database schema migration; schema remains `9`.

## 1.0.4 — Hunter QoL & Safety
- Added `hunter.mob-filter.mode: ALL|WHITELIST` and optional mob blacklist.
- Added separate rewarded `mob_kills` and `pvp_kills` counters/placeholders.
- Added `/cdrjobsadmin hunterdebug <player>`.
- Added configurable same-IP protection, minimum victim online/playtime, same-victim cooldown and rolling PvP reward limit.
- Mob and PvP actionbars are now distinguished.

## 1.0.3 — Hunter Configurability Hotfix
- Added configurable PvP Hunter XP and Trial participation.
- Added persistent same-victim PvP reward cooldown.
- Added configurable unlisted/fallback mob behavior.

## 1.0.2 — Hunter Progression Hotfix
- Hunter mob kills can progress even when the mob is not explicitly listed, using fallback XP.

## 1.0.1 — Stability Patch
- Added player inspect, hardened admin input/reload/config validation and XP overflow protection.

## 1.0.0 — Five Paths
- First standalone production baseline with Miner, Farmer, Hunter, Lumberjack and Fisher.
- Fate Essence, skill trees, trials, active abilities, persistence, anti-exploit, public API/events and PlaceholderAPI.
