# CdrJobs Roadmap

CdrJobs is developed profession-by-profession. A profession must be stable before the next profession is added.

## Runebound Delver — Miner
- [x] v0.1.0 — Core XP, Lv.1–100, Fate Essence, Path of Ascension, SQLite, anti placed-ore exploit
- [x] v0.1.1 — Profession Trials, Runic Surge active ability, persistent cooldowns
- [x] v0.1.2 — Miner stability & balance pass, Fate ledger, exploit hardening, configurable XP curve

Miner now acts as the reference implementation for future CdrJobs professions.

## Future standalone professions
- [ ] v0.2.x — Verdant Keeper (Farmer)
- [ ] v0.3.x — Bloodfang Stalker (Hunter)
- [ ] v0.4.x — Ironbark Warden (Lumberjack)
- [ ] v0.5.x — Tidebound Angler (Fisher)

## Core expansion
- [ ] v0.6.x — Cross-job balancing and shared core polish
- [ ] v0.7.x — Public CdrJobs API and custom profession foundation

## Integration phase
Only after standalone professions are stable:
- [ ] CdrJobs → MENKIESTESParty profession data integration
- [ ] MENKIESTESParty → CdrJobs Party Specialist system
- [ ] Party Profession Roster
- [ ] Party Profession Projects / Contracts
- [ ] Party Profession Resonance

Design rule: CdrJobs and MENKIESTESParty must remain independently usable. Integration is optional, never a hard dependency.
