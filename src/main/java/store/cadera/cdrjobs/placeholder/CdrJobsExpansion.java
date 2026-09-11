package store.cadera.cdrjobs.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.service.LevelService;

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
        return switch (params.toLowerCase()) {
            case "miner_level" -> String.valueOf(progress.level());
            case "miner_xp" -> String.valueOf(progress.xp());
            case "miner_xp_required" -> String.valueOf(levels.xpRequiredForNextLevel(progress.level()));
            case "miner_rank" -> progress.minerRankTitle();
            case "fate_essence" -> String.valueOf(database.getFateEssence(player.getUniqueId()));
            default -> null;
        };
    }
}
