package store.cadera.cdrjobs.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.MinerAbilityService;
import store.cadera.cdrjobs.util.Colors;

import java.util.ArrayList;
import java.util.List;

public final class JobsCommand implements CommandExecutor, TabCompleter {
    private final CdrJobsPlugin plugin;
    private final Database database;
    private final JobsMenu menu;
    private final LevelService levels;
    private final MinerAbilityService abilities;

    public JobsCommand(CdrJobsPlugin plugin, Database database, JobsMenu menu, LevelService levels, MinerAbilityService abilities) {
        this.plugin = plugin;
        this.database = database;
        this.menu = menu;
        this.levels = levels;
        this.abilities = abilities;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Colors.color(plugin.prefix() + plugin.message("player-only")));
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

        if (args[0].equalsIgnoreCase("skills")) {
            menu.openMiner(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("trials")) {
            menu.openTrials(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("ability")) {
            abilities.activateRunicSurge(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("stats")) {
            Player target = player;
            if (args.length >= 2 && player.hasPermission("cdrjobs.admin")) {
                Player found = Bukkit.getPlayerExact(args[1]);
                if (found != null) target = found;
            }
            JobProgress p = database.getProgress(target.getUniqueId(), JobType.MINER);
            long req = levels.xpRequiredForNextLevel(p.level());
            player.sendMessage(ChatColor.DARK_AQUA + "✦ CdrJobs — " + target.getName());
            player.sendMessage(ChatColor.AQUA + "Runebound Delver " + ChatColor.GRAY + "• " + p.minerRankTitle());
            player.sendMessage(ChatColor.WHITE + "Level: " + ChatColor.AQUA + p.level());
            player.sendMessage(ChatColor.WHITE + "XP: " + ChatColor.AQUA + (p.level() >= levels.maxLevel() ? "MAX" : p.xp() + "/" + req));
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Fate Essence: " + database.getFateEssence(target.getUniqueId()));
            player.sendMessage(ChatColor.DARK_PURPLE + "Runic Surge cooldown: " + ChatColor.GRAY + abilities.cooldownSeconds(target.getUniqueId()) + "s");
            return true;
        }

        menu.openMain(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return List.of("stats", "skills", "trials", "ability");
        if (args.length == 2 && args[0].equalsIgnoreCase("stats") && sender.hasPermission("cdrjobs.admin")) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        return List.of();
    }
}
