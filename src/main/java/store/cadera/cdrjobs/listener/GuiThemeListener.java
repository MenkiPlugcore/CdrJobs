package store.cadera.cdrjobs.listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import store.cadera.cdrjobs.gui.JobsMenu;
import store.cadera.cdrjobs.gui.RebirthMenu;

public final class GuiThemeListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onOpen(InventoryOpenEvent event) {
        String title = event.getView().getTitle();
        Theme theme = theme(title);
        if (theme == null) return;
        decorate(event.getInventory(), theme);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (theme(event.getView().getTitle()) == null) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            event.setCancelled(true);
        }
    }

    private void decorate(Inventory inventory, Theme theme) {
        int size = inventory.getSize();
        if (size <= 0 || size % 9 != 0) return;
        int rows = size / 9;
        for (int slot = 0; slot < size; slot++) {
            ItemStack current = inventory.getItem(slot);
            if (current != null && !current.getType().isAir()) continue;
            int row = slot / 9;
            int col = slot % 9;
            boolean edge = row == 0 || row == rows - 1 || col == 0 || col == 8;
            boolean accent = !edge && ((row + col) % 5 == 0);
            inventory.setItem(slot, pane(edge ? theme.border() : accent ? theme.accent() : theme.fill()));
        }
    }

    private ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + " ");
        item.setItemMeta(meta);
        return item;
    }

    private Theme theme(String title) {
        if (title.equals(JobsMenu.MAIN_TITLE) || title.startsWith(JobsMenu.PROFILE_TITLE_PREFIX)) {
            return new Theme(Material.BLACK_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        }
        if (title.equals(JobsMenu.MINER_TITLE) || title.equals(JobsMenu.TRIALS_TITLE)) {
            return new Theme(Material.BLACK_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE);
        }
        if (title.equals(JobsMenu.FARMER_TITLE) || title.equals(JobsMenu.FARMER_TRIALS_TITLE)) {
            return new Theme(Material.BLACK_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE);
        }
        if (title.equals(JobsMenu.HUNTER_TITLE) || title.equals(JobsMenu.HUNTER_TRIALS_TITLE)) {
            return new Theme(Material.BLACK_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE);
        }
        if (title.equals(JobsMenu.LUMBER_TITLE) || title.equals(JobsMenu.LUMBER_TRIALS_TITLE)) {
            return new Theme(Material.BLACK_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE);
        }
        if (title.equals(JobsMenu.FISHER_TITLE) || title.equals(JobsMenu.FISHER_TRIALS_TITLE)) {
            return new Theme(Material.BLACK_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE);
        }
        if (title.equals(RebirthMenu.SELECT_TITLE)) {
            return new Theme(Material.BLACK_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE);
        }
        if (title.startsWith(RebirthMenu.CONFIRM_TITLE_PREFIX)) {
            return new Theme(Material.BLACK_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE);
        }
        return null;
    }

    private record Theme(Material border, Material fill, Material accent) {}
}
