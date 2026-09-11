# CdrJobs

**Choose Your Path, Shape Your Fate.**

Standalone production line for five fantasy professions on Paper 1.21.x.
Current candidate: `v1.4.0 — Profession Contracts`.

## Professions
- ⛏ Runebound Delver — Miner
- 🌿 Verdant Keeper — Farmer
- ⚔ Bloodfang Stalker — Hunter
- 🪓 Ironbark Warden — Lumberjack
- 🎣 Tidebound Angler — Fisher

Each profession has Lv.1–100 progression, fantasy ranks, Path of Ascension, scarce global Fate Essence, profession trials, active abilities, awakened paths, post-Lv.100 Mastery, and repeatable Contracts.

## Player features
- `/cdrjobs` main GUI
- `/cdrjobs profile [player]`
- `/cdrjobs contracts`
- `/cdrjobs contracts claim <daily|weekly>`
- `/cdrjobs contracts reroll <daily|weekly>`
- `/cdrjobs mastery [job]` / `/cdrjobs prestige [job]`
- `/cdrjobs <job>` skill tree
- `/cdrjobs trials <job>`
- `/cdrjobs ability <job>`
- `/cdrjobs stats [player]`
- `/cdrjobs rebirth [job]` / `/cdrjobs respec [job]`

## Profession Contracts
Daily and Weekly Contracts are assigned deterministically per player and cycle. They do not randomly change after relog/restart.

Contract progress consumes the public `ProfessionActionEvent`, which means only activity that already passed CdrJobs anti-exploit checks contributes.

Default behavior:
```yaml
contracts:
  enabled: true
  timezone: Asia/Jakarta
  progress-actionbar: false
  daily-rerolls: 1
  weekly-rerolls: 1
```

Admins can create custom definitions in `contracts.definitions` with cadence, profession, target, XP reward, and Fate Essence reward. Daily defaults reward profession XP only; Weekly defaults reward more XP plus one Fate Essence.

At Lv.100, Contract XP flows through the normal progression path into Profession Mastery.

## Profession Mastery
After Lv.100, additional profession XP is routed into persistent Mastery instead of being discarded.

```yaml
mastery:
  enabled: true
  max-tier: 10
  base-xp: 5000
  growth-per-tier: 2500
  tier-up-sound: true
```

Mastery remains prestige/status-oriented and does not add direct combat, economy, or gathering power buffs.

## Rite of Rebirth
Rite of Rebirth resets only the selected profession's skill tree and preserves level/XP, Trials, statistics, Fate milestone claims, normal ability cooldowns, and Mastery.

## Leaderboards
- `/cdrjobs top <job>`
- `/cdrjobs top total`
- `/cdrjobs top pvp`
- `/cdrjobs top activity <job>`
- `/cdrjobs top mastery <job>`
- `/cdrjobs top mastery-total`

## PlaceholderAPI
Contracts support both `daily` and `weekly` variants:
- `%cdrjobs_contract_daily_name%`
- `%cdrjobs_contract_daily_job%`
- `%cdrjobs_contract_daily_progress%`
- `%cdrjobs_contract_daily_target%`
- `%cdrjobs_contract_daily_percent%`
- `%cdrjobs_contract_daily_complete%`
- `%cdrjobs_contract_daily_claimed%`
- `%cdrjobs_contract_daily_reward_xp%`
- `%cdrjobs_contract_daily_reward_fate%`
- `%cdrjobs_contract_daily_rerolls_left%`

Replace `daily` with `weekly` for Weekly Contract placeholders. Existing Profile, Mastery, profession, Hunter and legacy Miner placeholders remain supported.

## Production systems
- SQLite persistence + non-destructive schema creation
- one-time Fate milestone claim ledger
- profession-specific anti-exploit rules
- deterministic Daily/Weekly Contracts on accepted profession actions
- post-Lv.100 Mastery
- transactional Rebirth respec/refund handling
- cached leaderboard reads
- PlaceholderAPI optional
- Vault optional for Rebirth economy fees
- public Java API + Bukkit events
- GitHub Actions `clean verify`
- granular admin reset/debug/export tools

## Admin commands
Existing admin/debug commands remain available, including progression, Fate, Mastery, Rebirth, reset, inspect and export tools.

## Server testing
See [`docs/TESTING.md`](docs/TESTING.md), [`docs/RELEASE_PROCESS.md`](docs/RELEASE_PROCESS.md), and [`docs/updates/v1.4.0.md`](docs/updates/v1.4.0.md).

## Developer API
See [`docs/API.md`](docs/API.md).

## Build
Requires Java 21 and Paper API 1.21.4.
```bash
mvn clean verify
```

MENKIESTESParty remains intentionally optional and is not a dependency of the standalone release.
