# CdrJobs

**Choose Your Path, Shape Your Fate.**

Stable baseline: `v1.0.0 — Five Paths`.
Current testing candidate: `v1.0.1 — Stability Patch`.

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
- `/cdrjobsadmin inspect <player>` in v1.0.1

## Server testing
See [`docs/TESTING.md`](docs/TESTING.md) and [`docs/RELEASE_PROCESS.md`](docs/RELEASE_PROCESS.md).

## Developer API
See [`docs/API.md`](docs/API.md).

## Commands
`/cdrjobs`, `/cdrjobs stats`, `/cdrjobs <job>`, `/cdrjobs trials <job>`, `/cdrjobs ability <job>`

Admin:
- `/cdrjobsadmin diagnose`
- `/cdrjobsadmin inspect <player>`
- `/cdrjobsadmin reload`
- `/cdrjobsadmin reset <player>`
- `/cdrjobsadmin addxp <player> [job] <amount>`
- `/cdrjobsadmin setlevel <player> [job] <level>`

## Build
Requires Java 21 and Paper API 1.21.4.
```bash
mvn clean verify
```

MENKIESTESParty is intentionally not a dependency in the standalone release. Integration begins only after the standalone release train remains stable through regression testing.
