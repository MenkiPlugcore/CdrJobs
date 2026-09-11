package store.cadera.cdrjobs.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.model.MinerTrialProgress;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.MinerAbilityService;

public final class CdrJobsExpansion extends PlaceholderExpansion {
    private final CdrJobsPlugin plugin;
    private final Database database;
    private final LevelService levels;

    public CdrJobsExpansion(CdrJobsPlugin plugin, Database database, LevelService levels) {
        this.plugin = plugin;
        this.database = database;
        this.levels = levels;
    }

    @Override public @NotNull String getIdentifier() { return "cdrjobs"; }
    @Override public @NotNull String getAuthor() { return "CADERA"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getUniqueId() == null) return "";
        JobProgress progress = database.getProgress(player.getUniqueId(), JobType.MINER);
        MinerTrialProgress trials = database.getMinerTrialProgress(player.getUniqueId());
        return switch (params.toLowerCase()) {
            case "miner_level" -> String.valueOf(progress.level());
            case "miner_xp" -> String.valueOf(progress.xp());
            case "miner_xp_required" -> String.valueOf(levels.xpRequiredForNextLevel(progress.level()));
            case "miner_rank" -> progress.minerRankTitle();
            case "fate_essence" -> String.valueOf(database.getFateEssence(player.getUniqueId()));
            case "miner_trial_stone" -> trials.stoneComplete() ? "COMPLETED" : "IN PROGRESS";
            case "miner_trial_deep" -> trials.deepComplete() ? "COMPLETED" : "IN PROGRESS";
            case "miner_total_ores" -> String.valueOf(trials.totalOres());
            case "miner_deep_ores" -> String.valueOf(trials.deepOres());
            case "miner_rare_ores" -> String.valueOf(trials.rareOres());
            case "miner_ancient_debris" -> String.valueOf(trials.ancientDebris());
            case "runic_surge_cooldown" -> {
                long remaining = database.getAbilityReadyAt(player.getUniqueId(), MinerAbilityService.RUNIC_SURGE) - System.currentTimeMillis();
                yield String.valueOf(remaining <= 0 ? 0 : (remaining + 999L) / 1000L);
            }
            default -> null;
        };
    }
}
