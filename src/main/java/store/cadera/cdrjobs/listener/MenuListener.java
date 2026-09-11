package store.cadera.cdrjobs.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import store.cadera.cdrjobs.gui.JobsMenu;
import store.cadera.cdrjobs.model.*;
import store.cadera.cdrjobs.service.*;

public final class MenuListener implements Listener {
    private final JobsMenu menu;
    private final SkillService miner;
    private final FarmerService farmer;
    private final HunterService hunter;
    private final LumberjackService lumber;
    private final FisherService fisher;

    public MenuListener(JobsMenu menu, SkillService miner, FarmerService farmer, HunterService hunter,
                        LumberjackService lumber, FisherService fisher) {
        this.menu = menu;
        this.miner = miner;
        this.farmer = farmer;
        this.hunter = hunter;
        this.lumber = lumber;
        this.fisher = fisher;
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!isCdrJobsMenu(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(menu.actionKey(), PersistentDataType.STRING);
        String id = meta.getPersistentDataContainer().get(menu.skillKey(), PersistentDataType.STRING);
        if (action == null) return;
        try {
            switch (action) {
                case "open_profile" -> menu.openProfile(player, player);
                case "open_miner", "back_miner" -> menu.openMiner(player);
                case "open_miner_trials" -> menu.openTrials(player);
                case "open_farmer", "back_farmer" -> menu.openFarmer(player);
                case "open_farmer_trials" -> menu.openFarmerTrials(player);
                case "open_hunter", "back_hunter" -> menu.openHunter(player);
                case "open_hunter_trials" -> menu.openHunterTrials(player);
                case "open_lumber", "back_lumber" -> menu.openLumber(player);
                case "open_lumber_trials" -> menu.openLumberTrials(player);
                case "open_fisher", "back_fisher" -> menu.openFisher(player);
                case "open_fisher_trials" -> menu.openFisherTrials(player);
                case "back_main" -> menu.openMain(player);
                case "upgrade_miner_skill" -> { miner.tryUpgrade(player, MinerSkill.valueOf(id)); menu.openMiner(player); }
                case "upgrade_farmer_skill" -> { farmer.tryUpgrade(player, FarmerSkill.valueOf(id)); menu.openFarmer(player); }
                case "upgrade_hunter_skill" -> { hunter.tryUpgrade(player, HunterSkill.valueOf(id)); menu.openHunter(player); }
                case "upgrade_lumber_skill" -> { lumber.tryUpgrade(player, LumberjackSkill.valueOf(id)); menu.openLumber(player); }
                case "upgrade_fisher_skill" -> { fisher.tryUpgrade(player, FisherSkill.valueOf(id)); menu.openFisher(player); }
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isCdrJobsMenu(String title) {
        return title.equals(JobsMenu.MAIN_TITLE)
                || title.startsWith(JobsMenu.PROFILE_TITLE_PREFIX)
                || title.equals(JobsMenu.MINER_TITLE)
                || title.equals(JobsMenu.TRIALS_TITLE)
                || title.equals(JobsMenu.FARMER_TITLE)
                || title.equals(JobsMenu.FARMER_TRIALS_TITLE)
                || title.equals(JobsMenu.HUNTER_TITLE)
                || title.equals(JobsMenu.HUNTER_TRIALS_TITLE)
                || title.equals(JobsMenu.LUMBER_TITLE)
                || title.equals(JobsMenu.LUMBER_TRIALS_TITLE)
                || title.equals(JobsMenu.FISHER_TITLE)
                || title.equals(JobsMenu.FISHER_TRIALS_TITLE);
    }
}
