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
import store.cadera.cdrjobs.gui.JobsMenu;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.service.*;
import store.cadera.cdrjobs.util.Colors;
import store.cadera.cdrjobs.util.JobRanks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class JobsCommand implements CommandExecutor, TabCompleter {
    private final CdrJobsPlugin plugin;
    private final Database db;
    private final JobsMenu menu;
    private final LevelService levels;
    private final MinerAbilityService miner;
    private final FarmerService farmer;
    private final HunterService hunter;
    private final LumberjackService lumber;
    private final FisherService fisher;
    private final LeaderboardService leaderboards;

    public JobsCommand(CdrJobsPlugin plugin, Database db, JobsMenu menu, LevelService levels,
                       MinerAbilityService miner, FarmerService farmer, HunterService hunter,
                       LumberjackService lumber, FisherService fisher, LeaderboardService leaderboards) {
        this.plugin = plugin;
        this.db = db;
        this.menu = menu;
        this.levels = levels;
        this.miner = miner;
        this.farmer = farmer;
        this.hunter = hunter;
        this.lumber = lumber;
        this.fisher = fisher;
        this.leaderboards = leaderboards;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only");
            return true;
        }
        if (!player.hasPermission("cdrjobs.use")) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("no-permission")));
            return true;
        }
        if (args.length == 0) {
            menu.openMain(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "profile" -> openProfile(player, args);
            case "top", "leaderboard" -> showLeaderboard(player, args);
            case "miner", "skills" -> menu.openMiner(player);
            case "farmer" -> menu.openFarmer(player);
            case "hunter" -> menu.openHunter(player);
            case "lumberjack", "lumber" -> menu.openLumber(player);
            case "fisher", "fish" -> menu.openFisher(player);
            case "trials" -> {
                String job = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "miner";
                if (job.equals("farmer")) menu.openFarmerTrials(player);
                else if (job.equals("hunter")) menu.openHunterTrials(player);
                else if (job.startsWith("lumber")) menu.openLumberTrials(player);
                else if (job.startsWith("fish")) menu.openFisherTrials(player);
                else menu.openTrials(player);
            }
            case "ability" -> {
                String job = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "miner";
                if (job.equals("farmer")) farmer.activate(player);
                else if (job.equals("hunter")) hunter.activate(player);
                else if (job.startsWith("lumber")) lumber.activate(player);
                else if (job.startsWith("fish")) fisher.activate(player);
                else miner.activateRunicSurge(player);
            }
            case "stats" -> sendStats(player, args);
            default -> menu.openMain(player);
        }
        return true;
    }

    private void openProfile(Player viewer, String[] args) {
        Player target = viewer;
        if (args.length > 1) {
            if (!viewer.hasPermission("cdrjobs.profile.others")) {
                viewer.sendMessage("§cKamu tidak memiliki izin untuk melihat profile player lain.");
                return;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                viewer.sendMessage("§cPlayer harus online.");
                return;
            }
        }
        menu.openProfile(viewer, target);
    }

    private void showLeaderboard(Player viewer, String[] args) {
        if (args.length < 2) {
            viewer.sendMessage("§3CdrJobs Leaderboard");
            viewer.sendMessage("§7/cdrjobs top <miner|farmer|hunter|lumberjack|fisher>");
            viewer.sendMessage("§7/cdrjobs top total");
            viewer.sendMessage("§7/cdrjobs top pvp");
            viewer.sendMessage("§7/cdrjobs top activity <job>");
            return;
        }

        String type = args[1].toLowerCase(Locale.ROOT);
        if (type.equals("total")) {
            sendRows(viewer, "Total Profession Level", leaderboards.totalLevel(), RowMode.TOTAL);
            return;
        }
        if (type.equals("pvp") || type.equals("hunterpvp")) {
            sendRows(viewer, "Hunter PvP Kills", leaderboards.hunterPvp(), RowMode.COUNT);
            return;
        }
        if (type.equals("activity")) {
            if (args.length < 3) {
                viewer.sendMessage("§cUsage: /cdrjobs top activity <job>");
                return;
            }
            JobType job = parseJob(args[2]);
            if (job == null) {
                viewer.sendMessage("§cJob tidak valid.");
                return;
            }
            sendRows(viewer, job.displayName() + " Activity", leaderboards.activity(job), RowMode.COUNT);
            return;
        }

        JobType job = parseJob(type);
        if (job == null) {
            viewer.sendMessage("§cLeaderboard tidak valid. Gunakan job, total, pvp, atau activity <job>.");
            return;
        }
        sendRows(viewer, job.displayName() + " Profession", leaderboards.profession(job), RowMode.PROFESSION);
    }

    private void sendRows(Player viewer, String title, List<LeaderboardService.LeaderboardRow> rows, RowMode mode) {
        viewer.sendMessage("§3✦ CdrJobs Top — §f" + title + " §8(cache " + plugin.getConfig().getLong("leaderboards.cache-seconds", 30L) + "s)");
        if (rows.isEmpty()) {
            viewer.sendMessage("§7Belum ada data leaderboard.");
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardService.LeaderboardRow row = rows.get(i);
            String value = switch (mode) {
                case PROFESSION -> "§bLv." + row.value() + " §7• XP §f" + row.secondary();
                case TOTAL -> "§b" + row.value() + " §7total level";
                case COUNT -> "§b" + row.value();
            };
            viewer.sendMessage("§6#" + (i + 1) + " §f" + row.name() + " §8— " + value);
        }
    }

    private JobType parseJob(String raw) {
        String value = raw.toLowerCase(Locale.ROOT);
        if (value.equals("lumber")) value = "lumberjack";
        if (value.equals("fish")) value = "fisher";
        try { return JobType.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private void sendStats(Player viewer, String[] args) {
        Player target = viewer;
        if (args.length > 1 && viewer.hasPermission("cdrjobs.admin")) {
            Player found = Bukkit.getPlayerExact(args[1]);
            if (found != null) target = found;
        }
        viewer.sendMessage("§3✦ CdrJobs — " + target.getName());
        for (JobType job : JobType.values()) {
            JobProgress progress = db.getProgress(target.getUniqueId(), job);
            viewer.sendMessage("§b" + job.displayName() + " §7• " + JobRanks.title(job, progress.level())
                    + " §f| Lv." + progress.level() + " | "
                    + (progress.level() >= levels.maxLevel() ? "MAX" : progress.xp() + "/" + levels.xpRequiredForNextLevel(progress.level())));
        }
        viewer.sendMessage("§dFate Essence: " + db.getFateEssence(target.getUniqueId()));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return List.of("profile", "top", "stats", "miner", "farmer", "hunter", "lumberjack", "fisher", "trials", "ability");
        if (args.length == 2 && (args[0].equalsIgnoreCase("trials") || args[0].equalsIgnoreCase("ability"))) {
            return List.of("miner", "farmer", "hunter", "lumberjack", "fisher");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return List.of("total", "pvp", "activity", "miner", "farmer", "hunter", "lumberjack", "fisher");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("top") && args[1].equalsIgnoreCase("activity")) {
            return List.of("miner", "farmer", "hunter", "lumberjack", "fisher");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("profile"))) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> names.add(player.getName()));
            return names;
        }
        return List.of();
    }

    private enum RowMode { PROFESSION, TOTAL, COUNT }
}
