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
    private final Database database;
    private final ProgressionService progression;
    private final SkillService skills;
    private final LevelService levels;
    private final MinerTrialService trials;
    private final MinerAbilityService abilities;
    private Map<Material, Integer> oreXp;
    private final Map<UUID, Streak> streaks = new HashMap<>();

    public MinerListener(CdrJobsPlugin plugin, Database database, ProgressionService progression, SkillService skills,
                         LevelService levels, MinerTrialService trials, MinerAbilityService abilities) {
        this.plugin = plugin;
        this.database = database;
        this.progression = progression;
        this.skills = skills;
        this.levels = levels;
        this.trials = trials;
        this.abilities = abilities;
        this.oreXp = plugin.loadMinerXp();
    }

    public void refreshXpMap() {
        this.oreXp = plugin.loadMinerXp();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("miner.enabled", true)) return;
        if (!placedTrackingEnabled()) return;
        if (oreXp.containsKey(event.getBlockPlaced().getType())) {
            database.markPlacedOre(event.getBlockPlaced().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        trackPistonMoves(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        trackPistonMoves(event.getBlocks(), event.getDirection().getOppositeFace());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        cleanupExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        cleanupExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("miner.enabled", true)) return;

        Block block = event.getBlock();
        Integer base = oreXp.get(block.getType());
        if (base == null) return;

        if (placedTrackingEnabled() && database.consumePlacedOre(block.getLocation())) {
            return;
        }

        Player player = event.getPlayer();
        trials.onNaturalOreMined(player, block);

        double tuning = Math.max(0.01D, plugin.getConfig().getDouble("miner.xp-multiplier", 1.0D));
        long tunedBase = Math.max(1L, Math.round(base * tuning));
        long xp = skills.applyMinerXpModifiers(player, tunedBase);
        xp = abilities.applyXpBoost(player, xp);
        JobProgress progress = progression.addXp(player, JobType.MINER, xp);

        updateEchoStreak(player);

        if (plugin.getConfig().getBoolean("settings.actionbar-xp", true)) {
            long required = levels.xpRequiredForNextLevel(progress.level());
            String text = progress.level() >= levels.maxLevel()
                    ? "✦ Runebound Delver Lv." + levels.maxLevel() + " • MAX"
                    : "✦ Runebound +" + xp + " XP • Lv." + progress.level() + " • " + progress.xp() + "/" + required;
            player.sendActionBar(Component.text(text));
        }
    }

    private boolean placedTrackingEnabled() {
        return plugin.getConfig().getBoolean("anti-exploit.placed-ore-tracking", true);
    }

    private void cleanupExplosion(List<Block> blocks) {
        if (!placedTrackingEnabled()) return;
        if (!plugin.getConfig().getBoolean("anti-exploit.cleanup-exploded-ores", true)) return;
        for (Block block : blocks) {
            if (oreXp.containsKey(block.getType())) database.consumePlacedOre(block.getLocation());
        }
    }

    private void trackPistonMoves(List<Block> blocks, BlockFace moveDirection) {
        if (!placedTrackingEnabled()) return;
        if (!plugin.getConfig().getBoolean("anti-exploit.piston-move-tracking", true)) return;

        List<TrackedMove> moves = new ArrayList<>();
        for (Block block : blocks) {
            if (!oreXp.containsKey(block.getType())) continue;
            Location from = block.getLocation();
            if (!database.isPlacedOre(from)) continue;
            Location to = from.clone().add(moveDirection.getModX(), moveDirection.getModY(), moveDirection.getModZ());
            moves.add(new TrackedMove(from, to));
        }

        if (moves.isEmpty()) return;
        for (TrackedMove move : moves) database.consumePlacedOre(move.from());
        for (TrackedMove move : moves) database.markPlacedOre(move.to());
    }

    private void updateEchoStreak(Player player) {
        int rank = database.getSkillRank(player.getUniqueId(), MinerSkill.ECHO_OF_DEPTH);
        if (rank <= 0) return;

        long now = System.currentTimeMillis();
        Streak old = streaks.get(player.getUniqueId());
        int count = old == null || now - old.lastMineAt > 60_000L ? 1 : old.count + 1;
        streaks.put(player.getUniqueId(), new Streak(count, now));

        int trigger = Math.max(10, 25 - (rank - 1) * 5);
        if (count >= trigger) {
            int amplifier = rank >= 3 ? 1 : 0;
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 8, amplifier, true, false, true));
            streaks.put(player.getUniqueId(), new Streak(0, now));
        }
    }

    private record Streak(int count, long lastMineAt) {}
    private record TrackedMove(Location from, Location to) {}
}
