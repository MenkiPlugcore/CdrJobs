package store.cadera.cdrjobs.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.model.MinerSkill;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.MinerAbilityService;
import store.cadera.cdrjobs.service.MinerTrialService;
import store.cadera.cdrjobs.service.ProgressionService;
import store.cadera.cdrjobs.service.SkillService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MinerListener implements Listener {
    private final CdrJobsPlugin plugin;
    private final Database database;
    private final ProgressionService progression;
    private final SkillService skills;
    private final LevelService levels;
    private final MinerTrialService trials;
    private final MinerAbilityService abilities;
    private Map<Material, Integer> oreXp;
    private final Map<UUID, Streak> streaks = new HashMap<>();

    public MinerListener(CdrJobsPlugin plugin, Database database, ProgressionService progression, SkillService skills,
                         LevelService levels, MinerTrialService trials, MinerAbilityService abilities) {
        this.plugin = plugin;
        this.database = database;
        this.progression = progression;
        this.skills = skills;
        this.levels = levels;
        this.trials = trials;
        this.abilities = abilities;
        this.oreXp = plugin.loadMinerXp();
    }

    public void refreshXpMap() {
        this.oreXp = plugin.loadMinerXp();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("miner.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("anti-exploit.placed-ore-tracking", true)) return;
        if (oreXp.containsKey(event.getBlockPlaced().getType())) {
            database.markPlacedOre(event.getBlockPlaced().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("miner.enabled", true)) return;

        Block block = event.getBlock();
        Integer base = oreXp.get(block.getType());
        if (base == null) return;

        if (plugin.getConfig().getBoolean("anti-exploit.placed-ore-tracking", true)
                && database.consumePlacedOre(block.getLocation())) {
            return;
        }

        Player player = event.getPlayer();
        trials.onNaturalOreMined(player, block);

        long xp = skills.applyMinerXpModifiers(player, base);
        xp = abilities.applyXpBoost(player, xp);
        JobProgress progress = progression.addXp(player, JobType.MINER, xp);

        updateEchoStreak(player);

        if (plugin.getConfig().getBoolean("settings.actionbar-xp", true)) {
            long required = levels.xpRequiredForNextLevel(progress.level());
            String text = progress.level() >= levels.maxLevel()
                    ? "✦ Runebound Delver Lv.100 • MAX"
                    : "✦ Runebound +" + xp + " XP • Lv." + progress.level() + " • " + progress.xp() + "/" + required;
            player.sendActionBar(Component.text(text));
        }
    }

    private void updateEchoStreak(Player player) {
        int rank = database.getSkillRank(player.getUniqueId(), MinerSkill.ECHO_OF_DEPTH);
        if (rank <= 0) return;

        long now = System.currentTimeMillis();
        Streak old = streaks.get(player.getUniqueId());
        int count = old == null || now - old.lastMineAt > 60_000L ? 1 : old.count + 1;
        streaks.put(player.getUniqueId(), new Streak(count, now));

        int trigger = Math.max(10, 25 - (rank - 1) * 5);
        if (count >= trigger) {
            int amplifier = rank >= 3 ? 1 : 0;
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 8, amplifier, true, false, true));
            streaks.put(player.getUniqueId(), new Streak(0, now));
        }
    }

    private record Streak(int count, long lastMineAt) {}
}
