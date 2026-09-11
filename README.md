# CdrJobs

**Choose Your Path, Shape Your Fate.**

Current development version: `v0.4.0 — Oath of the Ancient Grove`.

## Released
- ⛏ Runebound Delver — Miner
- 🌿 Verdant Keeper — Farmer
- ⚔ Bloodfang Stalker — Hunter
- 🪓 Ironbark Warden — Lumberjack

Next: 🎣 Tidebound Angler — Fisher, then production hardening/API.

## Core systems
- Lv.1–100 per profession
- Global scarce Fate Essence
- Branching fantasy skill trees
- Trials + awakened endgame skills
- Active abilities with persistent cooldowns
- SQLite persistence
- Profession-specific anti-exploit systems
- PlaceholderAPI
- Admin test commands

## Commands
`/cdrjobs`, `/cdrjobs stats`, `/cdrjobs <job>`, `/cdrjobs trials <job>`, `/cdrjobs ability <job>`

Admin: `/cdrjobsadmin addxp <player> [job] <amount>`, `/cdrjobsadmin setlevel <player> [job] <level>`, `/cdrjobsadmin reset <player>`, `/cdrjobsadmin reload`.

## Build
Java 21 / Paper 1.21.4 API.
```bash
mvn clean package
```
