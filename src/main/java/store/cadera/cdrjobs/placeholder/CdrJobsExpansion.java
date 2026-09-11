package store.cadera.cdrjobs.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.model.MinerTrialProgress;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.util.JobRanks;

public final class CdrJobsExpansion extends PlaceholderExpansion {
    private final CdrJobsPlugin plugin;
    private final Database db;
    private final ProfessionStore store;
    private final LevelService levels;

    public CdrJobsExpansion(CdrJobsPlugin plugin, Database db, ProfessionStore store, LevelService levels) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;
        this.levels = levels;
    }

    @Override public @NotNull String getIdentifier() { return "cdrjobs"; }
    @Override public @NotNull String getAuthor() { return "CADERA"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String param) {
        if (player == null || player.getUniqueId() == null) return "";
        String query = param.toLowerCase();
        if (query.equals("fate_essence")) return String.valueOf(db.getFateEssence(player.getUniqueId()));

        for (JobType job : JobType.values()) {
            String prefix = job.name().toLowerCase() + "_";
            if (query.startsWith(prefix)) {
                JobProgress progress = db.getProgress(player.getUniqueId(), job);
                String tail = query.substring(prefix.length());
                return switch (tail) {
                    case "level" -> String.valueOf(progress.level());
                    case "xp" -> String.valueOf(progress.xp());
                    case "xp_required" -> String.valueOf(levels.xpRequiredForNextLevel(progress.level()));
                    case "rank" -> JobRanks.title(job, progress.level());
                    default -> metric(player, job, tail);
                };
            }
        }

        MinerTrialProgress trial = db.getMinerTrialProgress(player.getUniqueId());
        return switch (query) {
            case "miner_trial_stone" -> trial.stoneComplete() ? "COMPLETED" : "IN PROGRESS";
            case "miner_trial_deep" -> trial.deepComplete() ? "COMPLETED" : "IN PROGRESS";
            case "miner_total_ores" -> String.valueOf(trial.totalOres());
            default -> null;
        };
    }

    private String metric(OfflinePlayer player, JobType job, String tail) {
        String metric = switch (job) {
            case FARMER -> tail.equals("harvests") ? "harvests" : null;
            case HUNTER -> switch (tail) {
                case "kills" -> "kills";
                case "mob_kills" -> "mob_kills";
                case "pvp_kills" -> "pvp_kills";
                default -> null;
            };
            case LUMBERJACK -> tail.equals("logs") ? "logs" : null;
            case FISHER -> tail.equals("catches") ? "catches" : null;
            default -> null;
        };
        return metric == null ? null : String.valueOf(store.getCounter(player.getUniqueId(), job.name(), metric));
    }
}
