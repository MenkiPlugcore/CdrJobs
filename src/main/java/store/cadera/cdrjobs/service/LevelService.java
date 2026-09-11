package store.cadera.cdrjobs.service;

import store.cadera.cdrjobs.CdrJobsPlugin;

public final class LevelService {
    private final CdrJobsPlugin plugin;

    public LevelService(CdrJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public long xpRequiredForNextLevel(int level) {
        if (level >= maxLevel()) return 0L;

        long step = Math.max(0L, (long) level - 1L);
        long base = Math.max(1L, plugin.getConfig().getLong("settings.xp-curve.base-xp", 100L));
        long linear = Math.max(0L, plugin.getConfig().getLong("settings.xp-curve.linear-growth", 0L));
        long quadratic = Math.max(0L, plugin.getConfig().getLong("settings.xp-curve.quadratic-growth", 15L));

        try {
            long linearPart = Math.multiplyExact(step, linear);
            long square = Math.multiplyExact(step, step);
            long quadraticPart = Math.multiplyExact(square, quadratic);
            return Math.max(1L, Math.addExact(base, Math.addExact(linearPart, quadraticPart)));
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE / 4L;
        }
    }

    public int maxLevel() {
        return Math.max(1, plugin.getConfig().getInt("settings.max-level", 100));
    }
}
