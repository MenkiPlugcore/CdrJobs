package store.cadera.cdrjobs.integration;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import store.cadera.cdrjobs.CdrJobsPlugin;

import java.lang.reflect.Method;

public final class VaultEconomyHook {
    private final CdrJobsPlugin plugin;

    public VaultEconomyHook(CdrJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean available() {
        return provider() != null;
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (amount <= 0D) return true;
        Object provider = provider();
        if (provider == null) return false;
        try {
            Method method = provider.getClass().getMethod("has", OfflinePlayer.class, double.class);
            Object value = method.invoke(provider, player, amount);
            return value instanceof Boolean b && b;
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Vault economy has() reflection failed: " + e.getMessage());
            return false;
        }
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0D) return true;
        return transaction(player, amount, "withdrawPlayer");
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0D) return true;
        return transaction(player, amount, "depositPlayer");
    }

    private boolean transaction(OfflinePlayer player, double amount, String methodName) {
        Object provider = provider();
        if (provider == null) return false;
        try {
            Method method = provider.getClass().getMethod(methodName, OfflinePlayer.class, double.class);
            Object response = method.invoke(provider, player, amount);
            if (response == null) return false;
            Method success = response.getClass().getMethod("transactionSuccess");
            Object value = success.invoke(response);
            return value instanceof Boolean b && b;
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Vault economy " + methodName + " reflection failed: " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object provider() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Vault")) return null;
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = plugin.getServer().getServicesManager().getRegistration((Class) economyClass);
            return registration == null ? null : registration.getProvider();
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
