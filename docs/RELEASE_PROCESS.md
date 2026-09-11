# CdrJobs Release Process

Every CdrJobs update must pass the release gate before it is published as a GitHub Release.

## Release gate

1. CI must pass `mvn clean verify` on the exact release commit.
2. Existing profession features must be regression-tested, not only the newly added feature.
3. Persistence must be checked across a full server restart:
   - levels and XP
   - Fate Essence
   - skill ranks
   - trial progress/completion
   - ability cooldowns
4. Anti-exploit protections must be re-tested for every affected profession.
5. `/cdrjobsadmin diagnose` must report a healthy schema and expected version.
6. No new console exceptions, database errors, or obvious gameplay regressions may remain.
7. The update must receive a real-server smoke test before being marked stable.

## Profession regression matrix

- Miner: natural ore, placed ore, piston movement, explosion cleanup, trials, Runic Surge.
- Farmer: mature crops, immature crops, bone-meal/location cooldown, Rootbound, trials, Verdant Bloom.
- Hunter: natural mobs, spawner/bred mob protection, night/dangerous prey progress, trials, ability.
- Lumberjack: natural logs, placed logs, piston movement, explosion cleanup, trials, ability.
- Fisher: normal catch, ocean/deep-ocean, treasure, AFK protection, trials, Ocean's Call.

## Release policy

A green build alone is not enough for a stable release. If a regression is found during testing, fix it first and publish a new candidate build. Only a tested candidate is promoted to GitHub Releases.

For each published release, include:
- version and codename
- concise changelog
- compatibility requirements
- install/upgrade notes
- known issues, if any
- packaged JAR from the exact passing CI run

MENKIESTESParty integration must follow the same release gate and must not weaken CdrJobs standalone compatibility.
