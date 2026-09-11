package store.cadera.cdrjobs.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.api.event.ProfessionAwakeningEvent;
import store.cadera.cdrjobs.api.event.SkillUpgradeEvent;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.gui.JobsMenu;
import store.cadera.cdrjobs.model.*;
import store.cadera.cdrjobs.service.*;

import java.util.logging.Level;

public final class MenuListener implements Listener {
    private final CdrJobsPlugin plugin;
    private final Database database;
    private final ProfessionStore store;
    private final JobsMenu menu;
    private final SkillService miner;
    private final FarmerService farmer;
    private final HunterService hunter;
    private final LumberjackService lumber;
    private final FisherService fisher;

    public MenuListener(CdrJobsPlugin plugin, Database database, ProfessionStore store,
                        JobsMenu menu, SkillService miner, FarmerService farmer, HunterService hunter,
                        LumberjackService lumber, FisherService fisher) {
        this.plugin = plugin;
        this.database = database;
        this.store = store;
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
                case "upgrade_miner_skill" -> {
                    MinerSkill skill = MinerSkill.valueOf(id);
                    if (miner.tryUpgrade(player, skill)) {
                        int rank = database.getSkillRank(player.getUniqueId(), skill);
                        emitSkillUpgrade(player, JobType.MINER, skill.name(), rank, skill.essenceCost(), skill == MinerSkill.HEART_OF_MOUNTAIN);
                    }
                    menu.openMiner(player);
                }
                case "upgrade_farmer_skill" -> {
                    FarmerSkill skill = FarmerSkill.valueOf(id);
                    if (farmer.tryUpgrade(player, skill)) {
                        int rank = store.getSkillRank(player.getUniqueId(), skill.key());
                        emitSkillUpgrade(player, JobType.FARMER, skill.key(), rank, skill.essenceCost(), skill == FarmerSkill.VERDANT_DOMINION);
                    }
                    menu.openFarmer(player);
                }
                case "upgrade_hunter_skill" -> {
                    HunterSkill skill = HunterSkill.valueOf(id);
                    if (hunter.tryUpgrade(player, skill)) {
                        int rank = store.getSkillRank(player.getUniqueId(), skill.key());
                        emitSkillUpgrade(player, JobType.HUNTER, skill.key(), rank, skill.essenceCost(), skill == HunterSkill.APEX_PREDATOR);
                    }
                    menu.openHunter(player);
                }
                case "upgrade_lumber_skill" -> {
                    LumberjackSkill skill = LumberjackSkill.valueOf(id);
                    if (lumber.tryUpgrade(player, skill)) {
                        int rank = store.getSkillRank(player.getUniqueId(), skill.key());
                        emitSkillUpgrade(player, JobType.LUMBERJACK, skill.key(), rank, skill.essenceCost(), skill == LumberjackSkill.OATH_YGGDRASIL);
                    }
                    menu.openLumber(player);
                }
                case "upgrade_fisher_skill" -> {
                    FisherSkill skill = FisherSkill.valueOf(id);
                    if (fisher.tryUpgrade(player, skill)) {
                        int rank = store.getSkillRank(player.getUniqueId(), skill.key());
                        emitSkillUpgrade(player, JobType.FISHER, skill.key(), rank, skill.essenceCost(), skill == FisherSkill.KING_OF_TIDES);
                    }
                    menu.openFisher(player);
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING,
                    "GUI action failed for player=" + player.getName() + " action=" + action + " id=" + id,
                    exception);
            player.sendMessage("§cCdrJobs tidak dapat memproses menu ini. Error sudah dicatat di console.");
        }
    }

    private void emitSkillUpgrade(Player player, JobType job, String skillId, int rank, int cost, boolean awakening) {
        plugin.getServer().getPluginManager().callEvent(new SkillUpgradeEvent(player, job, skillId, rank, cost));
        if (awakening && rank > 0) {
            plugin.getServer().getPluginManager().callEvent(new ProfessionAwakeningEvent(player, job, skillId));
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
