package store.cadera.cdrjobs.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import store.cadera.cdrjobs.gui.JobsMenu;
import store.cadera.cdrjobs.model.MinerSkill;
import store.cadera.cdrjobs.service.SkillService;

public final class MenuListener implements Listener {
    private final JobsMenu menu;
    private final SkillService skills;

    public MenuListener(JobsMenu menu, SkillService skills) {
        this.menu = menu;
        this.skills = skills;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(JobsMenu.MAIN_TITLE) && !title.equals(JobsMenu.MINER_TITLE) && !title.equals(JobsMenu.TRIALS_TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(menu.actionKey(), PersistentDataType.STRING);
        if (action == null) return;

        switch (action) {
            case "open_miner", "back_miner" -> menu.openMiner(player);
            case "open_trials" -> menu.openTrials(player);
            case "back_main" -> menu.openMain(player);
            case "upgrade_skill" -> {
                String skillId = meta.getPersistentDataContainer().get(menu.skillKey(), PersistentDataType.STRING);
                if (skillId == null) return;
                try {
                    MinerSkill skill = MinerSkill.valueOf(skillId);
                    skills.tryUpgrade(player, skill);
                    menu.openMiner(player);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}
