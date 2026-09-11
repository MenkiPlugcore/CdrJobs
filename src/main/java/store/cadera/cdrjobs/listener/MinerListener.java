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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.api.ProfessionActionType;
import store.cadera.cdrjobs.api.event.ProfessionActionEvent;
import store.cadera.cdrjobs.api.event.TrialCompleteEvent;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.model.MinerSkill;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.MinerAbilityService;
import store.cadera.cdrjobs.service.MinerTrialService;
import store.cadera.cdrjobs.service.ProgressionService;
import store.cadera.cdrjobs.service.SkillService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MinerListener implements Listener {
    private final CdrJobsPlugin plugin;
    private final Database db;
    private final ProgressionService progression;
    private final SkillService skills;
    private final LevelService levels;
    private final MinerTrialService trials;
    private final MinerAbilityService abilities;
    private Map<Material, Integer> xp;
    private final Map<UUID, Streak> streaks = new HashMap<>();

    public MinerListener(CdrJobsPlugin plugin, Database db, ProgressionService progression, SkillService skills,
                         LevelService levels, MinerTrialService trials, MinerAbilityService abilities) {
        this.plugin = plugin;
        this.db = db;
        this.progression = progression;
        this.skills = skills;
        this.levels = levels;
        this.trials = trials;
        this.abilities = abilities;
        refreshXpMap();
    }

    public void refreshXpMap() { xp = plugin.loadMinerXp(); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void place(BlockPlaceEvent event) {
        if (plugin.getConfig().getBoolean("miner.enabled", true) && placed() && xp.containsKey(event.getBlockPlaced().getType())) {
            db.markPlacedOre(event.getBlockPlaced().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void extend(BlockPistonExtendEvent event) { move(event.getBlocks(), event.getDirection()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void retract(BlockPistonRetractEvent event) { move(event.getBlocks(), event.getDirection().getOppositeFace()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void explode(BlockExplodeEvent event) { cleanup(event.blockList()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void explodeEntity(EntityExplodeEvent event) { cleanup(event.blockList()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void breakOre(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("miner.enabled", true)) return;
        Block block = event.getBlock();
        Integer base = xp.get(block.getType());
        if (base == null) return;
        if (placed() && db.consumePlacedOre(block.getLocation())) return;

        Player player = event.getPlayer();
        boolean stoneBefore = trials.isStoneComplete(player);
        boolean deepBefore = trials.isDeepComplete(player);
        trials.onNaturalOreMined(player, block);
        if (!stoneBefore && trials.isStoneComplete(player)) {
            plugin.getServer().getPluginManager().callEvent(new TrialCompleteEvent(player, JobType.MINER, "trial_of_stone"));
        }
        if (!deepBefore && trials.isDeepComplete(player)) {
            plugin.getServer().getPluginManager().callEvent(new TrialCompleteEvent(player, JobType.MINER, "trial_of_the_deep"));
        }

        long tuned = Math.max(1L, Math.round(base * Math.max(0.01D, plugin.getConfig().getDouble("miner.xp-multiplier", 1D))));
        long amount = abilities.applyXpBoost(player, skills.applyMinerXpModifiers(player, tuned));
        plugin.getServer().getPluginManager().callEvent(new ProfessionActionEvent(player, JobType.MINER, ProfessionActionType.MINE_ORE, 1));
        JobProgress progress = progression.addXp(player, JobType.MINER, amount);
        streak(player);

        if (plugin.getConfig().getBoolean("settings.actionbar-xp", true)) {
            long required = levels.xpRequiredForNextLevel(progress.level());
            player.sendActionBar(Component.text(progress.level() >= levels.maxLevel()
                    ? "✦ Runebound Delver Lv." + levels.maxLevel() + " • MAX"
                    : "✦ Runebound +" + amount + " XP • Lv." + progress.level() + " • " + progress.xp() + "/" + required));
        }
    }

    private boolean placed() { return plugin.getConfig().getBoolean("anti-exploit.placed-ore-tracking", true); }

    private void cleanup(List<Block> blocks) {
        if (!placed() || !plugin.getConfig().getBoolean("anti-exploit.cleanup-exploded-ores", true)) return;
        for (Block block : blocks) if (xp.containsKey(block.getType())) db.consumePlacedOre(block.getLocation());
    }

    private void move(List<Block> blocks, BlockFace direction) {
        if (!placed() || !plugin.getConfig().getBoolean("anti-exploit.piston-move-tracking", true)) return;
        List<Location> destinations = new ArrayList<>();
        for (Block block : blocks) {
            if (xp.containsKey(block.getType()) && db.isPlacedOre(block.getLocation())) {
                db.consumePlacedOre(block.getLocation());
                destinations.add(block.getRelative(direction).getLocation());
            }
        }
        destinations.forEach(db::markPlacedOre);
    }

    private void streak(Player player) {
        int rank = db.getSkillRank(player.getUniqueId(), MinerSkill.ECHO_OF_DEPTH);
        if (rank <= 0) return;
        long now = System.currentTimeMillis();
        Streak previous = streaks.get(player.getUniqueId());
        int count = previous == null || now - previous.last() > 60_000L ? 1 : previous.count() + 1;
        if (count >= Math.max(10, 25 - (rank - 1) * 5)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 160, rank >= 3 ? 1 : 0, true, false, true));
            count = 0;
        }
        streaks.put(player.getUniqueId(), new Streak(count, now));
    }

    private record Streak(int count, long last) {}
}
