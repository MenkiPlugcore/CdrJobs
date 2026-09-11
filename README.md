# CdrJobs

**Choose Your Path, Shape Your Fate.**

Current production candidate: `v0.9.0 — Adventurer API`.

## Professions
- ⛏ Runebound Delver — Miner
- 🌿 Verdant Keeper — Farmer
- ⚔ Bloodfang Stalker — Hunter
- 🪓 Ironbark Warden — Lumberjack
- 🎣 Tidebound Angler — Fisher

Each profession has Lv.1–100 progression, fantasy ranks, Path of Ascension, scarce global Fate Essence, profession trials, an active ability and awakened endgame path.

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

## Server testing
See [`docs/TESTING.md`](docs/TESTING.md).

## Developer API
See [`docs/API.md`](docs/API.md).

## Commands
`/cdrjobs`, `/cdrjobs stats`, `/cdrjobs <job>`, `/cdrjobs trials <job>`, `/cdrjobs ability <job>`

Admin: `/cdrjobsadmin diagnose`, `reload`, `reset`, `addxp`, `setlevel`.

## Build
Requires Java 21 and Paper API 1.21.4.
```bash
mvn clean verify
```

MENKIESTESParty is intentionally not a dependency in the standalone release. Integration begins after v1.0.0.
