# CdrJobs

**Choose Your Path, Shape Your Fate.**

Current development version: `v0.5.0 — Call of the Deep`.

## Five standalone professions
- ⛏ Runebound Delver — Miner
- 🌿 Verdant Keeper — Farmer
- ⚔ Bloodfang Stalker — Hunter
- 🪓 Ironbark Warden — Lumberjack
- 🎣 Tidebound Angler — Fisher

Every profession has Lv.1–100 progression, fantasy rank titles, a branching Path of Ascension, trials, an active ability and an awakened endgame path.

## Shared systems
- Global scarce Fate Essence
- One-time Fate milestone claims per profession
- Configurable XP curve and profession multipliers
- SQLite persistence with non-destructive table creation
- Profession-specific anti-exploit mechanics
- GUI navigation
- PlaceholderAPI (optional)
- Admin testing commands

## Commands
- `/cdrjobs`
- `/cdrjobs stats [player]`
- `/cdrjobs <miner|farmer|hunter|lumberjack|fisher>`
- `/cdrjobs trials <job>`
- `/cdrjobs ability <job>`
- `/cdrjobsadmin addxp <player> [job] <amount>`
- `/cdrjobsadmin setlevel <player> [job] <level>`
- `/cdrjobsadmin reset <player>`
- `/cdrjobsadmin reload`

## Build
Java 21, Paper API 1.21.4.
```bash
mvn clean package
```

Next: `v0.9.0` production candidate hardening + public API. MENKIESTESParty integration starts only after standalone `v1.0.0`.
