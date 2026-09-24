# CdrJobs v1.7.0 — Living Professions

Release date: 2026-09-24

CdrJobs v1.7.0 turns profession progression into a more visible, reactive gameplay loop without replacing the Five Paths systems introduced in earlier releases.

## Highlights

- Added the Living Professions engine, driven only by accepted `ProfessionActionEvent` activity.
- Added Profession Momentum with four configurable stages and a conservative default XP bonus ceiling of 8%.
- Momentum expires after inactivity and uses fractional carry so low-XP actions receive the configured bonus fairly over time instead of being rounded away.
- Added profession-specific Path Encounters for Miner, Farmer, Hunter, Lumberjack and Fisher.
- Encounters have configurable chance, cooldown, target, duration and XP reward.
- Active Encounter progress and expiry are restart/relog safe through the existing profession tables; no database migration is required.
- Added persistent Encounter cooldowns using the existing ability cooldown storage.
- Added Fate Whispers for unlocked Fate Resonance pairings. Whispers are cosmetic/status feedback only and do not grant combat or economy power.
- Added profession Milestones at 1,000 / 5,000 / 25,000 / 100,000 accepted actions with profession-specific titles.
- Added richer actionbar profession feedback with progress bar, Momentum status and active Encounter progress.
- Added level-up, Mastery, Contract and Encounter presentation titles/particles/sounds.
- Added per-login Adventure Session tracking and `/cdrjobs session`.
- Added a configurable Living Professions world blacklist. Blacklisted worlds keep normal CdrJobs progression while suppressing Living Professions presentation/events.

## Default Path Encounters

- Miner: `Runic Vein` — 8 accepted ore actions in 45 seconds, 320 XP.
- Farmer: `Blessed Harvest` — 20 accepted crop actions in 60 seconds, 300 XP.
- Hunter: `Marked Prey` — 6 accepted kill actions in 90 seconds, 350 XP.
- Lumberjack: `Ancient Grove` — 12 accepted log actions in 60 seconds, 320 XP.
- Fisher: `Restless Waters` — 3 accepted catch actions in 120 seconds, 280 XP.

## Safety / Economy Design

- Living Professions never awards Vault money or direct economy currency.
- Encounter and Momentum progression consumes the existing accepted profession event stream, preserving current anti-exploit gates.
- Momentum bonuses apply to activity XP, not Encounter rewards.
- Fate Whispers remain cosmetic and do not change damage, gathering drops, sell prices or economy multipliers.
- Existing Contracts continue to progress from accepted `ProfessionActionEvent` activity exactly once.
- Existing Fate Resonance remains derived from canonical Job Level + Mastery.
- Existing Public API remains v2; this release does not break the v1/v2 API contract.

## Configuration

New root section: `living-professions`.

Key controls:

- `enabled`
- `world-blacklist`
- `feedback.*`
- `momentum.*`
- `encounters.*`
- `fate-whispers.*`
- `milestones.*`
- `session.enabled`

Existing server configs are merged with the new defaults on startup.

## Upgrade

1. Stop the server normally.
2. Replace the old CdrJobs JAR with `CdrJobs-1.7.0.jar`.
3. Keep the existing CdrJobs data directory/database.
4. Start the server.
5. Run `/cdrjobsadmin diagnose`.
6. Verify schema remains `9` and plugin version is `1.7.0`.
7. Run the smoke test below before production rollout.

## Recommended smoke test

- Perform one accepted action for every profession and verify the richer actionbar appears.
- Build Momentum to at least stage 1 and verify the XP bonus is conservative and resets after inactivity.
- Temporarily raise Encounter `chance-percent` for QA and verify all five Path Encounter definitions can start, progress, complete and reward exactly once.
- Restart/relog during an active Encounter and verify progress is restored until the stored expiry time.
- Verify Encounter cooldown prevents immediate retriggering.
- Reach a temporary low Milestone threshold in QA and verify the milestone announcement fires once when crossing it.
- Verify `/cdrjobs session` reports action, XP, level-up and Encounter counts for the current login session.
- Verify an unlocked Fate Resonance can produce a Fate Whisper with a temporarily increased QA chance.
- Add a test world to `living-professions.world-blacklist` and verify normal profession XP still works there while Living Professions effects do not.
- Re-run existing Contract, Mastery, Rebirth, Trial, leaderboard and Public API smoke tests.

No database schema migration; schema remains `9`.

Built by CADERA under the MENKIESTES software project licensing terms.
