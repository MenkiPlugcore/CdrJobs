package store.cadera.cdrjobs.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.api.ProfessionActionType;
import store.cadera.cdrjobs.api.event.ProfessionActionEvent;
import store.cadera.cdrjobs.api.event.TrialCompleteEvent;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.service.FisherService;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.ProgressionService;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FisherListener implements Listener {
    private static final Set<Material> TREASURE = EnumSet.of(
            Material.BOW, Material.ENCHANTED_BOOK, Material.FISHING_ROD,
            Material.NAME_TAG, Material.NAUTILUS_SHELL, Material.SADDLE
    );

    private final CdrJobsPlugin plugin;
    private final ProgressionService progression;
    private final FisherService fisher;
    private final LevelService levels;
    private final Map<UUID, MoveState> movement = new ConcurrentHashMap<>();
    private Map<Material, Integer> xp = new HashMap<>();

    public FisherListener(CdrJobsPlugin plugin, ProgressionService progression, FisherService fisher, LevelService levels) {
        this.plugin = plugin;
        this.progression = progression;
        this.fisher = fisher;
        this.levels = levels;
        refreshXpMap();
    }

    public void refreshXpMap() { xp = plugin.loadActivityXp("fisher.xp"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void move(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        MoveState state = movement.get(event.getPlayer().getUniqueId());
        if (state == null || !state.anchor().getWorld().equals(event.getTo().getWorld())
                || state.anchor().distanceSquared(event.getTo()) >= 4D) {
            movement.put(event.getPlayer().getUniqueId(), new MoveState(event.getTo(), System.currentTimeMillis()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void fish(PlayerFishEvent event) {
        if (!plugin.getConfig().getBoolean("fisher.enabled", true) || event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        if (afk(player)) {
            player.sendActionBar(Component.text("✦ Tidebound • move around to resume profession rewards"));
            return;
        }
        if (!(event.getCaught() instanceof Item item)) return;

        ItemStack stack = item.getItemStack();
        Material material = stack.getType();
        boolean treasure = TREASURE.contains(material);
        Integer base = xp.get(material);
        if (base == null) {
            if (!treasure) return;
            base = plugin.getConfig().getInt("fisher.treasure-xp", 20);
        }
        boolean ocean = player.getLocation().getBlock().getBiome().name().contains("OCEAN");

        boolean tideBefore = fisher.tideComplete(player);
        boolean abyssBefore = fisher.abyssComplete(player);
        fisher.recordCatch(player, treasure, ocean);
        if (!tideBefore && fisher.tideComplete(player)) {
            plugin.getServer().getPluginManager().callEvent(new TrialCompleteEvent(player, JobType.FISHER, "trial_of_tide"));
        }
        if (!abyssBefore && fisher.abyssComplete(player)) {
            plugin.getServer().getPluginManager().callEvent(new TrialCompleteEvent(player, JobType.FISHER, "trial_of_abyss"));
        }

        plugin.getServer().getPluginManager().callEvent(new ProfessionActionEvent(player, JobType.FISHER, ProfessionActionType.CATCH_FISH, 1));
        long amount = fisher.applyXp(player,
                Math.max(1L, Math.round(base * Math.max(0D, plugin.getConfig().getDouble("fisher.xp-multiplier", 1D)))),
                ocean, treasure);
        JobProgress progress = progression.addXp(player, JobType.FISHER, amount);
        if (plugin.getConfig().getBoolean("settings.actionbar-xp", true)) {
            long required = levels.xpRequiredForNextLevel(progress.level());
            player.sendActionBar(Component.text(progress.level() >= levels.maxLevel()
                    ? "✦ Tidebound Angler Lv." + levels.maxLevel() + " • MAX"
                    : "✦ Tidebound +" + amount + " XP • Lv." + progress.level() + " • " + progress.xp() + "/" + required));
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) { movement.remove(event.getPlayer().getUniqueId()); }

    private boolean afk(Player player) {
        long now = System.currentTimeMillis();
        MoveState state = movement.computeIfAbsent(player.getUniqueId(), ignored -> new MoveState(player.getLocation(), now));
        long threshold = Math.max(60L, plugin.getConfig().getLong("fisher.anti-exploit.afk-after-seconds", 600L)) * 1000L;
        return now - state.lastMoved() >= threshold;
    }

    private record MoveState(Location anchor, long lastMoved) {}
}
