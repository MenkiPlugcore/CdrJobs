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
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.model.MinerSkill;
import store.cadera.cdrjobs.model.MinerTrialProgress;
import store.cadera.cdrjobs.service.LevelService;
import store.cadera.cdrjobs.service.MinerTrialService;

import java.util.ArrayList;
import java.util.List;

public final class JobsMenu {
    public static final String MAIN_TITLE = ChatColor.DARK_AQUA + "CdrJobs • Path of Destiny";
    public static final String MINER_TITLE = ChatColor.DARK_GRAY + "Path of Ascension • Miner";
    public static final String TRIALS_TITLE = ChatColor.DARK_PURPLE + "Runebound • Profession Trials";

    private final CdrJobsPlugin plugin;
    private final Database database;
    private final LevelService levels;
    private final MinerTrialService trials;
    private final NamespacedKey actionKey;
    private final NamespacedKey skillKey;

    public JobsMenu(CdrJobsPlugin plugin, Database database, LevelService levels, MinerTrialService trials) {
        this.plugin = plugin;
        this.database = database;
        this.levels = levels;
        this.trials = trials;
        this.actionKey = new NamespacedKey(plugin, "menu_action");
        this.skillKey = new NamespacedKey(plugin, "skill_id");
    }

    public NamespacedKey actionKey() { return actionKey; }
    public NamespacedKey skillKey() { return skillKey; }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_TITLE);
        JobProgress miner = database.getProgress(player.getUniqueId(), JobType.MINER);
        int essence = database.getFateEssence(player.getUniqueId());

        inv.setItem(4, item(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "✦ Fate Essence: " + essence,
                List.of(ChatColor.GRAY + "Essence bersifat global.", ChatColor.GRAY + "Setiap pilihan menentukan path-mu."), null, null));

        int[] slots = {10, 12, 14, 16, 22};
        JobType[] jobs = {JobType.MINER, JobType.FARMER, JobType.HUNTER, JobType.LUMBERJACK, JobType.FISHER};
        for (int i = 0; i < jobs.length; i++) {
            JobType job = jobs[i];
            if (job == JobType.MINER) {
                long req = levels.xpRequiredForNextLevel(miner.level());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + miner.minerRankTitle());
                lore.add(ChatColor.WHITE + "Level: " + ChatColor.AQUA + miner.level());
                lore.add(ChatColor.WHITE + "XP: " + ChatColor.AQUA + (miner.level() >= levels.maxLevel() ? "MAX" : miner.xp() + "/" + req));
                lore.add("");
                lore.add(ChatColor.YELLOW + "Klik untuk membuka Path of Ascension.");
                inv.setItem(slots[i], item(job.icon(), ChatColor.AQUA + "⛏ " + job.displayName(), lore, "open_miner", null));
            } else {
                inv.setItem(slots[i], item(job.icon(), ChatColor.DARK_GRAY + job.displayName(),
                        List.of(ChatColor.GRAY + "Coming in a future CdrJobs chapter."), null, null));
            }
        }

        player.openInventory(inv);
    }

    public void openMiner(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MINER_TITLE);
        JobProgress progress = database.getProgress(player.getUniqueId(), JobType.MINER);
        int essence = database.getFateEssence(player.getUniqueId());

        inv.setItem(4, item(Material.DIAMOND_PICKAXE, ChatColor.AQUA + "Runebound Delver",
                List.of(ChatColor.GRAY + progress.minerRankTitle(), ChatColor.WHITE + "Level: " + ChatColor.AQUA + progress.level(),
                        ChatColor.LIGHT_PURPLE + "Fate Essence: " + essence), null, null));

        int[] slots = {10, 19, 21, 23, 28, 30, 49};
        MinerSkill[] skillList = {MinerSkill.STONEWHISPER, MinerSkill.RUNEBREAKER, MinerSkill.DEEPBORN,
                MinerSkill.RUNIC_SURGE, MinerSkill.GEMSEEKER, MinerSkill.ECHO_OF_DEPTH, MinerSkill.HEART_OF_MOUNTAIN};

        for (int i = 0; i < skillList.length; i++) {
            MinerSkill skill = skillList[i];
            int rank = database.getSkillRank(player.getUniqueId(), skill);
            boolean levelOk = progress.level() >= skill.requiredLevel();
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + skill.description());
            lore.add("");
            lore.add(ChatColor.WHITE + "Rank: " + ChatColor.AQUA + rank + "/" + skill.maxRank());
            lore.add(ChatColor.WHITE + "Requires Lv." + skill.requiredLevel());
            lore.add(ChatColor.LIGHT_PURPLE + "Cost: " + skill.essenceCost() + " Fate Essence");
            if (skill.prerequisite() != null) {
                lore.add(ChatColor.GRAY + "Requires " + skill.prerequisite().displayName() + " Rank " + skill.prerequisiteRank());
            }
            if (skill == MinerSkill.RUNIC_SURGE) {
                lore.add((trials.isStoneComplete(player) ? ChatColor.GREEN : ChatColor.RED) + "Requires Trial of Stone");
                lore.add(ChatColor.DARK_PURPLE + "Use: /cdrjobs ability");
            }
            if (skill == MinerSkill.HEART_OF_MOUNTAIN) {
                lore.add(ChatColor.GRAY + "Requires Gemseeker III + Echo of the Depth III");
                lore.add((trials.isDeepComplete(player) ? ChatColor.GREEN : ChatColor.RED) + "Requires Trial of the Deep");
            }
            lore.add("");
            lore.add(rank >= skill.maxRank() ? ChatColor.GREEN + "MAXIMUM RANK" : levelOk ? ChatColor.YELLOW + "Klik untuk upgrade." : ChatColor.RED + "Path masih terkunci.");
            inv.setItem(slots[i], item(skill.icon(), (rank > 0 ? ChatColor.AQUA : ChatColor.GRAY) + "✦ " + skill.displayName(), lore, "upgrade_skill", skill.name()));
        }

        inv.setItem(45, item(Material.ECHO_SHARD, ChatColor.LIGHT_PURPLE + "✦ Profession Trials",
                List.of(ChatColor.GRAY + "Buktikan bahwa path-mu layak untuk naik.", ChatColor.YELLOW + "Klik untuk melihat progress."), "open_trials", null));
        inv.setItem(53, item(Material.ARROW, ChatColor.YELLOW + "Kembali", List.of(), "back_main", null));
        player.openInventory(inv);
    }

    public void openTrials(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TRIALS_TITLE);
        MinerTrialProgress p = trials.progress(player);

        List<String> stoneLore = new ArrayList<>();
        stoneLore.add(ChatColor.GRAY + "The mountain tests persistence before power.");
        stoneLore.add("");
        stoneLore.add(ChatColor.WHITE + "Natural ores: " + progressColor(p.totalOres(), trials.stoneTarget()) + p.totalOres() + "/" + trials.stoneTarget());
        stoneLore.add("");
        stoneLore.add(p.stoneComplete() ? ChatColor.GREEN + "✓ COMPLETED" : ChatColor.YELLOW + "Unlocks access to Runic Surge.");
        inv.setItem(11, item(Material.IRON_PICKAXE, ChatColor.AQUA + "✦ Trial of Stone", stoneLore, null, null));

        List<String> deepLore = new ArrayList<>();
        deepLore.add(ChatColor.GRAY + "Only those who descend may hear the mountain's heart.");
        deepLore.add("");
        deepLore.add(ChatColor.WHITE + "Deep ores: " + progressColor(p.deepOres(), trials.deepTarget()) + p.deepOres() + "/" + trials.deepTarget());
        deepLore.add(ChatColor.WHITE + "Rare ores: " + progressColor(p.rareOres(), trials.rareTarget()) + p.rareOres() + "/" + trials.rareTarget());
        deepLore.add(ChatColor.WHITE + "Ancient Debris: " + progressColor(p.ancientDebris(), trials.ancientTarget()) + p.ancientDebris() + "/" + trials.ancientTarget());
        deepLore.add("");
        deepLore.add(p.deepComplete() ? ChatColor.GREEN + "✓ COMPLETED" : ChatColor.YELLOW + "Required for Heart of the Mountain.");
        inv.setItem(15, item(Material.SCULK_CATALYST, ChatColor.DARK_PURPLE + "✦ Trial of the Deep", deepLore, null, null));

        inv.setItem(22, item(Material.ARROW, ChatColor.YELLOW + "Kembali ke Path of Ascension", List.of(), "back_miner", null));
        player.openInventory(inv);
    }

    private ChatColor progressColor(int current, int target) {
        return current >= target ? ChatColor.GREEN : ChatColor.AQUA;
    }

    private ItemStack item(Material material, String name, List<String> lore, String action, String skillId) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (action != null) meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        if (skillId != null) meta.getPersistentDataContainer().set(skillKey, PersistentDataType.STRING, skillId);
        item.setItemMeta(meta);
        return item;
    }
}
