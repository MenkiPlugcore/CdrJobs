package store.cadera.cdrjobs.model;

import org.bukkit.Material;

public enum FarmerSkill {
    GREENBLOOD("Greenblood", Material.WHEAT, 10, 3, 1, null, 0, "+5% Farmer XP per rank."),
    ROOTBOUND("Rootbound", Material.WHEAT_SEEDS, 25, 1, 2, GREENBLOOD, 2, "Automatically replants supported mature crops."),
    SUNPETAL("Sunpetal", Material.SUNFLOWER, 25, 3, 1, GREENBLOOD, 2, "+8% Farmer XP in daylight per rank."),
    VERDANT_BLOOM("Verdant Bloom", Material.SPORE_BLOSSOM, 35, 1, 2, GREENBLOOD, 2, "Active blessing unlocked through Trial of Seed."),
    BLESSING_OF_GAIA("Blessing of Gaia", Material.FLOWERING_AZALEA, 45, 3, 2, SUNPETAL, 2, "4% chance per rank to double profession XP."),
    SPIRIT_OF_GROVE("Spirit of the Grove", Material.OAK_SAPLING, 45, 3, 2, ROOTBOUND, 1, "+6% Farmer XP per rank while Rootbound is active."),
    VERDANT_DOMINION("Verdant Dominion", Material.ENCHANTED_GOLDEN_APPLE, 100, 1, 5, null, 0, "Awakened path. Requires both branches and Trial of Gaia.");

    private final String displayName;
    private final Material icon;
    private final int requiredLevel;
    private final int maxRank;
    private final int essenceCost;
    private final FarmerSkill prerequisite;
    private final int prerequisiteRank;
    private final String description;

    FarmerSkill(String displayName, Material icon, int requiredLevel, int maxRank, int essenceCost,
                FarmerSkill prerequisite, int prerequisiteRank, String description) {
        this.displayName = displayName; this.icon = icon; this.requiredLevel = requiredLevel;
        this.maxRank = maxRank; this.essenceCost = essenceCost; this.prerequisite = prerequisite;
        this.prerequisiteRank = prerequisiteRank; this.description = description;
    }

    public String key() { return "FARMER_" + name(); }
    public String displayName() { return displayName; }
    public Material icon() { return icon; }
    public int requiredLevel() { return requiredLevel; }
    public int maxRank() { return maxRank; }
    public int essenceCost() { return essenceCost; }
    public FarmerSkill prerequisite() { return prerequisite; }
    public int prerequisiteRank() { return prerequisiteRank; }
    public String description() { return description; }
}
