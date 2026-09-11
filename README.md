# CdrJobs

**Choose Your Path, Shape Your Fate.**

CdrJobs is a fantasy profession progression plugin for Paper servers. Version `0.1.0 — The Runeborn` introduces the first playable profession: **Runebound Delver (Miner)**.

## v0.1.0 features
- Runebound Delver Lv.1–100
- Profession XP from natural ores
- Persistent anti ore-place XP exploit
- Global Fate Essence milestones
- Path of Ascension skill tree
- Multi-rank passive/trigger skills
- SQLite persistence
- GUI menus
- PlaceholderAPI support
- Admin XP / reset / reload commands

## Commands
- `/cdrjobs` — open profession menu
- `/cdrjobs stats [player]`
- `/cdrjobs skills`
- `/cdrjobsadmin addxp <player> <amount>`
- `/cdrjobsadmin setlevel <player> <level>`
- `/cdrjobsadmin reset <player>`
- `/cdrjobsadmin reload`

## Documentation
- [Roadmap](docs/ROADMAP.md)
- [v0.1.0 — The Runeborn](docs/updates/v0.1.0.md)
- [Changelog](CHANGELOG.md)

Every CdrJobs update is documented in `CHANGELOG.md` and receives a detailed version note under `docs/updates/`.

## Build
Requires Java 21 and Maven.

```bash
mvn clean package
```

Output: `target/CdrJobs-0.1.0.jar`

## Compatibility
Designed against Paper API 1.21.4 without NMS. Future jobs are planned as Farmer, Hunter, Lumberjack and Fisher.
