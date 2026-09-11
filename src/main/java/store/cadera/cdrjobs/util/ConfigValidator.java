package store.cadera.cdrjobs.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.model.JobType;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ConfigValidator {
    private static final String[] JOBS = {"miner", "farmer", "hunter", "lumberjack", "fisher"};
    private ConfigValidator() {}

    public static void validate(CdrJobsPlugin plugin) {
        int maxLevel = plugin.getConfig().getInt("settings.max-level", 100);
        if (maxLevel < 1) plugin.getLogger().warning("settings.max-level must be >= 1; runtime will clamp where possible.");

        long base = plugin.getConfig().getLong("settings.xp-curve.base-xp", 100L);
        long linear = plugin.getConfig().getLong("settings.xp-curve.linear-growth", 0L);
        long quadratic = plugin.getConfig().getLong("settings.xp-curve.quadratic-growth", 15L);
        if (base <= 0L) plugin.getLogger().warning("settings.xp-curve.base-xp should be > 0.");
        if (linear < 0L) plugin.getLogger().warning("settings.xp-curve.linear-growth should be >= 0.");
        if (quadratic < 0L) plugin.getLogger().warning("settings.xp-curve.quadratic-growth should be >= 0.");

        for (String job : JOBS) {
            double multiplier = plugin.getConfig().getDouble(job + ".xp-multiplier", 1.0D);
            if (multiplier < 0D) plugin.getLogger().warning(job + ".xp-multiplier is negative; rewards will be clamped.");
            ConfigurationSection xp = plugin.getConfig().getConfigurationSection(job + ".xp");
            if (plugin.getConfig().getBoolean(job + ".enabled", true) && (xp == null || xp.getKeys(false).isEmpty())) {
                plugin.getLogger().warning(job + " is enabled but has no XP entries configured.");
            }
        }

        String hunterMode = plugin.getConfig().getString("hunter.mob-filter.mode", "ALL").toUpperCase(Locale.ROOT);
        if (!hunterMode.equals("ALL") && !hunterMode.equals("WHITELIST")) {
            plugin.getLogger().warning("hunter.mob-filter.mode must be ALL or WHITELIST.");
        }
        for (String raw : plugin.getConfig().getStringList("hunter.mob-filter.blacklist")) {
            try { EntityType.valueOf(raw.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { plugin.getLogger().warning("Invalid hunter blacklist entity: " + raw); }
        }
        if (plugin.getConfig().getInt("hunter.fallback-mob-xp", 3) < 0) plugin.getLogger().warning("hunter.fallback-mob-xp should be >= 0.");
        if (plugin.getConfig().getInt("hunter.pvp.xp", 10) < 0) plugin.getLogger().warning("hunter.pvp.xp should be >= 0.");
        if (plugin.getConfig().getLong("hunter.pvp.same-victim-cooldown-seconds", 300L) < 0L) plugin.getLogger().warning("Hunter same-victim cooldown cannot be negative.");
        if (plugin.getConfig().getLong("hunter.pvp.anti-farm.minimum-online-seconds", 60L) < 0L) plugin.getLogger().warning("Hunter minimum online time cannot be negative.");
        if (plugin.getConfig().getLong("hunter.pvp.anti-farm.minimum-playtime-seconds", 300L) < 0L) plugin.getLogger().warning("Hunter minimum playtime cannot be negative.");
        if (plugin.getConfig().getInt("hunter.pvp.anti-farm.kill-streak.max-rewards-per-window", 5) < 0) plugin.getLogger().warning("Hunter PvP max rewards per window cannot be negative.");
        if (plugin.getConfig().getLong("hunter.pvp.anti-farm.kill-streak.window-seconds", 120L) < 1L) plugin.getLogger().warning("Hunter PvP reward window should be >= 1 second.");

        ConfigurationSection milestones = plugin.getConfig().getConfigurationSection("fate-essence-milestones");
        if (milestones != null) {
            int safeMax = Math.max(1, maxLevel);
            for (String key : milestones.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    int reward = milestones.getInt(key);
                    if (level < 1 || level > safeMax) plugin.getLogger().warning("Fate Essence milestone " + level + " is outside level range 1-" + safeMax + ".");
                    if (reward <= 0) plugin.getLogger().warning("Fate Essence milestone " + level + " should reward at least 1 Essence.");
                } catch (NumberFormatException exception) {
                    plugin.getLogger().warning("Invalid Fate Essence milestone key: " + key);
                }
            }
        }

        long farmerCooldown = plugin.getConfig().getLong("farmer.anti-exploit.location-cooldown-seconds", 30L);
        if (farmerCooldown < 0L) plugin.getLogger().warning("Farmer location cooldown cannot be negative.");
        long fisherAfk = plugin.getConfig().getLong("fisher.anti-exploit.afk-after-seconds", 600L);
        if (fisherAfk < 60L) plugin.getLogger().warning("Fisher AFK threshold below 60s may cause false positives.");

        long rebirthCooldown = plugin.getConfig().getLong("rite-of-rebirth.cooldown-seconds", 604800L);
        if (rebirthCooldown < 0L) plugin.getLogger().warning("rite-of-rebirth.cooldown-seconds cannot be negative.");
        int refundPercent = plugin.getConfig().getInt("rite-of-rebirth.refund-percent", 100);
        if (refundPercent < 0 || refundPercent > 100) plugin.getLogger().warning("rite-of-rebirth.refund-percent must be 0-100; runtime clamps it.");
        String feeMode = plugin.getConfig().getString("rite-of-rebirth.fee.mode", "NONE").toUpperCase(Locale.ROOT);
        if (!feeMode.equals("NONE") && !feeMode.equals("FATE") && !feeMode.equals("VAULT")) {
            plugin.getLogger().warning("rite-of-rebirth.fee.mode must be NONE, FATE or VAULT.");
        }
        if (plugin.getConfig().getInt("rite-of-rebirth.fee.fate-essence", 0) < 0) plugin.getLogger().warning("Rebirth Fate fee cannot be negative.");
        if (plugin.getConfig().getDouble("rite-of-rebirth.fee.vault", 0D) < 0D) plugin.getLogger().warning("Rebirth Vault fee cannot be negative.");

        int masteryMax = plugin.getConfig().getInt("mastery.max-tier", 10);
        if (masteryMax < 1 || masteryMax > 50) plugin.getLogger().warning("mastery.max-tier should be 1-50; runtime clamps it.");
        if (plugin.getConfig().getLong("mastery.base-xp", 5000L) <= 0L) plugin.getLogger().warning("mastery.base-xp must be > 0; runtime clamps it.");
        if (plugin.getConfig().getLong("mastery.growth-per-tier", 2500L) < 0L) plugin.getLogger().warning("mastery.growth-per-tier cannot be negative; runtime clamps it.");

        validateResonance(plugin, Math.max(1, maxLevel), Math.max(1, masteryMax));
    }

    private static void validateResonance(CdrJobsPlugin plugin, int maxLevel, int masteryMax) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("fate-resonance.definitions");
        if (root == null) {
            if (plugin.getConfig().getBoolean("fate-resonance.enabled", true)) {
                plugin.getLogger().warning("Fate Resonance is enabled but no definitions exist.");
            }
            return;
        }
        Set<String> pairs = new HashSet<>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) continue;
            List<String> jobs = section.getStringList("jobs");
            if (jobs.size() != 2) {
                plugin.getLogger().warning("Fate Resonance '" + id + "' must define exactly two jobs.");
                continue;
            }
            JobType first;
            JobType second;
            try {
                first = JobType.valueOf(jobs.get(0).toUpperCase(Locale.ROOT));
                second = JobType.valueOf(jobs.get(1).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Fate Resonance '" + id + "' contains an invalid job.");
                continue;
            }
            if (first == second) plugin.getLogger().warning("Fate Resonance '" + id + "' cannot pair a Job with itself.");
            String pair = first.ordinal() < second.ordinal() ? first.name() + ":" + second.name() : second.name() + ":" + first.name();
            if (!pairs.add(pair)) plugin.getLogger().warning("Duplicate Fate Resonance pair detected: " + pair + " (definition " + id + ").");
            int minLevel = section.getInt("min-level", 50);
            if (minLevel < 1 || minLevel > maxLevel) plugin.getLogger().warning("Fate Resonance '" + id + "' min-level should be 1-" + maxLevel + ".");
            int harmonized = section.getInt("harmonized-mastery-tier", 1);
            if (harmonized < 0 || harmonized > masteryMax) plugin.getLogger().warning("Fate Resonance '" + id + "' harmonized-mastery-tier should be 0-" + masteryMax + ".");
        }
    }
}
