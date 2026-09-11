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
import store.cadera.cdrjobs.model.MinerTrialProgress;
import store.cadera.cdrjobs.service.HunterService;
import store.cadera.cdrjobs.service.ProgressionService;
import store.cadera.cdrjobs.util.Colors;
import store.cadera.cdrjobs.util.JobRanks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

public final class AdminCommand implements CommandExecutor, TabCompleter {
    private static final int EXPECTED_SCHEMA = 9;
    private final CdrJobsPlugin plugin;
    private final Database database;
    private final ProfessionStore store;
    private final ProgressionService progression;
    private final HunterService hunter;

    public AdminCommand(CdrJobsPlugin plugin, Database database, ProfessionStore store, ProgressionService progression, HunterService hunter) {
        this.plugin = plugin; this.database = database; this.store = store; this.progression = progression; this.hunter = hunter;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("cdrjobs.admin")) { sender.sendMessage(Colors.color(plugin.prefix() + plugin.message("no-permission"))); return true; }
        if (args.length == 0) { sendUsage(sender); return true; }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "diagnose" -> handleDiagnose(sender, args);
            case "inspect" -> handleInspect(sender, args);
            case "hunterdebug" -> handleHunterDebug(sender, args);
            case "reload" -> handleReload(sender, args);
            case "reset" -> handleReset(sender, args);
            case "resetjob" -> handleResetJob(sender, args);
            case "resettrial" -> handleResetTrial(sender, args);
            case "resetcooldown" -> handleResetCooldown(sender, args);
            case "addessence", "setessence" -> handleEssence(sender, args);
            case "export" -> handleExport(sender, args);
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
        Player target = onlineTarget(sender, args, 2, "/cdrjobsadmin inspect <player>");
        if (target == null) return true;
        UUID uuid = target.getUniqueId();
        sender.sendMessage("§3CdrJobs Inspect §8— §f" + target.getName());
        sender.sendMessage("§fUUID: §7" + uuid);
        sender.sendMessage("§fFate Essence: §d" + database.getFateEssence(uuid));
        int highest = -1; List<String> highestJobs = new ArrayList<>();
        for (JobType job : JobType.values()) {
            JobProgress p = database.getProgress(uuid, job);
            sender.sendMessage("§7- §b" + job.displayName() + " §7Lv.§f" + p.level() + " §7XP §f" + p.xp() + " §8• §d" + JobRanks.title(job, p.level()));
            Map<String,Long> counters = store.getCounters(uuid, job.name());
            Map<String,Boolean> flags = store.getFlags(uuid, job.name());
            if (!counters.isEmpty()) sender.sendMessage("  §7Counters: §f" + counters);
            if (!flags.isEmpty()) sender.sendMessage("  §7Trials/flags: §f" + flags);
            if (p.level() > highest) { highest = p.level(); highestJobs.clear(); highestJobs.add(job.displayName()); }
            else if (p.level() == highest) highestJobs.add(job.displayName());
        }
        MinerTrialProgress miner = database.getMinerTrialProgress(uuid);
        sender.sendMessage("§7Miner trials: §f" + miner);
        sender.sendMessage("§7Skills: §f" + database.getSkillRanks(uuid));
        sender.sendMessage("§7Cooldowns: §f" + formatCooldowns(database.getAbilityCooldowns(uuid)));
        sender.sendMessage("§fHighest profession: §b" + String.join("§7 / §b", highestJobs) + " §7(Lv." + highest + ")");
        return true;
    }

    private String formatCooldowns(Map<String,Long> cooldowns) {
        if (cooldowns.isEmpty()) return "{}";
        long now = System.currentTimeMillis();
        Map<String,String> out = new LinkedHashMap<>();
        cooldowns.forEach((key, readyAt) -> out.put(key, Math.max(0L, (readyAt - now + 999L) / 1000L) + "s"));
        return out.toString();
    }

    private boolean handleHunterDebug(CommandSender sender, String[] args) {
        Player target = onlineTarget(sender, args, 2, "/cdrjobsadmin hunterdebug <player>");
        if (target == null) return true;
        hunter.debugLines(target).forEach(sender::sendMessage);
        sender.sendMessage("§fMob kills: §b" + store.getCounter(target.getUniqueId(), HunterService.JOB, "mob_kills"));
        sender.sendMessage("§fPvP kills: §b" + store.getCounter(target.getUniqueId(), HunterService.JOB, "pvp_kills"));
        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (args.length != 1) { sender.sendMessage("§cUsage: /cdrjobsadmin reload"); return true; }
        plugin.reloadPluginFiles(); sender.sendMessage(Colors.color(plugin.prefix() + plugin.message("admin-reload"))); return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        Player target = onlineTarget(sender, args, 2, "/cdrjobsadmin reset <player>");
        if (target == null) return true;
        database.resetPlayer(target.getUniqueId()); store.resetPlayer(target.getUniqueId());
        sender.sendMessage("§aSeluruh data CdrJobs direset: " + target.getName()); return true;
    }

    private boolean handleResetJob(CommandSender sender, String[] args) {
        if (args.length != 3) { sender.sendMessage("§cUsage: /cdrjobsadmin resetjob <player> <job>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]); if (target == null) { sender.sendMessage("§cPlayer harus online."); return true; }
        JobType job = parseJob(sender, args[2]); if (job == null) return true;
        database.resetProfession(target.getUniqueId(), job); store.resetProfessionData(target.getUniqueId(), job.name());
        sender.sendMessage("§aReset profession " + job.name() + " untuk " + target.getName() + ". Fate Essence global tidak dihapus."); return true;
    }

    private boolean handleResetTrial(CommandSender sender, String[] args) {
        if (args.length != 3) { sender.sendMessage("§cUsage: /cdrjobsadmin resettrial <player> <job>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]); if (target == null) { sender.sendMessage("§cPlayer harus online."); return true; }
        JobType job = parseJob(sender, args[2]); if (job == null) return true;
        if (job == JobType.MINER) database.resetMinerTrial(target.getUniqueId()); else store.resetTrialData(target.getUniqueId(), job.name());
        sender.sendMessage("§aTrial progress " + job.name() + " direset untuk " + target.getName() + ".");
        if (job != JobType.MINER) sender.sendMessage("§7Catatan: counter aktivitas Job tersebut ikut direset karena menjadi sumber progress Trial.");
        return true;
    }

    private boolean handleResetCooldown(CommandSender sender, String[] args) {
        if (args.length != 3) { sender.sendMessage("§cUsage: /cdrjobsadmin resetcooldown <player> <job>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]); if (target == null) { sender.sendMessage("§cPlayer harus online."); return true; }
        JobType job = parseJob(sender, args[2]); if (job == null) return true;
        database.clearAbilityCooldowns(target.getUniqueId(), job);
        sender.sendMessage("§aCooldown " + job.name() + " direset untuk " + target.getName() + "."); return true;
    }

    private boolean handleEssence(CommandSender sender, String[] args) {
        if (args.length != 3) { sender.sendMessage("§cUsage: /cdrjobsadmin " + args[0] + " <player> <amount>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]); if (target == null) { sender.sendMessage("§cPlayer harus online."); return true; }
        try {
            int amount = Integer.parseInt(args[2]);
            if (args[0].equalsIgnoreCase("addessence")) {
                if (amount <= 0) { sender.sendMessage("§cAmount harus lebih besar dari 0."); return true; }
                database.addFateEssence(target.getUniqueId(), amount);
            } else {
                if (amount < 0) { sender.sendMessage("§cEssence tidak boleh negatif."); return true; }
                database.setFateEssence(target.getUniqueId(), amount);
            }
            sender.sendMessage("§aFate Essence " + target.getName() + ": " + database.getFateEssence(target.getUniqueId()));
        } catch (NumberFormatException e) { sender.sendMessage("§cAmount harus angka valid."); }
        return true;
    }

    private boolean handleExport(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 3) { sender.sendMessage("§cUsage: /cdrjobsadmin export <player> [file|console|both]"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]); if (target == null) { sender.sendMessage("§cPlayer harus online."); return true; }
        String mode = args.length == 3 ? args[2].toLowerCase(Locale.ROOT) : "file";
        if (!List.of("file", "console", "both").contains(mode)) { sender.sendMessage("§cMode: file, console, atau both."); return true; }
        String report = buildReport(target);
        try {
            if (mode.equals("file") || mode.equals("both")) {
                File dir = new File(plugin.getDataFolder(), "debug"); if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Could not create debug directory");
                String safe = target.getName().replaceAll("[^A-Za-z0-9_.-]", "_");
                File out = new File(dir, safe + "-" + target.getUniqueId() + ".txt");
                Files.writeString(out.toPath(), report, StandardCharsets.UTF_8);
                sender.sendMessage("§aDebug export: §f" + out.getAbsolutePath());
            }
            if (mode.equals("console") || mode.equals("both")) {
                for (String line : report.split("\\R")) plugin.getLogger().info("[EXPORT] " + line);
                sender.sendMessage("§aDebug report dikirim ke console.");
            }
        } catch (Exception e) {
            sender.sendMessage("§cExport gagal: " + e.getMessage());
            plugin.getLogger().warning("Debug export failed: " + e.getMessage());
        }
        return true;
    }

    private String buildReport(Player target) {
        UUID uuid = target.getUniqueId(); StringBuilder b = new StringBuilder();
        b.append("CdrJobs Debug Export\n").append("generated_at=").append(Instant.now()).append('\n').append("version=").append(plugin.getPluginMeta().getVersion()).append('\n').append("schema=").append(store.getSchemaVersion()).append('\n').append("player=").append(target.getName()).append('\n').append("uuid=").append(uuid).append('\n').append("fate_essence=").append(database.getFateEssence(uuid)).append("\n\n");
        for (JobType job : JobType.values()) {
            JobProgress p = database.getProgress(uuid, job);
            b.append('[').append(job.name()).append("]\nlevel=").append(p.level()).append("\nxp=").append(p.xp()).append("\nrank=").append(JobRanks.title(job, p.level())).append("\ncounters=").append(store.getCounters(uuid, job.name())).append("\nflags=").append(store.getFlags(uuid, job.name())).append("\n\n");
        }
        b.append("miner_trials=").append(database.getMinerTrialProgress(uuid)).append('\n');
        b.append("skills=").append(database.getSkillRanks(uuid)).append('\n');
        b.append("cooldowns=").append(database.getAbilityCooldowns(uuid)).append('\n');
        b.append("hunter_last_decision=").append(hunter.lastDebugReason(target)).append('\n');
        return b.toString();
    }

    private boolean handleProgressCommand(CommandSender sender, String[] args) {
        boolean addXp = args[0].equalsIgnoreCase("addxp");
        if (args.length != 3 && args.length != 4) { sender.sendMessage("§cUsage: /cdrjobsadmin " + args[0] + " <player> [job] <value>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]); if (target == null) { sender.sendMessage("§cPlayer harus online."); return true; }
        JobType job = JobType.MINER; String rawValue;
        if (args.length == 4) { job = parseJob(sender, args[2]); if (job == null) return true; rawValue = args[3]; } else rawValue = args[2];
        try {
            if (addXp) {
                long amount = Long.parseLong(rawValue); if (amount <= 0L) { sender.sendMessage("§cXP harus lebih besar dari 0."); return true; }
                progression.addXp(target, job, amount); sender.sendMessage("§aAdded " + amount + " " + job.name() + " XP to " + target.getName() + ".");
            } else {
                int level = Integer.parseInt(rawValue); int maxLevel = Math.max(1, plugin.getConfig().getInt("settings.max-level", 100));
                if (level < 1 || level > maxLevel) { sender.sendMessage("§cLevel harus berada di antara 1 dan " + maxLevel + "."); return true; }
                progression.setLevel(target, job, level); sender.sendMessage("§a" + job.name() + " level " + target.getName() + " diubah ke " + level + ".");
            }
        } catch (NumberFormatException e) { sender.sendMessage("§cNilai harus berupa angka yang valid."); }
        return true;
    }

    private Player onlineTarget(CommandSender sender, String[] args, int exactLength, String usage) {
        if (args.length != exactLength) { sender.sendMessage("§cUsage: " + usage); return null; }
        Player target = Bukkit.getPlayerExact(args[1]); if (target == null) sender.sendMessage("§cPlayer harus online."); return target;
    }

    private JobType parseJob(CommandSender sender, String raw) {
        try { return JobType.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { sender.sendMessage("§cJob tidak valid. Gunakan: miner, farmer, hunter, lumberjack, fisher."); return null; }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§3CdrJobs Admin Commands");
        sender.sendMessage("§bdiagnose, inspect, hunterdebug, reload, export");
        sender.sendMessage("§baddxp, setlevel, addessence, setessence");
        sender.sendMessage("§breset, resetjob, resettrial, resetcooldown");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> root = List.of("diagnose","inspect","hunterdebug","reload","export","addxp","setlevel","addessence","setessence","reset","resetjob","resettrial","resetcooldown");
        if (args.length == 1) return filter(root, args[0]);
        if (args.length == 2 && !List.of("diagnose","reload").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> names = new ArrayList<>(); Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName())); return filter(names, args[1]);
        }
        if (args.length == 3 && List.of("resetjob","resettrial","resetcooldown","addxp","setlevel").contains(args[0].toLowerCase(Locale.ROOT))) return filter(Arrays.stream(JobType.values()).map(j -> j.name().toLowerCase(Locale.ROOT)).toList(), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("export")) return filter(List.of("file","console","both"), args[2]);
        return List.of();
    }

    private List<String> filter(List<String> values, String input) { String lower = input.toLowerCase(Locale.ROOT); return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(lower)).toList(); }
}
