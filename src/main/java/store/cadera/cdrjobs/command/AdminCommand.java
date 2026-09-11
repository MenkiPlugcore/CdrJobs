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
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.service.ProgressionService;
import store.cadera.cdrjobs.util.Colors;

import java.util.ArrayList;
import java.util.List;

public final class AdminCommand implements CommandExecutor, TabCompleter {
    private final CdrJobsPlugin plugin;
    private final Database database;
    private final ProgressionService progression;

    public AdminCommand(CdrJobsPlugin plugin, Database database, ProgressionService progression) {
        this.plugin = plugin;
        this.database = database;
        this.progression = progression;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("cdrjobs.admin")) {
            sender.sendMessage(Colors.color(plugin.prefix() + plugin.message("no-permission")));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPluginFiles();
            sender.sendMessage(Colors.color(plugin.prefix() + plugin.message("admin-reload")));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("reset")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage("Player harus online."); return true; }
            database.resetPlayer(target.getUniqueId());
            sender.sendMessage("§aCdrJobs data reset: " + target.getName());
            return true;
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("addxp")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage("Player harus online."); return true; }
            try {
                long amount = Long.parseLong(args[2]);
                progression.addXp(target, JobType.MINER, amount);
                sender.sendMessage("§aAdded " + amount + " Miner XP to " + target.getName());
            } catch (NumberFormatException e) {
                sender.sendMessage("§cAmount harus angka.");
            }
            return true;
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("setlevel")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage("Player harus online."); return true; }
            try {
                int level = Integer.parseInt(args[2]);
                progression.setLevel(target, JobType.MINER, level);
                sender.sendMessage("§aMiner level set untuk " + target.getName());
            } catch (NumberFormatException e) {
                sender.sendMessage("§cLevel harus angka.");
            }
            return true;
        }

        sender.sendMessage("§b/cdrjobsadmin addxp <player> <amount>");
        sender.sendMessage("§b/cdrjobsadmin setlevel <player> <level>");
        sender.sendMessage("§b/cdrjobsadmin reset <player>");
        sender.sendMessage("§b/cdrjobsadmin reload");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return List.of("addxp", "setlevel", "reset", "reload");
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        return List.of();
    }
}
