package store.cadera.cdrjobs.service;

public final class LevelService {
    private final int maxLevel;

    public LevelService(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public long xpRequiredForNextLevel(int level) {
        if (level >= maxLevel) return 0L;
        long n = Math.max(1, level);
        return 100L + (n - 1L) * (n - 1L) * 15L;
    }

    public int maxLevel() {
        return maxLevel;
    }
}
