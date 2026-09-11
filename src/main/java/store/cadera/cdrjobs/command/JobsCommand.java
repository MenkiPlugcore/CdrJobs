package store.cadera.cdrjobs.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.gui.JobsMenu;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.service.FarmerService;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.MinerAbilityService;
import store.cadera.cdrjobs.util.Colors;
import store.cadera.cdrjobs.util.JobRanks;

import java.util.*;

public final class JobsCommand implements CommandExecutor, TabCompleter {
    private final CdrJobsPlugin plugin; private final Database database; private final JobsMenu menu; private final LevelService levels; private final MinerAbilityService minerAbility; private final FarmerService farmer;
    public JobsCommand(CdrJobsPlugin plugin,Database database,JobsMenu menu,LevelService levels,MinerAbilityService minerAbility,FarmerService farmer){this.plugin=plugin;this.database=database;this.menu=menu;this.levels=levels;this.minerAbility=minerAbility;this.farmer=farmer;}
    @Override public boolean onCommand(@NotNull CommandSender sender,@NotNull Command command,@NotNull String label,@NotNull String[] args){
        if(!(sender instanceof Player p)){sender.sendMessage(Colors.color(plugin.prefix()+plugin.message("player-only")));return true;} if(!p.hasPermission("cdrjobs.use")){p.sendMessage(Colors.color(plugin.prefix()+plugin.message("no-permission")));return true;}
        if(args.length==0){menu.openMain(p);return true;}
        switch(args[0].toLowerCase()){
            case "skills","miner"->{menu.openMiner(p);return true;} case "farmer"->{menu.openFarmer(p);return true;}
            case "trials"->{if(args.length>1&&args[1].equalsIgnoreCase("farmer"))menu.openFarmerTrials(p);else menu.openTrials(p);return true;}
            case "ability"->{if(args.length>1&&args[1].equalsIgnoreCase("farmer"))farmer.activate(p);else minerAbility.activateRunicSurge(p);return true;}
            case "stats"->{Player target=p;if(args.length>=2&&p.hasPermission("cdrjobs.admin")){Player found=Bukkit.getPlayerExact(args[1]);if(found!=null)target=found;} p.sendMessage(ChatColor.DARK_AQUA+"✦ CdrJobs — "+target.getName()); for(JobType job:JobType.values()){if(!job.released())continue;JobProgress jp=database.getProgress(target.getUniqueId(),job);long req=levels.xpRequiredForNextLevel(jp.level());p.sendMessage(ChatColor.AQUA+job.displayName()+ChatColor.GRAY+" • "+JobRanks.title(job,jp.level())+ChatColor.WHITE+" | Lv."+jp.level()+" | "+(jp.level()>=levels.maxLevel()?"MAX":jp.xp()+"/"+req));} p.sendMessage(ChatColor.LIGHT_PURPLE+"Fate Essence: "+database.getFateEssence(target.getUniqueId()));return true;}
            default->{menu.openMain(p);return true;}
        }
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,@NotNull Command command,@NotNull String alias,@NotNull String[] args){if(args.length==1)return List.of("stats","miner","farmer","trials","ability");if(args.length==2&&args[0].equalsIgnoreCase("ability"))return List.of("miner","farmer");if(args.length==2&&args[0].equalsIgnoreCase("trials"))return List.of("miner","farmer");if(args.length==2&&args[0].equalsIgnoreCase("stats")&&sender.hasPermission("cdrjobs.admin")){List<String> n=new ArrayList<>();Bukkit.getOnlinePlayers().forEach(x->n.add(x.getName()));return n;}return List.of();}
}
