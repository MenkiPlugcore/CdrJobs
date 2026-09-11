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
import store.cadera.cdrjobs.api.ProfessionActionType;
import store.cadera.cdrjobs.api.event.ProfessionActionEvent;
import store.cadera.cdrjobs.api.event.TrialCompleteEvent;
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
    private static final Set<Material> REPLANT = EnumSet.of(Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART);
    private static final Set<Material> RARE = EnumSet.of(Material.MELON, Material.PUMPKIN, Material.NETHER_WART, Material.COCOA);

    private final CdrJobsPlugin plugin;
    private final ProfessionStore store;
    private final ProgressionService progression;
    private final FarmerService farmer;
    private final LevelService levels;
    private Map<Material, Integer> xp = new HashMap<>();

    public FarmerListener(CdrJobsPlugin plugin, Database ignored, ProfessionStore store, ProgressionService progression,
                          FarmerService farmer, LevelService levels) {
        this.plugin = plugin;
        this.store = store;
        this.progression = progression;
        this.farmer = farmer;
        this.levels = levels;
        refreshXpMap();
    }

    public void refreshXpMap() { xp = plugin.loadActivityXp("farmer.xp"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void harvest(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("farmer.enabled", true)) return;
        Block block = event.getBlock();
        Integer base = xp.get(block.getType());
        if (base == null || !mature(block)) return;

        long cooldownMillis = Math.max(0L, plugin.getConfig().getLong("farmer.anti-exploit.location-cooldown-seconds", 30L)) * 1000L;
        if (!store.tryClaimLocation(block.getLocation(), "farmer_harvest", cooldownMillis)) return;

        Player player = event.getPlayer();
        Material original = block.getType();
        BlockData old = block.getBlockData().clone();
        boolean seedBefore = farmer.seedTrialComplete(player);
        boolean gaiaBefore = farmer.gaiaTrialComplete(player);
        farmer.recordHarvest(player, RARE.contains(original));
        if (!seedBefore && farmer.seedTrialComplete(player)) {
            plugin.getServer().getPluginManager().callEvent(new TrialCompleteEvent(player, JobType.FARMER, "trial_of_seed"));
        }
        if (!gaiaBefore && farmer.gaiaTrialComplete(player)) {
            plugin.getServer().getPluginManager().callEvent(new TrialCompleteEvent(player, JobType.FARMER, "trial_of_gaia"));
        }

        plugin.getServer().getPluginManager().callEvent(new ProfessionActionEvent(player, JobType.FARMER, ProfessionActionType.HARVEST_CROP, 1));
        long amount = farmer.applyXpModifiers(player,
                Math.max(1L, Math.round(base * Math.max(0D, plugin.getConfig().getDouble("farmer.xp-multiplier", 1D)))));
        JobProgress progress = progression.addXp(player, JobType.FARMER, amount);

        if (farmer.autoReplant(player) && REPLANT.contains(original)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (block.getType() != Material.AIR) return;
                block.setType(original, false);
                BlockData data = block.getBlockData();
                if (data instanceof Ageable ageable) {
                    ageable.setAge(0);
                    block.setBlockData(ageable, false);
                } else {
                    block.setBlockData(old, false);
                }
            }, 1L);
        }

        if (plugin.getConfig().getBoolean("settings.actionbar-xp", true)) {
            long required = levels.xpRequiredForNextLevel(progress.level());
            player.sendActionBar(Component.text(progress.level() >= levels.maxLevel()
                    ? "✦ Verdant Keeper Lv." + levels.maxLevel() + " • MAX"
                    : "✦ Verdant +" + amount + " XP • Lv." + progress.level() + " • " + progress.xp() + "/" + required));
        }
    }

    private boolean mature(Block block) {
        if (block.getType() == Material.MELON || block.getType() == Material.PUMPKIN) return true;
        return block.getBlockData() instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge();
    }
}
