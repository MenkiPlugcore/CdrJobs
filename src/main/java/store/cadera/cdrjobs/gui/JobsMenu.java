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
import store.cadera.cdrjobs.service.*;
import store.cadera.cdrjobs.util.JobRanks;

import java.util.ArrayList;
import java.util.List;

public final class JobsMenu {
    public static final String MAIN_TITLE = ChatColor.DARK_AQUA + "CdrJobs • Path of Destiny";
    public static final String PROFILE_TITLE_PREFIX = ChatColor.DARK_AQUA + "CdrJobs • Profile • ";
    public static final String MINER_TITLE = ChatColor.DARK_GRAY + "Path • Miner";
    public static final String TRIALS_TITLE = ChatColor.DARK_PURPLE + "Runebound • Trials";
    public static final String FARMER_TITLE = ChatColor.DARK_GREEN + "Path • Farmer";
    public static final String FARMER_TRIALS_TITLE = ChatColor.GREEN + "Verdant • Trials";
    public static final String HUNTER_TITLE = ChatColor.DARK_RED + "Path • Hunter";
    public static final String HUNTER_TRIALS_TITLE = ChatColor.RED + "Bloodfang • Trials";
    public static final String LUMBER_TITLE = ChatColor.GOLD + "Path • Lumberjack";
    public static final String LUMBER_TRIALS_TITLE = ChatColor.DARK_GREEN + "Ironbark • Trials";
    public static final String FISHER_TITLE = ChatColor.BLUE + "Path • Fisher";
    public static final String FISHER_TRIALS_TITLE = ChatColor.DARK_AQUA + "Tidebound • Trials";

    private final Database db;
    private final ProfessionStore store;
    private final LevelService levels;
    private final MinerTrialService mt;
    private final FarmerService farmer;
    private final HunterService hunter;
    private final LumberjackService lumber;
    private final FisherService fisher;
    private final ProfileService profiles;
    private final NamespacedKey actionKey;
    private final NamespacedKey skillKey;

    public JobsMenu(CdrJobsPlugin plugin, Database db, ProfessionStore store, LevelService levels,
                    MinerTrialService mt, FarmerService farmer, HunterService hunter,
                    LumberjackService lumber, FisherService fisher, ProfileService profiles) {
        this.db = db;
        this.store = store;
        this.levels = levels;
        this.mt = mt;
        this.farmer = farmer;
        this.hunter = hunter;
        this.lumber = lumber;
        this.fisher = fisher;
        this.profiles = profiles;
        this.actionKey = new NamespacedKey(plugin, "menu_action");
        this.skillKey = new NamespacedKey(plugin, "skill_id");
    }

