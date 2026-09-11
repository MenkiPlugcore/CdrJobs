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
import store.cadera.cdrjobs.gui.JobsMenu;
import store.cadera.cdrjobs.listener.MenuListener;
import store.cadera.cdrjobs.listener.MinerListener;
import store.cadera.cdrjobs.placeholder.CdrJobsExpansion;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.ProgressionService;
import store.cadera.cdrjobs.service.SkillService;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public final class CdrJobsPlugin extends JavaPlugin {
    private FileConfiguration messages;
    private Database database;
    private LevelService levelService;
    private MinerListener minerListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        loadMessages();

        try {
            database = new Database(getDataFolder());
            database.connect();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize SQLite. CdrJobs cannot start.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        levelService = new LevelService(getConfig().getInt("settings.max-level", 100));
        ProgressionService progression = new ProgressionService(this, database, levelService);
        SkillService skills = new SkillService(this, database);
        JobsMenu menu = new JobsMenu(this, database, levelService);

        JobsCommand jobsCommand = new JobsCommand(this, database, menu, levelService);
        AdminCommand adminCommand = new AdminCommand(this, database, progression);
        registerCommand("cdrjobs", jobsCommand, jobsCommand);
        registerCommand("cdrjobsadmin", adminCommand, adminCommand);

        minerListener = new MinerListener(this, database, progression, skills, levelService);
        getServer().getPluginManager().registerEvents(minerListener, this);
        getServer().getPluginManager().registerEvents(new MenuListener(menu, skills), this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CdrJobsExpansion(this, database, levelService).register();
            getLogger().info("PlaceholderAPI detected. CdrJobs placeholders enabled.");
        }

        getLogger().info("CdrJobs v" + getPluginMeta().getVersion() + " — THE RUNEBORN enabled.");
        getLogger().info("Choose Your Path, Shape Your Fate.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            try {
                database.close();
            } catch (SQLException e) {
                getLogger().log(Level.WARNING, "Failed to close SQLite connection cleanly.", e);
            }
        }
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter completer) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) throw new IllegalStateException("Command missing from plugin.yml: " + name);
        cmd.setExecutor(executor);
        cmd.setTabCompleter(completer);
    }

    private void loadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public void reloadPluginFiles() {
        reloadConfig();
        loadMessages();
        if (minerListener != null) minerListener.refreshXpMap();
    }

    public String prefix() {
        return messages.getString("prefix", "&8[&bCdrJobs&8] &r");
    }

    public String message(String path) {
        return messages.getString(path, path);
    }

    public Map<Integer, Integer> fateMilestones() {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        ConfigurationSection section = getConfig().getConfigurationSection("fate-essence-milestones");
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            try {
                result.put(Integer.parseInt(key), section.getInt(key));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public Map<Material, Integer> loadMinerXp() {
        Map<Material, Integer> map = new HashMap<>();
        ConfigurationSection section = getConfig().getConfigurationSection("miner.xp");
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material != null) map.put(material, section.getInt(key));
        }
        return map;
    }
}
