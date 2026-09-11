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
import store.cadera.cdrjobs.service.FarmerService;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.MinerAbilityService;
import store.cadera.cdrjobs.util.JobRanks;

public final class CdrJobsExpansion extends PlaceholderExpansion {
    private final CdrJobsPlugin plugin;private final Database database;private final LevelService levels;private final FarmerService farmer;
    public CdrJobsExpansion(CdrJobsPlugin plugin,Database database,LevelService levels,FarmerService farmer){this.plugin=plugin;this.database=database;this.levels=levels;this.farmer=farmer;}
    @Override public @NotNull String getIdentifier(){return "cdrjobs";}@Override public @NotNull String getAuthor(){return "CADERA";}@Override public @NotNull String getVersion(){return plugin.getPluginMeta().getVersion();}@Override public boolean persist(){return true;}
    @Override public @Nullable String onRequest(OfflinePlayer player,@NotNull String params){if(player==null||player.getUniqueId()==null)return "";JobProgress miner=database.getProgress(player.getUniqueId(),JobType.MINER);JobProgress farm=database.getProgress(player.getUniqueId(),JobType.FARMER);MinerTrialProgress trials=database.getMinerTrialProgress(player.getUniqueId());return switch(params.toLowerCase()){
        case "miner_level"->String.valueOf(miner.level());case "miner_xp"->String.valueOf(miner.xp());case "miner_xp_required"->String.valueOf(levels.xpRequiredForNextLevel(miner.level()));case "miner_rank"->JobRanks.title(JobType.MINER,miner.level());
        case "farmer_level"->String.valueOf(farm.level());case "farmer_xp"->String.valueOf(farm.xp());case "farmer_xp_required"->String.valueOf(levels.xpRequiredForNextLevel(farm.level()));case "farmer_rank"->JobRanks.title(JobType.FARMER,farm.level());
        case "fate_essence"->String.valueOf(database.getFateEssence(player.getUniqueId()));case "miner_trial_stone"->trials.stoneComplete()?"COMPLETED":"IN PROGRESS";case "miner_trial_deep"->trials.deepComplete()?"COMPLETED":"IN PROGRESS";case "miner_total_ores"->String.valueOf(trials.totalOres());case "miner_deep_ores"->String.valueOf(trials.deepOres());case "miner_rare_ores"->String.valueOf(trials.rareOres());case "miner_ancient_debris"->String.valueOf(trials.ancientDebris());
        case "runic_surge_cooldown"->{long r=database.getAbilityReadyAt(player.getUniqueId(),MinerAbilityService.RUNIC_SURGE)-System.currentTimeMillis();yield String.valueOf(r<=0?0:(r+999L)/1000L);}default->null;};}
}
