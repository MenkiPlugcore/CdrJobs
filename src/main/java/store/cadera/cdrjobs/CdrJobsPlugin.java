package store.cadera.cdrjobs;

import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import store.cadera.cdrjobs.command.AdminCommand;
import store.cadera.cdrjobs.command.JobsCommand;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.gui.JobsMenu;
import store.cadera.cdrjobs.listener.FarmerListener;
import store.cadera.cdrjobs.listener.MenuListener;
import store.cadera.cdrjobs.listener.MinerListener;
import store.cadera.cdrjobs.placeholder.CdrJobsExpansion;
import store.cadera.cdrjobs.service.*;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public final class CdrJobsPlugin extends JavaPlugin {
    private FileConfiguration messages;
    private Database database;
    private ProfessionStore professionStore;
    private LevelService levelService;
    private MinerListener minerListener;
    private FarmerListener farmerListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        saveResource("messages.yml", false);
        loadMessages();

        try {
            database = new Database(getDataFolder());
            database.connect();
            professionStore = new ProfessionStore(getDataFolder());
            professionStore.connect();
            database.backfillFateMilestoneClaims(fateMilestones().keySet());
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize SQLite. CdrJobs cannot start.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        levelService = new LevelService(this);
        ProgressionService progression = new ProgressionService(this, database, levelService);
        MinerTrialService minerTrials = new MinerTrialService(this, database);
        SkillService minerSkills = new SkillService(this, database, minerTrials);
        MinerAbilityService minerAbilities = new MinerAbilityService(this, database);
        FarmerService farmer = new FarmerService(this, database, professionStore);
        JobsMenu menu = new JobsMenu(this, database, professionStore, levelService, minerTrials, farmer);

        JobsCommand jobsCommand = new JobsCommand(this, database, menu, levelService, minerAbilities, farmer);
        AdminCommand adminCommand = new AdminCommand(this, database, professionStore, progression);
        registerCommand("cdrjobs", jobsCommand, jobsCommand);
        registerCommand("cdrjobsadmin", adminCommand, adminCommand);

        minerListener = new MinerListener(this, database, progression, minerSkills, levelService, minerTrials, minerAbilities);
        farmerListener = new FarmerListener(this, database, professionStore, progression, farmer, levelService);
        getServer().getPluginManager().registerEvents(minerListener, this);
        getServer().getPluginManager().registerEvents(farmerListener, this);
        getServer().getPluginManager().registerEvents(new MenuListener(menu, minerSkills, farmer), this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CdrJobsExpansion(this, database, levelService, farmer).register();
            getLogger().info("PlaceholderAPI detected. CdrJobs placeholders enabled.");
        }

        getLogger().info("CdrJobs v" + getPluginMeta().getVersion() + " — VERDANT AWAKENING enabled.");
        getLogger().info("Choose Your Path, Shape Your Fate.");
    }

    @Override
    public void onDisable() {
        try { if (professionStore != null) professionStore.close(); } catch (SQLException e) { getLogger().log(Level.WARNING, "Failed to close profession store.", e); }
        try { if (database != null) database.close(); } catch (SQLException e) { getLogger().log(Level.WARNING, "Failed to close SQLite connection cleanly.", e); }
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter completer) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) throw new IllegalStateException("Command missing from plugin.yml: " + name);
        cmd.setExecutor(executor); cmd.setTabCompleter(completer);
    }

    private void loadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(file);
        try (var stream = getResource("messages.yml")) {
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
                messages.setDefaults(defaults); messages.options().copyDefaults(true); messages.save(file);
            }
        } catch (Exception e) { getLogger().log(Level.WARNING, "Could not merge messages.yml defaults.", e); }
    }

    public void reloadPluginFiles() {
        reloadConfig(); getConfig().options().copyDefaults(true); loadMessages();
        if (minerListener != null) minerListener.refreshXpMap();
        if (farmerListener != null) farmerListener.refreshXpMap();
    }

    public String prefix() { return messages.getString("prefix", "&8[&bCdrJobs&8] &r"); }
    public String message(String path) { return messages.getString(path, path); }

    public Map<Integer, Integer> fateMilestones() {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        ConfigurationSection section = getConfig().getConfigurationSection("fate-essence-milestones");
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            try { result.put(Integer.parseInt(key), section.getInt(key)); } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    public Map<Material, Integer> loadMinerXp() { return loadActivityXp("miner.xp"); }

    public Map<Material, Integer> loadActivityXp(String path) {
        Map<Material, Integer> map = new HashMap<>();
        ConfigurationSection section = getConfig().getConfigurationSection(path);
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material != null) map.put(material, section.getInt(key));
        }
        return map;
    }
}
