package store.cadera.cdrjobs.listener;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.gui.RebirthMenu;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.service.RebirthService;
import store.cadera.cdrjobs.util.Colors;

public final class RebirthListener implements Listener {
    private final CdrJobsPlugin plugin;
    private final RebirthMenu menu;
    private final RebirthService service;

    public RebirthListener(CdrJobsPlugin plugin, RebirthMenu menu, RebirthService service) {
        this.plugin = plugin;
        this.menu = menu;
        this.service = service;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(RebirthMenu.SELECT_TITLE) && !title.startsWith(RebirthMenu.CONFIRM_TITLE_PREFIX)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(menu.actionKey(), PersistentDataType.STRING);
        String rawJob = meta.getPersistentDataContainer().get(menu.jobKey(), PersistentDataType.STRING);
        if (action == null) return;

        JobType job = null;
        if (rawJob != null) {
            try { job = JobType.valueOf(rawJob); }
            catch (IllegalArgumentException ignored) { return; }
        }

        switch (action) {
            case "select" -> {
                if (job != null) menu.openConfirm(player, job);
            }
            case "cancel" -> menu.openSelect(player);
            case "confirm" -> {
                if (job == null) return;
                RebirthService.Outcome outcome = service.execute(player, job);
                switch (outcome.status()) {
                    case SUCCESS -> {
                        player.closeInventory();
                        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.2f);
                        player.sendMessage(Colors.color(plugin.prefix() + plugin.message("rebirth-success")
                                .replace("%job%", job.displayName())
                                .replace("%refund%", String.valueOf(outcome.refundEssence()))));
                    }
                    case NO_SKILLS -> player.sendMessage(Colors.color(plugin.prefix() + plugin.message("rebirth-no-skills")));
                    case COOLDOWN -> player.sendMessage(Colors.color(plugin.prefix() + plugin.message("rebirth-cooldown")
                            .replace("%seconds%", String.valueOf(outcome.cooldownRemainingSeconds()))));
                    case NOT_ENOUGH_FATE -> player.sendMessage(Colors.color(plugin.prefix() + plugin.message("rebirth-not-enough-fate")));
                    case VAULT_UNAVAILABLE -> player.sendMessage(Colors.color(plugin.prefix() + plugin.message("rebirth-vault-unavailable")));
                    case NOT_ENOUGH_VAULT -> player.sendMessage(Colors.color(plugin.prefix() + plugin.message("rebirth-not-enough-vault")));
                    case VAULT_TRANSACTION_FAILED, ERROR -> player.sendMessage(Colors.color(plugin.prefix() + plugin.message("rebirth-error")));
                    case DISABLED -> player.sendMessage(Colors.color(plugin.prefix() + plugin.message("rebirth-disabled")));
                }
                if (outcome.status() != RebirthService.Status.SUCCESS) menu.openConfirm(player, job);
            }
        }
    }
}
