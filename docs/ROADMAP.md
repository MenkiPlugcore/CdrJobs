# CdrJobs Roadmap

## Complete implementation + CI
- [x] Miner — Runebound Delver
- [x] Farmer — Verdant Keeper
- [x] Hunter — Bloodfang Stalker
- [x] Lumberjack — Ironbark Warden
- [x] Fisher — Tidebound Angler
- [x] v1.0.0 — Five Paths standalone production baseline
- [x] v1.0.1–v1.0.5 — Stability, Hunter safety, admin/debug hardening
- [x] v1.1.0 — Adventurer Profile
- [x] v1.1.1 — Leaderboard & Profile QoL
- [x] v1.2.0 — Rite of Rebirth
- [x] v1.3.0 — Profession Mastery
- [x] v1.4.0 — Profession Contracts
- [x] v1.5.0 — Fate Resonance

## Current candidate — v1.6.0 Public API v2 & GUI Refresh
- [x] API v1 compatibility methods retained
- [x] API version raised to `2`
- [x] Bukkit `ServicesManager` API publication
- [x] immutable Profile/Profession/Mastery/Contract/Resonance/Leaderboard snapshots
- [x] skill/statistic/Trial/flag/cooldown query surface
- [x] Skill/Trial/Mastery/Hunter PvP/Contract/Rebirth/Awakening public events
- [x] GUI aesthetic framing by profession/theme
- [x] GUI drag hardening + contextual error logging
- [x] Contract claim recovery checkpoints
- [x] expired reward-location pruning
- [x] schema remains `9`
- [x] final v1.6.0 CI `clean verify` on release source/docs candidate
- [ ] temporary integration plugin resolves API from `ServicesManager`
- [ ] API v1 compatibility integration smoke test
- [ ] all API v2 snapshot reads test on fresh + progressed player
- [ ] all new API v2 Bukkit events fire exactly once at valid transitions
- [ ] admin/API `addXp` must not fabricate `ProfessionActionEvent`
- [ ] Contract claim retry/error-path regression
- [ ] GUI click + drag exploit regression across all CdrJobs/Rebirth menus
- [ ] GUI Bedrock/Geyser usability smoke test if server exposes menus cross-platform
- [ ] restart persistence/regression across Five Paths, Mastery, Contracts, Resonance and Rebirth
- [ ] profile/leaderboard/PlaceholderAPI regression
- [ ] full Five Paths anti-exploit matrix
- [ ] promote v1.6.0 to stable only after real-server validation

## Next phase after standalone stability
Optional MENKIESTESParty integration using `CdrJobsAPI` v2 and Bukkit events.

Rules for Party integration:
- CdrJobs must remain standalone and optional.
- MENKIESTESParty must not read `cdrjobs.db` directly.
- Party Projects should consume anti-exploit-approved `ProfessionActionEvent`.
- Profession roster/specialist views should use API snapshots.
- Integration receives its own CI + real-server release gate.

Future standalone additions should be driven by real-server feedback rather than adding systems before the v1.6.0 baseline is validated.
