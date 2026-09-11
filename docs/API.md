# CdrJobs Public API — v0.9+

CdrJobs exposes a small stable API intended for optional integrations such as MENKIESTESParty.

## Getting the API

```java
CdrJobsPlugin plugin = (CdrJobsPlugin) Bukkit.getPluginManager().getPlugin("CdrJobs");
CdrJobsAPI api = plugin.getApi();
```

Integration plugins should declare `softdepend: [CdrJobs]` unless CdrJobs is truly mandatory.

## Read methods
- `getProgress(UUID, JobType)`
- `getLevel(UUID, JobType)`
- `getXp(UUID, JobType)`
- `getFateEssence(UUID)`
- `getRankTitle(UUID, JobType)`
- `getHighestProfession(UUID)`

## Controlled write
- `addXp(Player, JobType, long)` — uses normal CdrJobs progression, Fate milestones and XP events. It does **not** emit a profession activity event.

## Bukkit events

### ProfessionActionEvent
Fired only after a real profession activity passes CdrJobs anti-exploit validation.

Action types:
- `MINE_ORE`
- `HARVEST_CROP`
- `KILL_ENTITY`
- `CHOP_LOG`
- `CATCH_FISH`

This is the recommended event for Party Project contribution tracking.

### ProfessionXpGainEvent
Fired whenever CdrJobs progression receives XP, including API/admin progression.

### ProfessionLevelUpEvent
Fired for each crossed level.

## Integration rule
Never read CdrJobs SQLite directly from another plugin. Use API/events so database schema can evolve independently.
