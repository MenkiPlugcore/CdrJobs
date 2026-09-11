package store.cadera.cdrjobs.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.service.HunterService;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.ProgressionService;

import java.util.EnumMap;
import java.util.Map;

public final class HunterListener implements Listener {
    private final CdrJobsPlugin plugin;private final ProgressionService progression;private final HunterService hunter;private final LevelService levels;private final NamespacedKey originKey;private Map<EntityType,Integer> xp=new EnumMap<>(EntityType.class);
    public HunterListener(CdrJobsPlugin plugin,ProgressionService progression,HunterService hunter,LevelService levels){this.plugin=plugin;this.progression=progression;this.hunter=hunter;this.levels=levels;this.originKey=new NamespacedKey(plugin,"hunter_spawn_origin");refreshXpMap();}
    public void refreshXpMap(){Map<EntityType,Integer> m=new EnumMap<>(EntityType.class);ConfigurationSection s=plugin.getConfig().getConfigurationSection("hunter.xp");if(s!=null)for(String key:s.getKeys(false))try{m.put(EntityType.valueOf(key.toUpperCase()),s.getInt(key));}catch(IllegalArgumentException ignored){}xp=m;}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onSpawn(CreatureSpawnEvent e){e.getEntity().getPersistentDataContainer().set(originKey,PersistentDataType.STRING,e.getSpawnReason().name());}
    @EventHandler(priority=EventPriority.MONITOR) public void onDeath(EntityDeathEvent e){if(!plugin.getConfig().getBoolean("hunter.enabled",true))return;Player p=e.getEntity().getKiller();if(p==null)return;Integer base=xp.get(e.getEntityType());if(base==null)return;String origin=e.getEntity().getPersistentDataContainer().get(originKey,PersistentDataType.STRING);if(origin!=null){if(plugin.getConfig().getBoolean("hunter.anti-exploit.exclude-spawner",true)&&origin.equals("SPAWNER"))return;if(plugin.getConfig().getBoolean("hunter.anti-exploit.exclude-breeding",true)&&origin.equals("BREEDING"))return;}
        boolean dangerous=hunter.dangerous(e.getEntityType());long time=p.getWorld().getTime();boolean night=time>=13000&&time<=23000;hunter.recordKill(p,dangerous,night);double global=Math.max(0,plugin.getConfig().getDouble("hunter.xp-multiplier",1.0));long amount=hunter.applyXp(p,Math.max(1L,Math.round(base*global)),dangerous);JobProgress progress=progression.addXp(p,JobType.HUNTER,amount);if(plugin.getConfig().getBoolean("settings.actionbar-xp",true)){long req=levels.xpRequiredForNextLevel(progress.level());p.sendActionBar(Component.text(progress.level()>=levels.maxLevel()?"✦ Bloodfang Stalker Lv."+levels.maxLevel()+" • MAX":"✦ Bloodfang +"+amount+" XP • Lv."+progress.level()+" • "+progress.xp()+"/"+req));}}
}
