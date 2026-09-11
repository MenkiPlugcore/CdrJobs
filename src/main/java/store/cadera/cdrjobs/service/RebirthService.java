package store.cadera.cdrjobs.service;

import org.bukkit.entity.Player;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.RebirthStore;
import store.cadera.cdrjobs.integration.VaultEconomyHook;
import store.cadera.cdrjobs.model.*;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RebirthService {
    private final CdrJobsPlugin plugin;
    private final Database database;
    private final RebirthStore store;
    private final VaultEconomyHook vault;
    private final ProfileService profiles;

    public RebirthService(CdrJobsPlugin plugin, Database database, RebirthStore store,
                          VaultEconomyHook vault, ProfileService profiles) {
        this.plugin = plugin;
        this.database = database;
        this.store = store;
        this.vault = vault;
        this.profiles = profiles;
    }

    public Quote quote(UUID uuid, JobType job) {
        Map<String, Integer> ranks = database.getSkillRanks(uuid);
        Map<String, Integer> costs = skillCosts(job);
        long spent = 0L;
        int nodes = 0;
        int totalRanks = 0;
        for (Map.Entry<String, Integer> entry : costs.entrySet()) {
            int rank = Math.max(0, ranks.getOrDefault(entry.getKey(), 0));
            if (rank <= 0) continue;
            nodes++;
            totalRanks += rank;
            spent += (long) rank * Math.max(0, entry.getValue());
        }
        int refundPercent = refundPercent();
        int refund = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, spent * refundPercent / 100L));
        long remaining = cooldownRemainingSeconds(uuid, job);
        FeeMode feeMode = feeMode();
        return new Quote(job, nodes, totalRanks, (int) Math.min(Integer.MAX_VALUE, spent), refund,
                refundPercent, remaining, feeMode, fateFee(), vaultFee());
    }

    public Outcome execute(Player player, JobType job) {
        return execute(player, job, false);
    }

    public Outcome force(Player player, JobType job) {
        return execute(player, job, true);
    }

    private Outcome execute(Player player, JobType job, boolean bypass) {
        if (!bypass && !plugin.getConfig().getBoolean("rite-of-rebirth.enabled", true)) {
            return new Outcome(Status.DISABLED, 0, 0, 0, 0L);
        }

        FeeMode mode = bypass ? FeeMode.NONE : feeMode();
        double vaultCost = bypass ? 0D : vaultFee();
        int fateCost = bypass ? 0 : fateFee();
        boolean withdrewVault = false;

        if (mode == FeeMode.VAULT && vaultCost > 0D) {
            if (!vault.available()) return new Outcome(Status.VAULT_UNAVAILABLE, 0, 0, 0, 0L);
            if (!vault.has(player, vaultCost)) return new Outcome(Status.NOT_ENOUGH_VAULT, 0, 0, 0, 0L);
            if (!vault.withdraw(player, vaultCost)) return new Outcome(Status.VAULT_TRANSACTION_FAILED, 0, 0, 0, 0L);
            withdrewVault = true;
        }

        try {
            RebirthStore.Result result = store.rebirth(
                    player.getUniqueId(),
                    job,
                    skillCosts(job),
                    bypass ? 100 : refundPercent(),
                    mode == FeeMode.FATE ? fateCost : 0,
                    bypass ? 0L : cooldownSeconds(),
                    bypass
            );

            if (result.status() != RebirthStore.Status.SUCCESS) {
                if (withdrewVault) vault.deposit(player, vaultCost);
                Status status = switch (result.status()) {
                    case NO_SKILLS -> Status.NO_SKILLS;
                    case COOLDOWN -> Status.COOLDOWN;
                    case NOT_ENOUGH_FATE -> Status.NOT_ENOUGH_FATE;
                    case SUCCESS -> Status.SUCCESS;
                };
                long remaining = result.readyAt() <= 0L ? 0L : Math.max(0L, (result.readyAt() - System.currentTimeMillis() + 999L) / 1000L);
                return new Outcome(status, result.refund(), result.investedNodes(), result.investedRanks(), remaining);
            }

            profiles.invalidate(player.getUniqueId());
            return new Outcome(Status.SUCCESS, result.refund(), result.investedNodes(), result.investedRanks(),
                    result.readyAt() <= 0L ? 0L : Math.max(0L, (result.readyAt() - System.currentTimeMillis() + 999L) / 1000L));
        } catch (RuntimeException e) {
            if (withdrewVault && !vault.deposit(player, vaultCost)) {
                plugin.getLogger().severe("Rite of Rebirth failed and Vault rollback also failed for " + player.getName());
            }
            plugin.getLogger().warning("Rite of Rebirth failed for " + player.getName() + ": " + e.getMessage());
            return new Outcome(Status.ERROR, 0, 0, 0, 0L);
        }
    }

    public long cooldownRemainingSeconds(UUID uuid, JobType job) {
        long readyAt = store.cooldownReadyAt(uuid, job);
        long remaining = readyAt - System.currentTimeMillis();
        return remaining <= 0L ? 0L : (remaining + 999L) / 1000L;
    }

    public long cooldownSeconds() {
        return Math.max(0L, plugin.getConfig().getLong("rite-of-rebirth.cooldown-seconds", 604800L));
    }

    public int refundPercent() {
        return Math.max(0, Math.min(100, plugin.getConfig().getInt("rite-of-rebirth.refund-percent", 100)));
    }

    public int fateFee() {
        return Math.max(0, plugin.getConfig().getInt("rite-of-rebirth.fee.fate-essence", 0));
    }

    public double vaultFee() {
        return Math.max(0D, plugin.getConfig().getDouble("rite-of-rebirth.fee.vault", 0D));
    }

    public FeeMode feeMode() {
        String raw = plugin.getConfig().getString("rite-of-rebirth.fee.mode", "NONE");
        try { return FeeMode.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return FeeMode.NONE; }
    }

    private Map<String, Integer> skillCosts(JobType job) {
        Map<String, Integer> result = new LinkedHashMap<>();
        switch (job) {
            case MINER -> {
                for (MinerSkill skill : MinerSkill.values()) result.put(skill.name(), skill.essenceCost());
            }
            case FARMER -> {
                for (FarmerSkill skill : FarmerSkill.values()) result.put(skill.key(), skill.essenceCost());
            }
            case HUNTER -> {
                for (HunterSkill skill : HunterSkill.values()) result.put(skill.key(), skill.essenceCost());
            }
            case LUMBERJACK -> {
                for (LumberjackSkill skill : LumberjackSkill.values()) result.put(skill.key(), skill.essenceCost());
            }
            case FISHER -> {
                for (FisherSkill skill : FisherSkill.values()) result.put(skill.key(), skill.essenceCost());
            }
        }
        return result;
    }

    public enum FeeMode { NONE, FATE, VAULT }

    public enum Status {
        SUCCESS,
        NO_SKILLS,
        COOLDOWN,
        NOT_ENOUGH_FATE,
        VAULT_UNAVAILABLE,
        NOT_ENOUGH_VAULT,
        VAULT_TRANSACTION_FAILED,
        DISABLED,
        ERROR
    }

    public record Quote(JobType job, int investedNodes, int investedRanks, int spentEssence,
                        int refundEssence, int refundPercent, long cooldownRemainingSeconds,
                        FeeMode feeMode, int fateFee, double vaultFee) {}

    public record Outcome(Status status, int refundEssence, int investedNodes, int investedRanks,
                          long cooldownRemainingSeconds) {}
}
