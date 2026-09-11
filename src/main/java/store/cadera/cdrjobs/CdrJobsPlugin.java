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
import store.cadera.cdrjobs.listener.*;
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
    private FileConfiguration messages; private Database database; private ProfessionStore professionStore; private LevelService levelService;
    private MinerListener minerListener; private FarmerListener farmerListener; private HunterListener hunterListener;
    @Override public void onEnable(){
        saveDefaultConfig();getConfig().options().copyDefaults(true);saveConfig();saveResource("messages.yml",false);loadMessages();
        try{database=new Database(getDataFolder());database.connect();professionStore=new ProfessionStore(getDataFolder());professionStore.connect();database.backfillFateMilestoneClaims(fateMilestones().keySet());}catch(Exception e){getLogger().log(Level.SEVERE,"Failed to initialize SQLite. CdrJobs cannot start.",e);getServer().getPluginManager().disablePlugin(this);return;}
        levelService=new LevelService(this);ProgressionService progression=new ProgressionService(this,database,levelService);MinerTrialService minerTrials=new MinerTrialService(this,database);SkillService minerSkills=new SkillService(this,database,minerTrials);MinerAbilityService minerAbilities=new MinerAbilityService(this,database);FarmerService farmer=new FarmerService(this,database,professionStore);HunterService hunter=new HunterService(this,database,professionStore);JobsMenu menu=new JobsMenu(this,database,professionStore,levelService,minerTrials,farmer,hunter);
        JobsCommand jobs=new JobsCommand(this,database,menu,levelService,minerAbilities,farmer,hunter);AdminCommand admin=new AdminCommand(this,database,professionStore,progression);registerCommand("cdrjobs",jobs,jobs);registerCommand("cdrjobsadmin",admin,admin);
        minerListener=new MinerListener(this,database,progression,minerSkills,levelService,minerTrials,minerAbilities);farmerListener=new FarmerListener(this,database,professionStore,progression,farmer,levelService);hunterListener=new HunterListener(this,progression,hunter,levelService);
        getServer().getPluginManager().registerEvents(minerListener,this);getServer().getPluginManager().registerEvents(farmerListener,this);getServer().getPluginManager().registerEvents(hunterListener,this);getServer().getPluginManager().registerEvents(new MenuListener(menu,minerSkills,farmer,hunter),this);
        if(getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")){new CdrJobsExpansion(this,database,levelService,farmer).register();getLogger().info("PlaceholderAPI detected. CdrJobs placeholders enabled.");}
        getLogger().info("CdrJobs v"+getPluginMeta().getVersion()+" — BLOOD MOON enabled.");getLogger().info("Choose Your Path, Shape Your Fate.");
    }
    @Override public void onDisable(){try{if(professionStore!=null)professionStore.close();}catch(SQLException e){getLogger().log(Level.WARNING,"Failed to close profession store.",e);}try{if(database!=null)database.close();}catch(SQLException e){getLogger().log(Level.WARNING,"Failed to close SQLite connection.",e);}}
    private void registerCommand(String n,org.bukkit.command.CommandExecutor e,org.bukkit.command.TabCompleter t){PluginCommand c=getCommand(n);if(c==null)throw new IllegalStateException("Command missing: "+n);c.setExecutor(e);c.setTabCompleter(t);}
    private void loadMessages(){File f=new File(getDataFolder(),"messages.yml");messages=YamlConfiguration.loadConfiguration(f);try(var s=getResource("messages.yml")){if(s!=null){YamlConfiguration d=YamlConfiguration.loadConfiguration(new InputStreamReader(s,StandardCharsets.UTF_8));messages.setDefaults(d);messages.options().copyDefaults(true);messages.save(f);}}catch(Exception e){getLogger().log(Level.WARNING,"Could not merge messages.yml defaults.",e);}}
    public void reloadPluginFiles(){reloadConfig();getConfig().options().copyDefaults(true);loadMessages();if(minerListener!=null)minerListener.refreshXpMap();if(farmerListener!=null)farmerListener.refreshXpMap();if(hunterListener!=null)hunterListener.refreshXpMap();}
    public String prefix(){return messages.getString("prefix","&8[&bCdrJobs&8] &r");}public String message(String p){return messages.getString(p,p);}
    public Map<Integer,Integer> fateMilestones(){Map<Integer,Integer>r=new LinkedHashMap<>();ConfigurationSection s=getConfig().getConfigurationSection("fate-essence-milestones");if(s!=null)for(String k:s.getKeys(false))try{r.put(Integer.parseInt(k),s.getInt(k));}catch(NumberFormatException ignored){}return r;}
    public Map<Material,Integer> loadMinerXp(){return loadActivityXp("miner.xp");}public Map<Material,Integer> loadActivityXp(String path){Map<Material,Integer>m=new HashMap<>();ConfigurationSection s=getConfig().getConfigurationSection(path);if(s!=null)for(String k:s.getKeys(false)){Material mat=Material.matchMaterial(k);if(mat!=null)m.put(mat,s.getInt(k));}return m;}
}
