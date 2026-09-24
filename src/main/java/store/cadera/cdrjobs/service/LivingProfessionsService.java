package store.cadera.cdrjobs.service;

import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.api.ProfessionActionType;
import store.cadera.cdrjobs.api.event.ContractCompleteEvent;
import store.cadera.cdrjobs.api.event.MasteryTierUpEvent;
import store.cadera.cdrjobs.api.event.ProfessionActionEvent;
import store.cadera.cdrjobs.api.event.ProfessionLevelUpEvent;
import store.cadera.cdrjobs.api.event.ProfessionXpGainEvent;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * v1.7.0 Living Professions layer.
 *
 * Action-driven features consume only accepted ProfessionActionEvent activity,
 * so Momentum, Encounters and Milestones inherit existing profession anti-exploit gates.
 * Persistent state reuses existing generic profession tables; schema remains unchanged.
 */
public final class LivingProfessionsService implements Listener {
    private static final String ENCOUNTER_ACTIVE = "living_encounter_active";
    private static final String ENCOUNTER_PROGRESS = "living_encounter_progress";
    private static final String ENCOUNTER_EXPIRES = "living_encounter_expires";
    private static final String LIFETIME_ACTIONS = "living_actions";
    private static final String WHISPER_COOLDOWN = "living_fate_whisper";

    private final CdrJobsPlugin plugin;
    private final Database database;
    private final ProfessionStore store;
    private final ProgressionService progression;
    private final LevelService levels;
    private final FateResonanceService resonance;

    private final Map<PlayerJobKey, MomentumState> momentum = new HashMap<>();
    private final Map<PlayerJobKey, Long> recentActions = new HashMap<>();
    private final Map<UUID, SessionState> sessions = new HashMap<>();

    private final Map<UUID, EncounterState> activeEncounters = new HashMap<>();
    private final Set<UUID> encounterStateLoaded = new HashSet<>();
    private final Map<PlayerJobKey, Long> encounterReadyAt = new HashMap<>();
    private final Map<UUID, Long> whisperReadyAt = new HashMap<>();

    private final Map<UUID, PendingFeedback> pendingFeedback = new HashMap<>();
    private final Set<UUID> feedbackScheduled = new HashSet<>();
    private final Set<PlayerJobKey> bonusGuard = new HashSet<>();
    private final Set<PlayerJobKey> rewardGuard = new HashSet<>();

