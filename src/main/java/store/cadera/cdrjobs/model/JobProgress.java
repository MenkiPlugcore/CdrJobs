package store.cadera.cdrjobs.model;

public record JobProgress(int level, long xp) {
    public String minerRankTitle() {
        if (level >= 100) return "Lord of the Deep";
        if (level >= 75) return "Mountainborn";
        if (level >= 50) return "Deepforge Master";
        if (level >= 25) return "Runebound Delver";
        if (level >= 10) return "Cavewalker";
        return "Stone Initiate";
    }
}
