# CdrJobs

**Choose Your Path, Shape Your Fate.**

Standalone production line for five fantasy professions on Paper 1.21.x.
Current candidate: `v1.5.0 — Fate Resonance`.

## Professions
- ⛏ Runebound Delver — Miner
- 🌿 Verdant Keeper — Farmer
- ⚔ Bloodfang Stalker — Hunter
- 🪓 Ironbark Warden — Lumberjack
- 🎣 Tidebound Angler — Fisher

Each profession has Lv.1–100 progression, fantasy ranks, Path of Ascension, scarce global Fate Essence, profession trials, active abilities, awakened paths, post-Lv.100 Mastery, repeatable Contracts, and cross-profession Fate Resonance.

## Player features
- `/cdrjobs` main GUI
- `/cdrjobs profile [player]`
- `/cdrjobs resonance [id]` / `/cdrjobs fateresonance [id]`
- `/cdrjobs contracts`
- `/cdrjobs contracts claim <daily|weekly>`
- `/cdrjobs contracts reroll <daily|weekly>`
- `/cdrjobs mastery [job]` / `/cdrjobs prestige [job]`
- `/cdrjobs <job>` skill tree
- `/cdrjobs trials <job>`
- `/cdrjobs ability <job>`
- `/cdrjobs stats [player]`
- `/cdrjobs rebirth [job]` / `/cdrjobs respec [job]`

## Fate Resonance
Fate Resonance represents synergy between two progressed professions. It is derived directly from canonical Job Level and Mastery, so there is no separate player resonance state to desynchronise or exploit.

Two states exist:
- `RESONANT` — both professions meet the configured minimum level.
- `HARMONIZED` — the pair is Resonant and both professions also meet the configured Mastery tier.

Default definitions cover all ten unique pairs of the Five Paths: Earthbound, Groveborn, Cave Stalker, Deep Predator, Abyss Delver, Wildborn, Tide Gardener, Iron Harvest, Timber Tide, and Wildwood.

Example:
```yaml
fate-resonance:
  enabled: true
  definitions:
    earthbound:
      enabled: true
      name: "Earthbound"
      title: "Earthbound"
      badge: "✦"
      jobs: [MINER, LUMBERJACK]
      min-level: 50
      harmonized-mastery-tier: 1
```

`v1.5.0` intentionally keeps Resonance status/QoL-oriented: title, badge, status, score, command output, diagnostics, and PlaceholderAPI. It adds no direct damage, gathering, drop-rate, or economy multiplier.

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
Rite of Rebirth resets only the selected profession's skill tree and preserves level/XP, Trials, statistics, Fate milestone claims, normal ability cooldowns, and Mastery. Because Resonance is derived from Level/Mastery, Rebirth does not alter Resonance unless those canonical values change through another admin/reset operation.

## Leaderboards
- `/cdrjobs top <job>`
- `/cdrjobs top total`
- `/cdrjobs top pvp`
- `/cdrjobs top activity <job>`
- `/cdrjobs top mastery <job>`
- `/cdrjobs top mastery-total`

## PlaceholderAPI
Global Fate Resonance:
- `%cdrjobs_resonance_unlocked%`
- `%cdrjobs_resonance_harmonized%`
- `%cdrjobs_resonance_score%`
- `%cdrjobs_resonance_names%`
- `%cdrjobs_resonance_harmonized_names%`
- `%cdrjobs_profile_resonance_unlocked%`
- `%cdrjobs_profile_resonance_harmonized%`
- `%cdrjobs_profile_resonance_score%`

Per definition, replace `<id>` with e.g. `earthbound`:
- `%cdrjobs_resonance_<id>_unlocked%`
- `%cdrjobs_resonance_<id>_harmonized%`
- `%cdrjobs_resonance_<id>_status%`
- `%cdrjobs_resonance_<id>_display%`
- `%cdrjobs_resonance_<id>_title%`
- `%cdrjobs_resonance_<id>_badge%`

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
- derived Fate Resonance from canonical progression state
- post-Lv.100 Mastery
- transactional Rebirth respec/refund handling
- cached leaderboard reads
- PlaceholderAPI optional
- Vault optional for Rebirth economy fees
- public Java API + Bukkit events
- GitHub Actions `clean verify`
- granular admin reset/debug/export tools

## Admin commands
Existing admin/debug commands remain available, including progression, Fate, Mastery, Rebirth, reset, inspect and export tools. Fate Resonance adds:
- `/cdrjobsadmin resonancedebug <player>`

## Server testing
See [`docs/TESTING.md`](docs/TESTING.md), [`docs/RELEASE_PROCESS.md`](docs/RELEASE_PROCESS.md), and [`docs/updates/v1.5.0.md`](docs/updates/v1.5.0.md).

## Developer API
See [`docs/API.md`](docs/API.md).

## Build
Requires Java 21 and Paper API 1.21.4.
```bash
mvn clean verify
```

MENKIESTESParty remains intentionally optional and is not a dependency of the standalone release.
