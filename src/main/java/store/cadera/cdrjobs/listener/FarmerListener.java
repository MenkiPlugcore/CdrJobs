package store.cadera.cdrjobs.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.service.FarmerService;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.ProgressionService;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class FarmerListener implements Listener {
    private static final Set<Material> REPLANTABLE = EnumSet.of(Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART);
    private static final Set<Material> RARE = EnumSet.of(Material.MELON, Material.PUMPKIN, Material.NETHER_WART, Material.COCOA);

    private final CdrJobsPlugin plugin;
    private final ProfessionStore store;
    private final ProgressionService progression;
    private final FarmerService farmer;
    private final LevelService levels;
    private Map<Material, Integer> cropXp = new HashMap<>();

    public FarmerListener(CdrJobsPlugin plugin, Database database, ProfessionStore store, ProgressionService progression, FarmerService farmer, LevelService levels) {
        this.plugin = plugin; this.store = store; this.progression = progression; this.farmer = farmer; this.levels = levels;
        refreshXpMap();
    }

    public void refreshXpMap() { this.cropXp = plugin.loadActivityXp("farmer.xp"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("farmer.enabled", true)) return;
        Block block = event.getBlock();
        Integer base = cropXp.get(block.getType());
        if (base == null || !isMature(block)) return;

        long cooldown = Math.max(0L, plugin.getConfig().getLong("farmer.anti-exploit.location-cooldown-seconds", 30L)) * 1000L;
        if (!store.tryClaimLocation(block.getLocation(), "farmer_harvest", cooldown)) return;

        Player player = event.getPlayer();
        Material original = block.getType();
        BlockData originalData = block.getBlockData().clone();
        farmer.recordHarvest(player, RARE.contains(original));

        double global = Math.max(0.0, plugin.getConfig().getDouble("farmer.xp-multiplier", 1.0));
        long xp = farmer.applyXpModifiers(player, Math.max(1L, Math.round(base * global)));
        JobProgress progress = progression.addXp(player, JobType.FARMER, xp);

        if (farmer.autoReplant(player) && REPLANTABLE.contains(original)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (block.getType() != Material.AIR) return;
                block.setType(original, false);
                BlockData data = block.getBlockData();
                if (data instanceof Ageable ageable) {
                    ageable.setAge(0);
                    block.setBlockData(ageable, false);
                } else block.setBlockData(originalData, false);
            }, 1L);
        }

        if (plugin.getConfig().getBoolean("settings.actionbar-xp", true)) {
            long required = levels.xpRequiredForNextLevel(progress.level());
            String text = progress.level() >= levels.maxLevel()
                    ? "✦ Verdant Keeper Lv.100 • MAX"
                    : "✦ Verdant +" + xp + " XP • Lv." + progress.level() + " • " + progress.xp() + "/" + required;
            player.sendActionBar(Component.text(text));
        }
    }

    private boolean isMature(Block block) {
        if (block.getType() == Material.MELON || block.getType() == Material.PUMPKIN) return true;
        BlockData data = block.getBlockData();
        return data instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge();
    }
}
