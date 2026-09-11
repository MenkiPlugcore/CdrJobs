package store.cadera.cdrjobs.service;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.api.event.ContractClaimEvent;
import store.cadera.cdrjobs.api.event.ContractCompleteEvent;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.util.Colors;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public final class ContractService {
    public static final String STORE_JOB = "CONTRACTS";

    private final CdrJobsPlugin plugin;
    private final Database database;
    private final ProfessionStore store;
    private final ProgressionService progression;

    public ContractService(CdrJobsPlugin plugin, Database database, ProfessionStore store, ProgressionService progression) {
        this.plugin = plugin;
        this.database = database;
        this.store = store;
        this.progression = progression;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("contracts.enabled", true);
    }

    public ContractView view(UUID uuid, Cadence cadence) {
        long cycle = currentCycle(cadence);
        ensureCycle(uuid, cadence, cycle);
        long salt = store.getCounter(uuid, STORE_JOB, key(cadence, "salt"));
        ContractDefinition definition = select(uuid, cadence, cycle, salt);
        long progress = Math.min(definition.target(), store.getCounter(uuid, STORE_JOB, key(cadence, "progress")));
        boolean claimed = store.getCounter(uuid, STORE_JOB, key(cadence, "claimed")) > 0L;
        int usedRerolls = safeInt(store.getCounter(uuid, STORE_JOB, key(cadence, "rerolls")));
        return new ContractView(cadence, cycle, definition, progress, claimed, usedRerolls, rerollLimit(cadence));
    }

    public void record(Player player, JobType job, long amount) {
        if (!enabled() || amount <= 0L) return;
        for (Cadence cadence : Cadence.values()) {
            ContractView current = view(player.getUniqueId(), cadence);
            if (current.claimed() || current.complete() || current.definition().job() != job) continue;
            long next = saturatingAdd(current.progress(), amount);
            next = Math.min(current.definition().target(), next);
            store.setCounter(player.getUniqueId(), STORE_JOB, key(cadence, "progress"), next);
            if (next >= current.definition().target()) {
                player.sendMessage(Colors.color(plugin.prefix() + "&6✦ &e" + cadence.display() + " Contract Complete &6✦ &7" + current.definition().name() + " &7— gunakan &f/cdrjobs contracts claim " + cadence.id() + "&7."));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.15f);
                plugin.getServer().getPluginManager().callEvent(new ContractCompleteEvent(
                        player, cadence.id(), current.definition().id(), current.definition().job(), current.definition().target()));
            } else if (plugin.getConfig().getBoolean("contracts.progress-actionbar", false)) {
                player.sendActionBar(Colors.color("&b" + cadence.display() + " Contract &7" + next + "/" + current.definition().target()));
            }
        }
    }

    public ClaimResult claim(Player player, Cadence cadence) {
        if (!enabled()) return ClaimResult.DISABLED;
        synchronized (this) {
            UUID uuid = player.getUniqueId();
            ContractView current = view(uuid, cadence);
            if (current.claimed()) return ClaimResult.ALREADY_CLAIMED;
            if (!current.complete()) return ClaimResult.INCOMPLETE;

            ContractDefinition def = current.definition();
            try {
                // Component checkpoints make ordinary retry after an exception recoverable without
                // re-paying a component that was already delivered successfully.
                if (def.rewardFate() > 0 && store.getCounter(uuid, STORE_JOB, key(cadence, "reward_fate_paid")) == 0L) {
                    database.addFateEssence(uuid, def.rewardFate());
                    store.setCounter(uuid, STORE_JOB, key(cadence, "reward_fate_paid"), 1L);
                }
                if (def.rewardXp() > 0L && store.getCounter(uuid, STORE_JOB, key(cadence, "reward_xp_paid")) == 0L) {
                    progression.addXp(player, def.job(), def.rewardXp());
                    store.setCounter(uuid, STORE_JOB, key(cadence, "reward_xp_paid"), 1L);
                }
                store.setCounter(uuid, STORE_JOB, key(cadence, "claimed"), 1L);
            } catch (RuntimeException exception) {
                plugin.getLogger().severe("Contract reward delivery failed for " + player.getName() + " / " + cadence + ": " + exception.getMessage());
                return ClaimResult.REWARD_ERROR;
            }

            player.sendMessage(Colors.color(plugin.prefix() + "&aContract claimed: &f" + def.name() + " &8— &b+" + def.rewardXp() + " " + def.job().displayName() + " XP &8• &d+" + def.rewardFate() + " Fate Essence"));
            plugin.getServer().getPluginManager().callEvent(new ContractClaimEvent(
                    player, cadence.id(), def.id(), def.job(), def.rewardXp(), def.rewardFate()));
            return ClaimResult.SUCCESS;
        }
    }

    public RerollResult reroll(Player player, Cadence cadence) {
        if (!enabled()) return RerollResult.DISABLED;
        UUID uuid = player.getUniqueId();
        synchronized (this) {
            ContractView current = view(uuid, cadence);
            if (current.claimed() || current.complete()) return RerollResult.LOCKED;
            if (current.usedRerolls() >= current.rerollLimit()) return RerollResult.NO_REROLLS;
            List<ContractDefinition> candidates = definitions(cadence);
            if (candidates.size() <= 1) return RerollResult.NO_ALTERNATIVE;

            String oldId = current.definition().id();
            long salt = store.getCounter(uuid, STORE_JOB, key(cadence, "salt"));
            ContractDefinition replacement = current.definition();
            int attempts = candidates.size() + 2;
            while (attempts-- > 0 && replacement.id().equals(oldId)) {
                salt++;
                replacement = select(uuid, cadence, current.cycle(), salt);
            }
            if (replacement.id().equals(oldId)) return RerollResult.NO_ALTERNATIVE;

            store.setCounter(uuid, STORE_JOB, key(cadence, "salt"), salt);
            store.setCounter(uuid, STORE_JOB, key(cadence, "progress"), 0L);
            store.setCounter(uuid, STORE_JOB, key(cadence, "reward_fate_paid"), 0L);
            store.setCounter(uuid, STORE_JOB, key(cadence, "reward_xp_paid"), 0L);
            store.incrementCounter(uuid, STORE_JOB, key(cadence, "rerolls"), 1L);
            player.sendMessage(Colors.color(plugin.prefix() + "&e" + cadence.display() + " Contract rerolled &7→ &f" + replacement.name()));
            return RerollResult.SUCCESS;
        }
    }

    public void reset(UUID uuid, Cadence cadence) {
        long cycle = currentCycle(cadence);
        store.setCounter(uuid, STORE_JOB, key(cadence, "cycle"), cycle);
        store.setCounter(uuid, STORE_JOB, key(cadence, "salt"), 0L);
        store.setCounter(uuid, STORE_JOB, key(cadence, "progress"), 0L);
        store.setCounter(uuid, STORE_JOB, key(cadence, "claimed"), 0L);
        store.setCounter(uuid, STORE_JOB, key(cadence, "rerolls"), 0L);
        store.setCounter(uuid, STORE_JOB, key(cadence, "reward_fate_paid"), 0L);
        store.setCounter(uuid, STORE_JOB, key(cadence, "reward_xp_paid"), 0L);
    }

    public List<String> lines(UUID uuid) {
        List<String> out = new ArrayList<>();
        for (Cadence cadence : Cadence.values()) {
            ContractView view = view(uuid, cadence);
            ContractDefinition d = view.definition();
            String status = view.claimed() ? "§aCLAIMED" : view.complete() ? "§eREADY TO CLAIM" : "§b" + view.progress() + "/" + d.target();
            out.add("§3" + cadence.display() + " Contract §8— §f" + d.name());
            out.add("§7Job: §b" + d.job().displayName() + " §8• §7Progress: " + status);
            out.add("§7Reward: §b" + d.rewardXp() + " XP §8• §d" + d.rewardFate() + " Fate §8• §7Reroll " + view.usedRerolls() + "/" + view.rerollLimit());
        }
        return out;
    }

    public ContractDefinition definition(UUID uuid, Cadence cadence) {
        return view(uuid, cadence).definition();
    }

    private void ensureCycle(UUID uuid, Cadence cadence, long currentCycle) {
        String cycleKey = key(cadence, "cycle");
        long storedCycle = store.getCounter(uuid, STORE_JOB, cycleKey);
        if (storedCycle == currentCycle) return;
        reset(uuid, cadence);
    }

    private long currentCycle(Cadence cadence) {
        LocalDate date = LocalDate.now(zone());
        if (cadence == Cadence.DAILY) return date.toEpochDay();
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return Math.floorDiv(monday.toEpochDay(), 7L);
    }

    private ZoneId zone() {
        String raw = plugin.getConfig().getString("contracts.timezone", "Asia/Jakarta");
        try { return ZoneId.of(raw); }
        catch (Exception ignored) { return ZoneId.of("Asia/Jakarta"); }
    }

    private int rerollLimit(Cadence cadence) {
        return Math.max(0, Math.min(10, plugin.getConfig().getInt("contracts." + cadence.id() + "-rerolls", 1)));
    }

    private ContractDefinition select(UUID uuid, Cadence cadence, long cycle, long salt) {
        List<ContractDefinition> candidates = definitions(cadence);
        if (candidates.isEmpty()) return fallback(cadence);
        long hash = Objects.hash(uuid.toString(), cadence.name(), cycle, salt);
        int index = Math.floorMod(hash, candidates.size());
        return candidates.get(index);
    }

    public List<ContractDefinition> definitions(Cadence cadence) {
        var section = plugin.getConfig().getConfigurationSection("contracts.definitions");
        if (section == null) return List.of(fallback(cadence));
        List<ContractDefinition> result = new ArrayList<>();
        List<String> ids = new ArrayList<>(section.getKeys(false));
        Collections.sort(ids);
        for (String id : ids) {
            String base = "contracts.definitions." + id + ".";
            Cadence parsed = Cadence.parse(plugin.getConfig().getString(base + "cadence", ""));
            if (parsed != cadence) continue;
            JobType job;
            try { job = JobType.valueOf(plugin.getConfig().getString(base + "job", "MINER").toUpperCase(Locale.ROOT)); }
            catch (Exception ignored) { continue; }
            long target = Math.max(1L, plugin.getConfig().getLong(base + "target", 1L));
            long rewardXp = Math.max(0L, plugin.getConfig().getLong(base + "reward-xp", 0L));
            int rewardFate = Math.max(0, plugin.getConfig().getInt(base + "reward-fate", 0));
            String name = plugin.getConfig().getString(base + "name", pretty(id));
            result.add(new ContractDefinition(id, name, cadence, job, target, rewardXp, rewardFate));
        }
        return result.isEmpty() ? List.of(fallback(cadence)) : List.copyOf(result);
    }

    private ContractDefinition fallback(Cadence cadence) {
        if (cadence == Cadence.DAILY) return new ContractDefinition("fallback-daily", "Daily Miner Duty", cadence, JobType.MINER, 100L, 500L, 0);
        return new ContractDefinition("fallback-weekly", "Weekly Miner Commission", cadence, JobType.MINER, 750L, 2500L, 1);
    }

    private ContractDefinition select(UUID uuid, Cadence cadence, long cycle, int salt) {
        return select(uuid, cadence, cycle, (long) salt);
    }

    private long saturatingAdd(long current, long amount) {
        if (amount > 0L && current > Long.MAX_VALUE - amount) return Long.MAX_VALUE;
        return current + amount;
    }

    private String key(Cadence cadence, String suffix) { return cadence.id() + "_" + suffix; }
    private int safeInt(long value) { return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, value)); }
    private String pretty(String id) {
        String[] parts = id.replace('-', ' ').replace('_', ' ').split("\\s+");
        StringBuilder b = new StringBuilder();
        for (String part : parts) if (!part.isBlank()) b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        return b.toString().trim();
    }

    public enum Cadence {
        DAILY("daily", "Daily"), WEEKLY("weekly", "Weekly");
        private final String id; private final String display;
        Cadence(String id, String display) { this.id = id; this.display = display; }
        public String id() { return id; }
        public String display() { return display; }
        public static Cadence parse(String raw) {
            if (raw == null) return null;
            return switch (raw.toUpperCase(Locale.ROOT)) { case "DAILY" -> DAILY; case "WEEKLY" -> WEEKLY; default -> null; };
        }
    }

    public record ContractDefinition(String id, String name, Cadence cadence, JobType job, long target, long rewardXp, int rewardFate) {}
    public record ContractView(Cadence cadence, long cycle, ContractDefinition definition, long progress, boolean claimed, int usedRerolls, int rerollLimit) {
        public boolean complete() { return progress >= definition.target(); }
        public int percent() {
            if (definition.target() <= 0L || progress <= 0L) return 0;
            if (progress >= definition.target()) return 100;
            return (int) Math.max(0D, Math.min(100D, (progress * 100.0D) / definition.target()));
        }
    }
    public enum ClaimResult { SUCCESS, DISABLED, INCOMPLETE, ALREADY_CLAIMED, REWARD_ERROR }
    public enum RerollResult { SUCCESS, DISABLED, LOCKED, NO_REROLLS, NO_ALTERNATIVE }
}
