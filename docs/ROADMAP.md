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
- [x] v1.4.0 — Profession Contracts implementation + CI

## Current candidate
- [x] v1.5.0 — Fate Resonance implementation
- [x] v1.5.0 CI `clean verify`
- [ ] all ten unique Five Paths pair definitions load from config
- [ ] Resonant threshold regression at configured minimum Job levels
- [ ] Harmonized threshold regression at configured Mastery tier
- [ ] lowering/resetting a paired Job must immediately lock derived Resonance again
- [ ] Rebirth must not alter Resonance when Job Level/Mastery stay unchanged
- [ ] `resonancedebug`, stats and PlaceholderAPI regression test
- [ ] config reload test for custom names/titles/badges/thresholds and disabled definitions
- [ ] invalid/duplicate pair validator warning test
- [ ] restart test: derived state must remain identical without separate resonance persistence
- [ ] re-run Contracts + Mastery + Five Paths regression matrix
- [ ] promote v1.5.0 to stable only after real-server validation

## Later standalone roadmap
- [ ] v1.6.0 — Public API v2

## After standalone stability
Optional MENKIESTESParty integration using CdrJobs public API/events. Party integration remains soft/optional and must pass the same release gate.
