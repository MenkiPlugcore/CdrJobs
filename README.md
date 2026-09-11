# CdrJobs

**Choose Your Path, Shape Your Fate.**

Standalone production line for five fantasy professions on Paper 1.21.x.
Current candidate: `v1.1.1 — Leaderboard & Profile QoL`.

## Professions
- ⛏ Runebound Delver — Miner
- 🌿 Verdant Keeper — Farmer
- ⚔ Bloodfang Stalker — Hunter
- 🪓 Ironbark Warden — Lumberjack
- 🎣 Tidebound Angler — Fisher

Each profession has Lv.1–100 progression, fantasy ranks, Path of Ascension, scarce global Fate Essence, profession trials, an active ability and awakened endgame path.

## Player features
- `/cdrjobs` main GUI
- `/cdrjobs profile [player]` Adventurer Profile
- `/cdrjobs <job>` skill tree
- `/cdrjobs trials <job>` Trial progress
- `/cdrjobs ability <job>` active ability
- `/cdrjobs stats [player]` compact text stats

## Leaderboards
- `/cdrjobs top <job>` — profession level/XP ranking
- `/cdrjobs top total` — total Five Paths level
- `/cdrjobs top pvp` — rewarded Hunter PvP kills
- `/cdrjobs top activity <job>` — primary profession activity

Leaderboard reads use a dedicated SQLite read connection and short-lived cache. Defaults:
```yaml
leaderboards:
  limit: 10
  cache-seconds: 30
```

## Adventurer Profile
Profile summarizes all Five Paths: total profession level, highest profession, Fate Essence, completed Trials, unlocked skills, awakened paths and profession activity statistics.

## PlaceholderAPI
- `%cdrjobs_profile_total_level%`
- `%cdrjobs_profile_highest_profession%`
- `%cdrjobs_profile_highest_level%`
- `%cdrjobs_profile_trials_completed%`
- `%cdrjobs_profile_skills_unlocked%`
- `%cdrjobs_profile_awakened_count%`
- `%cdrjobs_profile_awakened_paths%`

Legacy Miner Trial placeholders remain supported:
- `%cdrjobs_miner_trial_stone%`
- `%cdrjobs_miner_trial_deep%`
- `%cdrjobs_miner_total_ores%`

## Production systems
- SQLite persistence + non-destructive schema creation
- one-time Fate milestone claim ledger
- profession-specific anti-exploit rules
- configurable XP curve/multipliers
- cached leaderboard reads
- PlaceholderAPI optional
- public Java API + Bukkit events
- GitHub Actions `clean verify`
- automatic CI JAR artifact
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
- `/cdrjobsadmin addxp <player> [job] <amount>`
- `/cdrjobsadmin setlevel <player> [job] <level>`
- `/cdrjobsadmin addessence <player> <amount>`
- `/cdrjobsadmin setessence <player> <amount>`
- `/cdrjobsadmin export <player> [file|console|both]`

## Server testing
See [`docs/TESTING.md`](docs/TESTING.md) and [`docs/RELEASE_PROCESS.md`](docs/RELEASE_PROCESS.md).

## Developer API
See [`docs/API.md`](docs/API.md).

## Build
Requires Java 21 and Paper API 1.21.4.
```bash
mvn clean verify
```

MENKIESTESParty is intentionally not a dependency in the standalone release. Integration begins only after the standalone release train remains stable through regression testing.
