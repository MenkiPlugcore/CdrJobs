# Changelog

## 1.1.1 — Leaderboard & Profile QoL
- Added `/cdrjobs top <job>` profession level/XP rankings.
- Added `/cdrjobs top total` for combined Five Paths level.
- Added `/cdrjobs top pvp` for rewarded Hunter PvP kills.
- Added `/cdrjobs top activity <job>` for primary profession activity rankings.
- Added dedicated read-side SQLite leaderboard connection with configurable cache TTL and result limit.
- Total-level ranking treats missing profession rows as default level 1 so players are not undercounted before every Job row exists.
- Leaderboard cache is cleared on `/cdrjobsadmin reload` and closed cleanly on plugin disable.
- No database schema migration; schema remains `9`.

## 1.1.0 — Adventurer Profile
- Added `/cdrjobs profile [player]` with a dedicated Five Paths profile GUI.
- Added a Profile button to the main CdrJobs menu.
- Profile shows total profession level, highest profession, Fate Essence, Trial completion count, unlocked skill count, awakened paths and per-profession activity.
- Added short-lived profile snapshot caching to reduce repeated SQLite reads from GUI/PlaceholderAPI usage.
- Added Profile PlaceholderAPI values for total level, highest profession/level, completed Trials, unlocked skills and awakened paths.
- Fixed legacy Miner Trial placeholders so `miner_trial_*` is resolved before generic `miner_*` parsing.
- No database schema migration; schema remains `9`.

## 1.0.5 — Admin & Debug Tools
- Added granular Fate Essence, profession reset, Trial reset, cooldown reset, inspect and export tools.
- Preserved Fate milestone claims on profession reset to prevent duplicate Essence.
- Hardened Trial reset so non-Miner lifetime statistics survive while Trial progress restarts from zero.
- No database schema migration; schema remains `9`.

## 1.0.4 — Hunter QoL & Safety
- Added configurable Hunter mob filters/blacklist and layered PvP anti-farm controls.
- Added separate rewarded mob/PvP kill counters/placeholders and Hunter debug command.

## 1.0.3 — Hunter Configurability Hotfix
- Added configurable PvP Hunter XP and Trial participation.
- Added persistent same-victim PvP reward cooldown.

## 1.0.2 — Hunter Progression Hotfix
- Hunter mob kills can progress even when the mob is not explicitly listed, using fallback XP.

## 1.0.1 — Stability Patch
- Added player inspect, hardened admin input/reload/config validation and XP overflow protection.

## 1.0.0 — Five Paths
- First standalone production baseline with Miner, Farmer, Hunter, Lumberjack and Fisher.
