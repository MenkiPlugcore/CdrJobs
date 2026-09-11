package store.cadera.cdrjobs.model;
import org.bukkit.Material;
public enum FisherSkill {
 TIDEWHISPER("Tidewhisper",Material.COD,10,3,1,null,0,"+5% Fisher XP per rank."),
 SWIFT_CURRENT("Swift Current",Material.SALMON,25,3,1,TIDEWHISPER,2,"+5% catch XP per rank."),
 ABYSSAL_INSTINCT("Abyssal Instinct",Material.PRISMARINE_SHARD,25,3,1,TIDEWHISPER,2,"+8% Fisher XP in ocean biomes per rank."),
 OCEANS_CALL("Ocean's Call",Material.HEART_OF_THE_SEA,35,1,2,SWIFT_CURRENT,2,"Active tide blessing unlocked by Trial of the Tide."),
 OCEANS_FAVOR("Ocean's Favor",Material.NAUTILUS_SHELL,45,3,2,SWIFT_CURRENT,2,"4% chance per rank to double profession XP."),
 TREASURE_CALLER("Treasure Caller",Material.ENCHANTED_BOOK,45,3,2,ABYSSAL_INSTINCT,2,"+10% treasure catch XP per rank."),
 KING_OF_TIDES("King of the Tides",Material.CONDUIT,100,1,5,null,0,"Awakened path. Requires both branches and Trial of the Abyss.");
 private final String display;private final Material icon;private final int level,max,cost;private final FisherSkill pre;private final int preRank;private final String desc;
 FisherSkill(String d,Material i,int l,int m,int c,FisherSkill p,int pr,String ds){display=d;icon=i;level=l;max=m;cost=c;pre=p;preRank=pr;desc=ds;}public String key(){return "FISHER_"+name();}public String displayName(){return display;}public Material icon(){return icon;}public int requiredLevel(){return level;}public int maxRank(){return max;}public int essenceCost(){return cost;}public FisherSkill prerequisite(){return pre;}public int prerequisiteRank(){return preRank;}public String description(){return desc;}
}
