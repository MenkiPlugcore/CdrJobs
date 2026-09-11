package store.cadera.cdrjobs.model;

import org.bukkit.Material;

public enum MinerSkill {
    STONEWHISPER("Stonewhisper", Material.STONE, 10, 3, 1, null, 0,
            "+5% Miner XP per rank."),
    RUNEBREAKER("Runebreaker", Material.IRON_ORE, 25, 3, 1, STONEWHISPER, 2,
            "+5% ore XP per rank."),
    DEEPBORN("Deepborn", Material.DEEPSLATE, 25, 3, 1, STONEWHISPER, 2,
            "+7% XP below Y=0 per rank."),
    GEMSEEKER("Gemseeker", Material.DIAMOND, 45, 3, 2, RUNEBREAKER, 2,
            "4% per rank chance to double profession XP."),
    ECHO_OF_DEPTH("Echo of the Depth", Material.SCULK, 45, 3, 2, DEEPBORN, 2,
            "Mining streaks awaken a temporary Haste blessing."),
    HEART_OF_MOUNTAIN("Heart of the Mountain", Material.NETHER_STAR, 100, 1, 5, null, 0,
            "Awakened path. Requires both branches at Rank III.");

    private final String displayName;
    private final Material icon;
    private final int requiredLevel;
    private final int maxRank;
    private final int essenceCost;
    private final MinerSkill prerequisite;
    private final int prerequisiteRank;
    private final String description;

    MinerSkill(String displayName, Material icon, int requiredLevel, int maxRank, int essenceCost,
               MinerSkill prerequisite, int prerequisiteRank, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.requiredLevel = requiredLevel;
        this.maxRank = maxRank;
        this.essenceCost = essenceCost;
        this.prerequisite = prerequisite;
        this.prerequisiteRank = prerequisiteRank;
        this.description = description;
    }

    public String displayName() { return displayName; }
    public Material icon() { return icon; }
    public int requiredLevel() { return requiredLevel; }
    public int maxRank() { return maxRank; }
    public int essenceCost() { return essenceCost; }
    public MinerSkill prerequisite() { return prerequisite; }
    public int prerequisiteRank() { return prerequisiteRank; }
    public String description() { return description; }
}
