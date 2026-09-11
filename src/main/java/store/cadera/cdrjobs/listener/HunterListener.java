package store.cadera.cdrjobs.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.api.ProfessionActionType;
import store.cadera.cdrjobs.api.event.ProfessionActionEvent;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.service.HunterService;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.ProgressionService;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HunterListener implements Listener {
    private final CdrJobsPlugin plugin;
    private final ProgressionService progression;
    private final HunterService hunter;
    private final LevelService levels;
    private final NamespacedKey origin;
    private Map<EntityType, Integer> xp = new EnumMap<>(EntityType.class);
    private Set<EntityType> blacklist = EnumSet.noneOf(EntityType.class);
    private String mobMode = "ALL";

    public HunterListener(CdrJobsPlugin plugin, ProgressionService progression, HunterService hunter, LevelService levels) {
        this.plugin = plugin;
        this.progression = progression;
        this.hunter = hunter;
        this.levels = levels;
        this.origin = new NamespacedKey(plugin, "hunter_spawn_origin");
        refreshXpMap();
    }

    public void refreshXpMap() {
        Map<EntityType, Integer> loaded = new EnumMap<>(EntityType.class);
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("hunter.xp");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    loaded.put(EntityType.valueOf(key.toUpperCase(Locale.ROOT)), section.getInt(key));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Unknown hunter.xp entity type: " + key);
                }
            }
        }
        xp = loaded;

        Set<EntityType> blocked = EnumSet.noneOf(EntityType.class);
        for (String raw : plugin.getConfig().getStringList("hunter.mob-filter.blacklist")) {
            try {
                blocked.add(EntityType.valueOf(raw.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Unknown hunter.mob-filter.blacklist entity type: " + raw);
            }
        }
        blacklist = blocked;

        String configuredMode = plugin.getConfig().getString("hunter.mob-filter.mode");
        if (configuredMode == null) {
            mobMode = plugin.getConfig().getBoolean("hunter.allow-unlisted-mobs", true) ? "ALL" : "WHITELIST";
        } else {
            mobMode = configuredMode.toUpperCase(Locale.ROOT);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        hunter.markOnline(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        hunter.markOffline(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        event.getEntity().getPersistentDataContainer().set(origin, PersistentDataType.STRING, event.getSpawnReason().name());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        if (!plugin.getConfig().getBoolean("hunter.enabled", true)) return;
        if (event.getEntity() instanceof ArmorStand) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (event.getEntity() instanceof Player victim) {
            handlePlayerKill(killer, victim);
            return;
        }

        EntityType type = event.getEntityType();
        if (blacklist.contains(type)) {
            hunter.setDebugReason(killer, "Mob denied: " + type + " is blacklisted.");
            return;
        }

        String spawnOrigin = event.getEntity().getPersistentDataContainer().get(origin, PersistentDataType.STRING);
        if (spawnOrigin != null) {
            boolean excludeSpawner = plugin.getConfig().getBoolean("hunter.anti-exploit.exclude-spawner", true) && spawnOrigin.equals("SPAWNER");
            boolean excludeBreeding = plugin.getConfig().getBoolean("hunter.anti-exploit.exclude-breeding", true) && spawnOrigin.equals("BREEDING");
            if (excludeSpawner || excludeBreeding) {
                hunter.setDebugReason(killer, "Mob denied: spawn origin " + spawnOrigin + " is excluded.");
                return;
            }
        }

        Integer configured = xp.get(type);
        if ("WHITELIST".equals(mobMode) && configured == null) {
            hunter.setDebugReason(killer, "Mob denied: " + type + " is not in hunter.xp whitelist.");
            return;
        }

        int fallbackXp = Math.max(0, plugin.getConfig().getInt("hunter.fallback-mob-xp", 3));
        int baseXp = configured != null ? configured : fallbackXp;
        if (baseXp <= 0) {
            hunter.setDebugReason(killer, "Mob denied: resolved XP is 0.");
            return;
        }

        boolean dangerous = hunter.dangerous(type);
        hunter.recordMobReward(killer);
        hunter.setDebugReason(killer, "Mob reward accepted: " + type + " base XP " + baseXp + ".");
        grantHunterProgress(killer, baseXp, dangerous, true, "Mob Kill");
    }

    private void handlePlayerKill(Player killer, Player victim) {
        if (!plugin.getConfig().getBoolean("hunter.pvp.enabled", true)) {
            hunter.setDebugReason(killer, "PvP denied: hunter.pvp.enabled is false.");
            return;
        }
        if (killer.getUniqueId().equals(victim.getUniqueId())) {
            hunter.setDebugReason(killer, "PvP denied: self kill.");
            return;
        }
        if (!hunter.tryClaimPvpReward(killer, victim)) return;

        int baseXp = Math.max(0, plugin.getConfig().getInt("hunter.pvp.xp", 10));
        if (baseXp <= 0) {
            hunter.setDebugReason(killer, "PvP denied: hunter.pvp.xp is 0.");
            return;
        }

        hunter.recordPvpReward(killer);
        boolean countTowardTrials = plugin.getConfig().getBoolean("hunter.pvp.count-toward-trials", true);
        grantHunterProgress(killer, baseXp, false, countTowardTrials, "PvP Kill");
    }

    private void grantHunterProgress(Player killer, int baseXp, boolean dangerous, boolean countTowardTrials, String source) {
        long time = killer.getWorld().getTime();
        boolean night = time >= 13000 && time <= 23000;

        if (countTowardTrials) hunter.recordKill(killer, dangerous, night);
        plugin.getServer().getPluginManager().callEvent(new ProfessionActionEvent(killer, JobType.HUNTER, ProfessionActionType.KILL_ENTITY, 1));

        double multiplier = Math.max(0.0D, plugin.getConfig().getDouble("hunter.xp-multiplier", 1.0D));
        long tunedBase = Math.max(1L, Math.round(baseXp * multiplier));
        long amount = hunter.applyXp(killer, tunedBase, dangerous);
        JobProgress progress = progression.addXp(killer, JobType.HUNTER, amount);

        if (plugin.getConfig().getBoolean("settings.actionbar-xp", true)) {
            long required = levels.xpRequiredForNextLevel(progress.level());
            String text = progress.level() >= levels.maxLevel()
                    ? "✦ Bloodfang " + source + " • Lv." + levels.maxLevel() + " MAX"
                    : "✦ Bloodfang " + source + " +" + amount + " XP • Lv." + progress.level() + " • " + progress.xp() + "/" + required;
            killer.sendActionBar(Component.text(text));
        }
    }
}
