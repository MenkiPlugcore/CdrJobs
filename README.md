# CdrJobs

**Choose Your Path, Shape Your Fate.**

Standalone production line for five fantasy professions on Paper 1.21.x.
Current candidate: `v1.1.0 — Adventurer Profile`.

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

Adventurer Profile summarizes all Five Paths: total profession level, highest profession, Fate Essence, completed Trials, unlocked skills, awakened paths and profession activity statistics.

## Production systems
- SQLite persistence + non-destructive schema creation
- one-time Fate milestone claim ledger
- profession-specific anti-exploit rules
- configurable XP curve/multipliers
- PlaceholderAPI optional
- public Java API + Bukkit events
- GitHub Actions `clean verify`
- automatic CI JAR artifact
- `/cdrjobsadmin diagnose`
- granular admin reset/debug/export tools

## PlaceholderAPI
Existing per-job placeholders remain supported. v1.1.0 adds:
- `%cdrjobs_profile_total_level%`
- `%cdrjobs_profile_highest_profession%`
- `%cdrjobs_profile_highest_level%`
- `%cdrjobs_profile_trials_completed%`
- `%cdrjobs_profile_skills_unlocked%`
- `%cdrjobs_profile_awakened_count%`
- `%cdrjobs_profile_awakened_paths%`

Legacy Miner Trial placeholders are explicitly preserved:
- `%cdrjobs_miner_trial_stone%`
- `%cdrjobs_miner_trial_deep%`
- `%cdrjobs_miner_total_ores%`

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
