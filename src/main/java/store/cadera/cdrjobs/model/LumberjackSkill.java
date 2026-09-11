package store.cadera.cdrjobs.model;
import org.bukkit.Material;
public enum LumberjackSkill {
 TIMBERBORN("Timberborn",Material.OAK_LOG,10,3,1,null,0,"+5% Lumberjack XP per rank."),
 WHISPERING_AXE("Whispering Axe",Material.IRON_AXE,25,3,1,TIMBERBORN,2,"+5% woodcutting XP per rank."),
 IRONBARK("Ironbark",Material.IRON_BLOCK,25,1,2,TIMBERBORN,2,"Chopping streaks grant brief Resistance."),
 GROVE_RHYTHM("Grove Rhythm",Material.AMETHYST_SHARD,35,1,2,WHISPERING_AXE,2,"Active forest rhythm unlocked by Trial of Timber."),
 FORESTS_FAVOR("Forest's Favor",Material.OAK_LEAVES,45,3,2,WHISPERING_AXE,2,"4% chance per rank to double profession XP."),
 SPIRITWOOD("Spiritwood",Material.MANGROVE_LOG,45,3,2,IRONBARK,1,"+6% natural-log XP per rank."),
 OATH_YGGDRASIL("Oath of Yggdrasil",Material.TOTEM_OF_UNDYING,100,1,5,null,0,"Awakened path. Requires both branches and Trial of the Ancient Grove.");
 private final String display;private final Material icon;private final int level,max,cost;private final LumberjackSkill pre;private final int preRank;private final String desc;
 LumberjackSkill(String d,Material i,int l,int m,int c,LumberjackSkill p,int pr,String ds){display=d;icon=i;level=l;max=m;cost=c;pre=p;preRank=pr;desc=ds;}public String key(){return "LUMBERJACK_"+name();}public String displayName(){return display;}public Material icon(){return icon;}public int requiredLevel(){return level;}public int maxRank(){return max;}public int essenceCost(){return cost;}public LumberjackSkill prerequisite(){return pre;}public int prerequisiteRank(){return preRank;}public String description(){return desc;}
}
