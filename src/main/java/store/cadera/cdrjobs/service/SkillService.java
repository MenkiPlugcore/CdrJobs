package store.cadera.cdrjobs.service;

import org.bukkit.entity.Player;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.model.MinerSkill;
import store.cadera.cdrjobs.util.Colors;

public final class SkillService {
    private final CdrJobsPlugin plugin;
    private final Database database;
    private final MinerTrialService trials;

    public SkillService(CdrJobsPlugin plugin, Database database, MinerTrialService trials) {
        this.plugin = plugin;
        this.database = database;
        this.trials = trials;
    }

    public boolean tryUpgrade(Player player, MinerSkill skill) {
        int rank = database.getSkillRank(player.getUniqueId(), skill);
        if (rank >= skill.maxRank()) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("max-skill")));
            return false;
        }

        int level = database.getProgress(player.getUniqueId(), JobType.MINER).level();
        if (level < skill.requiredLevel()) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("skill-requirement")));
            return false;
        }

        if (skill.prerequisite() != null && database.getSkillRank(player.getUniqueId(), skill.prerequisite()) < skill.prerequisiteRank()) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("skill-requirement")));
            return false;
        }

        if (skill == MinerSkill.RUNIC_SURGE && !trials.isStoneComplete(player)) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("trial-required").replace("%trial%", "Trial of Stone")));
            return false;
        }

        if (skill == MinerSkill.HEART_OF_MOUNTAIN) {
            if (database.getSkillRank(player.getUniqueId(), MinerSkill.GEMSEEKER) < 3
                    || database.getSkillRank(player.getUniqueId(), MinerSkill.ECHO_OF_DEPTH) < 3) {
                player.sendMessage(Colors.color(plugin.prefix() + plugin.message("skill-requirement")));
                return false;
            }
            if (!trials.isDeepComplete(player)) {
                player.sendMessage(Colors.color(plugin.prefix() + plugin.message("trial-required").replace("%trial%", "Trial of the Deep")));
                return false;
            }
        }

        int essence = database.getFateEssence(player.getUniqueId());
        if (essence < skill.essenceCost()) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("not-enough-essence")));
            return false;
        }

        database.addFateEssence(player.getUniqueId(), -skill.essenceCost());
        database.setSkillRank(player.getUniqueId(), skill, rank + 1);
        player.sendMessage(Colors.color(plugin.prefix() + plugin.message("skill-unlocked")
                .replace("%skill%", skill.displayName())
                .replace("%rank%", String.valueOf(rank + 1))));
        return true;
    }

    public long applyMinerXpModifiers(Player player, long baseXp) {
        int stone = database.getSkillRank(player.getUniqueId(), MinerSkill.STONEWHISPER);
        int rune = database.getSkillRank(player.getUniqueId(), MinerSkill.RUNEBREAKER);
        int deep = database.getSkillRank(player.getUniqueId(), MinerSkill.DEEPBORN);
        int heart = database.getSkillRank(player.getUniqueId(), MinerSkill.HEART_OF_MOUNTAIN);

        double multiplier = 1.0 + (stone * 0.05) + (rune * 0.05);
        if (player.getLocation().getBlockY() < 0) multiplier += deep * 0.07;
        if (heart > 0 && player.getLocation().getBlockY() < 0) multiplier += 0.20;

        long result = Math.max(1, Math.round(baseXp * multiplier));

        int gem = database.getSkillRank(player.getUniqueId(), MinerSkill.GEMSEEKER);
        if (gem > 0 && Math.random() < gem * 0.04) result *= 2;
        return result;
    }
}
