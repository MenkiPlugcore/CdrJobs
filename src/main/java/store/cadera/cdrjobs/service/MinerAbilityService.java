package store.cadera.cdrjobs.service;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.model.MinerSkill;
import store.cadera.cdrjobs.util.Colors;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MinerAbilityService {
    public static final String RUNIC_SURGE = "runic_surge";

    private final CdrJobsPlugin plugin;
    private final Database database;
    private final Map<UUID, Long> activeUntil = new ConcurrentHashMap<>();

    public MinerAbilityService(CdrJobsPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public boolean activateRunicSurge(Player player) {
        if (database.getSkillRank(player.getUniqueId(), MinerSkill.RUNIC_SURGE) <= 0) {
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("ability-locked")));
            return false;
        }

        long now = System.currentTimeMillis();
        long readyAt = database.getAbilityReadyAt(player.getUniqueId(), RUNIC_SURGE);
        if (readyAt > now) {
            long seconds = Math.max(1, (readyAt - now + 999L) / 1000L);
            player.sendMessage(Colors.color(plugin.prefix() + plugin.message("ability-cooldown")
                    .replace("%seconds%", String.valueOf(seconds))));
            return false;
        }

        int duration = Math.max(1, plugin.getConfig().getInt("miner.abilities.runic-surge.duration-seconds", 20));
        int cooldown = Math.max(duration, plugin.getConfig().getInt("miner.abilities.runic-surge.cooldown-seconds", 300));
        int hasteLevel = Math.max(0, plugin.getConfig().getInt("miner.abilities.runic-surge.haste-level", 1));

        activeUntil.put(player.getUniqueId(), now + duration * 1000L);
        database.setAbilityReadyAt(player.getUniqueId(), RUNIC_SURGE, now + cooldown * 1000L);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration * 20, Math.max(0, hasteLevel - 1), true, false, true));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.8f);
        player.sendMessage(Colors.color(plugin.prefix() + plugin.message("runic-surge-activated")
                .replace("%duration%", String.valueOf(duration))));
        return true;
    }

    public long applyXpBoost(Player player, long xp) {
        Long until = activeUntil.get(player.getUniqueId());
        if (until == null) return xp;
        if (until <= System.currentTimeMillis()) {
            activeUntil.remove(player.getUniqueId());
            return xp;
        }
        double bonus = Math.max(0, plugin.getConfig().getDouble("miner.abilities.runic-surge.xp-bonus-percent", 25.0));
        return Math.max(1, Math.round(xp * (1.0 + bonus / 100.0)));
    }

    public long cooldownSeconds(UUID uuid) {
        long remaining = database.getAbilityReadyAt(uuid, RUNIC_SURGE) - System.currentTimeMillis();
        return remaining <= 0 ? 0 : (remaining + 999L) / 1000L;
    }
}
