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
- [x] v1.3.0 — Profession Mastery implementation + CI

## Current candidate
- [x] v1.4.0 — Profession Contracts implementation
- [x] v1.4.0 CI `clean verify`
- [ ] Daily/Weekly deterministic assignment regression test
- [ ] all five ProfessionActionEvent progress paths test
- [ ] anti-exploit activities must not advance Contracts
- [ ] claim/reward exactly-once test
- [ ] reroll limit/reset/lock test
- [ ] timezone daily + Monday weekly rollover test
- [ ] restart persistence test for contract cycle/progress/claimed/rerolls
- [ ] PlaceholderAPI contract regression test
- [ ] custom contract definition + `/cdrjobsadmin reload` test
- [ ] re-run v1.3.0 Mastery + Five Paths regression matrix
- [ ] promote v1.4.0 to stable only after real-server validation

## Later standalone roadmap
- [ ] v1.5.0 — Fate Resonance
- [ ] v1.6.0 — Public API v2

## After standalone stability
Optional MENKIESTESParty integration using CdrJobs public API/events. Party integration remains soft/optional and must pass the same release gate.
