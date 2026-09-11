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
import store.cadera.cdrjobs.gui.RebirthMenu;
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
    private final RebirthMenu rebirthMenu;
    private final LevelService levels;
    private final MinerAbilityService miner;
    private final FarmerService farmer;
    private final HunterService hunter;
    private final LumberjackService lumber;
    private final FisherService fisher;
    private final LeaderboardService leaderboards;
    private final MasteryService mastery;
    private final ContractService contracts;
    private final FateResonanceService resonance;

    public JobsCommand(CdrJobsPlugin plugin, Database db, JobsMenu menu, RebirthMenu rebirthMenu,
                       LevelService levels, MinerAbilityService miner, FarmerService farmer,
                       HunterService hunter, LumberjackService lumber, FisherService fisher,
                       LeaderboardService leaderboards, MasteryService mastery, ContractService contracts,
                       FateResonanceService resonance) {
        this.plugin = plugin;
        this.db = db;
        this.menu = menu;
        this.rebirthMenu = rebirthMenu;
        this.levels = levels;
        this.miner = miner;
        this.farmer = farmer;
        this.hunter = hunter;
        this.lumber = lumber;
        this.fisher = fisher;
        this.leaderboards = leaderboards;
        this.mastery = mastery;
        this.contracts = contracts;
        this.resonance = resonance;
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
            case "mastery", "prestige" -> showMastery(player, args);
            case "contracts", "contract" -> showContracts(player, args);
            case "resonance", "fateresonance" -> showResonance(player, args);
            case "rebirth", "respec" -> openRebirth(player, args);
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

    private void showResonance(Player player, String[] args) {
        if (!resonance.enabled()) {
            player.sendMessage("§cFate Resonance sedang dinonaktifkan.");
            return;
        }
        if (args.length == 1) {
            player.sendMessage("§d✦ Fate Resonance §8— §7cross-profession synergy");
            for (FateResonanceService.State state : resonance.states(player.getUniqueId())) {
                FateResonanceService.Definition def = state.definition();
                if (state.unlocked()) {
                    player.sendMessage("§7- §d" + def.name() + " §8— §f" + resonance.display(state));
                } else {
                    player.sendMessage("§7- §8" + def.name() + " §7— " + def.first().displayName() + " "
                            + state.firstLevel() + "/" + def.minLevel() + " + " + def.second().displayName() + " "
                            + state.secondLevel() + "/" + def.minLevel());
                }
            }
            player.sendMessage("§7Unlocked: §f" + resonance.unlockedCount(player.getUniqueId())
                    + " §8• §7Harmonized: §6" + resonance.harmonizedCount(player.getUniqueId())
                    + " §8• §7Score: §d" + resonance.score(player.getUniqueId()));
            return;
        }
        FateResonanceService.Definition def = resonance.definition(args[1]).orElse(null);
        if (def == null) {
            player.sendMessage("§cResonance tidak ditemukan. Gunakan /cdrjobs resonance untuk melihat daftar.");
            return;
        }
        FateResonanceService.State state = resonance.state(player.getUniqueId(), def);
        player.sendMessage("§d✦ " + def.name());
        player.sendMessage("§7Path: §f" + def.first().displayName() + " + " + def.second().displayName());
        player.sendMessage("§7Unlock requirement: §fLv." + def.minLevel() + " pada kedua profession");
        player.sendMessage("§7Current: §f" + state.firstLevel() + " / " + state.secondLevel());
        player.sendMessage("§7Status: §f" + resonance.display(state));
        player.sendMessage("§7Harmonized requirement: §6Mastery " + mastery.roman(def.harmonizedMasteryTier()) + " §7pada kedua profession");
        player.sendMessage("§7Mastery current: §f" + mastery.roman(state.firstMastery()) + " / " + mastery.roman(state.secondMastery()));
    }

    private void showContracts(Player player, String[] args) {
        if (!contracts.enabled()) {
            player.sendMessage("§cProfession Contracts sedang dinonaktifkan.");
            return;
        }
        if (args.length == 1) {
            player.sendMessage("§6✦ Profession Contracts");
            contracts.lines(player.getUniqueId()).forEach(player::sendMessage);
            player.sendMessage("§7Claim: §f/cdrjobs contracts claim <daily|weekly>");
            player.sendMessage("§7Reroll: §f/cdrjobs contracts reroll <daily|weekly>");
            return;
        }
        if (args.length != 3) {
            player.sendMessage("§cUsage: /cdrjobs contracts <claim|reroll> <daily|weekly>");
            return;
        }
        ContractService.Cadence cadence = ContractService.Cadence.parse(args[2]);
        if (cadence == null) {
            player.sendMessage("§cCadence tidak valid. Gunakan daily atau weekly.");
            return;
        }
        if (args[1].equalsIgnoreCase("claim")) {
            ContractService.ClaimResult result = contracts.claim(player, cadence);
            switch (result) {
                case SUCCESS -> { }
                case INCOMPLETE -> player.sendMessage("§eContract belum selesai.");
                case ALREADY_CLAIMED -> player.sendMessage("§eReward contract cycle ini sudah di-claim.");
                case DISABLED -> player.sendMessage("§cProfession Contracts sedang dinonaktifkan.");
                case REWARD_ERROR -> player.sendMessage("§cReward gagal dikirim. Hubungi admin dan cek console.");
            }
            return;
        }
        if (args[1].equalsIgnoreCase("reroll")) {
            ContractService.RerollResult result = contracts.reroll(player, cadence);
            switch (result) {
                case SUCCESS -> { }
                case LOCKED -> player.sendMessage("§eContract yang selesai/claimed tidak bisa direroll.");
                case NO_REROLLS -> player.sendMessage("§eJatah reroll cycle ini sudah habis.");
                case NO_ALTERNATIVE -> player.sendMessage("§eTidak ada contract alternatif untuk cadence ini.");
                case DISABLED -> player.sendMessage("§cProfession Contracts sedang dinonaktifkan.");
            }
            return;
        }
        player.sendMessage("§cUsage: /cdrjobs contracts <claim|reroll> <daily|weekly>");
    }

    private void showMastery(Player player, String[] args) {
        if (!mastery.enabled()) {
            player.sendMessage("§cProfession Mastery sedang dinonaktifkan.");
            return;
        }
        if (args.length == 1) {
            player.sendMessage("§6✦ Profession Mastery §8— §7XP setelah Lv." + levels.maxLevel());
            for (JobType job : JobType.values()) {
                JobProgress progress = db.getProgress(player.getUniqueId(), job);
                MasteryService.State state = mastery.state(player.getUniqueId(), job);
                if (progress.level() < levels.maxLevel() && state.tier() == 0) {
                    player.sendMessage("§7- §b" + job.displayName() + " §8— §7Locked until Lv." + levels.maxLevel());
                } else {
                    player.sendMessage(masteryLine(job, state));
                }
            }
            player.sendMessage("§7Total Mastery tiers: §f" + mastery.totalMasteryTiers(player.getUniqueId())
                    + " §8• §7Total Mastery XP: §f" + mastery.totalMasteryXp(player.getUniqueId()));
            return;
        }
        JobType job = parseJob(args[1]);
        if (job == null) {
            player.sendMessage("§cJob tidak valid. Gunakan miner, farmer, hunter, lumberjack, atau fisher.");
            return;
        }
        JobProgress progress = db.getProgress(player.getUniqueId(), job);
        MasteryService.State state = mastery.state(player.getUniqueId(), job);
        player.sendMessage("§6✦ " + job.displayName() + " Mastery");
        player.sendMessage("§7Profession level: §f" + progress.level() + "§7/" + levels.maxLevel());
        player.sendMessage("§7Tier: §f" + (state.tier() == 0 ? "Unmastered" : "Mastery " + mastery.roman(state.tier())));
        player.sendMessage("§7Prestige title: §f" + mastery.display(job, state));
        player.sendMessage("§7Mastery XP: §f" + (state.maxed() ? "MAX" : state.xp() + "/" + state.requiredXp()));
        player.sendMessage("§7Total Mastery XP: §f" + state.totalXp());
    }

    private String masteryLine(JobType job, MasteryService.State state) {
        String tier = state.tier() == 0 ? "Unmastered" : "Mastery " + mastery.roman(state.tier());
        String xp = state.maxed() ? "MAX" : state.xp() + "/" + state.requiredXp();
        return "§7- §b" + job.displayName() + " §8— §6" + tier + " §8• §f" + xp + " §8• §7" + state.title();
    }

    private void openRebirth(Player player, String[] args) {
        if (!player.hasPermission("cdrjobs.rebirth")) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("no-permission")));
            return;
        }
        if (args.length == 1) {
            rebirthMenu.openSelect(player);
            return;
        }
        JobType job = parseJob(args[1]);
        if (job == null) {
            player.sendMessage("§cJob tidak valid. Gunakan miner, farmer, hunter, lumberjack, atau fisher.");
            return;
        }
        rebirthMenu.openConfirm(player, job);
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
            viewer.sendMessage("§7/cdrjobs top total | pvp | mastery-total");
            viewer.sendMessage("§7/cdrjobs top activity <job>");
            viewer.sendMessage("§7/cdrjobs top mastery <job>");
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
        if (type.equals("mastery-total") || type.equals("masterytotal") || type.equals("prestige")) {
            sendMasteryTotalRows(viewer, leaderboards.masteryTotal());
            return;
        }
        if (type.equals("mastery")) {
            if (args.length < 3) {
                viewer.sendMessage("§cUsage: /cdrjobs top mastery <job>");
                return;
            }
            JobType job = parseJob(args[2]);
            if (job == null) {
                viewer.sendMessage("§cJob tidak valid.");
                return;
            }
            sendMasteryRows(viewer, job, leaderboards.mastery(job));
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
            viewer.sendMessage("§cLeaderboard tidak valid.");
            return;
        }
        sendRows(viewer, job.displayName() + " Profession", leaderboards.profession(job), RowMode.PROFESSION);
    }

    private void sendMasteryRows(Player viewer, JobType job, List<LeaderboardService.LeaderboardRow> rows) {
        viewer.sendMessage("§6✦ CdrJobs Top — §f" + job.displayName() + " Mastery");
        if (rows.isEmpty()) {
            viewer.sendMessage("§7Belum ada data Mastery.");
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardService.LeaderboardRow row = rows.get(i);
            MasteryService.State state = mastery.stateFromTotal(row.value());
            viewer.sendMessage("§6#" + (i + 1) + " §f" + row.name() + " §8— §6Mastery "
                    + mastery.roman(state.tier()) + " §8• §7" + state.title() + " §8• §f" + row.value() + " XP");
        }
    }

    private void sendMasteryTotalRows(Player viewer, List<LeaderboardService.LeaderboardRow> rows) {
        viewer.sendMessage("§6✦ CdrJobs Top — §fTotal Prestige");
        if (rows.isEmpty()) {
            viewer.sendMessage("§7Belum ada data Mastery.");
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardService.LeaderboardRow row = rows.get(i);
            viewer.sendMessage("§6#" + (i + 1) + " §f" + row.name() + " §8— §6"
                    + mastery.totalMasteryTiers(row.uuid()) + " tiers §8• §f" + row.value() + " Mastery XP");
        }
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
            MasteryService.State state = mastery.state(target.getUniqueId(), job);
            String masteryText = state.tier() > 0 ? " §6| M." + mastery.roman(state.tier()) : "";
            viewer.sendMessage("§b" + job.displayName() + " §7• " + JobRanks.title(job, progress.level())
                    + " §f| Lv." + progress.level() + " | "
                    + (progress.level() >= levels.maxLevel() ? "MAX" : progress.xp() + "/" + levels.xpRequiredForNextLevel(progress.level()))
                    + masteryText);
        }
        viewer.sendMessage("§dFate Essence: " + db.getFateEssence(target.getUniqueId()));
        viewer.sendMessage("§6Total Mastery: " + mastery.totalMasteryTiers(target.getUniqueId()) + " tiers");
        viewer.sendMessage("§dFate Resonance: " + resonance.unlockedCount(target.getUniqueId()) + " unlocked | "
                + resonance.harmonizedCount(target.getUniqueId()) + " harmonized | score " + resonance.score(target.getUniqueId()));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return List.of("profile", "top", "mastery", "contracts", "resonance", "rebirth", "stats", "miner", "farmer", "hunter", "lumberjack", "fisher", "trials", "ability");
        if (args.length == 2 && (args[0].equalsIgnoreCase("trials") || args[0].equalsIgnoreCase("ability")
                || args[0].equalsIgnoreCase("rebirth") || args[0].equalsIgnoreCase("respec")
                || args[0].equalsIgnoreCase("mastery") || args[0].equalsIgnoreCase("prestige"))) {
            return List.of("miner", "farmer", "hunter", "lumberjack", "fisher");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("resonance") || args[0].equalsIgnoreCase("fateresonance"))) {
            return resonance.definitions().stream().map(FateResonanceService.Definition::id).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("contracts") || args[0].equalsIgnoreCase("contract"))) {
            return List.of("claim", "reroll");
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("contracts") || args[0].equalsIgnoreCase("contract"))
                && (args[1].equalsIgnoreCase("claim") || args[1].equalsIgnoreCase("reroll"))) {
            return List.of("daily", "weekly");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return List.of("total", "pvp", "activity", "mastery", "mastery-total", "miner", "farmer", "hunter", "lumberjack", "fisher");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("top")
                && (args[1].equalsIgnoreCase("activity") || args[1].equalsIgnoreCase("mastery"))) {
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
