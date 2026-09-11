package store.cadera.cdrjobs.service;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.FarmerSkill;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.util.Colors;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FarmerService {
    public static final String JOB = "FARMER";
    public static final String VERDANT_BLOOM = "farmer_verdant_bloom";

    private final CdrJobsPlugin plugin;
    private final Database database;
    private final ProfessionStore store;
    private final Map<UUID, Long> activeUntil = new ConcurrentHashMap<>();

    public FarmerService(CdrJobsPlugin plugin, Database database, ProfessionStore store) {
        this.plugin = plugin;
        this.database = database;
        this.store = store;
    }

    public int rank(Player player, FarmerSkill skill) {
        return store.getSkillRank(player.getUniqueId(), skill.key());
    }

    public boolean tryUpgrade(Player player, FarmerSkill skill) {
        int rank = rank(player, skill);
        if (rank >= skill.maxRank()) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("max-skill")));
            return false;
        }
        int level = database.getProgress(player.getUniqueId(), JobType.FARMER).level();
        if (level < skill.requiredLevel()) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("skill-requirement")));
            return false;
        }
        if (skill.prerequisite() != null && rank(player, skill.prerequisite()) < skill.prerequisiteRank()) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("skill-requirement")));
            return false;
        }
        if (skill == FarmerSkill.VERDANT_BLOOM && !seedTrialComplete(player)) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("trial-required").replace("%trial%", "Trial of Seed")));
            return false;
        }
        if (skill == FarmerSkill.VERDANT_DOMINION
                && (rank(player, FarmerSkill.BLESSING_OF_GAIA) < 3
                || rank(player, FarmerSkill.SPIRIT_OF_GROVE) < 3
                || !gaiaTrialComplete(player))) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("skill-requirement")));
            return false;
        }
        if (database.getFateEssence(player.getUniqueId()) < skill.essenceCost()) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("not-enough-essence")));
            return false;
        }
        database.addFateEssence(player.getUniqueId(), -skill.essenceCost());
        store.setSkillRank(player.getUniqueId(), skill.key(), rank + 1);
        player.sendMessage(Colors.color(plugin.prefix() + plugin.message("skill-unlocked")
                .replace("%skill%", skill.displayName())
                .replace("%rank%", String.valueOf(rank + 1))));
        return true;
    }

    public long applyXpModifiers(Player player, long baseXp) {
        double multiplier = 1.0 + rank(player, FarmerSkill.GREENBLOOD) * 0.05;
        long time = player.getWorld().getTime();
        boolean day = time < 12300 || time > 23850;
        if (day) multiplier += rank(player, FarmerSkill.SUNPETAL) * 0.08;
        if (rank(player, FarmerSkill.ROOTBOUND) > 0) multiplier += rank(player, FarmerSkill.SPIRIT_OF_GROVE) * 0.06;
        if (rank(player, FarmerSkill.VERDANT_DOMINION) > 0) multiplier += 0.20;
        long xp = Math.max(1L, Math.round(baseXp * multiplier));
        int blessing = rank(player, FarmerSkill.BLESSING_OF_GAIA);
        if (blessing > 0 && Math.random() < blessing * 0.04) xp *= 2L;
        Long until = activeUntil.get(player.getUniqueId());
        if (until != null) {
            if (until > System.currentTimeMillis()) {
                double bonus = Math.max(0.0, plugin.getConfig().getDouble("farmer.abilities.verdant-bloom.xp-bonus-percent", 25.0));
                xp = Math.max(1L, Math.round(xp * (1.0 + bonus / 100.0)));
            } else {
                activeUntil.remove(player.getUniqueId());
            }
        }
        return xp;
    }

    public void recordHarvest(Player player, boolean rare) {
        UUID uuid = player.getUniqueId();
        store.incrementCounter(uuid, JOB, "harvests", 1);
        if (rare) store.incrementCounter(uuid, JOB, "rare_harvests", 1);

        long harvests = harvests(player);
        if (!seedTrialComplete(player) && harvests >= seedTarget()) {
            store.setFlag(uuid, JOB, "trial_seed", true);
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("trial-seed-complete")));
        }
        if (!gaiaTrialComplete(player)
                && harvests >= gaiaHarvestTarget()
                && rareHarvests(player) >= gaiaRareTarget()) {
            store.setFlag(uuid, JOB, "trial_gaia", true);
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("trial-gaia-complete")));
        }
    }

    public boolean activate(Player player) {
        if (rank(player, FarmerSkill.VERDANT_BLOOM) <= 0) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("farmer-ability-locked")));
            return false;
        }
        long now = System.currentTimeMillis();
        long ready = store.getAbilityReadyAt(player.getUniqueId(), VERDANT_BLOOM);
        if (ready > now) {
            long seconds = Math.max(1L, (ready - now + 999L) / 1000L);
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("farmer-ability-cooldown")
                    .replace("%seconds%", String.valueOf(seconds))));
            return false;
        }
        int duration = Math.max(1, plugin.getConfig().getInt("farmer.abilities.verdant-bloom.duration-seconds", 20));
        int cooldown = Math.max(duration, plugin.getConfig().getInt("farmer.abilities.verdant-bloom.cooldown-seconds", 300));
        activeUntil.put(player.getUniqueId(), now + duration * 1000L);
        store.setAbilityReadyAt(player.getUniqueId(), VERDANT_BLOOM, now + cooldown * 1000L);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration * 20, 0, true, false, true));
        player.playSound(player.getLocation(), Sound.BLOCK_AZALEA_LEAVES_BREAK, 1.0f, 1.2f);
        player.sendMessage(Colors.color(plugin.prefix() + plugin.message("verdant-bloom-activated")
                .replace("%duration%", String.valueOf(duration))));
        return true;
    }

    public boolean autoReplant(Player player) {
        return rank(player, FarmerSkill.ROOTBOUND) > 0;
    }

    public long harvests(Player player) {
        return store.getTrialProgress(player.getUniqueId(), JOB, "harvests");
    }

    public long rareHarvests(Player player) {
        return store.getTrialProgress(player.getUniqueId(), JOB, "rare_harvests");
    }

    public boolean seedTrialComplete(Player player) {
        return store.getFlag(player.getUniqueId(), JOB, "trial_seed");
    }

    public boolean gaiaTrialComplete(Player player) {
        return store.getFlag(player.getUniqueId(), JOB, "trial_gaia");
    }

    public int seedTarget() {
        return Math.max(1, plugin.getConfig().getInt("farmer.trials.trial-of-seed.harvests", 500));
    }

    public int gaiaHarvestTarget() {
        return Math.max(1, plugin.getConfig().getInt("farmer.trials.trial-of-gaia.harvests", 2000));
    }

    public int gaiaRareTarget() {
        return Math.max(1, plugin.getConfig().getInt("farmer.trials.trial-of-gaia.rare-harvests", 250));
    }

    public long cooldownSeconds(UUID uuid) {
        long remaining = store.getAbilityReadyAt(uuid, VERDANT_BLOOM) - System.currentTimeMillis();
        return remaining <= 0 ? 0 : (remaining + 999L) / 1000L;
    }
}
