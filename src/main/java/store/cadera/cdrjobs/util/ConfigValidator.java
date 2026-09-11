package store.cadera.cdrjobs.util;

import org.bukkit.configuration.ConfigurationSection;
import store.cadera.cdrjobs.CdrJobsPlugin;

public final class ConfigValidator {
    private static final String[] JOBS = {"miner", "farmer", "hunter", "lumberjack", "fisher"};

    private ConfigValidator() {
    }

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
            if (multiplier < 0D) plugin.getLogger().warning(job + ".xp-multiplier is negative; rewards will be clamped by profession logic.");
            ConfigurationSection xp = plugin.getConfig().getConfigurationSection(job + ".xp");
            if (plugin.getConfig().getBoolean(job + ".enabled", true) && (xp == null || xp.getKeys(false).isEmpty())) {
                plugin.getLogger().warning(job + " is enabled but has no XP entries configured.");
            }
        }

        int fallbackMobXp = plugin.getConfig().getInt("hunter.fallback-mob-xp", 3);
        if (fallbackMobXp < 0) plugin.getLogger().warning("hunter.fallback-mob-xp cannot be negative; runtime clamps it to 0.");
        int pvpXp = plugin.getConfig().getInt("hunter.pvp.xp", 10);
        if (pvpXp < 0) plugin.getLogger().warning("hunter.pvp.xp cannot be negative; runtime clamps it to 0.");
        long pvpCooldown = plugin.getConfig().getLong("hunter.pvp.same-victim-cooldown-seconds", 300L);
        if (pvpCooldown < 0L) plugin.getLogger().warning("hunter.pvp.same-victim-cooldown-seconds cannot be negative; runtime clamps it to 0.");
        if (plugin.getConfig().getBoolean("hunter.pvp.enabled", true) && pvpCooldown == 0L) {
            plugin.getLogger().warning("Hunter PvP same-victim cooldown is disabled; this may allow kill farming.");
        }

        ConfigurationSection milestones = plugin.getConfig().getConfigurationSection("fate-essence-milestones");
        if (milestones != null) {
            int safeMax = Math.max(1, maxLevel);
            for (String key : milestones.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    int reward = milestones.getInt(key);
                    if (level < 1 || level > safeMax) plugin.getLogger().warning("Fate Essence milestone " + level + " is outside the configured level range 1-" + safeMax + ".");
                    if (reward <= 0) plugin.getLogger().warning("Fate Essence milestone " + level + " should reward at least 1 Essence.");
                } catch (NumberFormatException exception) {
                    plugin.getLogger().warning("Invalid Fate Essence milestone key: " + key);
                }
            }
        }

        long farmerCooldown = plugin.getConfig().getLong("farmer.anti-exploit.location-cooldown-seconds", 30L);
        if (farmerCooldown < 0L) plugin.getLogger().warning("Farmer location cooldown cannot be negative; runtime will clamp it.");

        long fisherAfk = plugin.getConfig().getLong("fisher.anti-exploit.afk-after-seconds", 600L);
        if (fisherAfk < 60L) plugin.getLogger().warning("Fisher AFK threshold below 60s is aggressive and may cause false positives.");
    }
}
