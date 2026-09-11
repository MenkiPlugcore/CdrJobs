# CdrJobs Roadmap

## Complete
- [x] Miner — Runebound Delver
- [x] Farmer — Verdant Keeper
- [x] Hunter — Bloodfang Stalker
- [x] Lumberjack — Ironbark Warden
- [x] Fisher — Tidebound Angler
- [x] v1.0.0 — Five Paths standalone production baseline
- [x] v1.0.1–v1.0.5 — Stability, Hunter safety, admin/debug hardening
- [x] v1.1.0 — Adventurer Profile implementation + CI
- [x] v1.1.1 — Leaderboard & Profile QoL implementation + CI
- [x] v1.2.0 — Rite of Rebirth implementation + CI

## Current candidate
- [x] v1.3.0 — Profession Mastery implementation
- [ ] v1.3.0 CI `clean verify`
- [ ] Level 100 XP routing + overflow regression test across all five professions
- [ ] Mastery I–X curve/config reload test
- [ ] restart/persistence test for Mastery XP
- [ ] Mastery leaderboard/cache test
- [ ] PlaceholderAPI Mastery regression test
- [ ] Rebirth must preserve Mastery; resetjob/full reset must clear affected Mastery
- [ ] re-run Five Paths progression + anti-exploit regression matrix
- [ ] promote v1.3.0 to stable only after real-server validation

## Later standalone roadmap
- [ ] v1.4.0 — Profession Contracts
- [ ] v1.5.0 — Fate Resonance
- [ ] v1.6.0 — Public API v2

## After standalone stability
Optional MENKIESTESParty integration using CdrJobs public API/events. Party integration remains soft/optional and must pass the same release gate.
