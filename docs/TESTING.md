# CdrJobs Production Test Matrix

Run `/cdrjobsadmin diagnose` first.

Expected for `v1.6.0` candidate:
- plugin version `1.6.0`
- schema `9`
- five professions available
- Mastery/Contracts/Fate Resonance load normally
- no startup exception
- no SQLite lock warning during normal startup

## Core persistence
- Fresh server creates `plugins/CdrJobs/cdrjobs.db`.
- Upgrade an existing database without deleting it.
- `/cdrjobs` opens all five professions.
- `/cdrjobs stats` shows all five.
- Fate Essence is awarded once per milestone per profession.
- Lowering/resetting and raising a Job does not duplicate already claimed Fate milestone Essence.
- Ability cooldown survives restart.
- Rebirth cooldown survives restart.
- `/cdrjobsadmin reset <player>` resets player progression/extension data as documented.
- Reload merges new config/message defaults without deleting custom values.
- Startup pruning of expired reward-location rows causes no gameplay regression.

## GUI regression / exploit test
Test every main, Profile, profession Path, Trial and Rebirth inventory.

- Empty functional spaces are framed with the expected stained-glass theme.
- Functional buttons/items are not replaced by decorative panes.
- Miner theme: gray/purple.
- Farmer: green/lime.
- Hunter: red/purple.
- Lumberjack: orange/yellow.
- Fisher: blue/cyan.
- Main/Profile: cyan/light blue.
- Rebirth: purple/red.
- Clicking decorative panes does nothing and does not throw console errors.
- Shift-click/number-key/double-click cannot place player items into the GUI.
- Dragging across top-inventory slots is cancelled.
- Dragging an item through several GUI slots cannot duplicate/delete/move the item into the menu.
- Invalid/missing PDC action should log a useful warning rather than silently failing.
- Close/reopen every menu after a failed action to confirm the player inventory remains intact.

If Geyser/Floodgate is used on the target server, repeat basic open/click/back/Rebirth-confirm tests with one Bedrock client.

## Miner
- Natural configured ore gives XP/Trial/Contract activity.
- Player-placed ore gives no XP, including after restart.
- Piston-moved placed ore remains blocked.
- Exploded tracked ore does not leave stale tracking.
- Trial of Stone / Deep gates and Runic Surge work.
- `TrialCompleteEvent` fires exactly once for Stone and once for Deep completion.

## Farmer
- Immature crops give no XP.
- Mature configured crops give XP.
- Same location cannot immediately farm reward inside cooldown.
- Rootbound replants supported crop at age 0.
- Trial of Seed / Gaia and Verdant Bloom work.
- expired reward-location rows may be pruned after restart without granting duplicate immediate progress incorrectly.
- `TrialCompleteEvent` fires only on first completion transition.

## Hunter
- Configured naturally spawned mob gives XP.
- WHITELIST/ALL/blacklist behavior matches config.
- Spawner mob gives no XP when exclusion enabled.
- Bred mob gives no XP when exclusion enabled.
- Unlisted mob fallback behaves as configured.
- valid PvP kill passes online/playtime/same-victim/window gates.
- rejected PvP kill does not give XP/Contract/Trial reward.
- `HunterPvpRewardEvent` fires only for accepted PvP reward.
- Trial of Fang / Crimson Moon and Crimson Hunt work.
- Trial completion events fire once.

## Lumberjack
- Natural configured logs/stems give XP.
- Player-placed log gives no XP after restart.
- Piston-moved placed log remains blocked.
- Ironbark streak grants brief Resistance.
- Trial of Timber / Ancient Grove and Grove Rhythm work.
- Trial completion events fire once.

## Fisher
- Configured fish catch gives XP.
- Vanilla treasure catch gives treasure XP/count.
- Ocean catches increment ocean counter.
- After stationary AFK threshold, vanilla catch remains but CdrJobs reward pauses.
- Moving ~2 blocks resumes profession rewards.
- Trial of Tide / Abyss and Ocean's Call work.
- Trial completion events fire once.

## Mastery
- XP overflow from the level-up that reaches Lv.100 is preserved as Mastery XP.
- XP at Lv.100 goes to Mastery instead of Job XP.
- Mastery tier changes persist after restart.
- `MasteryTierUpEvent` fires once when normal progression crosses a Mastery tier.
- admin Mastery tools remain deterministic.
- Rebirth preserves Mastery; resetjob/full reset remove affected Mastery as documented.

