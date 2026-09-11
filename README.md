# CdrJobs

**Choose Your Path, Shape Your Fate.**

Standalone production line for five fantasy professions on Paper 1.21.x.
Current candidate: `v1.3.0 — Profession Mastery`.

## Professions
- ⛏ Runebound Delver — Miner
- 🌿 Verdant Keeper — Farmer
- ⚔ Bloodfang Stalker — Hunter
- 🪓 Ironbark Warden — Lumberjack
- 🎣 Tidebound Angler — Fisher

Each profession has Lv.1–100 progression, fantasy ranks, Path of Ascension, scarce global Fate Essence, profession trials, an active ability, awakened endgame path, and post-Lv.100 Mastery.

## Player features
- `/cdrjobs` main GUI
- `/cdrjobs profile [player]` Adventurer Profile
- `/cdrjobs mastery [job]` / `/cdrjobs prestige [job]` post-100 Mastery
- `/cdrjobs <job>` skill tree
- `/cdrjobs trials <job>` Trial progress
- `/cdrjobs ability <job>` active ability
- `/cdrjobs stats [player]` compact text stats
- `/cdrjobs rebirth [job]` / `/cdrjobs respec [job]` skill-tree respec GUI

## Profession Mastery
After a profession reaches Lv.100, additional profession XP is routed into Mastery instead of being discarded.

Default curve:
```yaml
mastery:
  enabled: true
  max-tier: 10
  base-xp: 5000
  growth-per-tier: 2500
  tier-up-sound: true
```

Mastery is intentionally cosmetic/status-oriented. It adds prestige tiers, titles, badges and rankings without direct combat/economy/gathering buffs.

Mastery commands:
- `/cdrjobs mastery`
- `/cdrjobs mastery <job>`
- `/cdrjobs top mastery <job>`
- `/cdrjobs top mastery-total`

## Rite of Rebirth
Rite of Rebirth resets only the selected profession's skill tree. It preserves profession level/XP, Trial progress, statistics, Fate milestone claims, active-ability cooldowns, and Mastery.

```yaml
rite-of-rebirth:
  enabled: true
  cooldown-seconds: 604800
  refund-percent: 100
  fee:
    mode: NONE
    fate-essence: 0
    vault: 0.0
```

Fee modes are `NONE`, `FATE`, and `VAULT`. Vault is a soft dependency and is only needed when `VAULT` mode is selected.

## Leaderboards
- `/cdrjobs top <job>` — profession level/XP
- `/cdrjobs top total` — total Five Paths level
- `/cdrjobs top pvp` — rewarded Hunter PvP kills
- `/cdrjobs top activity <job>` — profession activity
- `/cdrjobs top mastery <job>` — profession Mastery
- `/cdrjobs top mastery-total` — total prestige Mastery XP

## PlaceholderAPI
Global/profile Mastery:
- `%cdrjobs_profile_mastery_tiers%`
- `%cdrjobs_profile_mastery_xp%`

Per profession (replace `<job>` with miner/farmer/hunter/lumberjack/fisher):
- `%cdrjobs_<job>_mastery_tier%`
- `%cdrjobs_<job>_mastery_roman%`
- `%cdrjobs_<job>_mastery_xp%`
- `%cdrjobs_<job>_mastery_xp_required%`
- `%cdrjobs_<job>_mastery_total_xp%`
- `%cdrjobs_<job>_mastery_title%`
- `%cdrjobs_<job>_mastery_badge%`
- `%cdrjobs_<job>_mastery_display%`

Existing profile and legacy Miner placeholders remain supported.

## Production systems
- SQLite persistence + non-destructive schema creation
- one-time Fate milestone claim ledger
- post-Lv.100 Mastery stored in existing profession counters
- profession-specific anti-exploit rules
- transactional Rebirth respec/refund handling
- configurable XP curves
- cached leaderboard reads
- PlaceholderAPI optional
- Vault optional for Rebirth economy fees
- public Java API + Bukkit events
- GitHub Actions `clean verify`
- granular admin reset/debug/export tools

## Admin commands
- `/cdrjobsadmin diagnose`
- `/cdrjobsadmin inspect <player>`
- `/cdrjobsadmin hunterdebug <player>`
- `/cdrjobsadmin reload`
- `/cdrjobsadmin reset <player>`
- `/cdrjobsadmin resetjob <player> <job>`
- `/cdrjobsadmin resettrial <player> <job>`
- `/cdrjobsadmin resetcooldown <player> <job>`
- `/cdrjobsadmin forcerespec <player> <job>`
- `/cdrjobsadmin addxp <player> [job] <amount>`
- `/cdrjobsadmin setlevel <player> [job] <level>`
- `/cdrjobsadmin addessence <player> <amount>`
- `/cdrjobsadmin setessence <player> <amount>`
- `/cdrjobsadmin addmasteryxp <player> <job> <amount>`
- `/cdrjobsadmin setmastery <player> <job> <tier>`
- `/cdrjobsadmin export <player> [file|console|both]`

## Server testing
See [`docs/TESTING.md`](docs/TESTING.md), [`docs/RELEASE_PROCESS.md`](docs/RELEASE_PROCESS.md), and [`docs/updates/v1.3.0.md`](docs/updates/v1.3.0.md).

## Developer API
See [`docs/API.md`](docs/API.md).

## Build
Requires Java 21 and Paper API 1.21.4.
```bash
mvn clean verify
```

MENKIESTESParty remains intentionally optional and is not a dependency of the standalone release.
