# CdrJobs

**Choose Your Path, Shape Your Fate.**

Fantasy profession progression for Paper. Current chapter: `v0.3.0 — Blood Moon`.

## Released professions
- ⛏ Runebound Delver — Miner
- 🌿 Verdant Keeper — Farmer
- ⚔ Bloodfang Stalker — Hunter

Planned before production:
- 🪓 Ironbark Warden — Lumberjack
- 🎣 Tidebound Angler — Fisher

## Shared systems
- Lv.1–100 per profession
- Global scarce Fate Essence
- Branching Path of Ascension
- Profession Trials
- Active abilities with persistent cooldowns
- SQLite persistence and non-destructive migrations
- PlaceholderAPI
- Configurable XP curve and per-profession tuning
- Admin testing commands

## Commands
- `/cdrjobs`
- `/cdrjobs stats [player]`
- `/cdrjobs <miner|farmer|hunter>`
- `/cdrjobs trials <job>`
- `/cdrjobs ability <job>`
- `/cdrjobsadmin addxp <player> [job] <amount>`
- `/cdrjobsadmin setlevel <player> [job] <level>`
- `/cdrjobsadmin reset <player>`
- `/cdrjobsadmin reload`

## Build
Java 21 + Maven:
```bash
mvn clean package
```
Output: `target/CdrJobs-0.3.0.jar`

## Documentation
Every update is recorded in `CHANGELOG.md` and `docs/updates/`.
