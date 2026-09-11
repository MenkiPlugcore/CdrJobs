# CdrJobs Public API v2

CdrJobs exposes a stable public Java facade and Bukkit events for optional integrations such as MENKIESTESParty, scoreboards, quest systems and custom server modules.

`CdrJobsAPI.API_VERSION` is `2` in CdrJobs `v1.6.0`.

API v2 keeps the original v1 methods for source compatibility while adding immutable snapshots for the newer standalone systems.

## Getting the API

### Recommended — Bukkit ServicesManager

```java
RegisteredServiceProvider<CdrJobsAPI> registration =
        Bukkit.getServicesManager().getRegistration(CdrJobsAPI.class);

if (registration == null) {
    // CdrJobs is not available.
    return;
}

CdrJobsAPI api = registration.getProvider();
```

### Backward-compatible plugin access

```java
CdrJobsPlugin plugin = (CdrJobsPlugin) Bukkit.getPluginManager().getPlugin("CdrJobs");
CdrJobsAPI api = plugin.getApi();
```

Integration plugins should normally declare:

```yaml
softdepend: [CdrJobs]
```

Use `depend: [CdrJobs]` only when your plugin cannot function without CdrJobs.

## API version check

```java
if (api.getApiVersion() < 2) {
    // Fall back to the v1 surface or disable v2-only integration.
}
```

## v1-compatible methods

These methods remain available in API v2:

- `getProgress(UUID, JobType)`
- `getLevel(UUID, JobType)`
- `getXp(UUID, JobType)`
- `getFateEssence(UUID)`
- `getRankTitle(UUID, JobType)`
- `getHighestProfession(UUID)`
- `addXp(Player, JobType, long)`

`addXp(...)` uses the normal CdrJobs progression pipeline, including level-ups, Fate milestone claims, Mastery routing and XP/level events. It does not fabricate a profession activity event.

## Player and profession snapshots

```java
CdrJobsAPI.PlayerSnapshot snapshot = api.getPlayerSnapshot(player.getUniqueId());
```

The snapshot includes:

- total Five Paths level
- all tied highest professions
- Fate Essence
- completed Trial count
- unlocked skill-node count
- awakened professions
- total Mastery tiers and XP
- Resonance unlocked/harmonized counts and score
- immutable per-profession snapshots

Useful methods:

- `getHighestProfessions(UUID)`
- `getPlayerSnapshot(UUID)`
- `getSkillRanks(UUID)`
- `getSkillRank(UUID, String)`
- `getStatistics(UUID, JobType)`
- `getStatistic(UUID, JobType, String)`
- `getFlags(UUID, JobType)`
- `getFlag(UUID, JobType, String)`
- `getTrialProgress(UUID, JobType, String)`
- `getMinerTrialProgress(UUID)`
- `getAbilityReadyAt(UUID, String)`
- `getAbilityCooldownSeconds(UUID, String)`

Miner Trials use their dedicated `MinerTrialProgress` storage. The generic `getTrialProgress(...)` helper is intended for the profession-counter based Trials used by Farmer, Hunter, Lumberjack and Fisher.

## Mastery

```java
CdrJobsAPI.MasterySnapshot mastery = api.getMastery(uuid, JobType.MINER);
```

The snapshot contains tier, current tier XP, required XP, total Mastery XP, title, badge and maxed state.

## Profession Contracts

```java
CdrJobsAPI.ContractSnapshot daily = api.getContract(uuid, "daily");
CdrJobsAPI.ContractSnapshot weekly = api.getContract(uuid, "weekly");
```

Contract snapshots expose assignment/cycle, profession, progress, target, completion/claim state, rewards and reroll usage.

Contract state is read-only through API v2. Use CdrJobs commands/UI for claim and reroll so anti-dupe handling stays centralized.

## Fate Resonance

```java
List<CdrJobsAPI.ResonanceSnapshot> states = api.getResonances(uuid);
int score = api.getResonanceScore(uuid);
```

Each Resonance snapshot includes both professions, thresholds, current levels/Mastery tiers, Resonant state and Harmonized state.

Resonance is derived state and has no independent player ledger.

## Leaderboards

```java
List<CdrJobsAPI.LeaderboardEntry> topMiner =
        api.getLeaderboard(CdrJobsAPI.LeaderboardType.PROFESSION, JobType.MINER);

List<CdrJobsAPI.LeaderboardEntry> topTotal =
        api.getLeaderboard(CdrJobsAPI.LeaderboardType.TOTAL_LEVEL, null);
```

Available types:

- `PROFESSION` — requires a Job
- `TOTAL_LEVEL`
- `HUNTER_PVP`
- `ACTIVITY` — requires a Job
- `MASTERY` — requires a Job
- `MASTERY_TOTAL`

The API reuses CdrJobs' cached leaderboard service; integrations should not query SQLite directly.

## Bukkit events

### Existing events

`ProfessionActionEvent`

Fired only after a real profession activity passes CdrJobs anti-exploit validation. Action types are `MINE_ORE`, `HARVEST_CROP`, `KILL_ENTITY`, `CHOP_LOG`, and `CATCH_FISH`. This remains the recommended event for Party Project contribution tracking.

`ProfessionXpGainEvent`

Fired whenever CdrJobs progression receives profession XP, including API/admin progression and XP routed toward Mastery.

`ProfessionLevelUpEvent`

Fired for every crossed profession level.

### API v2 events

`SkillUpgradeEvent`

Fired after a skill rank is purchased successfully. Exposes profession, skill id, resulting rank and Fate Essence cost.

`ProfessionAwakeningEvent`

Fired when the final awakened skill of a profession is unlocked.

`TrialCompleteEvent`

Fired once on the incomplete -> completed transition for a profession Trial.

`MasteryTierUpEvent`

Fired when normal profession XP raises a Mastery tier. Exposes old/new tier and canonical total Mastery XP.

`HunterPvpRewardEvent`

Fired only after a Hunter PvP kill passes CdrJobs anti-farm gates and resolves to a valid reward.

`ContractCompleteEvent`

Fired when a Daily/Weekly Contract first reaches its target.

`ContractClaimEvent`

Fired after all configured Contract reward components are delivered and the Contract is finalized as claimed.

`RebirthEvent`

Fired after a successful normal or admin-forced Rite of Rebirth. Exposes refund, invested nodes/ranks and whether the operation was forced.

## Event guarantees

API v2 events are observational/read-only Bukkit events. They are intentionally not cancellable. Integrations should react to completed CdrJobs state changes instead of trying to mutate the core transaction mid-flight.

`ProfessionActionEvent` remains the canonical anti-exploit-approved activity signal. Do not infer valid gathering/hunting activity from raw Bukkit block/entity events in an integration plugin.

## Threading

Treat CdrJobs API calls as main-thread operations unless a method is explicitly documented otherwise. The current persistence implementation uses synchronized SQLite access and is designed around Paper gameplay callbacks.

Do not invoke progression writes asynchronously.

## Integration rules

1. Never read or write `cdrjobs.db` directly from another plugin.
2. Prefer immutable API snapshots and Bukkit events.
3. Check `getApiVersion()` before using v2-only methods.
4. Do not cache API snapshot objects indefinitely; fetch a fresh snapshot when current state matters.
5. Keep CdrJobs optional unless your plugin truly requires it.
6. For Party contribution, consume `ProfessionActionEvent`, not raw Minecraft events.

These rules allow CdrJobs storage/config internals to evolve without breaking integrations.
