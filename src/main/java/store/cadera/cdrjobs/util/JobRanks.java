package store.cadera.cdrjobs.util;

import store.cadera.cdrjobs.model.JobType;

public final class JobRanks {
    private JobRanks() {}

    public static String title(JobType job, int level) {
        return switch (job) {
            case MINER -> level >= 100 ? "Lord of the Deep" : level >= 75 ? "Mountainborn" : level >= 50 ? "Deepforge Master" : level >= 25 ? "Runebound Delver" : level >= 10 ? "Cavewalker" : "Stone Initiate";
            case FARMER -> level >= 100 ? "Gaia's Chosen" : level >= 75 ? "Verdant Sage" : level >= 50 ? "Grove Keeper" : level >= 25 ? "Harvest Warden" : level >= 10 ? "Greenhand" : "Seedling";
            case HUNTER -> level >= 100 ? "Apex Hunter" : level >= 75 ? "Nightstalker" : level >= 50 ? "Bloodfang" : level >= 25 ? "Fangwalker" : level >= 10 ? "Tracker" : "Trail Initiate";
            case LUMBERJACK -> level >= 100 ? "Ancient Warden" : level >= 75 ? "Grove Warden" : level >= 50 ? "Ironbark" : level >= 25 ? "Timberborn" : level >= 10 ? "Woodhand" : "Sapling Hand";
            case FISHER -> level >= 100 ? "Lord of the Tides" : level >= 75 ? "Leviathan Seeker" : level >= 50 ? "Abyss Fisher" : level >= 25 ? "Tidebound Angler" : level >= 10 ? "Tide Seeker" : "River Initiate";
        };
    }
}
