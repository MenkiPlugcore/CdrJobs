package store.cadera.cdrjobs.service;

import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.HunterSkill;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.util.Colors;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class HunterService {
    public static final String JOB="HUNTER"; public static final String CRIMSON_HUNT="hunter_crimson_hunt";
    private static final Set<EntityType> DANGEROUS=EnumSet.of(EntityType.WARDEN,EntityType.WITHER,EntityType.ELDER_GUARDIAN,EntityType.RAVAGER,EntityType.EVOKER);
    private final CdrJobsPlugin plugin; private final Database database; private final ProfessionStore store; private final Map<UUID,Long> activeUntil=new ConcurrentHashMap<>();
    public HunterService(CdrJobsPlugin plugin,Database database,ProfessionStore store){this.plugin=plugin;this.database=database;this.store=store;}
    public int rank(Player p,HunterSkill s){return store.getSkillRank(p.getUniqueId(),s.key());}
    public boolean dangerous(EntityType type){return DANGEROUS.contains(type);}
    public boolean tryUpgrade(Player p,HunterSkill s){int r=rank(p,s);if(r>=s.maxRank()){p.sendMessage(Colors.color(plugin.prefix()+plugin.message("max-skill")));return false;}int lv=database.getProgress(p.getUniqueId(),JobType.HUNTER).level();if(lv<s.requiredLevel()){p.sendMessage(Colors.color(plugin.prefix()+plugin.message("skill-requirement")));return false;}if(s.prerequisite()!=null&&rank(p,s.prerequisite())<s.prerequisiteRank()){p.sendMessage(Colors.color(plugin.prefix()+plugin.message("skill-requirement")));return false;}if(s==HunterSkill.CRIMSON_HUNT&&!fangComplete(p)){p.sendMessage(Colors.color(plugin.prefix()+plugin.message("trial-required").replace("%trial%","Trial of Fang")));return false;}if(s==HunterSkill.APEX_PREDATOR&&(rank(p,HunterSkill.SOULMARK)<3||rank(p,HunterSkill.WARDENS_OATH)<3||!moonComplete(p))){p.sendMessage(Colors.color(plugin.prefix()+plugin.message("skill-requirement")));return false;}if(database.getFateEssence(p.getUniqueId())<s.essenceCost()){p.sendMessage(Colors.color(plugin.prefix()+plugin.message("not-enough-essence")));return false;}database.addFateEssence(p.getUniqueId(),-s.essenceCost());store.setSkillRank(p.getUniqueId(),s.key(),r+1);p.sendMessage(Colors.color(plugin.prefix()+plugin.message("skill-unlocked").replace("%skill%",s.displayName()).replace("%rank%",String.valueOf(r+1))));return true;}
    public long applyXp(Player p,long base,boolean dangerous){double m=1.0+rank(p,HunterSkill.PREDATORS_INSTINCT)*0.05+rank(p,HunterSkill.BLOODTRAIL)*0.05;long time=p.getWorld().getTime();if(time>=13000&&time<=23000)m+=rank(p,HunterSkill.MOONFANG)*0.08;if(dangerous)m+=rank(p,HunterSkill.WARDENS_OATH)*0.12;if(rank(p,HunterSkill.APEX_PREDATOR)>0)m+=0.20;long xp=Math.max(1L,Math.round(base*m));int soul=rank(p,HunterSkill.SOULMARK);if(soul>0&&Math.random()<soul*0.04)xp*=2;Long until=activeUntil.get(p.getUniqueId());if(until!=null){if(until>System.currentTimeMillis()){double bonus=Math.max(0,plugin.getConfig().getDouble("hunter.abilities.crimson-hunt.xp-bonus-percent",25.0));xp=Math.max(1L,Math.round(xp*(1.0+bonus/100.0)));}else activeUntil.remove(p.getUniqueId());}return xp;}
    public void recordKill(Player p,boolean dangerous,boolean night){long kills=store.incrementCounter(p.getUniqueId(),JOB,"kills",1);if(night)store.incrementCounter(p.getUniqueId(),JOB,"night_kills",1);if(dangerous)store.incrementCounter(p.getUniqueId(),JOB,"dangerous_kills",1);if(!fangComplete(p)&&kills>=fangTarget()){store.setFlag(p.getUniqueId(),JOB,"trial_fang",true);p.sendMessage(Colors.color(plugin.prefix()+plugin.message("trial-fang-complete")));}if(!moonComplete(p)&&kills>=moonKillTarget()&&nightKills(p)>=moonNightTarget()&&dangerousKills(p)>=moonDangerTarget()){store.setFlag(p.getUniqueId(),JOB,"trial_moon",true);p.sendMessage(Colors.color(plugin.prefix()+plugin.message("trial-moon-complete")));}}
    public boolean activate(Player p){if(rank(p,HunterSkill.CRIMSON_HUNT)<=0){p.sendMessage(Colors.color(plugin.prefix()+plugin.message("hunter-ability-locked")));return false;}long now=System.currentTimeMillis(),ready=store.getAbilityReadyAt(p.getUniqueId(),CRIMSON_HUNT);if(ready>now){p.sendMessage(Colors.color(plugin.prefix()+plugin.message("hunter-ability-cooldown").replace("%seconds%",String.valueOf((ready-now+999)/1000))));return false;}int duration=Math.max(1,plugin.getConfig().getInt("hunter.abilities.crimson-hunt.duration-seconds",20));int cooldown=Math.max(duration,plugin.getConfig().getInt("hunter.abilities.crimson-hunt.cooldown-seconds",300));activeUntil.put(p.getUniqueId(),now+duration*1000L);store.setAbilityReadyAt(p.getUniqueId(),CRIMSON_HUNT,now+cooldown*1000L);p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,duration*20,0,true,false,true));p.playSound(p.getLocation(), Sound.ENTITY_WOLF_GROWL,0.8f,0.8f);p.sendMessage(Colors.color(plugin.prefix()+plugin.message("crimson-hunt-activated").replace("%duration%",String.valueOf(duration))));return true;}
    public long kills(Player p){return store.getCounter(p.getUniqueId(),JOB,"kills");}public long nightKills(Player p){return store.getCounter(p.getUniqueId(),JOB,"night_kills");}public long dangerousKills(Player p){return store.getCounter(p.getUniqueId(),JOB,"dangerous_kills");}public boolean fangComplete(Player p){return store.getFlag(p.getUniqueId(),JOB,"trial_fang");}public boolean moonComplete(Player p){return store.getFlag(p.getUniqueId(),JOB,"trial_moon");}
    public int fangTarget(){return Math.max(1,plugin.getConfig().getInt("hunter.trials.trial-of-fang.kills",300));}public int moonKillTarget(){return Math.max(1,plugin.getConfig().getInt("hunter.trials.trial-of-crimson-moon.kills",1200));}public int moonNightTarget(){return Math.max(1,plugin.getConfig().getInt("hunter.trials.trial-of-crimson-moon.night-kills",250));}public int moonDangerTarget(){return Math.max(1,plugin.getConfig().getInt("hunter.trials.trial-of-crimson-moon.dangerous-kills",10));}
    public long cooldownSeconds(UUID uuid){long r=store.getAbilityReadyAt(uuid,CRIMSON_HUNT)-System.currentTimeMillis();return r<=0?0:(r+999)/1000;}
}