## Contracts
- Daily/Weekly assignment stays deterministic across relog/restart.
- Valid `ProfessionActionEvent` increments only the matching assigned Job contract.
- anti-exploit-rejected activity does not increment Contracts.
- completion message/event fires once when target is first reached.
- claim grants configured XP/Fate once.
- second claim returns already claimed.
- reroll clears progress and uses one reroll allowance.
- completed/claimed contract cannot reroll.
- cycle rollover clears progress/claim/reward checkpoints/reroll state.
- simulate a reward component exception where practical and verify retry does not intentionally repay a component whose checkpoint was already recorded.
- Contract XP at Lv.100 reaches Mastery.
- `ContractCompleteEvent` and `ContractClaimEvent` each fire once for a normal successful lifecycle.

## Fate Resonance
- all ten default pair definitions load.
- both Jobs below threshold => `LOCKED`.
- both at configured level threshold => `RESONANT`.
- both also at required Mastery => `HARMONIZED`.
- lowering/resetting one canonical Job immediately changes derived state.
- Rebirth alone does not change Resonance when Level/Mastery stay unchanged.
- custom config definitions/reload behave correctly.
- duplicate/invalid pair warnings appear without disabling valid definitions.

## Rebirth
- quote matches invested skill ranks and Fate costs.
- successful Rebirth resets only selected skill tree.
- level/XP/Trials/statistics/milestone claims/ability cooldown/Mastery stay preserved.
- refund is applied once.
- cooldown persists after restart.
- normal double-click/spam does not create duplicate refund.
- Vault fee failure/rollback path behaves as documented if Vault mode is enabled.
- `RebirthEvent` fires after success and exposes `forced=true` for admin force-respec.

## Public API v2 smoke test
Create a temporary integration plugin with `softdepend: [CdrJobs]`.

### Discovery
Resolve API through Bukkit services:

```java
RegisteredServiceProvider<CdrJobsAPI> registration =
        Bukkit.getServicesManager().getRegistration(CdrJobsAPI.class);
```

Expected:
- registration exists after CdrJobs enable.
- `registration.getProvider().getApiVersion() == 2`.
- provider disappears when CdrJobs disables.
- legacy `((CdrJobsPlugin) plugin).getApi()` still returns the same functional API surface.

### v1 compatibility
Verify these calls still work:
- `getProgress`
- `getLevel`
- `getXp`
- `getFateEssence`
- `getRankTitle`
- `getHighestProfession`
- `addXp`

### v2 snapshots
Verify on a fresh player and a progressed player:
- `getHighestProfessions`
- `getPlayerSnapshot`
- skill rank/map reads
- statistics/flags/Trial reads
- ability cooldown reads
- Mastery snapshot
- Daily + Weekly Contract snapshots
- Resonance snapshots/score
- all leaderboard types

Returned maps/lists/snapshots should be treated as immutable; integration code must not rely on mutating CdrJobs state through them.

### Event matrix
Temporary listener plugin should receive:
- one `ProfessionActionEvent` per accepted profession action
- `ProfessionXpGainEvent` after progression update
- `ProfessionLevelUpEvent` on level crossing
- `SkillUpgradeEvent` after a successful skill purchase
- `ProfessionAwakeningEvent` when final awakened node unlocks
- `TrialCompleteEvent` on each incomplete -> complete transition
- `MasteryTierUpEvent` on a normal Mastery tier crossing
- `HunterPvpRewardEvent` only after valid Hunter PvP reward
- `ContractCompleteEvent` at target transition
- `ContractClaimEvent` after successful reward finalization
- `RebirthEvent` after successful Rebirth

Admin/API `addXp` must **not** emit `ProfessionActionEvent` because it is not a real anti-exploit-approved gameplay action.

## Final regression gate
Before stable promotion:
- full restart, not only `/reload`
- no new console exception
- no SQLite lock storm
- all five profession XP paths work
- all anti-exploit guards still reject their known exploit paths
- Profile, leaderboards and PlaceholderAPI remain correct
- API v1 compatibility and API v2 integration listener both pass
- GUI click/drag regression passes
- Contracts/Mastery/Resonance/Rebirth persistence/regression passes
