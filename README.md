# CdrJobs

**Choose Your Path, Shape Your Fate.**

CdrJobs is a fantasy profession progression plugin for Paper servers. Current development chapter: `v0.1.1 — Trials of the Deep`, focused on the first profession: **Runebound Delver (Miner)**.

## Current Miner features
- Runebound Delver Lv.1–100
- Fantasy profession rank titles
- Profession XP from natural ores
- Persistent anti ore-place XP exploit
- Global and intentionally scarce Fate Essence
- Path of Ascension skill tree
- Passive, trigger and active skills
- Trial of Stone + Trial of the Deep
- Runic Surge active ability with persistent cooldown
- SQLite persistence
- GUI menus
- PlaceholderAPI support
- Admin XP / reset / reload commands

## Commands
- `/cdrjobs` — open profession menu
- `/cdrjobs stats [player]`
- `/cdrjobs skills`
- `/cdrjobs trials`
- `/cdrjobs ability` — activate Runic Surge after it is learned
- `/cdrjobsadmin addxp <player> <amount>`
- `/cdrjobsadmin setlevel <player> <level>`
- `/cdrjobsadmin reset <player>`
- `/cdrjobsadmin reload`

## Documentation
- [Roadmap](docs/ROADMAP.md)
- [v0.1.0 — The Runeborn](docs/updates/v0.1.0.md)
- [v0.1.1 — Trials of the Deep](docs/updates/v0.1.1.md)
- [Changelog](CHANGELOG.md)

Every CdrJobs update is documented in `CHANGELOG.md` and receives a detailed version note under `docs/updates/`.

## Build
Requires Java 21 and Maven.

```bash
mvn clean package
```

Output: `target/CdrJobs-0.1.1.jar`

## Compatibility
Designed against Paper API 1.21.4 without NMS. Planned standalone professions: Farmer, Hunter, Lumberjack and Fisher. MENKIESTESParty integration comes only after the standalone profession core is stable.
