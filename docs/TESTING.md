# CdrJobs Production Test Matrix

Run `/cdrjobsadmin diagnose` first. Expected for v0.9: schema `9`, five professions available, and no startup exception.

## Core
- Fresh server creates `plugins/CdrJobs/cdrjobs.db`
- Upgrade an existing v0.1–v0.5 database without deleting it
- `/cdrjobs` opens all five professions
- `/cdrjobs stats` shows all five
- Fate Essence is awarded once per milestone per profession
- Lowering then raising a level does not duplicate previously claimed Fate Essence
- Ability cooldown survives restart
- `/cdrjobsadmin reset <player>` resets player progression/extension data
- Reload merges new config/message defaults without deleting custom values

## Miner
- Natural configured ore gives XP/trial progress
- Player-placed ore gives no XP, including after restart
- Piston-moved placed ore remains blocked
- Exploded tracked ore does not leave stale tracking
- Trial of Stone / Deep gates and Runic Surge work

## Farmer
- Immature crops give no XP
- Mature configured crops give XP
- Same location cannot immediately farm reward inside cooldown
- Rootbound replants supported crop at age 0
- Trial of Seed / Gaia and Verdant Bloom work

## Hunter
- Configured naturally spawned mob gives XP
- Spawner mob gives no XP when exclusion enabled
- Bred mob gives no XP when exclusion enabled
- Night and dangerous counters increment correctly
- Trial of Fang / Crimson Moon and Crimson Hunt work

## Lumberjack
- Natural configured logs/stems give XP
- Player-placed log gives no XP after restart
- Piston-moved placed log remains blocked
- Ironbark streak grants brief Resistance
- Trial of Timber / Ancient Grove and Grove Rhythm work

## Fisher
- Configured fish catch gives XP
- Vanilla treasure catch gives treasure XP/count
- Ocean catches increment ocean counter
- After stationary AFK threshold, vanilla catch remains but CdrJobs reward pauses
- Moving ~2 blocks resumes profession rewards
- Trial of Tide / Abyss and Ocean's Call work

## API smoke test
A temporary listener plugin should receive:
- one `ProfessionActionEvent` per valid profession action
- `ProfessionXpGainEvent` after progression update
- `ProfessionLevelUpEvent` on level crossing

Admin `addxp` must not emit `ProfessionActionEvent`.
