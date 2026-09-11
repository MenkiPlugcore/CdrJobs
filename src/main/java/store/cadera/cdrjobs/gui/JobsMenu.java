package store.cadera.cdrjobs.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.*;
import store.cadera.cdrjobs.service.FarmerService;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.MinerTrialService;
import store.cadera.cdrjobs.util.JobRanks;

import java.util.ArrayList;
import java.util.List;

public final class JobsMenu {
    public static final String MAIN_TITLE = ChatColor.DARK_AQUA + "CdrJobs • Path of Destiny";
    public static final String MINER_TITLE = ChatColor.DARK_GRAY + "Path of Ascension • Miner";
    public static final String TRIALS_TITLE = ChatColor.DARK_PURPLE + "Runebound • Profession Trials";
    public static final String FARMER_TITLE = ChatColor.DARK_GREEN + "Path of Ascension • Farmer";
    public static final String FARMER_TRIALS_TITLE = ChatColor.GREEN + "Verdant • Profession Trials";

    private final CdrJobsPlugin plugin;
    private final Database database;
    private final ProfessionStore store;
    private final LevelService levels;
    private final MinerTrialService minerTrials;
    private final FarmerService farmer;
    private final NamespacedKey actionKey;
    private final NamespacedKey skillKey;

    public JobsMenu(CdrJobsPlugin plugin, Database database, ProfessionStore store, LevelService levels, MinerTrialService minerTrials, FarmerService farmer) {
        this.plugin = plugin; this.database = database; this.store = store; this.levels = levels; this.minerTrials = minerTrials; this.farmer = farmer;
        this.actionKey = new NamespacedKey(plugin, "menu_action"); this.skillKey = new NamespacedKey(plugin, "skill_id");
    }
    public NamespacedKey actionKey() { return actionKey; }
    public NamespacedKey skillKey() { return skillKey; }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_TITLE);
        inv.setItem(4, item(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "✦ Fate Essence: " + database.getFateEssence(player.getUniqueId()),
                List.of(ChatColor.GRAY + "Global untuk seluruh profession.", ChatColor.GRAY + "Tidak semua path bisa dimaksimalkan."), null, null));
        int[] slots = {10,12,14,16,22};
        JobType[] jobs = JobType.values();
        for (int i=0;i<jobs.length;i++) {
            JobType job = jobs[i];
            JobProgress p = database.getProgress(player.getUniqueId(), job);
            if (!job.released()) {
                inv.setItem(slots[i], item(job.icon(), ChatColor.DARK_GRAY + job.displayName(), List.of(ChatColor.GRAY + "Coming in a future CdrJobs chapter."), null, null));
                continue;
            }
            long req = levels.xpRequiredForNextLevel(p.level());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + JobRanks.title(job, p.level()));
            lore.add(ChatColor.WHITE + "Level: " + ChatColor.AQUA + p.level());
            lore.add(ChatColor.WHITE + "XP: " + ChatColor.AQUA + (p.level() >= levels.maxLevel() ? "MAX" : p.xp()+"/"+req));
            lore.add(""); lore.add(ChatColor.YELLOW + "Klik untuk membuka Path of Ascension.");
            String action = job == JobType.MINER ? "open_miner" : "open_farmer";
            inv.setItem(slots[i], item(job.icon(), ChatColor.AQUA + "✦ " + job.displayName(), lore, action, null));
        }
        player.openInventory(inv);
    }

    public void openMiner(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MINER_TITLE);
        JobProgress progress = database.getProgress(player.getUniqueId(), JobType.MINER);
        inv.setItem(4, item(Material.DIAMOND_PICKAXE, ChatColor.AQUA + "Runebound Delver", List.of(ChatColor.GRAY + JobRanks.title(JobType.MINER, progress.level()), ChatColor.WHITE+"Level: "+ChatColor.AQUA+progress.level(), ChatColor.LIGHT_PURPLE+"Fate Essence: "+database.getFateEssence(player.getUniqueId())), null, null));
        int[] slots={10,19,21,23,28,30,49};
        MinerSkill[] list={MinerSkill.STONEWHISPER,MinerSkill.RUNEBREAKER,MinerSkill.DEEPBORN,MinerSkill.RUNIC_SURGE,MinerSkill.GEMSEEKER,MinerSkill.ECHO_OF_DEPTH,MinerSkill.HEART_OF_MOUNTAIN};
        for(int i=0;i<list.length;i++){
            MinerSkill skill=list[i]; int rank=database.getSkillRank(player.getUniqueId(),skill);
            List<String> lore=new ArrayList<>(); lore.add(ChatColor.GRAY+skill.description()); lore.add(""); lore.add(ChatColor.WHITE+"Rank: "+ChatColor.AQUA+rank+"/"+skill.maxRank()); lore.add(ChatColor.WHITE+"Requires Lv."+skill.requiredLevel()); lore.add(ChatColor.LIGHT_PURPLE+"Cost: "+skill.essenceCost()+" Fate Essence");
            if(skill.prerequisite()!=null) lore.add(ChatColor.GRAY+"Requires "+skill.prerequisite().displayName()+" Rank "+skill.prerequisiteRank());
            if(skill==MinerSkill.RUNIC_SURGE) lore.add((minerTrials.isStoneComplete(player)?ChatColor.GREEN:ChatColor.RED)+"Requires Trial of Stone");
            if(skill==MinerSkill.HEART_OF_MOUNTAIN){ lore.add(ChatColor.GRAY+"Requires Gemseeker III + Echo III"); lore.add((minerTrials.isDeepComplete(player)?ChatColor.GREEN:ChatColor.RED)+"Requires Trial of the Deep"); }
            lore.add(""); lore.add(rank>=skill.maxRank()?ChatColor.GREEN+"MAXIMUM RANK":ChatColor.YELLOW+"Klik untuk upgrade.");
            inv.setItem(slots[i],item(skill.icon(),(rank>0?ChatColor.AQUA:ChatColor.GRAY)+"✦ "+skill.displayName(),lore,"upgrade_miner_skill",skill.name()));
        }
        inv.setItem(45,item(Material.ECHO_SHARD,ChatColor.LIGHT_PURPLE+"✦ Profession Trials",List.of(ChatColor.YELLOW+"Klik untuk melihat progress."),"open_miner_trials",null));
        inv.setItem(53,item(Material.ARROW,ChatColor.YELLOW+"Kembali",List.of(),"back_main",null)); player.openInventory(inv);
    }

    public void openTrials(Player player) {
        Inventory inv=Bukkit.createInventory(null,27,TRIALS_TITLE); MinerTrialProgress p=minerTrials.progress(player);
        inv.setItem(11,item(Material.IRON_PICKAXE,ChatColor.AQUA+"✦ Trial of Stone",List.of(ChatColor.WHITE+"Natural ores: "+p.totalOres()+"/"+minerTrials.stoneTarget(), p.stoneComplete()?ChatColor.GREEN+"✓ COMPLETED":ChatColor.YELLOW+"Required for Runic Surge"),null,null));
        inv.setItem(15,item(Material.SCULK_CATALYST,ChatColor.DARK_PURPLE+"✦ Trial of the Deep",List.of(ChatColor.WHITE+"Deep ores: "+p.deepOres()+"/"+minerTrials.deepTarget(),ChatColor.WHITE+"Rare ores: "+p.rareOres()+"/"+minerTrials.rareTarget(),ChatColor.WHITE+"Ancient Debris: "+p.ancientDebris()+"/"+minerTrials.ancientTarget(),p.deepComplete()?ChatColor.GREEN+"✓ COMPLETED":ChatColor.YELLOW+"Required for Heart of the Mountain"),null,null));
        inv.setItem(22,item(Material.ARROW,ChatColor.YELLOW+"Kembali",List.of(),"back_miner",null)); player.openInventory(inv);
    }

    public void openFarmer(Player player) {
        Inventory inv=Bukkit.createInventory(null,54,FARMER_TITLE); JobProgress p=database.getProgress(player.getUniqueId(),JobType.FARMER);
        inv.setItem(4,item(Material.GOLDEN_HOE,ChatColor.GREEN+"Verdant Keeper",List.of(ChatColor.GRAY+JobRanks.title(JobType.FARMER,p.level()),ChatColor.WHITE+"Level: "+ChatColor.GREEN+p.level(),ChatColor.LIGHT_PURPLE+"Fate Essence: "+database.getFateEssence(player.getUniqueId())),null,null));
        int[] slots={10,19,21,23,28,30,49}; FarmerSkill[] list=FarmerSkill.values();
        for(int i=0;i<list.length;i++){
            FarmerSkill skill=list[i]; int rank=store.getSkillRank(player.getUniqueId(),skill.key());
            List<String> lore=new ArrayList<>(); lore.add(ChatColor.GRAY+skill.description()); lore.add(""); lore.add(ChatColor.WHITE+"Rank: "+ChatColor.GREEN+rank+"/"+skill.maxRank()); lore.add(ChatColor.WHITE+"Requires Lv."+skill.requiredLevel()); lore.add(ChatColor.LIGHT_PURPLE+"Cost: "+skill.essenceCost()+" Fate Essence");
            if(skill.prerequisite()!=null) lore.add(ChatColor.GRAY+"Requires "+skill.prerequisite().displayName()+" Rank "+skill.prerequisiteRank());
            if(skill==FarmerSkill.VERDANT_BLOOM) lore.add((farmer.seedTrialComplete(player)?ChatColor.GREEN:ChatColor.RED)+"Requires Trial of Seed");
            if(skill==FarmerSkill.VERDANT_DOMINION){ lore.add(ChatColor.GRAY+"Requires Blessing of Gaia III + Spirit III"); lore.add((farmer.gaiaTrialComplete(player)?ChatColor.GREEN:ChatColor.RED)+"Requires Trial of Gaia"); }
            lore.add(""); lore.add(rank>=skill.maxRank()?ChatColor.GREEN+"MAXIMUM RANK":ChatColor.YELLOW+"Klik untuk upgrade.");
            inv.setItem(slots[i],item(skill.icon(),(rank>0?ChatColor.GREEN:ChatColor.GRAY)+"✦ "+skill.displayName(),lore,"upgrade_farmer_skill",skill.name()));
        }
        inv.setItem(45,item(Material.FLOWERING_AZALEA,ChatColor.GREEN+"✦ Profession Trials",List.of(ChatColor.YELLOW+"Klik untuk melihat progress."),"open_farmer_trials",null));
        inv.setItem(53,item(Material.ARROW,ChatColor.YELLOW+"Kembali",List.of(),"back_main",null)); player.openInventory(inv);
    }

    public void openFarmerTrials(Player player) {
        Inventory inv=Bukkit.createInventory(null,27,FARMER_TRIALS_TITLE);
        inv.setItem(11,item(Material.WHEAT,ChatColor.GREEN+"✦ Trial of Seed",List.of(ChatColor.WHITE+"Mature harvests: "+farmer.harvests(player)+"/"+farmer.seedTarget(),farmer.seedTrialComplete(player)?ChatColor.GREEN+"✓ COMPLETED":ChatColor.YELLOW+"Required for Verdant Bloom"),null,null));
        inv.setItem(15,item(Material.ENCHANTED_GOLDEN_APPLE,ChatColor.DARK_GREEN+"✦ Trial of Gaia",List.of(ChatColor.WHITE+"Harvests: "+farmer.harvests(player)+"/"+farmer.gaiaHarvestTarget(),ChatColor.WHITE+"Rare harvests: "+farmer.rareHarvests(player)+"/"+farmer.gaiaRareTarget(),farmer.gaiaTrialComplete(player)?ChatColor.GREEN+"✓ COMPLETED":ChatColor.YELLOW+"Required for Verdant Dominion"),null,null));
        inv.setItem(22,item(Material.ARROW,ChatColor.YELLOW+"Kembali",List.of(),"back_farmer",null)); player.openInventory(inv);
    }

    private ItemStack item(Material material,String name,List<String> lore,String action,String skillId){ ItemStack item=new ItemStack(material); ItemMeta meta=item.getItemMeta(); meta.setDisplayName(name); meta.setLore(lore); meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); if(action!=null)meta.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,action); if(skillId!=null)meta.getPersistentDataContainer().set(skillKey,PersistentDataType.STRING,skillId); item.setItemMeta(meta); return item; }
}
