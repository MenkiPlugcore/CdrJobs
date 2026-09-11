# CdrJobs v1.6.0 — Public API v2 & GUI Refresh

Release date: 2026-09-11

CdrJobs is a standalone fantasy profession progression plugin for Paper servers, built around five simultaneous profession paths: Miner, Farmer, Hunter, Lumberjack, and Fisher.

## Highlights

- Public API v2 with immutable snapshots for profile, professions, skills, trials, statistics, cooldowns, Mastery, Contracts, Fate Resonance, and leaderboards.
- Bukkit `ServicesManager` registration for cleaner third-party integration while retaining `CdrJobsPlugin#getApi()` compatibility.
- New public events for skill upgrades, awakening, Trial completion, Mastery tier-up, Hunter PvP rewards, Contract completion/claim, and Rebirth.
- Profession-specific GUI visual refresh with hardened inventory interaction handling.
- Inventory drag protection for CdrJobs and Rebirth GUI top inventories.
- Contextual GUI error logging instead of silent exception swallowing.
- Contract claim recovery checkpoints for XP/Fate delivery to reduce partial-reward failure risk.
- Overflow-safe Contract percentage handling.
- Startup pruning for expired reward-location cooldown rows.
- SQLite `busy_timeout=3000` for the profession store.
- No database migration; schema remains `9`.

## Existing systems included

- Five Paths profession progression, Lv.1–100.
- Fate Essence and branching profession skill trees.
- Profession Trials and awakened paths.
- Hunter mob + PvP progression with anti-farm controls.
- Adventurer Profile and PlaceholderAPI support.
- Profession leaderboards.
- Rite of Rebirth skill respec.
- Post-Level-100 Profession Mastery.
- Daily/Weekly Profession Contracts.
- Fate Resonance across profession pairings.
- Admin/debug/export tooling.

## Integration

CdrJobs v1.6.0 is the minimum recommended build for MENKIESTESParty v2.1.0 Five Paths integration. MENKIESTESParty consumes CdrJobs Public API v2 and accepted `ProfessionActionEvent` activity without directly reading the CdrJobs SQLite database.

## Requirements

- Java 21
- Paper 1.21.x
- PlaceholderAPI optional
- Vault optional, only needed when configured for Rebirth economy fees

## Upgrade

1. Stop the server normally.
2. Replace the old CdrJobs JAR with `CdrJobs-1.6.0.jar`.
3. Keep the existing CdrJobs data directory/database.
4. Start the server.
5. Run `/cdrjobsadmin diagnose`.
6. Verify schema `9` and plugin version `1.6.0`.
7. Smoke-test all five professions and the GUI before production rollout.

## Recommended smoke test

- Open every CdrJobs/Profile/Rebirth GUI and test click, shift-click, and drag protection.
- Verify one valid activity for Miner, Farmer, Hunter, Lumberjack, and Fisher.
- Verify rejected exploit activity does not progress the profession.
- Test Hunter mob and PvP rewards plus anti-farm gates.
- Complete and claim a Contract, then confirm a second claim is rejected.
- Test Rebirth and restart persistence.
- Test Mastery tier-up at Lv.100.
- Verify PlaceholderAPI/profile output.
- If MENKIESTESParty v2.1.0 is installed, verify `/party cdrjobs` reports API v2 healthy and one accepted CdrJobs action contributes exactly once.

Built by CADERA under the MENKIESTES software project licensing terms.
