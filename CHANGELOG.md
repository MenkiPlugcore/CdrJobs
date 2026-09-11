# Changelog

## 1.6.0 — Public API v2 & GUI Refresh
- Raised `CdrJobsAPI.API_VERSION` from `1` to `2` while retaining all v1 read/progression methods.
- Added Bukkit `ServicesManager` publication for `CdrJobsAPI`; `CdrJobsPlugin#getApi()` remains supported.
- Added immutable API snapshots for player/profile state, professions, skills, Trial/statistics data, ability cooldowns, Mastery, Contracts, Fate Resonance and cached leaderboards.
- Added tie-safe `getHighestProfessions(UUID)` while keeping the original single-result `getHighestProfession(UUID)` method.
- Added public `SkillUpgradeEvent`, `ProfessionAwakeningEvent`, `TrialCompleteEvent`, `MasteryTierUpEvent`, `HunterPvpRewardEvent`, `ContractCompleteEvent`, `ContractClaimEvent` and `RebirthEvent`.
- Trial events fire only on incomplete-to-complete transitions; Hunter PvP reward events fire only after existing anti-farm gates accept a kill.
- Added aesthetic GUI theming with profession-specific stained-glass framing while preserving all functional item slots/PDC actions.
- Added inventory drag protection for CdrJobs/Rebirth GUI top inventories.
- Replaced silent GUI action exception swallowing with contextual warning logs and a safe player-facing error message.
- Hardened Contract claims with XP/Fate delivery checkpoints so ordinary retry after a component failure does not repay already checkpointed components.
- Hardened Contract progress percentage math against long overflow.
- Added startup pruning for expired `reward_locations` rows to prevent indefinite cooldown-location table growth.
- Added `PRAGMA busy_timeout=3000` to ProfessionStore's SQLite connection.
- Updated API/testing documentation and release matrix for current systems.
- No database schema migration; schema remains `9`.

## 1.5.0 — Fate Resonance
- Added derived cross-profession Fate Resonance for all ten unique Five Paths pairings.
- Resonance unlocks dynamically from canonical profession levels; no separate player resonance state is stored.
- Added two progression states: `RESONANT` when both paired professions meet the configured level requirement, and `HARMONIZED` when both also meet the configured Mastery tier requirement.
- Added `/cdrjobs resonance [id]` / `/cdrjobs fateresonance [id]` with progress/status details.
- Added configurable resonance definitions with custom name, title, badge, profession pair, minimum level, and harmonized Mastery tier.
- Added global and per-resonance PlaceholderAPI values for unlocked/harmonized state, score, display, title and badge.
- Added `/cdrjobsadmin resonancedebug <player>` plus Resonance state in diagnose, inspect and debug export.
- Added Resonance summary to `/cdrjobs stats`.
- Added configuration validation for invalid Jobs, duplicate pairings, level range, and Mastery thresholds.
- Resonance is intentionally status/QoL oriented; v1.5.0 adds no damage, gathering, drop-rate, or economy multiplier.
- No database schema migration; schema remains `9`.

## 1.4.0 — Profession Contracts
- Added deterministic per-player Daily and Weekly Profession Contracts.
- Contract progress is driven by `ProfessionActionEvent`, so only profession activity already accepted by anti-exploit logic counts.
- Added `/cdrjobs contracts`, `/cdrjobs contracts claim <daily|weekly>`, and `/cdrjobs contracts reroll <daily|weekly>`.
- Added configurable custom contract definitions with cadence, Job, target, XP reward, and Fate Essence reward.
- Added configurable timezone, reroll limits, and optional progress actionbar.
- Daily/Weekly assignment remains stable across relog/restart and rotates only when the configured cycle changes or the player uses a reroll.
- Reroll resets progress and is blocked after completion/claim; default limit is one reroll per cadence per cycle.
- Added Contract PlaceholderAPI values for id, name, Job, progress, target, percentage, completion/claim state, rewards, and rerolls left.
- Default Daily rewards use profession XP only; default Weekly rewards add one Fate Essence to preserve Fate scarcity.
- Contract state reuses `profession_counters`; no schema migration, schema remains `9`.

## 1.3.0 — Profession Mastery
- Added post-Level-100 Profession Mastery for all five Jobs.
- Profession XP earned at max level is now routed into persistent Mastery XP instead of being discarded.
- Added configurable Mastery tier cap and XP curve with default Mastery I–X progression.
- Added cosmetic prestige titles/badges without combat, economy, or gathering power buffs.
- Added `/cdrjobs mastery [job]` / `/cdrjobs prestige [job]`.
- Added `/cdrjobs top mastery <job>` and `/cdrjobs top mastery-total` cached leaderboards.
- Added Mastery PlaceholderAPI values per profession plus profile-wide total tiers/XP.
- Added `/cdrjobsadmin addmasteryxp` and `/cdrjobsadmin setmastery` for testing/administration.
- Admin inspect/export now surface Mastery state.
- Rebirth leaves Mastery untouched; profession reset/full reset removes it through existing profession counter cleanup.
- Mastery uses the existing `profession_counters` table with one canonical `mastery_total_xp` metric per Job; schema remains `9`.

## 1.2.0 — Rite of Rebirth
- Added `/cdrjobs rebirth [job]` and `/cdrjobs respec [job]` with a dedicated two-step confirmation GUI.
- Added per-profession skill-tree respec without resetting Job level, XP, Trials, lifetime statistics, Fate milestone claims, or active-ability cooldowns.
- Added deterministic Fate Essence refunds calculated from current purchased skill ranks and each skill's configured code cost.
- Added configurable refund percentage and persistent per-profession Rebirth cooldown.
- Added configurable fee modes: `NONE`, `FATE`, or optional `VAULT` economy.
- Vault integration is soft/reflective; CdrJobs does not require Vault unless `rite-of-rebirth.fee.mode: VAULT` is selected.
- Rebirth skill deletion, Fate refund/fee, and Rebirth cooldown are committed transactionally in SQLite to prevent double-click/restart refund exploits.
- Added `/cdrjobsadmin forcerespec <player> <job>` / `forcerebirth` for admin bypass of fee and cooldown while preserving the normal refund.
- `/cdrjobsadmin inspect` and debug export now surface Rebirth cooldown state.
- No database schema migration; schema remains `9`.

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
