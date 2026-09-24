# CdrJobs

**Choose Your Path, Shape Your Fate.**

Standalone fantasy profession progression for Paper 1.21.x.
Current candidate: `v1.7.0 — Living Professions`.

## Five Paths
- ⛏ Runebound Delver — Miner
- 🌿 Verdant Keeper — Farmer
- ⚔ Bloodfang Stalker — Hunter
- 🪓 Ironbark Warden — Lumberjack
- 🎣 Tidebound Angler — Fisher

Each profession has Lv.1–100 progression, fantasy ranks, Path of Ascension, scarce global Fate Essence, Trials, active abilities, awakened paths, post-Lv.100 Mastery, repeatable Contracts, cross-profession Fate Resonance, and Living Professions feedback/events.

## Living Professions — v1.7.0

The Living Professions layer makes accepted profession activity more visible without replacing the Five Paths core.

- Profession Momentum: sustained accepted activity builds up to a conservative configurable XP bonus.
- Path Encounters: profession-specific timed objectives with XP rewards.
- Fate Whispers: atmospheric feedback for unlocked Fate Resonance pairings with no direct combat/economy power.
- Profession Milestones: persistent accepted-action milestones with profession-specific cosmetic titles.
- Adventure Session: `/cdrjobs session` shows current-login actions, XP, level-ups, Encounter completions and Momentum.
- Feedback Engine: richer actionbar progress, Path Ascended / Mastery / Contract presentation, Encounter and Milestone effects.
- World blacklist: disable only Living Professions presentation/events in selected worlds while keeping normal profession progression active.

Living Professions consumes accepted `ProfessionActionEvent` activity, so its action-driven systems inherit the existing profession anti-exploit gates.

Default Momentum is four stages at +2% XP per stage, capped at +8%, expiring after 45 seconds of inactivity. Fractional XP carry preserves the configured percentage on low-value actions.

Default Path Encounters:

- Miner — Runic Vein
- Farmer — Blessed Harvest
- Hunter — Marked Prey
- Lumberjack — Ancient Grove
- Fisher — Restless Waters

Encounter state reuses existing profession tables and remains relog/restart safe without a schema migration.

## Player commands
- `/cdrjobs` — main GUI
- `/cdrjobs profile [player]`
- `/cdrjobs session`
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

## Public API v2

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

CdrJobs also publishes `CdrJobsAPI` through Bukkit `ServicesManager`, while `CdrJobsPlugin#getApi()` remains supported.

Public observational Bukkit events include:
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

CdrJobs menus use consistent profession-specific stained-glass framing without replacing functional items:
- Main/Profile — cyan/light blue
- Miner — gray/purple
- Farmer — green/lime
- Hunter — red/purple
- Lumberjack — orange/yellow
- Fisher — blue/cyan
- Rebirth — purple/red

The same layer blocks inventory drag attempts into CdrJobs GUI slots. GUI action exceptions are logged with player/action context and the player receives a safe error message.

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
- Living Professions Momentum / Encounters / Milestones / session feedback
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
- [`docs/updates/v1.7.0.md`](docs/updates/v1.7.0.md)

A CI-green build is a testing candidate. Promotion to stable still requires real-server regression/restart validation.

## Build
```bash
mvn clean verify
```

MENKIESTESParty remains optional. Integrations should consume the API/events rather than CdrJobs SQLite internals.
