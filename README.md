# CdrJobs

**Choose Your Path, Shape Your Fate.**

CdrJobs is a fantasy profession progression plugin for Paper servers. Current chapter: `v0.2.0 — Verdant Awakening`.

## Released professions

### Runebound Delver — Miner
- Lv.1–100
- Natural ore progression
- Persistent anti placed-ore exploit
- Trial of Stone / Trial of the Deep
- Runic Surge active ability
- Heart of the Mountain awakening

### Verdant Keeper — Farmer
- Lv.1–100
- Mature crop progression
- Persistent location cooldown against rapid regrow/bonemeal reward spam
- Rootbound auto-replant
- Trial of Seed / Trial of Gaia
- Verdant Bloom active ability
- Verdant Dominion awakening

## Shared progression
- Global Fate Essence
- One-time milestone claims per profession
- Configurable XP curve
- SQLite persistence with non-destructive migrations
- GUI profession selection
- PlaceholderAPI
- Java 21 / Paper 1.21.4 API

## Commands
- `/cdrjobs`
- `/cdrjobs stats [player]`
- `/cdrjobs miner`
- `/cdrjobs farmer`
- `/cdrjobs trials [miner|farmer]`
- `/cdrjobs ability [miner|farmer]`
- `/cdrjobsadmin addxp <player> [job] <amount>`
- `/cdrjobsadmin setlevel <player> [job] <level>`
- `/cdrjobsadmin reset <player>`
- `/cdrjobsadmin reload`

## Documentation
- [Roadmap](docs/ROADMAP.md)
- [v0.1.0](docs/updates/v0.1.0.md)
- [v0.1.1](docs/updates/v0.1.1.md)
- [v0.1.2](docs/updates/v0.1.2.md)
- [v0.2.0 — Verdant Awakening](docs/updates/v0.2.0.md)
- [Changelog](CHANGELOG.md)

## Build
```bash
mvn clean package
```
Output: `target/CdrJobs-0.2.0.jar`

## Road to production
Hunter, Lumberjack and Fisher are implemented next. After all five professions compile and pass their server checklists, CdrJobs moves to a shared API/production-candidate phase before `v1.0.0`.
