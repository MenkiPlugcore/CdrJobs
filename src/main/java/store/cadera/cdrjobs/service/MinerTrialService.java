package store.cadera.cdrjobs.service;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.model.MinerTrialProgress;
import store.cadera.cdrjobs.util.Colors;

import java.util.EnumSet;
import java.util.Set;

public final class MinerTrialService {
    private static final Set<Material> RARE_ORES = EnumSet.of(
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.ANCIENT_DEBRIS
    );

    private final CdrJobsPlugin plugin;
    private final Database database;

    public MinerTrialService(CdrJobsPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void onNaturalOreMined(Player player, Block block) {
        boolean deep = block.getY() < 0;
        boolean rare = RARE_ORES.contains(block.getType());
        boolean ancient = block.getType() == Material.ANCIENT_DEBRIS;
        MinerTrialProgress progress = database.incrementMinerTrial(player.getUniqueId(), deep, rare, ancient);

        if (!progress.stoneComplete() && progress.totalOres() >= stoneTarget()) {
            database.setStoneTrialComplete(player.getUniqueId());
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("trial-stone-complete")));
        }

        if (!progress.deepComplete()
                && progress.deepOres() >= deepTarget()
                && progress.rareOres() >= rareTarget()
                && progress.ancientDebris() >= ancientTarget()) {
            database.setDeepTrialComplete(player.getUniqueId());
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("trial-deep-complete")));
        }
    }

    public MinerTrialProgress progress(Player player) {
        return database.getMinerTrialProgress(player.getUniqueId());
    }

    public boolean isStoneComplete(Player player) {
        return progress(player).stoneComplete();
    }

    public boolean isDeepComplete(Player player) {
        return progress(player).deepComplete();
    }

    public int stoneTarget() {
        return Math.max(1, plugin.getConfig().getInt("miner.trials.trial-of-stone.total-ores", 500));
    }

    public int deepTarget() {
        return Math.max(1, plugin.getConfig().getInt("miner.trials.trial-of-the-deep.deep-ores", 1200));
    }

    public int rareTarget() {
        return Math.max(1, plugin.getConfig().getInt("miner.trials.trial-of-the-deep.rare-ores", 80));
    }

    public int ancientTarget() {
        return Math.max(1, plugin.getConfig().getInt("miner.trials.trial-of-the-deep.ancient-debris", 10));
    }
}
