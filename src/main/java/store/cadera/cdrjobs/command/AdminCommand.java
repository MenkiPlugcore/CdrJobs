package store.cadera.cdrjobs.command;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.service.ProgressionService;
import store.cadera.cdrjobs.util.Colors;

import java.util.*;

public final class AdminCommand implements CommandExecutor,TabCompleter{
    private final CdrJobsPlugin plugin;private final Database database;private final ProfessionStore store;private final ProgressionService progression;
    public AdminCommand(CdrJobsPlugin plugin,Database database,ProfessionStore store,ProgressionService progression){this.plugin=plugin;this.database=database;this.store=store;this.progression=progression;}
    @Override public boolean onCommand(@NotNull CommandSender sender,@NotNull Command command,@NotNull String label,@NotNull String[] args){
        if(!sender.hasPermission("cdrjobs.admin")){sender.sendMessage(Colors.color(plugin.prefix()+plugin.message("no-permission")));return true;}
        if(args.length==1&&args[0].equalsIgnoreCase("reload")){plugin.reloadPluginFiles();sender.sendMessage(Colors.color(plugin.prefix()+plugin.message("admin-reload")));return true;}
        if(args.length>=2&&args[0].equalsIgnoreCase("reset")){Player t=Bukkit.getPlayerExact(args[1]);if(t==null){sender.sendMessage("§cPlayer harus online.");return true;}database.resetPlayer(t.getUniqueId());store.resetPlayer(t.getUniqueId());sender.sendMessage("§aCdrJobs data reset: "+t.getName());return true;}
        if(args.length>=3&&(args[0].equalsIgnoreCase("addxp")||args[0].equalsIgnoreCase("setlevel"))){Player t=Bukkit.getPlayerExact(args[1]);if(t==null){sender.sendMessage("§cPlayer harus online.");return true;}JobType job=JobType.MINER;String valueArg=args[2];if(args.length>=4){try{job=JobType.valueOf(args[2].toUpperCase());valueArg=args[3];}catch(IllegalArgumentException e){sender.sendMessage("§cJob: miner, farmer, hunter, lumberjack, fisher");return true;}}try{if(args[0].equalsIgnoreCase("addxp")){long amount=Long.parseLong(valueArg);progression.addXp(t,job,amount);sender.sendMessage("§aAdded "+amount+" "+job.name()+" XP to "+t.getName());}else{int lv=Integer.parseInt(valueArg);progression.setLevel(t,job,lv);sender.sendMessage("§a"+job.name()+" level set untuk "+t.getName());}}catch(NumberFormatException e){sender.sendMessage("§cNilai harus angka.");}return true;}
        sender.sendMessage("§b/cdrjobsadmin addxp <player> [job] <amount>");sender.sendMessage("§b/cdrjobsadmin setlevel <player> [job] <level>");sender.sendMessage("§b/cdrjobsadmin reset <player>");sender.sendMessage("§b/cdrjobsadmin reload");return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,@NotNull Command command,@NotNull String alias,@NotNull String[] args){if(args.length==1)return List.of("addxp","setlevel","reset","reload");if(args.length==2&&!args[0].equalsIgnoreCase("reload")){List<String>n=new ArrayList<>();Bukkit.getOnlinePlayers().forEach(p->n.add(p.getName()));return n;}if(args.length==3&&(args[0].equalsIgnoreCase("addxp")||args[0].equalsIgnoreCase("setlevel")))return List.of("miner","farmer","hunter","lumberjack","fisher");return List.of();}
}
