package store.cadera.cdrjobs.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import store.cadera.cdrjobs.gui.JobsMenu;
import store.cadera.cdrjobs.model.FarmerSkill;
import store.cadera.cdrjobs.model.MinerSkill;
import store.cadera.cdrjobs.service.FarmerService;
import store.cadera.cdrjobs.service.SkillService;

public final class MenuListener implements Listener {
    private final JobsMenu menu; private final SkillService minerSkills; private final FarmerService farmer;
    public MenuListener(JobsMenu menu, SkillService minerSkills, FarmerService farmer){this.menu=menu;this.minerSkills=minerSkills;this.farmer=farmer;}
    @EventHandler public void onClick(InventoryClickEvent event){
        String t=event.getView().getTitle();
        if(!t.equals(JobsMenu.MAIN_TITLE)&&!t.equals(JobsMenu.MINER_TITLE)&&!t.equals(JobsMenu.TRIALS_TITLE)&&!t.equals(JobsMenu.FARMER_TITLE)&&!t.equals(JobsMenu.FARMER_TRIALS_TITLE))return;
        event.setCancelled(true); if(!(event.getWhoClicked() instanceof Player p))return; ItemStack clicked=event.getCurrentItem(); if(clicked==null||!clicked.hasItemMeta())return; ItemMeta meta=clicked.getItemMeta(); String action=meta.getPersistentDataContainer().get(menu.actionKey(),PersistentDataType.STRING); if(action==null)return;
        switch(action){
            case "open_miner","back_miner"->menu.openMiner(p); case "open_miner_trials"->menu.openTrials(p);
            case "open_farmer","back_farmer"->menu.openFarmer(p); case "open_farmer_trials"->menu.openFarmerTrials(p); case "back_main"->menu.openMain(p);
            case "upgrade_miner_skill"->{String id=meta.getPersistentDataContainer().get(menu.skillKey(),PersistentDataType.STRING);if(id!=null)try{minerSkills.tryUpgrade(p,MinerSkill.valueOf(id));menu.openMiner(p);}catch(IllegalArgumentException ignored){}}
            case "upgrade_farmer_skill"->{String id=meta.getPersistentDataContainer().get(menu.skillKey(),PersistentDataType.STRING);if(id!=null)try{farmer.tryUpgrade(p,FarmerSkill.valueOf(id));menu.openFarmer(p);}catch(IllegalArgumentException ignored){}}
        }
    }
}
