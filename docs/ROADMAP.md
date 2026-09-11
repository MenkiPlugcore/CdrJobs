# CdrJobs Roadmap

CdrJobs is developed job-by-job so every profession can be tested independently before cross-plugin integration is introduced.

## Standalone profession phases

- `v0.1.x` — Runebound Delver (Miner)
  - Core progression
  - Fate Essence
  - Path of Ascension
  - Miner balancing, trials, abilities, exploit protection
- `v0.2.x` — Verdant Keeper (Farmer)
- `v0.3.x` — Bloodfang Stalker (Hunter)
- `v0.4.x` — Ironbark Warden (Lumberjack)
- `v0.5.x` — Tidebound Angler (Fisher)
- `v0.6.x` — Cross-profession balancing and core polishing
- `v0.7.x` — Public API

## Integration phase

Only after the standalone professions are stable:

1. CdrJobs exposes stable profession/player progression APIs.
2. MENKIESTESParty reads CdrJobs profession data.
3. Party profession roles, specialists, projects and contracts are added.
4. Both plugins remain optional dependencies and must still function independently.

## Documentation rule

Every update must include:

- `CHANGELOG.md` entry
- Detailed file under `docs/updates/`
- Command/config migration notes when applicable
- Testing notes and known limitations
