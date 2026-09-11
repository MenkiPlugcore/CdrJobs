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
import store.cadera.cdrjobs.service.*;
import store.cadera.cdrjobs.util.JobRanks;

public final class CdrJobsExpansion extends PlaceholderExpansion {
    private final CdrJobsPlugin plugin;
    private final Database db;
    private final ProfessionStore store;
    private final LevelService levels;
    private final ProfileService profiles;
    private final MasteryService mastery;
    private final ContractService contracts;
    private final FateResonanceService resonance;

    public CdrJobsExpansion(CdrJobsPlugin plugin, Database db, ProfessionStore store,
                            LevelService levels, ProfileService profiles, MasteryService mastery,
                            ContractService contracts, FateResonanceService resonance) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;
        this.levels = levels;
        this.profiles = profiles;
        this.mastery = mastery;
        this.contracts = contracts;
        this.resonance = resonance;
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

        if (query.equals("resonance_unlocked")) return String.valueOf(resonance.unlockedCount(player.getUniqueId()));
        if (query.equals("resonance_harmonized")) return String.valueOf(resonance.harmonizedCount(player.getUniqueId()));
        if (query.equals("resonance_score")) return String.valueOf(resonance.score(player.getUniqueId()));
        if (query.equals("resonance_names")) return resonance.unlockedNames(player.getUniqueId());
        if (query.equals("resonance_harmonized_names")) return resonance.harmonizedNames(player.getUniqueId());
        if (query.startsWith("resonance_")) {
            String body = query.substring("resonance_".length());
            for (FateResonanceService.Definition def : resonance.definitions()) {
                String prefix = def.id() + "_";
                if (!body.startsWith(prefix)) continue;
                FateResonanceService.State state = resonance.state(player.getUniqueId(), def);
                String tail = body.substring(prefix.length());
                return switch (tail) {
                    case "unlocked" -> String.valueOf(state.unlocked());
                    case "harmonized" -> String.valueOf(state.harmonized());
                    case "status" -> state.harmonized() ? "HARMONIZED" : state.unlocked() ? "RESONANT" : "LOCKED";
                    case "display" -> resonance.display(state);
                    case "title" -> def.title();
                    case "badge" -> def.badge();
                    default -> null;
                };
            }
        }

        if (query.startsWith("contract_daily_")) return contractValue(player, ContractService.Cadence.DAILY, query.substring("contract_daily_".length()));
        if (query.startsWith("contract_weekly_")) return contractValue(player, ContractService.Cadence.WEEKLY, query.substring("contract_weekly_".length()));

        if (query.equals("miner_trial_stone") || query.equals("miner_trial_deep") || query.equals("miner_total_ores")) {
            MinerTrialProgress trial = db.getMinerTrialProgress(player.getUniqueId());
            return switch (query) {
                case "miner_trial_stone" -> trial.stoneComplete() ? "COMPLETED" : "IN PROGRESS";
                case "miner_trial_deep" -> trial.deepComplete() ? "COMPLETED" : "IN PROGRESS";
                case "miner_total_ores" -> String.valueOf(trial.totalOres());
                default -> null;
            };
        }

        if (query.startsWith("profile_")) {
            ProfileService.ProfileSnapshot snapshot = profiles.snapshot(player.getUniqueId());
            return switch (query.substring("profile_".length())) {
                case "total_level" -> String.valueOf(snapshot.totalLevel());
                case "highest_profession" -> snapshot.highestJob().displayName();
                case "highest_level" -> String.valueOf(snapshot.highestLevel());
                case "trials_completed" -> String.valueOf(snapshot.completedTrials());
                case "skills_unlocked" -> String.valueOf(snapshot.unlockedSkills());
                case "awakened_count" -> String.valueOf(snapshot.awakenedJobs().size());
                case "awakened_paths" -> snapshot.awakenedNames();
                case "mastery_tiers" -> String.valueOf(mastery.totalMasteryTiers(player.getUniqueId()));
                case "mastery_xp" -> String.valueOf(mastery.totalMasteryXp(player.getUniqueId()));
                case "resonance_unlocked" -> String.valueOf(resonance.unlockedCount(player.getUniqueId()));
                case "resonance_harmonized" -> String.valueOf(resonance.harmonizedCount(player.getUniqueId()));
                case "resonance_score" -> String.valueOf(resonance.score(player.getUniqueId()));
                default -> null;
            };
        }

        for (JobType job : JobType.values()) {
            String prefix = job.name().toLowerCase() + "_";
            if (query.startsWith(prefix)) {
                JobProgress progress = db.getProgress(player.getUniqueId(), job);
                String tail = query.substring(prefix.length());
                MasteryService.State state = mastery.state(player.getUniqueId(), job);
                return switch (tail) {
                    case "level" -> String.valueOf(progress.level());
                    case "xp" -> String.valueOf(progress.xp());
                    case "xp_required" -> progress.level() >= levels.maxLevel() ? "0" : String.valueOf(levels.xpRequiredForNextLevel(progress.level()));
                    case "rank" -> JobRanks.title(job, progress.level());
                    case "mastery_tier" -> String.valueOf(state.tier());
                    case "mastery_roman" -> mastery.roman(state.tier());
                    case "mastery_xp" -> String.valueOf(state.xp());
                    case "mastery_xp_required" -> String.valueOf(state.requiredXp());
                    case "mastery_total_xp" -> String.valueOf(state.totalXp());
                    case "mastery_title" -> state.title();
                    case "mastery_badge" -> state.badge();
                    case "mastery_display" -> mastery.display(job, state);
                    default -> metric(player, job, tail);
                };
            }
        }
        return null;
    }

    private String contractValue(OfflinePlayer player, ContractService.Cadence cadence, String tail) {
        ContractService.ContractView view = contracts.view(player.getUniqueId(), cadence);
        ContractService.ContractDefinition def = view.definition();
        return switch (tail) {
            case "id" -> def.id();
            case "name" -> def.name();
            case "job" -> def.job().displayName();
            case "progress" -> String.valueOf(view.progress());
            case "target" -> String.valueOf(def.target());
            case "percent" -> String.valueOf(view.percent());
            case "complete" -> String.valueOf(view.complete());
            case "claimed" -> String.valueOf(view.claimed());
            case "reward_xp" -> String.valueOf(def.rewardXp());
            case "reward_fate" -> String.valueOf(def.rewardFate());
            case "rerolls_left" -> String.valueOf(Math.max(0, view.rerollLimit() - view.usedRerolls()));
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
