package store.cadera.cdrjobs;

import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import store.cadera.cdrjobs.api.CdrJobsAPI;
import store.cadera.cdrjobs.command.AdminCommand;
import store.cadera.cdrjobs.command.JobsCommand;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.data.RebirthStore;
import store.cadera.cdrjobs.gui.JobsMenu;
import store.cadera.cdrjobs.gui.RebirthMenu;
import store.cadera.cdrjobs.integration.VaultEconomyHook;
import store.cadera.cdrjobs.listener.*;
import store.cadera.cdrjobs.placeholder.CdrJobsExpansion;
import store.cadera.cdrjobs.service.*;
import store.cadera.cdrjobs.util.ConfigValidator;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class CdrJobsPlugin extends JavaPlugin {
    private FileConfiguration messages;
    private Database database;
    private ProfessionStore professionStore;
    private RebirthStore rebirthStore;
    private LeaderboardService leaderboardService;
    private LevelService levelService;
    private CdrJobsAPI api;
    private MinerListener minerListener;
    private FarmerListener farmerListener;
    private HunterListener hunterListener;
    private LumberjackListener lumberjackListener;
    private FisherListener fisherListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        saveResource("messages.yml", false);
        loadMessages();
        ConfigValidator.validate(this);

        try {
            database = new Database(getDataFolder());
            database.connect();
            professionStore = new ProfessionStore(getDataFolder());
            professionStore.connect();
            professionStore.setSchemaVersion(9);
            database.backfillFateMilestoneClaims(fateMilestones().keySet());
            leaderboardService = new LeaderboardService(this, getDataFolder());
            rebirthStore = new RebirthStore(getDataFolder());
            rebirthStore.connect();
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to initialize SQLite", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        levelService = new LevelService(this);
        ProgressionService progression = new ProgressionService(this, database, levelService);
        api = new CdrJobsAPI(database, progression);

        MinerTrialService minerTrials = new MinerTrialService(this, database);
        SkillService minerSkills = new SkillService(this, database, minerTrials);
        MinerAbilityService minerAbility = new MinerAbilityService(this, database);
        FarmerService farmer = new FarmerService(this, database, professionStore);
        HunterService hunter = new HunterService(this, database, professionStore);
        LumberjackService lumberjack = new LumberjackService(this, database, professionStore);
        FisherService fisher = new FisherService(this, database, professionStore);
        ProfileService profiles = new ProfileService(database, professionStore);
        VaultEconomyHook vault = new VaultEconomyHook(this);
        RebirthService rebirth = new RebirthService(this, database, rebirthStore, vault, profiles);

        JobsMenu menu = new JobsMenu(this, database, professionStore, levelService, minerTrials,
                farmer, hunter, lumberjack, fisher, profiles);
        RebirthMenu rebirthMenu = new RebirthMenu(this, rebirth);
        JobsCommand jobsCommand = new JobsCommand(this, database, menu, rebirthMenu, levelService, minerAbility,
                farmer, hunter, lumberjack, fisher, leaderboardService);
        AdminCommand adminCommand = new AdminCommand(this, database, professionStore, progression, hunter);
        registerCommand("cdrjobs", jobsCommand, jobsCommand);
        registerCommand("cdrjobsadmin", adminCommand, adminCommand);

        minerListener = new MinerListener(this, database, progression, minerSkills, levelService, minerTrials, minerAbility);
        farmerListener = new FarmerListener(this, database, professionStore, progression, farmer, levelService);
        hunterListener = new HunterListener(this, progression, hunter, levelService);
        lumberjackListener = new LumberjackListener(this, professionStore, progression, lumberjack, levelService);
        fisherListener = new FisherListener(this, progression, fisher, levelService);

        for (var listener : List.of(minerListener, farmerListener, hunterListener, lumberjackListener, fisherListener,
                new MenuListener(menu, minerSkills, farmer, hunter, lumberjack, fisher),
                new RebirthListener(this, rebirthMenu, rebirth))) {
            getServer().getPluginManager().registerEvents(listener, this);
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CdrJobsExpansion(this, database, professionStore, levelService, profiles).register();
        }
        getLogger().info("CdrJobs v" + getPluginMeta().getVersion() + " — RITE OF REBIRTH enabled.");
    }

    @Override
    public void onDisable() {
        api = null;
        try { if (rebirthStore != null) rebirthStore.close(); }
        catch (SQLException exception) { getLogger().warning(exception.getMessage()); }
        try { if (leaderboardService != null) leaderboardService.close(); }
        catch (SQLException exception) { getLogger().warning(exception.getMessage()); }
        try { if (professionStore != null) professionStore.close(); }
        catch (SQLException exception) { getLogger().warning(exception.getMessage()); }
        try { if (database != null) database.close(); }
        catch (SQLException exception) { getLogger().warning(exception.getMessage()); }
    }

    public CdrJobsAPI getApi() {
        if (api == null) throw new IllegalStateException("CdrJobs API is not available before plugin enable or after disable");
        return api;
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor,
                                 org.bukkit.command.TabCompleter completer) {
        PluginCommand command = getCommand(name);
        if (command == null) throw new IllegalStateException("Missing command " + name);
        command.setExecutor(executor);
        command.setTabCompleter(completer);
    }

    private void loadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(file);
        try (var stream = getResource("messages.yml")) {
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
                messages.setDefaults(defaults);
                messages.options().copyDefaults(true);
                messages.save(file);
            }
        } catch (Exception exception) {
            getLogger().log(Level.WARNING, "Could not merge messages", exception);
        }
    }

    public void reloadPluginFiles() {
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadMessages();
        ConfigValidator.validate(this);
        if (leaderboardService != null) leaderboardService.invalidateAll();
        if (minerListener != null) minerListener.refreshXpMap();
        if (farmerListener != null) farmerListener.refreshXpMap();
        if (hunterListener != null) hunterListener.refreshXpMap();
        if (lumberjackListener != null) lumberjackListener.refreshXpMap();
        if (fisherListener != null) fisherListener.refreshXpMap();
    }

    public String prefix() { return messages.getString("prefix", "&8[&bCdrJobs&8] &r"); }
    public String message(String path) { return messages.getString(path, path); }

    public Map<Integer, Integer> fateMilestones() {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        ConfigurationSection section = getConfig().getConfigurationSection("fate-essence-milestones");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try { result.put(Integer.parseInt(key), section.getInt(key)); }
                catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    public Map<Material, Integer> loadMinerXp() { return loadActivityXp("miner.xp"); }

    public Map<Material, Integer> loadActivityXp(String path) {
        Map<Material, Integer> result = new HashMap<>();
        ConfigurationSection section = getConfig().getConfigurationSection(path);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material != null) result.put(material, section.getInt(key));
            }
        }
        return result;
    }
}
