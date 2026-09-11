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
import java.util.Map;

public final class HunterListener implements Listener {
    private final CdrJobsPlugin plugin;
    private final ProgressionService progression;
    private final HunterService hunter;
    private final LevelService levels;
    private final NamespacedKey origin;
    private Map<EntityType, Integer> xp = new EnumMap<>(EntityType.class);

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
                    loaded.put(EntityType.valueOf(key.toUpperCase()), section.getInt(key));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Unknown hunter.xp entity type: " + key);
                }
            }
        }
        xp = loaded;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        event.getEntity().getPersistentDataContainer().set(origin, PersistentDataType.STRING, event.getSpawnReason().name());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        if (!plugin.getConfig().getBoolean("hunter.enabled", true)) return;

        // Hunter is a mob-hunting profession. PvP/player kills never grant Hunter progression.
        if (event.getEntity() instanceof Player || event.getEntity() instanceof ArmorStand) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        String spawnOrigin = event.getEntity().getPersistentDataContainer().get(origin, PersistentDataType.STRING);
        if (spawnOrigin != null) {
            boolean excludeSpawner = plugin.getConfig().getBoolean("hunter.anti-exploit.exclude-spawner", true)
                    && spawnOrigin.equals("SPAWNER");
            boolean excludeBreeding = plugin.getConfig().getBoolean("hunter.anti-exploit.exclude-breeding", true)
                    && spawnOrigin.equals("BREEDING");
            if (excludeSpawner || excludeBreeding) return;
        }

        EntityType type = event.getEntityType();
        int configuredXp = xp.getOrDefault(type, -1);
        int fallbackXp = Math.max(0, plugin.getConfig().getInt("hunter.fallback-mob-xp", 3));
        int baseXp = configuredXp >= 0 ? configuredXp : fallbackXp;
        if (baseXp <= 0) return;

        boolean dangerous = hunter.dangerous(type);
        long time = killer.getWorld().getTime();
        boolean night = time >= 13000 && time <= 23000;

        hunter.recordKill(killer, dangerous, night);
        plugin.getServer().getPluginManager().callEvent(
                new ProfessionActionEvent(killer, JobType.HUNTER, ProfessionActionType.KILL_ENTITY, 1)
        );

        double multiplier = Math.max(0.0D, plugin.getConfig().getDouble("hunter.xp-multiplier", 1.0D));
        long tunedBase = Math.max(1L, Math.round(baseXp * multiplier));
        long amount = hunter.applyXp(killer, tunedBase, dangerous);
        JobProgress progress = progression.addXp(killer, JobType.HUNTER, amount);

        if (plugin.getConfig().getBoolean("settings.actionbar-xp", true)) {
            long required = levels.xpRequiredForNextLevel(progress.level());
            String text = progress.level() >= levels.maxLevel()
                    ? "✦ Bloodfang Stalker Lv." + levels.maxLevel() + " • MAX"
                    : "✦ Bloodfang +" + amount + " XP • Lv." + progress.level() + " • " + progress.xp() + "/" + required;
            killer.sendActionBar(Component.text(text));
        }
    }
}
