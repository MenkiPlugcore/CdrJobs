package store.cadera.cdrjobs.model;

import org.bukkit.Material;

public enum HunterSkill {
    PREDATORS_INSTINCT("Predator's Instinct", Material.SPIDER_EYE, 10, 3, 1, null, 0, "+5% Hunter XP per rank."),
    BLOODTRAIL("Bloodtrail", Material.REDSTONE, 25, 3, 1, PREDATORS_INSTINCT, 2, "+5% Hunter XP per rank."),
    MOONFANG("Moonfang", Material.ENDER_PEARL, 25, 3, 1, PREDATORS_INSTINCT, 2, "+8% Hunter XP at night per rank."),
    CRIMSON_HUNT("Crimson Hunt", Material.NETHERITE_SWORD, 35, 1, 2, BLOODTRAIL, 2, "Active hunting trance unlocked by Trial of Fang."),
    SOULMARK("Soulmark", Material.ECHO_SHARD, 45, 3, 2, MOONFANG, 2, "4% chance per rank to double profession XP."),
    WARDENS_OATH("Warden's Oath", Material.SCULK_CATALYST, 45, 3, 2, BLOODTRAIL, 2, "+12% XP against dangerous prey per rank."),
    APEX_PREDATOR("Apex Predator", Material.DRAGON_HEAD, 100, 1, 5, null, 0, "Awakened path. Requires both branches and Trial of the Crimson Moon.");

    private final String displayName; private final Material icon; private final int requiredLevel,maxRank,essenceCost; private final HunterSkill prerequisite; private final int prerequisiteRank; private final String description;
    HunterSkill(String displayName,Material icon,int requiredLevel,int maxRank,int essenceCost,HunterSkill prerequisite,int prerequisiteRank,String description){this.displayName=displayName;this.icon=icon;this.requiredLevel=requiredLevel;this.maxRank=maxRank;this.essenceCost=essenceCost;this.prerequisite=prerequisite;this.prerequisiteRank=prerequisiteRank;this.description=description;}
    public String key(){return "HUNTER_"+name();} public String displayName(){return displayName;} public Material icon(){return icon;} public int requiredLevel(){return requiredLevel;} public int maxRank(){return maxRank;} public int essenceCost(){return essenceCost;} public HunterSkill prerequisite(){return prerequisite;} public int prerequisiteRank(){return prerequisiteRank;} public String description(){return description;}
}
