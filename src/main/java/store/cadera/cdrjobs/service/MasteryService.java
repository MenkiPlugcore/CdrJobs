package store.cadera.cdrjobs.service;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.api.event.MasteryTierUpEvent;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.util.Colors;

import java.util.UUID;

public final class MasteryService {
    public static final String METRIC = "mastery_total_xp";

    private final CdrJobsPlugin plugin;
    private final ProfessionStore store;

    public MasteryService(CdrJobsPlugin plugin, ProfessionStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("mastery.enabled", true);
    }

    public int maxTier() {
        return Math.max(1, Math.min(50, plugin.getConfig().getInt("mastery.max-tier", 10)));
    }

    public long requirementForTier(int currentTier) {
        long base = Math.max(1L, plugin.getConfig().getLong("mastery.base-xp", 5000L));
        long growth = Math.max(0L, plugin.getConfig().getLong("mastery.growth-per-tier", 2500L));
        return saturatingAdd(base, saturatingMultiply(growth, Math.max(0, currentTier)));
    }

    public State state(UUID uuid, JobType job) {
        return stateFromTotal(store.getCounter(uuid, job.name(), METRIC));
    }

    public State stateFromTotal(long rawTotal) {
        long total = Math.max(0L, rawTotal);
        int tier = 0;
        long remaining = total;
        while (tier < maxTier()) {
            long required = requirementForTier(tier);
            if (remaining < required) break;
            remaining -= required;
            tier++;
        }
        if (tier >= maxTier()) {
            long cap = cumulativeForTier(maxTier());
            return new State(maxTier(), 0L, 0L, Math.min(total, cap), title(maxTier()), badge(maxTier()));
        }
        return new State(tier, remaining, requirementForTier(tier), total, title(tier), badge(tier));
    }

    public synchronized State addXp(Player player, JobType job, long amount) {
        State before = state(player.getUniqueId(), job);
        if (!enabled() || amount <= 0L || before.tier() >= maxTier()) return before;

        long cap = cumulativeForTier(maxTier());
        long next = Math.min(cap, saturatingAdd(before.totalXp(), amount));
        store.setCounter(player.getUniqueId(), job.name(), METRIC, next);
        State after = stateFromTotal(next);

        if (after.tier() > before.tier()) {
            String message = plugin.message("mastery-tier-up")
                    .replace("%job%", job.displayName())
                    .replace("%tier%", roman(after.tier()))
                    .replace("%title%", after.title());
            player.sendMessage(Colors.color(plugin.prefix() + message));
            if (plugin.getConfig().getBoolean("mastery.tier-up-sound", true)) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.35f);
            }
            plugin.getServer().getPluginManager().callEvent(
                    new MasteryTierUpEvent(player, job, before.tier(), after.tier(), after.totalXp()));
        }
        return after;
    }

    public synchronized State setTier(UUID uuid, JobType job, int tier) {
        int safe = Math.max(0, Math.min(maxTier(), tier));
        long total = cumulativeForTier(safe);
        store.setCounter(uuid, job.name(), METRIC, total);
        return stateFromTotal(total);
    }

    public synchronized State addAdminXp(UUID uuid, JobType job, long amount) {
        State before = state(uuid, job);
        if (amount <= 0L) return before;
        long next = Math.min(cumulativeForTier(maxTier()), saturatingAdd(before.totalXp(), amount));
        store.setCounter(uuid, job.name(), METRIC, next);
        return stateFromTotal(next);
    }

    public long totalMasteryXp(UUID uuid) {
        long total = 0L;
        for (JobType job : JobType.values()) total = saturatingAdd(total, state(uuid, job).totalXp());
        return total;
    }

    public int totalMasteryTiers(UUID uuid) {
        int total = 0;
        for (JobType job : JobType.values()) total += state(uuid, job).tier();
        return total;
    }

    public String display(JobType job, State state) {
        if (state.tier() <= 0) return job.displayName() + " • Unmastered";
        return state.badge() + " " + state.title() + " " + job.displayName() + " • Mastery " + roman(state.tier());
    }

    public String roman(int number) {
        if (number <= 0) return "0";
        if (number > 20) return String.valueOf(number);
        String[] values = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
                "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};
        return values[number];
    }

    private String title(int tier) {
        if (tier <= 0) return "Unmastered";
        if (tier <= 2) return "Initiate";
        if (tier <= 4) return "Veteran";
        if (tier <= 6) return "Elite";
        if (tier <= 8) return "Ascendant";
        if (tier == 9) return "Paragon";
        return "Fatebound";
    }

    private String badge(int tier) {
        if (tier <= 0) return "";
        int stars = Math.min(5, 1 + ((tier - 1) / 2));
        return "✦".repeat(stars);
    }

    private long cumulativeForTier(int tier) {
        long total = 0L;
        for (int i = 0; i < tier; i++) total = saturatingAdd(total, requirementForTier(i));
        return total;
    }

    private long saturatingAdd(long a, long b) {
        if (b > 0L && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }

    private long saturatingMultiply(long a, long b) {
        if (a == 0L || b == 0L) return 0L;
        if (a > Long.MAX_VALUE / b) return Long.MAX_VALUE;
        return a * b;
    }

    public record State(int tier, long xp, long requiredXp, long totalXp, String title, String badge) {
        public boolean maxed() { return requiredXp <= 0L; }
    }
}
