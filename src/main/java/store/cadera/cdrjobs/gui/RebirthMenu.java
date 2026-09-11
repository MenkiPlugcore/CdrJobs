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
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.service.RebirthService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RebirthMenu {
    public static final String SELECT_TITLE = ChatColor.DARK_PURPLE + "CdrJobs • Rite of Rebirth";
    public static final String CONFIRM_TITLE_PREFIX = ChatColor.DARK_RED + "Rebirth • Confirm • ";

    private final RebirthService service;
    private final NamespacedKey actionKey;
    private final NamespacedKey jobKey;

    public RebirthMenu(CdrJobsPlugin plugin, RebirthService service) {
        this.service = service;
        this.actionKey = new NamespacedKey(plugin, "rebirth_action");
        this.jobKey = new NamespacedKey(plugin, "rebirth_job");
    }

    public NamespacedKey actionKey() { return actionKey; }
    public NamespacedKey jobKey() { return jobKey; }

    public void openSelect(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, SELECT_TITLE);
        inv.setItem(4, item(Material.NETHER_STAR, "§d✦ Rite of Rebirth", List.of(
                "§7Reset skill tree satu profession.",
                "§7Level, XP, Trial, statistik dan milestone tetap aman.",
                "§7Refund mengikuti konfigurasi server."
        ), null, null));

        int[] slots = {10, 12, 14, 16, 22};
        JobType[] jobs = JobType.values();
        for (int i = 0; i < jobs.length; i++) {
            JobType job = jobs[i];
            RebirthService.Quote quote = service.quote(player.getUniqueId(), job);
            List<String> lore = new ArrayList<>();
            lore.add("§fSkill nodes: §b" + quote.investedNodes());
            lore.add("§fTotal ranks: §b" + quote.investedRanks());
            lore.add("§fSpent Essence: §d" + quote.spentEssence());
            lore.add("§fRefund: §a" + quote.refundEssence() + " §7(" + quote.refundPercent() + "%)");
            lore.add("§fFee: §e" + feeText(quote));
            lore.add("§fCooldown: " + (quote.cooldownRemainingSeconds() > 0 ? "§c" + formatDuration(quote.cooldownRemainingSeconds()) : "§aREADY"));
            lore.add("");
            lore.add(quote.investedRanks() <= 0 ? "§7Tidak ada skill untuk di-respec." : "§eKlik untuk review.");
            inv.setItem(slots[i], item(job.icon(), "§d✦ " + job.displayName(), lore, "select", job));
        }
        player.openInventory(inv);
    }

    public void openConfirm(Player player, JobType job) {
        RebirthService.Quote quote = service.quote(player.getUniqueId(), job);
        Inventory inv = Bukkit.createInventory(null, 27, CONFIRM_TITLE_PREFIX + job.displayName());
        inv.setItem(13, item(job.icon(), "§d✦ " + job.displayName(), List.of(
                "§fSkill nodes: §b" + quote.investedNodes(),
                "§fTotal ranks: §b" + quote.investedRanks(),
                "§fRefund: §a" + quote.refundEssence() + " Fate Essence",
                "§fFee: §e" + feeText(quote),
                "§fCooldown setelah Rebirth: §c" + formatDuration(service.cooldownSeconds()),
                "",
                "§cLevel, XP, Trial dan statistik TIDAK direset."
        ), null, job));

        inv.setItem(11, item(Material.RED_STAINED_GLASS_PANE, "§c✖ Batal", List.of("§7Kembali ke daftar profession."), "cancel", job));

        List<String> confirmLore = new ArrayList<>();
        if (quote.investedRanks() <= 0) confirmLore.add("§cTidak ada skill yang bisa di-reset.");
        else if (quote.cooldownRemainingSeconds() > 0) confirmLore.add("§cCooldown: " + formatDuration(quote.cooldownRemainingSeconds()));
        else {
            confirmLore.add("§aSkill tree akan dikosongkan.");
            confirmLore.add("§aRefund: " + quote.refundEssence() + " Fate Essence.");
            confirmLore.add("§7Klik untuk konfirmasi final.");
        }
        inv.setItem(15, item(quote.investedRanks() > 0 && quote.cooldownRemainingSeconds() == 0 ? Material.LIME_STAINED_GLASS_PANE : Material.BARRIER,
                "§a✔ Konfirmasi Rebirth", confirmLore, "confirm", job));
        player.openInventory(inv);
    }

    private String feeText(RebirthService.Quote quote) {
        return switch (quote.feeMode()) {
            case NONE -> "None";
            case FATE -> quote.fateFee() + " Fate Essence";
            case VAULT -> String.format(Locale.US, "%.2f Vault economy", quote.vaultFee());
        };
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "0s";
        long days = seconds / 86400L;
        long hours = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        StringBuilder out = new StringBuilder();
        if (days > 0) out.append(days).append("d ");
        if (hours > 0) out.append(hours).append("h ");
        if (minutes > 0) out.append(minutes).append("m ");
        if (days == 0 && hours == 0 && secs > 0) out.append(secs).append("s");
        return out.toString().trim();
    }

    private ItemStack item(Material material, String name, List<String> lore, String action, JobType job) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (action != null) meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        if (job != null) meta.getPersistentDataContainer().set(jobKey, PersistentDataType.STRING, job.name());
        item.setItemMeta(meta);
        return item;
    }
}
