package store.cadera.cdrjobs.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.service.HunterService;
import store.cadera.cdrjobs.service.ProgressionService;
import store.cadera.cdrjobs.util.Colors;
import store.cadera.cdrjobs.util.JobRanks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class AdminCommand implements CommandExecutor, TabCompleter {
    private static final int EXPECTED_SCHEMA = 9;
    private final CdrJobsPlugin plugin;
    private final Database database;
    private final ProfessionStore store;
    private final ProgressionService progression;
    private final HunterService hunter;

    public AdminCommand(CdrJobsPlugin plugin, Database database, ProfessionStore store, ProgressionService progression, HunterService hunter) {
        this.plugin = plugin;
        this.database = database;
        this.store = store;
        this.progression = progression;
        this.hunter = hunter;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("cdrjobs.admin")) {
            sender.sendMessage(Colors.color(plugin.prefix() + plugin.message("no-permission")));
            return true;
        }
        if (args.length == 0) { sendUsage(sender); return true; }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "diagnose" -> handleDiagnose(sender, args);
            case "inspect" -> handleInspect(sender, args);
            case "hunterdebug" -> handleHunterDebug(sender, args);
            case "reload" -> handleReload(sender, args);
            case "reset" -> handleReset(sender, args);
            case "addxp", "setlevel" -> handleProgressCommand(sender, args);
            default -> { sendUsage(sender); yield true; }
        };
    }

    private boolean handleDiagnose(CommandSender sender, String[] args) {
        if (args.length != 1) { sender.sendMessage("§cUsage: /cdrjobsadmin diagnose"); return true; }
        int schema = store.getSchemaVersion();
        sender.sendMessage("§3CdrJobs Diagnostics");
        sender.sendMessage("§fVersion: §b" + plugin.getPluginMeta().getVersion());
        sender.sendMessage("§fSchema: §b" + schema + " §7(expected " + EXPECTED_SCHEMA + ")");
        sender.sendMessage("§fSchema health: " + (schema == EXPECTED_SCHEMA ? "§aHEALTHY" : "§cMISMATCH"));
        sender.sendMessage("§fMinecraft: §b" + Bukkit.getMinecraftVersion());
        sender.sendMessage("§fPlaceholderAPI: §b" + (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? "ENABLED" : "NOT INSTALLED"));
        sender.sendMessage("§fProfessions: §b5/5 enabled in core");
        return true;
    }

    private boolean handleInspect(CommandSender sender, String[] args) {
        if (args.length != 2) { sender.sendMessage("§cUsage: /cdrjobsadmin inspect <player>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage("§cPlayer harus online."); return true; }
        int highestLevel = -1;
        List<String> highest = new ArrayList<>();
        sender.sendMessage("§3CdrJobs Inspect §8— §f" + target.getName());
        sender.sendMessage("§fUUID: §7" + target.getUniqueId());
        sender.sendMessage("§fFate Essence: §d" + database.getFateEssence(target.getUniqueId()));
        for (JobType job : JobType.values()) {
            JobProgress progress = database.getProgress(target.getUniqueId(), job);
            sender.sendMessage("§7- §b" + job.displayName() + " §7Lv.§f" + progress.level() + " §7XP §f" + progress.xp() + " §8• §d" + JobRanks.title(job, progress.level()));
            if (progress.level() > highestLevel) { highestLevel = progress.level(); highest.clear(); highest.add(job.displayName()); }
            else if (progress.level() == highestLevel) highest.add(job.displayName());
        }
        sender.sendMessage("§fHighest profession: §b" + String.join("§7 / §b", highest) + " §7(Lv." + highestLevel + ")");
        return true;
    }

    private boolean handleHunterDebug(CommandSender sender, String[] args) {
        if (args.length != 2) { sender.sendMessage("§cUsage: /cdrjobsadmin hunterdebug <player>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage("§cPlayer harus online."); return true; }
        hunter.debugLines(target).forEach(sender::sendMessage);
        sender.sendMessage("§fMob kills: §b" + store.getCounter(target.getUniqueId(), HunterService.JOB, "mob_kills"));
        sender.sendMessage("§fPvP kills: §b" + store.getCounter(target.getUniqueId(), HunterService.JOB, "pvp_kills"));
        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (args.length != 1) { sender.sendMessage("§cUsage: /cdrjobsadmin reload"); return true; }
        plugin.reloadPluginFiles();
        sender.sendMessage(Colors.color(plugin.prefix() + plugin.message("admin-reload")));
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (args.length != 2) { sender.sendMessage("§cUsage: /cdrjobsadmin reset <player>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage("§cPlayer harus online."); return true; }
        database.resetPlayer(target.getUniqueId());
        store.resetPlayer(target.getUniqueId());
        sender.sendMessage("§aCdrJobs data reset: " + target.getName());
        return true;
    }

    private boolean handleProgressCommand(CommandSender sender, String[] args) {
        boolean addXp = args[0].equalsIgnoreCase("addxp");
        if (args.length != 3 && args.length != 4) { sender.sendMessage("§cUsage: /cdrjobsadmin " + args[0] + " <player> [job] <value>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage("§cPlayer harus online."); return true; }
        JobType job = JobType.MINER;
        String rawValue;
        if (args.length == 4) {
            try { job = JobType.valueOf(args[2].toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { sender.sendMessage("§cJob tidak valid. Gunakan: miner, farmer, hunter, lumberjack, fisher."); return true; }
            rawValue = args[3];
        } else rawValue = args[2];
        try {
            if (addXp) {
                long amount = Long.parseLong(rawValue);
                if (amount <= 0L) { sender.sendMessage("§cXP harus lebih besar dari 0."); return true; }
                progression.addXp(target, job, amount);
                sender.sendMessage("§aAdded " + amount + " " + job.name() + " XP to " + target.getName() + ".");
            } else {
                int level = Integer.parseInt(rawValue);
                int maxLevel = Math.max(1, plugin.getConfig().getInt("settings.max-level", 100));
                if (level < 1 || level > maxLevel) { sender.sendMessage("§cLevel harus berada di antara 1 dan " + maxLevel + "."); return true; }
                progression.setLevel(target, job, level);
                sender.sendMessage("§a" + job.name() + " level " + target.getName() + " diubah ke " + level + ".");
            }
        } catch (NumberFormatException ex) { sender.sendMessage("§cNilai harus berupa angka yang valid."); }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§3CdrJobs Admin Commands");
        sender.sendMessage("§b/cdrjobsadmin diagnose");
        sender.sendMessage("§b/cdrjobsadmin inspect <player>");
        sender.sendMessage("§b/cdrjobsadmin hunterdebug <player>");
        sender.sendMessage("§b/cdrjobsadmin reload");
        sender.sendMessage("§b/cdrjobsadmin reset <player>");
        sender.sendMessage("§b/cdrjobsadmin addxp <player> [job] <amount>");
        sender.sendMessage("§b/cdrjobsadmin setlevel <player> [job] <level>");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return filter(List.of("diagnose", "inspect", "hunterdebug", "reload", "addxp", "setlevel", "reset"), args[0]);
        if (args.length == 2 && List.of("inspect", "hunterdebug", "addxp", "setlevel", "reset").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> names = new ArrayList<>(); Bukkit.getOnlinePlayers().forEach(player -> names.add(player.getName())); return filter(names, args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("addxp") || args[0].equalsIgnoreCase("setlevel"))) return filter(Arrays.stream(JobType.values()).map(job -> job.name().toLowerCase(Locale.ROOT)).toList(), args[2]);
        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
