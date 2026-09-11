package store.cadera.cdrjobs.util;

import org.bukkit.ChatColor;

public final class Colors {
    private Colors() {}

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
