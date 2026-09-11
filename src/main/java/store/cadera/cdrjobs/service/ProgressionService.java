package store.cadera.cdrjobs.service;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.util.Colors;

import java.util.Map;

public final class ProgressionService {
    private final CdrJobsPlugin plugin;
    private final Database database;
    private final LevelService levels;

    public ProgressionService(CdrJobsPlugin plugin, Database database, LevelService levels) {
        this.plugin = plugin;
        this.database = database;
        this.levels = levels;
    }

    public JobProgress addXp(Player player, JobType job, long amount) {
        if (amount <= 0) return database.getProgress(player.getUniqueId(), job);

        JobProgress current = database.getProgress(player.getUniqueId(), job);
        int level = current.level();
        long xp = current.xp() + amount;

        while (level < levels.maxLevel()) {
            long required = levels.xpRequiredForNextLevel(level);
            if (xp < required) break;
            xp -= required;
            level++;
            onLevelUp(player, job, level);
        }

        if (level >= levels.maxLevel()) xp = 0;
        database.setProgress(player.getUniqueId(), job, level, xp);
        return new JobProgress(level, xp);
    }

    private void onLevelUp(Player player, JobType job, int newLevel) {
        String msg = plugin.message("level-up")
                .replace("%level%", String.valueOf(newLevel))
                .replace("%job%", job.displayName());
        player.sendMessage(Colors.color(plugin.prefix() + msg));

        if (plugin.getConfig().getBoolean("settings.levelup-sound", true)) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        }

        Map<Integer, Integer> milestones = plugin.fateMilestones();
        int reward = milestones.getOrDefault(newLevel, 0);
        if (reward > 0 && database.claimFateMilestoneReward(player.getUniqueId(), job, newLevel, reward)) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("fate-earned")
                    .replace("%amount%", String.valueOf(reward))));
        }
    }

    public void setLevel(Player player, JobType job, int level) {
        int safe = Math.max(1, Math.min(levels.maxLevel(), level));
        database.setProgress(player.getUniqueId(), job, safe, 0);
    }
}