    public NamespacedKey actionKey() { return actionKey; }
    public NamespacedKey skillKey() { return skillKey; }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_TITLE);
        inv.setItem(4, item(Material.NETHER_STAR, "§d✦ Fate Essence: " + db.getFateEssence(player.getUniqueId()),
                List.of("§7Global untuk seluruh profession."), null, null));
        int[] slots = {10, 12, 14, 16, 22};
        for (int i = 0; i < JobType.values().length; i++) {
            JobType job = JobType.values()[i];
            JobProgress progress = db.getProgress(player.getUniqueId(), job);
            String action = switch (job) {
                case MINER -> "open_miner";
                case FARMER -> "open_farmer";
                case HUNTER -> "open_hunter";
                case LUMBERJACK -> "open_lumber";
                case FISHER -> "open_fisher";
            };
            inv.setItem(slots[i], item(job.icon(), "§b✦ " + job.displayName(), List.of(
                    "§7" + JobRanks.title(job, progress.level()),
                    "§fLevel: §b" + progress.level(),
                    "§fXP: §b" + (progress.level() >= levels.maxLevel() ? "MAX" : progress.xp() + "/" + levels.xpRequiredForNextLevel(progress.level())),
                    "",
                    "§eKlik untuk Path of Ascension."
            ), action, null));
        }
        inv.setItem(18, item(Material.RECOVERY_COMPASS, "§3✦ Adventurer Profile",
                List.of("§7Ringkasan seluruh Five Paths.", "§eKlik untuk membuka profile."), "open_profile", null));
        player.openInventory(inv);
    }

    public void openProfile(Player viewer, Player target) {
        ProfileService.ProfileSnapshot snapshot = profiles.snapshot(target.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 45, PROFILE_TITLE_PREFIX + trim(target.getName(), 16));
        inv.setItem(4, item(Material.BOOK, "§3✦ " + target.getName(), List.of(
                "§fTotal Profession Level: §b" + snapshot.totalLevel(),
                "§fHighest Path: §b" + snapshot.highestJob().displayName() + " §7(Lv." + snapshot.highestLevel() + ")",
                "§fFate Essence: §d" + snapshot.fateEssence(),
                "§fTrials Completed: §a" + snapshot.completedTrials() + "§7/10",
                "§fSkills Unlocked: §e" + snapshot.unlockedSkills(),
                "§fAwakened Paths: §6" + snapshot.awakenedJobs().size() + "§7/5"
        ), null, null));

        int[] slots = {10, 12, 14, 16, 22};
        for (int i = 0; i < JobType.values().length; i++) {
            JobType job = JobType.values()[i];
            JobProgress progress = snapshot.progress().get(job);
            long activity = snapshot.activities().getOrDefault(job, 0L);
            String activityName = switch (job) {
                case MINER -> "Natural ores";
                case FARMER -> "Harvests";
                case HUNTER -> "Rewarded kills";
                case LUMBERJACK -> "Natural logs";
                case FISHER -> "Catches";
            };
            List<String> lore = new ArrayList<>();
            lore.add("§7" + JobRanks.title(job, progress.level()));
            lore.add("§fLevel: §b" + progress.level());
            lore.add("§fXP: §b" + (progress.level() >= levels.maxLevel() ? "MAX" : progress.xp() + "/" + levels.xpRequiredForNextLevel(progress.level())));
            lore.add("§f" + activityName + ": §e" + activity);
            if (job == JobType.HUNTER) {
                lore.add("§7Mob: §f" + store.getCounter(target.getUniqueId(), JobType.HUNTER.name(), "mob_kills")
                        + " §8• §7PvP: §f" + store.getCounter(target.getUniqueId(), JobType.HUNTER.name(), "pvp_kills"));
            }
            if (snapshot.awakenedJobs().contains(job)) lore.add("§6✦ AWAKENED");
            inv.setItem(slots[i], item(job.icon(), "§b✦ " + job.displayName(), lore, null, null));
        }

        inv.setItem(28, item(Material.AMETHYST_SHARD, "§dFate Essence", List.of("§f" + snapshot.fateEssence()), null, null));
        inv.setItem(30, item(Material.ECHO_SHARD, "§aTrials", List.of("§fCompleted: §a" + snapshot.completedTrials() + "§7/10"), null, null));
        inv.setItem(32, item(Material.ENCHANTED_BOOK, "§eSkills", List.of("§fUnlocked nodes: §e" + snapshot.unlockedSkills()), null, null));
        inv.setItem(34, item(Material.NETHER_STAR, "§6Awakened Paths", List.of("§f" + snapshot.awakenedNames()), null, null));
        inv.setItem(40, back("back_main"));
        viewer.openInventory(inv);
    }

    private String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    public void openMiner(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, MINER_TITLE); header(inv,p,JobType.MINER,Material.DIAMOND_PICKAXE);
        int[] slots={10,19,21,23,28,30,49}; MinerSkill[] skills={MinerSkill.STONEWHISPER,MinerSkill.RUNEBREAKER,MinerSkill.DEEPBORN,MinerSkill.RUNIC_SURGE,MinerSkill.GEMSEEKER,MinerSkill.ECHO_OF_DEPTH,MinerSkill.HEART_OF_MOUNTAIN};
        for(int i=0;i<skills.length;i++){MinerSkill x=skills[i];int r=db.getSkillRank(p.getUniqueId(),x);List<String> l=lore(x.description(),r,x.maxRank(),x.requiredLevel(),x.essenceCost());if(x.prerequisite()!=null)l.add("§7Requires "+x.prerequisite().displayName()+" "+x.prerequisiteRank());if(x==MinerSkill.RUNIC_SURGE)l.add((mt.isStoneComplete(p)?"§a":"§c")+"Trial of Stone");if(x==MinerSkill.HEART_OF_MOUNTAIN)l.add((mt.isDeepComplete(p)?"§a":"§c")+"Trial of the Deep");inv.setItem(slots[i],item(x.icon(),"§b✦ "+x.displayName(),l,"upgrade_miner_skill",x.name()));}
        inv.setItem(45,item(Material.ECHO_SHARD,"§d✦ Trials",List.of("§eLihat progress"),"open_miner_trials",null));inv.setItem(53,back("back_main"));p.openInventory(inv);
    }
    public void openTrials(Player p){Inventory inv=Bukkit.createInventory(null,27,TRIALS_TITLE);MinerTrialProgress x=mt.progress(p);inv.setItem(11,item(Material.IRON_PICKAXE,"§b✦ Trial of Stone",List.of("§fOres: "+x.totalOres()+"/"+mt.stoneTarget(),x.stoneComplete()?"§a✓ COMPLETED":"§eRunic Surge gate"),null,null));inv.setItem(15,item(Material.SCULK_CATALYST,"§5✦ Trial of the Deep",List.of("§fDeep: "+x.deepOres()+"/"+mt.deepTarget(),"§fRare: "+x.rareOres()+"/"+mt.rareTarget(),"§fDebris: "+x.ancientDebris()+"/"+mt.ancientTarget(),x.deepComplete()?"§a✓ COMPLETED":"§eAwakening gate"),null,null));inv.setItem(22,back("back_miner"));p.openInventory(inv);}
    public void openFarmer(Player p){Inventory inv=Bukkit.createInventory(null,54,FARMER_TITLE);header(inv,p,JobType.FARMER,Material.GOLDEN_HOE);int[]s={10,19,21,23,28,30,49};FarmerSkill[]a=FarmerSkill.values();for(int i=0;i<a.length;i++){FarmerSkill x=a[i];int r=store.getSkillRank(p.getUniqueId(),x.key());List<String>l=lore(x.description(),r,x.maxRank(),x.requiredLevel(),x.essenceCost());if(x.prerequisite()!=null)l.add("§7Requires "+x.prerequisite().displayName()+" "+x.prerequisiteRank());if(x==FarmerSkill.VERDANT_BLOOM)l.add((farmer.seedTrialComplete(p)?"§a":"§c")+"Trial of Seed");if(x==FarmerSkill.VERDANT_DOMINION)l.add((farmer.gaiaTrialComplete(p)?"§a":"§c")+"Trial of Gaia");inv.setItem(s[i],item(x.icon(),"§a✦ "+x.displayName(),l,"upgrade_farmer_skill",x.name()));}inv.setItem(45,item(Material.FLOWERING_AZALEA,"§a✦ Trials",List.of("§eLihat progress"),"open_farmer_trials",null));inv.setItem(53,back("back_main"));p.openInventory(inv);}
    public void openFarmerTrials(Player p){Inventory inv=Bukkit.createInventory(null,27,FARMER_TRIALS_TITLE);inv.setItem(11,item(Material.WHEAT,"§a✦ Trial of Seed",List.of("§fHarvests: "+farmer.harvests(p)+"/"+farmer.seedTarget(),farmer.seedTrialComplete(p)?"§a✓ COMPLETED":"§eVerdant Bloom gate"),null,null));inv.setItem(15,item(Material.ENCHANTED_GOLDEN_APPLE,"§2✦ Trial of Gaia",List.of("§fHarvests: "+farmer.harvests(p)+"/"+farmer.gaiaHarvestTarget(),"§fRare: "+farmer.rareHarvests(p)+"/"+farmer.gaiaRareTarget(),farmer.gaiaTrialComplete(p)?"§a✓ COMPLETED":"§eAwakening gate"),null,null));inv.setItem(22,back("back_farmer"));p.openInventory(inv);}
    public void openHunter(Player p){Inventory inv=Bukkit.createInventory(null,54,HUNTER_TITLE);header(inv,p,JobType.HUNTER,Material.IRON_SWORD);int[]s={10,19,21,23,28,30,49};HunterSkill[]a=HunterSkill.values();for(int i=0;i<a.length;i++){HunterSkill x=a[i];int r=store.getSkillRank(p.getUniqueId(),x.key());List<String>l=lore(x.description(),r,x.maxRank(),x.requiredLevel(),x.essenceCost());if(x.prerequisite()!=null)l.add("§7Requires "+x.prerequisite().displayName()+" "+x.prerequisiteRank());if(x==HunterSkill.CRIMSON_HUNT)l.add((hunter.fangComplete(p)?"§a":"§c")+"Trial of Fang");if(x==HunterSkill.APEX_PREDATOR)l.add((hunter.moonComplete(p)?"§a":"§c")+"Trial of Crimson Moon");inv.setItem(s[i],item(x.icon(),"§c✦ "+x.displayName(),l,"upgrade_hunter_skill",x.name()));}inv.setItem(45,item(Material.WITHER_SKELETON_SKULL,"§c✦ Trials",List.of("§eLihat progress"),"open_hunter_trials",null));inv.setItem(53,back("back_main"));p.openInventory(inv);}
    public void openHunterTrials(Player p){Inventory inv=Bukkit.createInventory(null,27,HUNTER_TRIALS_TITLE);inv.setItem(11,item(Material.BONE,"§c✦ Trial of Fang",List.of("§fKills: "+hunter.kills(p)+"/"+hunter.fangTarget(),hunter.fangComplete(p)?"§a✓ COMPLETED":"§eCrimson Hunt gate"),null,null));inv.setItem(15,item(Material.ECHO_SHARD,"§4✦ Crimson Moon",List.of("§fKills: "+hunter.kills(p)+"/"+hunter.moonKillTarget(),"§fNight: "+hunter.nightKills(p)+"/"+hunter.moonNightTarget(),"§fDangerous: "+hunter.dangerousKills(p)+"/"+hunter.moonDangerTarget(),hunter.moonComplete(p)?"§a✓ COMPLETED":"§eAwakening gate"),null,null));inv.setItem(22,back("back_hunter"));p.openInventory(inv);}
    public void openLumber(Player p){Inventory inv=Bukkit.createInventory(null,54,LUMBER_TITLE);header(inv,p,JobType.LUMBERJACK,Material.IRON_AXE);int[]s={10,19,21,23,28,30,49};LumberjackSkill[]a=LumberjackSkill.values();for(int i=0;i<a.length;i++){LumberjackSkill x=a[i];int r=store.getSkillRank(p.getUniqueId(),x.key());List<String>l=lore(x.description(),r,x.maxRank(),x.requiredLevel(),x.essenceCost());if(x.prerequisite()!=null)l.add("§7Requires "+x.prerequisite().displayName()+" "+x.prerequisiteRank());if(x==LumberjackSkill.GROVE_RHYTHM)l.add((lumber.timberComplete(p)?"§a":"§c")+"Trial of Timber");if(x==LumberjackSkill.OATH_YGGDRASIL)l.add((lumber.groveComplete(p)?"§a":"§c")+"Trial of Ancient Grove");inv.setItem(s[i],item(x.icon(),"§6✦ "+x.displayName(),l,"upgrade_lumber_skill",x.name()));}inv.setItem(45,item(Material.OAK_SAPLING,"§6✦ Trials",List.of("§eLihat progress"),"open_lumber_trials",null));inv.setItem(53,back("back_main"));p.openInventory(inv);}
    public void openLumberTrials(Player p){Inventory inv=Bukkit.createInventory(null,27,LUMBER_TRIALS_TITLE);inv.setItem(11,item(Material.OAK_LOG,"§6✦ Trial of Timber",List.of("§fNatural logs: "+lumber.logs(p)+"/"+lumber.timberTarget(),lumber.timberComplete(p)?"§a✓ COMPLETED":"§eGrove Rhythm gate"),null,null));inv.setItem(15,item(Material.TOTEM_OF_UNDYING,"§2✦ Ancient Grove",List.of("§fLogs: "+lumber.logs(p)+"/"+lumber.groveLogTarget(),"§fElder: "+lumber.elderLogs(p)+"/"+lumber.groveElderTarget(),lumber.groveComplete(p)?"§a✓ COMPLETED":"§eAwakening gate"),null,null));inv.setItem(22,back("back_lumber"));p.openInventory(inv);}
    public void openFisher(Player p){Inventory inv=Bukkit.createInventory(null,54,FISHER_TITLE);header(inv,p,JobType.FISHER,Material.FISHING_ROD);int[]s={10,19,21,23,28,30,49};FisherSkill[]a=FisherSkill.values();for(int i=0;i<a.length;i++){FisherSkill x=a[i];int r=store.getSkillRank(p.getUniqueId(),x.key());List<String>l=lore(x.description(),r,x.maxRank(),x.requiredLevel(),x.essenceCost());if(x.prerequisite()!=null)l.add("§7Requires "+x.prerequisite().displayName()+" "+x.prerequisiteRank());if(x==FisherSkill.OCEANS_CALL)l.add((fisher.tideComplete(p)?"§a":"§c")+"Trial of the Tide");if(x==FisherSkill.KING_OF_TIDES)l.add((fisher.abyssComplete(p)?"§a":"§c")+"Trial of the Abyss");inv.setItem(s[i],item(x.icon(),"§9✦ "+x.displayName(),l,"upgrade_fisher_skill",x.name()));}inv.setItem(45,item(Material.HEART_OF_THE_SEA,"§9✦ Trials",List.of("§eLihat progress"),"open_fisher_trials",null));inv.setItem(53,back("back_main"));p.openInventory(inv);}
    public void openFisherTrials(Player p){Inventory inv=Bukkit.createInventory(null,27,FISHER_TRIALS_TITLE);inv.setItem(11,item(Material.COD,"§9✦ Trial of the Tide",List.of("§fCatches: "+fisher.catches(p)+"/"+fisher.tideTarget(),fisher.tideComplete(p)?"§a✓ COMPLETED":"§eOcean's Call gate"),null,null));inv.setItem(15,item(Material.CONDUIT,"§3✦ Trial of the Abyss",List.of("§fCatches: "+fisher.catches(p)+"/"+fisher.abyssCatchTarget(),"§fTreasure: "+fisher.treasures(p)+"/"+fisher.abyssTreasureTarget(),"§fOcean catches: "+fisher.oceanCatches(p)+"/"+fisher.abyssOceanTarget(),fisher.abyssComplete(p)?"§a✓ COMPLETED":"§eAwakening gate"),null,null));inv.setItem(22,back("back_fisher"));p.openInventory(inv);}

    private void header(Inventory inv, Player player, JobType job, Material material){JobProgress progress=db.getProgress(player.getUniqueId(),job);inv.setItem(4,item(material,"§b"+job.displayName(),List.of("§7"+JobRanks.title(job,progress.level()),"§fLevel: §b"+progress.level(),"§dFate Essence: "+db.getFateEssence(player.getUniqueId())),null,null));}
    private List<String> lore(String description,int rank,int max,int level,int cost){List<String> lore=new ArrayList<>();lore.add("§7"+description);lore.add("");lore.add("§fRank: §b"+rank+"/"+max);lore.add("§fRequires Lv."+level);lore.add("§dCost: "+cost+" Fate Essence");lore.add("");lore.add(rank>=max?"§aMAXIMUM RANK":"§eKlik untuk upgrade.");return lore;}
    private ItemStack back(String action){return item(Material.ARROW,"§eKembali",List.of(),action,null);}
    private ItemStack item(Material material,String name,List<String> lore,String action,String skill){ItemStack stack=new ItemStack(material);ItemMeta meta=stack.getItemMeta();meta.setDisplayName(name);meta.setLore(lore);meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);if(action!=null)meta.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,action);if(skill!=null)meta.getPersistentDataContainer().set(skillKey,PersistentDataType.STRING,skill);stack.setItemMeta(meta);return stack;}
}
