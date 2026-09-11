package store.cadera.cdrjobs.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.api.ProfessionActionType;
import store.cadera.cdrjobs.api.event.ProfessionActionEvent;
import store.cadera.cdrjobs.api.event.TrialCompleteEvent;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.LumberjackService;
import store.cadera.cdrjobs.service.ProgressionService;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LumberjackListener implements Listener {
    private static final Set<Material> ELDER = EnumSet.of(Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG);

    private final CdrJobsPlugin plugin;
    private final ProfessionStore store;
    private final ProgressionService progression;
    private final LumberjackService lumber;
    private final LevelService levels;
    private Map<Material, Integer> xp = new HashMap<>();

    public LumberjackListener(CdrJobsPlugin plugin, ProfessionStore store, ProgressionService progression,
                              LumberjackService lumber, LevelService levels) {
        this.plugin = plugin;
        this.store = store;
        this.progression = progression;
        this.lumber = lumber;
        this.levels = levels;
        refreshXpMap();
    }

    public void refreshXpMap() { xp = plugin.loadActivityXp("lumberjack.xp"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void place(BlockPlaceEvent event) {
        if (plugin.getConfig().getBoolean("lumberjack.enabled", true) && xp.containsKey(event.getBlockPlaced().getType())) {
            store.markPlacedJobBlock(event.getBlockPlaced().getLocation(), LumberjackService.JOB);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void cut(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("lumberjack.enabled", true)) return;
        Integer base = xp.get(event.getBlock().getType());
        if (base == null || store.consumePlacedJobBlock(event.getBlock().getLocation(), LumberjackService.JOB)) return;

        Player player = event.getPlayer();
        boolean timberBefore = lumber.timberComplete(player);
        boolean groveBefore = lumber.groveComplete(player);
        lumber.recordChop(player, ELDER.contains(event.getBlock().getType()));
        if (!timberBefore && lumber.timberComplete(player)) {
            plugin.getServer().getPluginManager().callEvent(new TrialCompleteEvent(player, JobType.LUMBERJACK, "trial_of_timber"));
        }
        if (!groveBefore && lumber.groveComplete(player)) {
            plugin.getServer().getPluginManager().callEvent(new TrialCompleteEvent(player, JobType.LUMBERJACK, "trial_of_ancient_grove"));
        }

        plugin.getServer().getPluginManager().callEvent(new ProfessionActionEvent(player, JobType.LUMBERJACK, ProfessionActionType.CHOP_LOG, 1));
        long amount = lumber.applyXp(player,
                Math.max(1L, Math.round(base * Math.max(0D, plugin.getConfig().getDouble("lumberjack.xp-multiplier", 1D)))));
        JobProgress progress = progression.addXp(player, JobType.LUMBERJACK, amount);
        if (plugin.getConfig().getBoolean("settings.actionbar-xp", true)) {
            long required = levels.xpRequiredForNextLevel(progress.level());
            player.sendActionBar(Component.text(progress.level() >= levels.maxLevel()
                    ? "✦ Ironbark Warden Lv." + levels.maxLevel() + " • MAX"
                    : "✦ Ironbark +" + amount + " XP • Lv." + progress.level() + " • " + progress.xp() + "/" + required));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void extend(BlockPistonExtendEvent event) { move(event.getBlocks(), event.getDirection()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void retract(BlockPistonRetractEvent event) { move(event.getBlocks(), event.getDirection().getOppositeFace()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockExplode(BlockExplodeEvent event) { clean(event.blockList()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void entityExplode(EntityExplodeEvent event) { clean(event.blockList()); }

    private void move(List<Block> blocks, BlockFace direction) {
        if (!plugin.getConfig().getBoolean("lumberjack.anti-exploit.piston-tracking", true)) return;
        List<Location> destinations = new ArrayList<>();
        for (Block block : blocks) {
            if (xp.containsKey(block.getType()) && store.consumePlacedJobBlock(block.getLocation(), LumberjackService.JOB)) {
                destinations.add(block.getRelative(direction).getLocation());
            }
        }
        destinations.forEach(location -> store.markPlacedJobBlock(location, LumberjackService.JOB));
    }

    private void clean(List<Block> blocks) {
        for (Block block : blocks) {
            if (xp.containsKey(block.getType())) store.consumePlacedJobBlock(block.getLocation(), LumberjackService.JOB);
        }
    }
}
