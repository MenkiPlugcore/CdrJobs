# CdrJobs

**Choose Your Path, Shape Your Fate.**

Standalone fantasy profession progression for Paper 1.21.x.
Current candidate: `v1.6.0 — Public API v2 & GUI Refresh`.

## Five Paths
- ⛏ Runebound Delver — Miner
- 🌿 Verdant Keeper — Farmer
- ⚔ Bloodfang Stalker — Hunter
- 🪓 Ironbark Warden — Lumberjack
- 🎣 Tidebound Angler — Fisher

Each profession has Lv.1–100 progression, fantasy ranks, Path of Ascension, scarce global Fate Essence, Trials, active abilities, awakened paths, post-Lv.100 Mastery, repeatable Contracts, and cross-profession Fate Resonance.

## Player commands
- `/cdrjobs` — main GUI
- `/cdrjobs profile [player]`
- `/cdrjobs resonance [id]`
- `/cdrjobs contracts`
- `/cdrjobs contracts claim <daily|weekly>`
- `/cdrjobs contracts reroll <daily|weekly>`
- `/cdrjobs mastery [job]`
- `/cdrjobs <job>` — skill tree
- `/cdrjobs trials <job>`
- `/cdrjobs ability <job>`
- `/cdrjobs stats [player]`
- `/cdrjobs rebirth [job]`

## v1.6.0 — Public API v2

`CdrJobsAPI.API_VERSION = 2`.

API v2 keeps the original v1 progression/read methods and adds immutable snapshots for:
- full Adventurer Profile
- profession progression
- skills
- Trial counters/flags
- activity statistics
- ability cooldowns
- Mastery
- Daily/Weekly Contracts
- Fate Resonance
- cached leaderboards

CdrJobs now also publishes `CdrJobsAPI` through Bukkit `ServicesManager`, while `CdrJobsPlugin#getApi()` remains supported.

New observational Bukkit events include:
- `SkillUpgradeEvent`
- `ProfessionAwakeningEvent`
- `TrialCompleteEvent`
- `MasteryTierUpEvent`
- `HunterPvpRewardEvent`
- `ContractCompleteEvent`
- `ContractClaimEvent`
- `RebirthEvent`

Existing `ProfessionActionEvent`, `ProfessionXpGainEvent`, and `ProfessionLevelUpEvent` remain supported.

See [`docs/API.md`](docs/API.md) for the complete contract.

## GUI refresh & hardening

CdrJobs menus now receive a consistent stained-glass frame/theme without replacing functional items:
- Main/Profile — cyan/light blue
- Miner — gray/purple
- Farmer — green/lime
- Hunter — red/purple
- Lumberjack — orange/yellow
- Fisher — blue/cyan
- Rebirth — purple/red

The same layer blocks inventory drag attempts into CdrJobs GUI slots. GUI action exceptions are no longer silently swallowed; failures are logged with player/action context and the player receives a safe error message.

## Audit hardening in v1.6.0

- Contract reward delivery now uses per-component checkpoints for XP/Fate recovery before final `claimed` state.
- Contract progress/percentage calculations are overflow-hardened.
- expired Farmer/reward-location cooldown rows are pruned on startup.
- ProfessionStore uses an SQLite busy timeout to reduce transient lock failures.
- Trial API events only fire on incomplete -> complete transitions.
- Hunter PvP API events only fire after existing anti-farm gates accept the kill.
- no database schema migration; schema remains `9`.

## Fate Resonance

Resonance is derived from canonical Job Level + Mastery. There is no separate player Resonance ledger.

Default states:
- `RESONANT` — both paired Jobs satisfy the level threshold.
- `HARMONIZED` — both also satisfy the configured Mastery threshold.

The default configuration covers all ten unique pairings of the Five Paths and intentionally gives status/QoL identity rather than direct combat/economy power.

## Profession Contracts

Daily and Weekly Contracts are deterministic per player/cycle and progress only from anti-exploit-approved `ProfessionActionEvent` activity.

At Lv.100, Contract XP flows through normal progression into Profession Mastery.

## Profession Mastery

Additional profession XP after Lv.100 becomes persistent Mastery XP with configurable tiers, prestige titles, badges and leaderboards without direct power creep.

## Rite of Rebirth

Rebirth resets only the selected Job's skill tree. Profession level/XP, Trials, statistics, Fate milestone claims, active ability cooldowns and Mastery remain intact.

## Production systems
- Java 21 / Paper API 1.21.4
- SQLite WAL persistence
- one-time Fate milestone ledger
- profession anti-exploit rules
- deterministic Contracts
- post-Lv.100 Mastery
- derived Fate Resonance
- transactional Rebirth core
- cached leaderboard reads
- PlaceholderAPI optional
- Vault optional for Rebirth economy fees
- Public API v2 + Bukkit events
- GitHub Actions `mvn clean verify`
- granular admin diagnostics/reset/export tools

## Admin/debug
Important commands include:
- `/cdrjobsadmin diagnose`
- `/cdrjobsadmin inspect <player>`
- `/cdrjobsadmin hunterdebug <player>`
- `/cdrjobsadmin resonancedebug <player>`
- `/cdrjobsadmin export <player> [file|console|both]`
- `/cdrjobsadmin resetjob <player> <job>`
- `/cdrjobsadmin resettrial <player> <job>`
- `/cdrjobsadmin resetcooldown <player> <job>`
- `/cdrjobsadmin forcerespec <player> <job>`
- progression/Fate/Mastery administration commands

## Testing & release gate
See:
- [`docs/TESTING.md`](docs/TESTING.md)
- [`docs/RELEASE_PROCESS.md`](docs/RELEASE_PROCESS.md)
- [`docs/updates/v1.6.0.md`](docs/updates/v1.6.0.md)

A CI-green build is a testing candidate. Promotion to stable still requires real-server regression/restart validation.

## Build
```bash
mvn clean verify
```

MENKIESTESParty remains optional. Future Party integration should consume the API/events rather than CdrJobs SQLite internals.
