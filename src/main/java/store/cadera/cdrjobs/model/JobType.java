package store.cadera.cdrjobs.model;

import org.bukkit.Material;

public enum JobType {
    MINER("Runebound Delver", Material.DIAMOND_PICKAXE, true),
    FARMER("Verdant Keeper", Material.GOLDEN_HOE, true),
    HUNTER("Bloodfang Stalker", Material.IRON_SWORD, false),
    LUMBERJACK("Ironbark Warden", Material.IRON_AXE, false),
    FISHER("Tidebound Angler", Material.FISHING_ROD, false);

    private final String displayName;
    private final Material icon;
    private final boolean released;

    JobType(String displayName, Material icon, boolean released) {
        this.displayName = displayName; this.icon = icon; this.released = released;
    }
    public String displayName() { return displayName; }
    public Material icon() { return icon; }
    public boolean released() { return released; }
}