    public LivingProfessionsService(CdrJobsPlugin plugin, Database database, ProfessionStore store,
                                    ProgressionService progression, LevelService levels,
                                    FateResonanceService resonance) {
        this.plugin = plugin;
        this.database = database;
        this.store = store;
        this.progression = progression;
        this.levels = levels;
        this.resonance = resonance;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("living-professions.enabled", true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAction(ProfessionActionEvent event) {
        if (!enabled()) return;
        Player player = event.getPlayer();
        if (worldBlocked(player)) return;

        long amount = Math.max(1L, event.getAmount());
        JobType job = event.getProfession();
        PlayerJobKey key = new PlayerJobKey(player.getUniqueId(), job);
        long now = System.currentTimeMillis();

        session(player).addAction(job, amount);
        recentActions.put(key, now);
        updateMomentum(player, key, amount, now);
        updateMilestones(player, job, amount);

        EncounterState encounter = encounterFor(player.getUniqueId(), now);
        if (encounter != null) {
            advanceEncounter(player, encounter, job, event.getAction(), amount, now);
        } else {
            maybeStartEncounter(player, job, event.getAction(), now);
        }

        maybeWhisper(player, job, now);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onXp(ProfessionXpGainEvent event) {
        if (!enabled()) return;
        Player player = event.getPlayer();
        if (worldBlocked(player)) return;

        JobType job = event.getProfession();
        PlayerJobKey key = new PlayerJobKey(player.getUniqueId(), job);
        long gained = Math.max(0L, event.getGainedXp());
        if (gained <= 0L) return;

        session(player).addXp(job, gained);

        if (bonusGuard.contains(key) || rewardGuard.contains(key)) return;

        long bonus = 0L;
        Long actionAt = recentActions.get(key);
        boolean actionXp = actionAt != null && System.currentTimeMillis() - actionAt <= 250L;
        if (actionXp) {
            bonus = momentumBonus(player, key, gained);
            recentActions.remove(key);
            if (bonus > 0L) {
                bonusGuard.add(key);
                try {
                    progression.addXp(player, job, bonus);
                } finally {
                    bonusGuard.remove(key);
                }
            }
        }

        queueFeedback(player, job, saturatingAdd(gained, bonus));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelUp(ProfessionLevelUpEvent event) {
        if (!enabled()) return;
        Player player = event.getPlayer();
        if (worldBlocked(player)) return;

        session(player).addLevel(event.getProfession());
        if (!plugin.getConfig().getBoolean("living-professions.feedback.levelup-title", true)) return;

        player.sendTitle("§d✦ PATH ASCENDED ✦",
                "§f" + event.getProfession().displayName() + " §8• §bLv." + event.getNewLevel(),
                8, 42, 10);
        feedbackBurst(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMasteryTier(MasteryTierUpEvent event) {
        Player player = event.getPlayer();
        if (!enabled() || worldBlocked(player)
                || !plugin.getConfig().getBoolean("living-professions.feedback.mastery-title", true)) return;

        player.sendTitle("§6✦ MASTERY ADVANCED ✦",
                "§f" + event.getProfession().displayName() + " §8• §6Tier " + event.getNewTier(),
                8, 45, 10);
        feedbackBurst(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onContractComplete(ContractCompleteEvent event) {
        Player player = event.getPlayer();
        if (!enabled() || worldBlocked(player)
                || !plugin.getConfig().getBoolean("living-professions.feedback.contract-title", true)) return;

        player.sendTitle("§6✦ CONTRACT COMPLETE ✦",
                "§f" + event.getProfession().displayName() + " §8• §e" + event.getCadence(),
                6, 36, 8);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        sessions.remove(uuid);
        activeEncounters.remove(uuid);
        encounterStateLoaded.remove(uuid);
        whisperReadyAt.remove(uuid);
        pendingFeedback.remove(uuid);
        feedbackScheduled.remove(uuid);

        momentum.keySet().removeIf(key -> key.player().equals(uuid));
        recentActions.keySet().removeIf(key -> key.player().equals(uuid));
        encounterReadyAt.keySet().removeIf(key -> key.player().equals(uuid));
        bonusGuard.removeIf(key -> key.player().equals(uuid));
        rewardGuard.removeIf(key -> key.player().equals(uuid));
    }

    public void sendSession(Player player) {
        if (!enabled() || !plugin.getConfig().getBoolean("living-professions.session.enabled", true)) {
            player.sendMessage("§cLiving Professions session tracking sedang dinonaktifkan.");
            return;
        }

        SessionState state = session(player);
        long elapsed = Math.max(0L, System.currentTimeMillis() - state.startedAt);
        player.sendMessage("§8§m                                      ");
        player.sendMessage("§d✦ §fADVENTURE SESSION §8• §7" + formatDuration(elapsed));

        boolean any = false;
        for (JobType job : JobType.values()) {
            long actions = state.actions.getOrDefault(job, 0L);
            long xp = state.xp.getOrDefault(job, 0L);
            long levelUps = state.levels.getOrDefault(job, 0L);
            long encounters = state.encounters.getOrDefault(job, 0L);
            if (actions == 0L && xp == 0L && levelUps == 0L && encounters == 0L) continue;

            any = true;
            double bonus = currentMomentumBonus(player.getUniqueId(), job);
            long lifetime = store.getCounter(player.getUniqueId(), job.name(), LIFETIME_ACTIONS);
            player.sendMessage("§7• §b" + job.displayName()
                    + " §8| §f" + actions + " actions"
                    + " §8| §a+" + xp + " XP"
                    + (levelUps > 0 ? " §8| §e+" + levelUps + " level" : "")
                    + (encounters > 0 ? " §8| §d" + encounters + " encounter" : ""));
            player.sendMessage("  §8Momentum: §f+" + formatPercent(bonus)
                    + "% §8• §7Lifetime actions: §f" + lifetime + nextMilestoneSuffix(lifetime));
        }

        if (!any) player.sendMessage("§7Belum ada aktivitas profession pada sesi ini.");

        EncounterState encounter = encounterFor(player.getUniqueId(), System.currentTimeMillis());
        if (encounter != null) {
            EncounterDefinition def = definition(encounter.job);
            long left = Math.max(0L, (encounter.expiresAt - System.currentTimeMillis()) / 1000L);
            player.sendMessage("§dActive Encounter: §f" + def.name + " §8• §7"
                    + encounter.progress + "/" + def.target + " §8• §7" + left + "s");
        }
        player.sendMessage("§8§m                                      ");
    }

    private void updateMomentum(Player player, PlayerJobKey key, long amount, long now) {
        if (!plugin.getConfig().getBoolean("living-professions.momentum.enabled", true)) return;

        long expire = Math.max(5L,
                plugin.getConfig().getLong("living-professions.momentum.expire-seconds", 45L)) * 1000L;
        int perStage = Math.max(1,
                plugin.getConfig().getInt("living-professions.momentum.actions-per-stage", 12));
        int maxStage = Math.max(1,
                plugin.getConfig().getInt("living-professions.momentum.max-stage", 4));

        MomentumState state = momentum.computeIfAbsent(key, ignored -> new MomentumState());
        if (state.lastAction > 0L && now - state.lastAction > expire) {
            state.actions = 0L;
            state.stage = 0;
            state.fractionalBonus = 0D;
        }

        int oldStage = state.stage;
        long cap = (long) perStage * maxStage;
        state.actions = Math.min(cap, saturatingAdd(state.actions, amount));
        state.stage = Math.min(maxStage, (int) (state.actions / perStage));
        state.lastAction = now;

        if (state.stage > oldStage
                && plugin.getConfig().getBoolean("living-professions.feedback.momentum-stage-sound", true)) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    0.45f, 1.35f + Math.min(0.35f, state.stage * 0.07f));
        }
    }

    private long momentumBonus(Player player, PlayerJobKey key, long gainedXp) {
        if (!plugin.getConfig().getBoolean("living-professions.momentum.enabled", true)) return 0L;
        MomentumState state = momentum.get(key);
        if (state == null || state.stage <= 0) return 0L;

        long expire = Math.max(5L,
                plugin.getConfig().getLong("living-professions.momentum.expire-seconds", 45L)) * 1000L;
        if (System.currentTimeMillis() - state.lastAction > expire) return 0L;

        double percent = currentMomentumBonus(player.getUniqueId(), key.job());
        if (percent <= 0D) return 0L;

        double raw = (gainedXp * percent / 100D) + state.fractionalBonus;
        if (!Double.isFinite(raw) || raw <= 0D) return 0L;

        long bonus = raw >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.floor(raw);
        state.fractionalBonus = raw - bonus;
        return Math.max(0L, bonus);
    }

    private double currentMomentumBonus(UUID uuid, JobType job) {
        if (!plugin.getConfig().getBoolean("living-professions.momentum.enabled", true)) return 0D;
        MomentumState state = momentum.get(new PlayerJobKey(uuid, job));
        if (state == null || state.stage <= 0) return 0D;

        long expire = Math.max(5L,
                plugin.getConfig().getLong("living-professions.momentum.expire-seconds", 45L)) * 1000L;
        if (System.currentTimeMillis() - state.lastAction > expire) return 0D;

        double perStage = Math.max(0D,
                plugin.getConfig().getDouble("living-professions.momentum.xp-bonus-per-stage-percent", 2D));
        double max = Math.max(0D,
                plugin.getConfig().getDouble("living-professions.momentum.max-xp-bonus-percent", 8D));
        return Math.min(max, state.stage * perStage);
    }

    private void queueFeedback(Player player, JobType job, long gainedXp) {
        if (!plugin.getConfig().getBoolean("living-professions.feedback.actionbar", true)) return;

        UUID uuid = player.getUniqueId();
        PendingFeedback previous = pendingFeedback.get(uuid);
        if (previous != null && previous.job == job) {
            previous.xp = saturatingAdd(previous.xp, gainedXp);
        } else {
            pendingFeedback.put(uuid, new PendingFeedback(job, gainedXp));
        }

        if (!feedbackScheduled.add(uuid)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            feedbackScheduled.remove(uuid);
            PendingFeedback pending = pendingFeedback.remove(uuid);
            Player online = plugin.getServer().getPlayer(uuid);
            if (online == null || pending == null || !online.isOnline() || worldBlocked(online)) return;
            sendActionbar(online, pending);
        });
    }

    private void sendActionbar(Player player, PendingFeedback pending) {
        JobProgress progress = database.getProgress(player.getUniqueId(), pending.job);
        double momentumBonus = currentMomentumBonus(player.getUniqueId(), pending.job);
        String momentumText = momentumBonus > 0D
                ? " • Momentum +" + formatPercent(momentumBonus) + "%" : "";

        EncounterState encounter = encounterFor(player.getUniqueId(), System.currentTimeMillis());
        String encounterText = "";
        if (encounter != null && encounter.job == pending.job) {
            EncounterDefinition def = definition(encounter.job);
            encounterText = " • " + def.name + " " + encounter.progress + "/" + def.target;
        }

        String text;
        if (progress.level() >= levels.maxLevel()) {
            text = "✦ " + pending.job.displayName() + " • +" + pending.xp + " XP • Lv."
                    + levels.maxLevel() + " MAX" + momentumText + encounterText;
        } else {
            long required = Math.max(1L, levels.xpRequiredForNextLevel(progress.level()));
            double pct = Math.max(0D, Math.min(100D, progress.xp() * 100D / required));
            text = "✦ " + pending.job.displayName() + " • +" + pending.xp + " XP • Lv."
                    + progress.level() + " " + bar(pct) + " " + Math.round(pct) + "%"
                    + momentumText + encounterText;
        }
        player.sendActionBar(Component.text(text));
    }

    /** Loads persistent encounter state at most once per login, then serves the hot path from memory. */
    private EncounterState encounterFor(UUID uuid, long now) {
        EncounterState cached = activeEncounters.get(uuid);
        if (cached != null) {
            if (cached.expiresAt > now) return cached;
            clearEncounter(uuid, cached);
            return null;
        }

        if (!encounterStateLoaded.add(uuid)) return null;

        for (JobType job : JobType.values()) {
            if (!store.getFlag(uuid, job.name(), ENCOUNTER_ACTIVE)) continue;

            long expiresAt = store.getCounter(uuid, job.name(), ENCOUNTER_EXPIRES);
            long progress = store.getCounter(uuid, job.name(), ENCOUNTER_PROGRESS);
            EncounterState loaded = new EncounterState(job, progress, expiresAt);
            if (expiresAt <= now) {
                clearEncounter(uuid, loaded);
                continue;
            }

            activeEncounters.put(uuid, loaded);
            return loaded;
        }
        return null;
    }

    private void maybeStartEncounter(Player player, JobType job, ProfessionActionType action, long now) {
        if (!plugin.getConfig().getBoolean("living-professions.encounters.enabled", true)) return;
        if (action != actionFor(job)) return;

        double chance = Math.max(0D, Math.min(100D,
                plugin.getConfig().getDouble("living-professions.encounters.chance-percent", 1.5D)));
        if (chance <= 0D || ThreadLocalRandom.current().nextDouble(100D) >= chance) return;

        PlayerJobKey key = new PlayerJobKey(player.getUniqueId(), job);
        long readyAt = encounterReadyAt.computeIfAbsent(key,
                ignored -> store.getAbilityReadyAt(player.getUniqueId(), encounterCooldownKey(job)));
        if (readyAt > now) return;

        EncounterDefinition def = definition(job);
        long expiresAt = now + def.durationSeconds * 1000L;
        long cooldown = Math.max(0L,
                plugin.getConfig().getLong("living-professions.encounters.cooldown-seconds", 300L)) * 1000L;
        long nextReady = now + cooldown;

        EncounterState state = new EncounterState(job, 0L, expiresAt);
        activeEncounters.put(player.getUniqueId(), state);
        encounterStateLoaded.add(player.getUniqueId());
        encounterReadyAt.put(key, nextReady);

        store.setFlag(player.getUniqueId(), job.name(), ENCOUNTER_ACTIVE, true);
        store.setCounter(player.getUniqueId(), job.name(), ENCOUNTER_PROGRESS, 0L);
        store.setCounter(player.getUniqueId(), job.name(), ENCOUNTER_EXPIRES, expiresAt);
        store.setAbilityReadyAt(player.getUniqueId(), encounterCooldownKey(job), nextReady);

        player.sendTitle("§d✦ PATH ENCOUNTER ✦",
                "§f" + def.name + " §8• §7" + def.target + " actions", 8, 44, 10);
        player.sendMessage("§8[§dCdrJobs§8] §f" + def.intro);

        if (plugin.getConfig().getBoolean("living-professions.feedback.encounter-sound", true)) {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.75f, 0.8f);
        }
        if (plugin.getConfig().getBoolean("living-professions.feedback.particles", true)) {
            player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1.0, 0),
                    18, 0.6, 0.7, 0.6, 0.03);
        }
    }

    private void advanceEncounter(Player player, EncounterState state, JobType job,
                                  ProfessionActionType action, long amount, long now) {
        if (state.expiresAt <= now) {
            player.sendMessage("§8[§dCdrJobs§8] §7The encounter fades before it can be completed.");
            clearEncounter(player.getUniqueId(), state);
            return;
        }
        if (state.job != job || action != actionFor(job)) return;

        EncounterDefinition def = definition(job);
        state.progress = Math.min(def.target, saturatingAdd(state.progress, amount));
        store.setCounter(player.getUniqueId(), job.name(), ENCOUNTER_PROGRESS, state.progress);
        if (state.progress < def.target) return;

        clearEncounter(player.getUniqueId(), state);
        session(player).addEncounter(job);

        player.sendTitle("§a✦ ENCOUNTER COMPLETE ✦",
                "§f" + def.name + " §8• §a+" + def.rewardXp + " XP", 6, 42, 10);
        player.sendMessage("§8[§dCdrJobs§8] §a" + def.name
                + " completed. §7Reward: §f+" + def.rewardXp + " profession XP");

        if (plugin.getConfig().getBoolean("living-professions.feedback.encounter-sound", true)) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.15f);
        }
        if (plugin.getConfig().getBoolean("living-professions.feedback.particles", true)) {
            player.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1.0, 0),
                    22, 0.7, 0.8, 0.7, 0.04);
        }

        if (def.rewardXp > 0L) {
            PlayerJobKey key = new PlayerJobKey(player.getUniqueId(), job);
            rewardGuard.add(key);
            try {
                progression.addXp(player, job, def.rewardXp);
            } finally {
                rewardGuard.remove(key);
            }
        }
    }

    private void clearEncounter(UUID uuid, EncounterState state) {
        store.setFlag(uuid, state.job.name(), ENCOUNTER_ACTIVE, false);
        store.setCounter(uuid, state.job.name(), ENCOUNTER_PROGRESS, 0L);
        store.setCounter(uuid, state.job.name(), ENCOUNTER_EXPIRES, 0L);
        activeEncounters.remove(uuid);
        encounterStateLoaded.add(uuid);
    }

    private EncounterDefinition definition(JobType job) {
        String root = "living-professions.encounters.definitions." + job.name();
        String name = plugin.getConfig().getString(root + ".name", defaultEncounterName(job));
        String intro = plugin.getConfig().getString(root + ".intro", defaultEncounterIntro(job));
        long target = Math.max(1L,
                plugin.getConfig().getLong(root + ".target", defaultEncounterTarget(job)));
        long duration = Math.max(5L,
                plugin.getConfig().getLong(root + ".duration-seconds", defaultEncounterDuration(job)));
        long reward = Math.max(0L,
                plugin.getConfig().getLong(root + ".reward-xp", defaultEncounterReward(job)));
        return new EncounterDefinition(name, intro, target, duration, reward);
    }

    private void updateMilestones(Player player, JobType job, long amount) {
        if (!plugin.getConfig().getBoolean("living-professions.milestones.enabled", true)) return;

        long total = store.incrementCounter(player.getUniqueId(), job.name(), LIFETIME_ACTIONS, amount);
        long previous = Math.max(0L, total - amount);
        List<Long> thresholds = milestoneThresholds();
        for (int i = 0; i < thresholds.size(); i++) {
            long threshold = thresholds.get(i);
            if (previous >= threshold || total < threshold) continue;

            String title = milestoneTitle(job, i);
            player.sendTitle("§b✦ PROFESSION MILESTONE ✦",
                    "§f" + title + " §8• §7" + threshold + " actions", 8, 45, 10);
            player.sendMessage("§8[§dCdrJobs§8] §bMilestone unlocked: §f" + title
                    + " §8(§7" + threshold + " valid actions§8)");
            feedbackBurst(player);
        }
    }

    private void maybeWhisper(Player player, JobType job, long now) {
        if (!plugin.getConfig().getBoolean("living-professions.fate-whispers.enabled", true)) return;
        if (!resonance.enabled()) return;

        double chance = Math.max(0D, Math.min(100D,
                plugin.getConfig().getDouble("living-professions.fate-whispers.chance-percent", 0.35D)));
        if (chance <= 0D || ThreadLocalRandom.current().nextDouble(100D) >= chance) return;

        UUID uuid = player.getUniqueId();
        long readyAt = whisperReadyAt.computeIfAbsent(uuid,
                ignored -> store.getAbilityReadyAt(uuid, WHISPER_COOLDOWN));
        if (readyAt > now) return;

        List<FateResonanceService.State> candidates = new ArrayList<>();
        for (FateResonanceService.State state : resonance.states(uuid)) {
            if (!state.unlocked()) continue;
            FateResonanceService.Definition def = state.definition();
            if (def.first() == job || def.second() == job) candidates.add(state);
        }
        if (candidates.isEmpty()) return;

        FateResonanceService.State state = candidates.get(
                ThreadLocalRandom.current().nextInt(candidates.size()));
        long cooldown = Math.max(30L,
                plugin.getConfig().getLong("living-professions.fate-whispers.cooldown-seconds", 300L)) * 1000L;
        long nextReady = now + cooldown;
        whisperReadyAt.put(uuid, nextReady);
        store.setAbilityReadyAt(uuid, WHISPER_COOLDOWN, nextReady);

        String status = state.harmonized()
                ? "answers in perfect harmony."
                : "stirs as your paths converge.";
        player.sendMessage("§d✦ Fate whispers: §f" + state.definition().name() + " §7" + status);

        if (plugin.getConfig().getBoolean("living-professions.feedback.fate-sound", true)) {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE,
                    0.55f, state.harmonized() ? 1.35f : 1.0f);
        }
        if (plugin.getConfig().getBoolean("living-professions.feedback.particles", true)) {
            player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1.1, 0),
                    10, 0.45, 0.55, 0.45, 0.02);
        }
    }

    private void feedbackBurst(Player player) {
        if (!plugin.getConfig().getBoolean("living-professions.feedback.particles", true)) return;
        player.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1.0, 0),
                14, 0.55, 0.7, 0.55, 0.03);
    }

    private boolean worldBlocked(Player player) {
        for (String world : plugin.getConfig().getStringList("living-professions.world-blacklist")) {
            if (player.getWorld().getName().equalsIgnoreCase(world)) return true;
        }
        return false;
    }

    private SessionState session(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(),
                ignored -> new SessionState(System.currentTimeMillis()));
    }

    private ProfessionActionType actionFor(JobType job) {
        return switch (job) {
            case MINER -> ProfessionActionType.MINE_ORE;
            case FARMER -> ProfessionActionType.HARVEST_CROP;
            case HUNTER -> ProfessionActionType.KILL_ENTITY;
            case LUMBERJACK -> ProfessionActionType.CHOP_LOG;
            case FISHER -> ProfessionActionType.CATCH_FISH;
        };
    }

    private String encounterCooldownKey(JobType job) {
        return "living_encounter_" + job.name().toLowerCase(Locale.ROOT);
    }

    private List<Long> milestoneThresholds() {
        List<Long> configured = plugin.getConfig().getLongList("living-professions.milestones.thresholds");
        if (configured.isEmpty()) configured = List.of(1000L, 5000L, 25000L, 100000L);
        return configured.stream().filter(value -> value > 0L).distinct().sorted().toList();
    }

    private String nextMilestoneSuffix(long lifetime) {
        for (long threshold : milestoneThresholds()) {
            if (threshold > lifetime) return " §8• §7Next: §f" + threshold;
        }
        return " §8• §bAll milestones reached";
    }

    private String milestoneTitle(JobType job, int index) {
        String[] titles = switch (job) {
            case MINER -> new String[]{"Stonewalker", "Deep Delver", "Runeseeker", "Earthborn"};
            case FARMER -> new String[]{"Seedling", "Cultivator", "Harvest Keeper", "Verdant Sage"};
            case HUNTER -> new String[]{"Tracker", "Predator", "Bloodfang", "Apex Stalker"};
            case LUMBERJACK -> new String[]{"Woodcutter", "Grove Warden", "Ironbark", "Ancient Keeper"};
            case FISHER -> new String[]{"Riverhand", "Tidecaller", "Deep Angler", "Abyssal Mariner"};
        };
        return titles[Math.min(index, titles.length - 1)];
    }

    private String defaultEncounterName(JobType job) {
        return switch (job) {
            case MINER -> "Runic Vein";
            case FARMER -> "Blessed Harvest";
            case HUNTER -> "Marked Prey";
            case LUMBERJACK -> "Ancient Grove";
            case FISHER -> "Restless Waters";
        };
    }

    private String defaultEncounterIntro(JobType job) {
        return switch (job) {
            case MINER -> "Something ancient stirs beneath the stone...";
            case FARMER -> "The soil answers your presence. A blessed harvest has begun.";
            case HUNTER -> "A dangerous presence has been marked by your Path.";
            case LUMBERJACK -> "The forest remembers your axe. An ancient grove is watching.";
            case FISHER -> "The water stirs. A rare shoal has entered your reach.";
        };
    }

    private long defaultEncounterTarget(JobType job) {
        return switch (job) {
            case MINER -> 8L;
            case FARMER -> 20L;
            case HUNTER -> 6L;
            case LUMBERJACK -> 12L;
            case FISHER -> 3L;
        };
    }

    private long defaultEncounterDuration(JobType job) {
        return switch (job) {
            case MINER -> 45L;
            case FARMER -> 60L;
            case HUNTER -> 90L;
            case LUMBERJACK -> 60L;
            case FISHER -> 120L;
        };
    }

    private long defaultEncounterReward(JobType job) {
        return switch (job) {
            case MINER -> 320L;
            case FARMER -> 300L;
            case HUNTER -> 350L;
            case LUMBERJACK -> 320L;
            case FISHER -> 280L;
        };
    }

    private String bar(double percent) {
        int filled = (int) Math.round(Math.max(0D, Math.min(100D, percent)) / 10D);
        return "▰".repeat(filled) + "▱".repeat(Math.max(0, 10 - filled));
    }

    private String formatPercent(double value) {
        if (Math.rint(value) == value) return String.valueOf((long) value);
        return String.format(Locale.US, "%.1f", value);
    }

    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    private long saturatingAdd(long current, long amount) {
        if (amount > 0L && current > Long.MAX_VALUE - amount) return Long.MAX_VALUE;
        return current + amount;
    }

    private record PlayerJobKey(UUID player, JobType job) {}

    private static final class MomentumState {
        private long actions;
        private int stage;
        private long lastAction;
        private double fractionalBonus;
    }

    private static final class EncounterState {
        private final JobType job;
        private long progress;
        private final long expiresAt;

        private EncounterState(JobType job, long progress, long expiresAt) {
            this.job = job;
            this.progress = progress;
            this.expiresAt = expiresAt;
        }
    }

    private static final class PendingFeedback {
        private final JobType job;
        private long xp;

        private PendingFeedback(JobType job, long xp) {
            this.job = job;
            this.xp = xp;
        }
    }

    private static final class SessionState {
        private final long startedAt;
        private final EnumMap<JobType, Long> actions = new EnumMap<>(JobType.class);
        private final EnumMap<JobType, Long> xp = new EnumMap<>(JobType.class);
        private final EnumMap<JobType, Long> levels = new EnumMap<>(JobType.class);
        private final EnumMap<JobType, Long> encounters = new EnumMap<>(JobType.class);

        private SessionState(long startedAt) {
            this.startedAt = startedAt;
        }

        private void addAction(JobType job, long amount) { actions.merge(job, amount, Long::sum); }
        private void addXp(JobType job, long amount) { xp.merge(job, amount, Long::sum); }
        private void addLevel(JobType job) { levels.merge(job, 1L, Long::sum); }
        private void addEncounter(JobType job) { encounters.merge(job, 1L, Long::sum); }
    }

    private record EncounterDefinition(String name, String intro, long target,
                                       long durationSeconds, long rewardXp) {}
}
