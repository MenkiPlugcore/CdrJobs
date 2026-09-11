package store.cadera.cdrjobs.service;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.HunterSkill;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.util.Colors;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HunterService {
    public static final String JOB = "HUNTER";
    public static final String CRIMSON_HUNT = "hunter_crimson_hunt";
    private static final String PVP_COOLDOWN_PREFIX = "hunter_pvp_";
    private static final Set<EntityType> DANGEROUS = EnumSet.of(
            EntityType.WARDEN, EntityType.WITHER, EntityType.ELDER_GUARDIAN, EntityType.RAVAGER, EntityType.EVOKER
    );

    private final CdrJobsPlugin plugin;
    private final Database database;
    private final ProfessionStore store;
    private final Map<UUID, Long> activeUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> onlineSince = new ConcurrentHashMap<>();
    private final Map<UUID, ArrayDeque<Long>> pvpRewardWindows = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastDebugReason = new ConcurrentHashMap<>();

    public HunterService(CdrJobsPlugin plugin, Database database, ProfessionStore store) {
        this.plugin = plugin;
        this.database = database;
        this.store = store;
        long now = System.currentTimeMillis();
        Bukkit.getOnlinePlayers().forEach(player -> onlineSince.put(player.getUniqueId(), now));
    }

    public int rank(Player player, HunterSkill skill) {
        return store.getSkillRank(player.getUniqueId(), skill.key());
    }

    public boolean dangerous(EntityType type) {
        return DANGEROUS.contains(type);
    }

    public void markOnline(Player player) {
        onlineSince.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void markOffline(Player player) {
        onlineSince.remove(player.getUniqueId());
    }

    public long onlineSeconds(Player player) {
        long since = onlineSince.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
        return Math.max(0L, (System.currentTimeMillis() - since) / 1000L);
    }

    public long playtimeSeconds(Player player) {
        return Math.max(0L, player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L);
    }

    public void setDebugReason(Player player, String reason) {
        lastDebugReason.put(player.getUniqueId(), reason);
    }

    public String lastDebugReason(Player player) {
        return lastDebugReason.getOrDefault(player.getUniqueId(), "No Hunter kill decision recorded since plugin start.");
    }

    public boolean tryClaimPvpReward(Player killer, Player victim) {
        if (plugin.getConfig().getBoolean("hunter.pvp.anti-farm.block-same-ip", false) && sameIp(killer, victim)) {
            setDebugReason(killer, "PvP denied: killer and victim share the same IP.");
            return false;
        }

        long minOnline = Math.max(0L, plugin.getConfig().getLong("hunter.pvp.anti-farm.minimum-online-seconds", 60L));
        long victimOnline = onlineSeconds(victim);
        if (victimOnline < minOnline) {
            setDebugReason(killer, "PvP denied: victim online " + victimOnline + "s / required " + minOnline + "s.");
            return false;
        }

        long minPlaytime = Math.max(0L, plugin.getConfig().getLong("hunter.pvp.anti-farm.minimum-playtime-seconds", 300L));
        long victimPlaytime = playtimeSeconds(victim);
        if (victimPlaytime < minPlaytime) {
            setDebugReason(killer, "PvP denied: victim playtime " + victimPlaytime + "s / required " + minPlaytime + "s.");
            return false;
        }

        if (!withinPvpWindowLimit(killer)) return false;

        long cooldownSeconds = Math.max(0L, plugin.getConfig().getLong("hunter.pvp.same-victim-cooldown-seconds", 300L));
        long now = System.currentTimeMillis();
        if (cooldownSeconds > 0L) {
            String key = PVP_COOLDOWN_PREFIX + victim.getUniqueId();
            long readyAt = store.getAbilityReadyAt(killer.getUniqueId(), key);
            if (readyAt > now) {
                setDebugReason(killer, "PvP denied: same-victim cooldown " + ((readyAt - now + 999L) / 1000L) + "s remaining.");
                return false;
            }
            store.setAbilityReadyAt(killer.getUniqueId(), key, safeAddMillis(now, cooldownSeconds));
        }

        recordWindowReward(killer, now);
        setDebugReason(killer, "PvP reward accepted against " + victim.getName() + ".");
        return true;
    }

    private boolean sameIp(Player a, Player b) {
        InetSocketAddress aa = a.getAddress();
        InetSocketAddress bb = b.getAddress();
        if (aa == null || bb == null || aa.getAddress() == null || bb.getAddress() == null) return false;
        return aa.getAddress().equals(bb.getAddress());
    }

    private boolean withinPvpWindowLimit(Player killer) {
        int max = Math.max(0, plugin.getConfig().getInt("hunter.pvp.anti-farm.kill-streak.max-rewards-per-window", 5));
        long windowSeconds = Math.max(1L, plugin.getConfig().getLong("hunter.pvp.anti-farm.kill-streak.window-seconds", 120L));
        if (max <= 0) return true;
        long cutoff = System.currentTimeMillis() - windowSeconds * 1000L;
        ArrayDeque<Long> queue = pvpRewardWindows.computeIfAbsent(killer.getUniqueId(), ignored -> new ArrayDeque<>());
        while (!queue.isEmpty() && queue.peekFirst() < cutoff) queue.removeFirst();
        if (queue.size() >= max) {
            setDebugReason(killer, "PvP denied: kill-streak limit " + queue.size() + "/" + max + " within " + windowSeconds + "s.");
            return false;
        }
        return true;
    }

    private void recordWindowReward(Player killer, long now) {
        pvpRewardWindows.computeIfAbsent(killer.getUniqueId(), ignored -> new ArrayDeque<>()).addLast(now);
    }

    private long safeAddMillis(long now, long seconds) {
        try {
            return Math.addExact(now, Math.multiplyExact(seconds, 1000L));
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    public int recentPvpRewards(Player killer) {
        long windowSeconds = Math.max(1L, plugin.getConfig().getLong("hunter.pvp.anti-farm.kill-streak.window-seconds", 120L));
        long cutoff = System.currentTimeMillis() - windowSeconds * 1000L;
        ArrayDeque<Long> queue = pvpRewardWindows.computeIfAbsent(killer.getUniqueId(), ignored -> new ArrayDeque<>());
        while (!queue.isEmpty() && queue.peekFirst() < cutoff) queue.removeFirst();
        return queue.size();
    }

    public List<String> debugLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("§3Hunter Debug §8— §f" + player.getName());
        lines.add("§fLast decision: §e" + lastDebugReason(player));
        lines.add("§fPvP enabled: §b" + plugin.getConfig().getBoolean("hunter.pvp.enabled", true));
        lines.add("§fMob filter: §b" + plugin.getConfig().getString("hunter.mob-filter.mode", "ALL") + " §7blacklist=" + plugin.getConfig().getStringList("hunter.mob-filter.blacklist").size());
        lines.add("§fOnline time: §b" + onlineSeconds(player) + "s");
        lines.add("§fTotal playtime: §b" + playtimeSeconds(player) + "s");
        lines.add("§fRecent rewarded PvP kills: §b" + recentPvpRewards(player));
        lines.add("§fSame-IP block: §b" + plugin.getConfig().getBoolean("hunter.pvp.anti-farm.block-same-ip", false));
        lines.add("§fMinimum victim online: §b" + plugin.getConfig().getLong("hunter.pvp.anti-farm.minimum-online-seconds", 60L) + "s");
        lines.add("§fMinimum victim playtime: §b" + plugin.getConfig().getLong("hunter.pvp.anti-farm.minimum-playtime-seconds", 300L) + "s");
        return lines;
    }

    public void recordMobReward(Player player) {
        store.incrementCounter(player.getUniqueId(), JOB, "mob_kills", 1L);
    }

    public void recordPvpReward(Player player) {
        store.incrementCounter(player.getUniqueId(), JOB, "pvp_kills", 1L);
    }

    public boolean tryUpgrade(Player p, HunterSkill s) {
        int r = rank(p, s);
        if (r >= s.maxRank()) {
            p.sendMessage(Colors.color(plugin.prefix() + plugin.message("max-skill")));
            return false;
        }
        int lv = database.getProgress(p.getUniqueId(), JobType.HUNTER).level();
        if (lv < s.requiredLevel()) {
            p.sendMessage(Colors.color(plugin.prefix() + plugin.message("skill-requirement")));
            return false;
        }
        if (s.prerequisite() != null && rank(p, s.prerequisite()) < s.prerequisiteRank()) {
            p.sendMessage(Colors.color(plugin.prefix() + plugin.message("skill-requirement")));
            return false;
        }
        if (s == HunterSkill.CRIMSON_HUNT && !fangComplete(p)) {
            p.sendMessage(Colors.color(plugin.prefix() + plugin.message("trial-required").replace("%trial%", "Trial of Fang")));
            return false;
        }
        if (s == HunterSkill.APEX_PREDATOR
                && (rank(p, HunterSkill.SOULMARK) < 3
                || rank(p, HunterSkill.WARDENS_OATH) < 3
                || !moonComplete(p))) {
            p.sendMessage(Colors.color(plugin.prefix() + plugin.message("skill-requirement")));
            return false;
        }
        if (database.getFateEssence(p.getUniqueId()) < s.essenceCost()) {
            p.sendMessage(Colors.color(plugin.prefix() + plugin.message("not-enough-essence")));
            return false;
        }
        database.addFateEssence(p.getUniqueId(), -s.essenceCost());
        store.setSkillRank(p.getUniqueId(), s.key(), r + 1);
        p.sendMessage(Colors.color(plugin.prefix() + plugin.message("skill-unlocked")
                .replace("%skill%", s.displayName())
                .replace("%rank%", String.valueOf(r + 1))));
        return true;
    }

    public long applyXp(Player p, long base, boolean dangerous) {
        double m = 1.0 + rank(p, HunterSkill.PREDATORS_INSTINCT) * 0.05 + rank(p, HunterSkill.BLOODTRAIL) * 0.05;
        long time = p.getWorld().getTime();
        if (time >= 13000 && time <= 23000) m += rank(p, HunterSkill.MOONFANG) * 0.08;
        if (dangerous) m += rank(p, HunterSkill.WARDENS_OATH) * 0.12;
        if (rank(p, HunterSkill.APEX_PREDATOR) > 0) m += 0.20;
        long xp = Math.max(1L, Math.round(base * m));
        int soul = rank(p, HunterSkill.SOULMARK);
        if (soul > 0 && Math.random() < soul * 0.04) xp *= 2;
        Long until = activeUntil.get(p.getUniqueId());
        if (until != null) {
            if (until > System.currentTimeMillis()) {
                double bonus = Math.max(0, plugin.getConfig().getDouble("hunter.abilities.crimson-hunt.xp-bonus-percent", 25.0));
                xp = Math.max(1L, Math.round(xp * (1.0 + bonus / 100.0)));
            } else {
                activeUntil.remove(p.getUniqueId());
            }
        }
        return xp;
    }

    public void recordKill(Player p, boolean dangerous, boolean night) {
        UUID uuid = p.getUniqueId();
        store.incrementCounter(uuid, JOB, "kills", 1);
        if (night) store.incrementCounter(uuid, JOB, "night_kills", 1);
        if (dangerous) store.incrementCounter(uuid, JOB, "dangerous_kills", 1);

        long kills = kills(p);
        if (!fangComplete(p) && kills >= fangTarget()) {
            store.setFlag(uuid, JOB, "trial_fang", true);
            p.sendMessage(Colors.color(plugin.prefix() + plugin.message("trial-fang-complete")));
        }
        if (!moonComplete(p)
                && kills >= moonKillTarget()
                && nightKills(p) >= moonNightTarget()
                && dangerousKills(p) >= moonDangerTarget()) {
            store.setFlag(uuid, JOB, "trial_moon", true);
            p.sendMessage(Colors.color(plugin.prefix() + plugin.message("trial-moon-complete")));
        }
    }

    public boolean activate(Player p) {
        if (rank(p, HunterSkill.CRIMSON_HUNT) <= 0) {
            p.sendMessage(Colors.color(plugin.prefix() + plugin.message("hunter-ability-locked")));
            return false;
        }
        long now = System.currentTimeMillis();
        long ready = store.getAbilityReadyAt(p.getUniqueId(), CRIMSON_HUNT);
        if (ready > now) {
            p.sendMessage(Colors.color(plugin.prefix() + plugin.message("hunter-ability-cooldown")
                    .replace("%seconds%", String.valueOf((ready - now + 999) / 1000))));
            return false;
        }
        int duration = Math.max(1, plugin.getConfig().getInt("hunter.abilities.crimson-hunt.duration-seconds", 20));
        int cooldown = Math.max(duration, plugin.getConfig().getInt("hunter.abilities.crimson-hunt.cooldown-seconds", 300));
        activeUntil.put(p.getUniqueId(), now + duration * 1000L);
        store.setAbilityReadyAt(p.getUniqueId(), CRIMSON_HUNT, now + cooldown * 1000L);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration * 20, 0, true, false, true));
        p.playSound(p.getLocation(), Sound.ENTITY_WOLF_GROWL, 0.8f, 0.8f);
        p.sendMessage(Colors.color(plugin.prefix() + plugin.message("crimson-hunt-activated")
                .replace("%duration%", String.valueOf(duration))));
        return true;
    }

    public long kills(Player p) {
        return store.getTrialProgress(p.getUniqueId(), JOB, "kills");
    }

    public long nightKills(Player p) {
        return store.getTrialProgress(p.getUniqueId(), JOB, "night_kills");
    }

    public long dangerousKills(Player p) {
        return store.getTrialProgress(p.getUniqueId(), JOB, "dangerous_kills");
    }

    public boolean fangComplete(Player p) {
        return store.getFlag(p.getUniqueId(), JOB, "trial_fang");
    }

    public boolean moonComplete(Player p) {
        return store.getFlag(p.getUniqueId(), JOB, "trial_moon");
    }

    public int fangTarget() {
        return Math.max(1, plugin.getConfig().getInt("hunter.trials.trial-of-fang.kills", 300));
    }

    public int moonKillTarget() {
        return Math.max(1, plugin.getConfig().getInt("hunter.trials.trial-of-crimson-moon.kills", 1200));
    }

    public int moonNightTarget() {
        return Math.max(1, plugin.getConfig().getInt("hunter.trials.trial-of-crimson-moon.night-kills", 250));
    }

    public int moonDangerTarget() {
        return Math.max(1, plugin.getConfig().getInt("hunter.trials.trial-of-crimson-moon.dangerous-kills", 10));
    }

    public long cooldownSeconds(UUID uuid) {
        long remaining = store.getAbilityReadyAt(uuid, CRIMSON_HUNT) - System.currentTimeMillis();
        return remaining <= 0 ? 0 : (remaining + 999) / 1000;
    }
}
