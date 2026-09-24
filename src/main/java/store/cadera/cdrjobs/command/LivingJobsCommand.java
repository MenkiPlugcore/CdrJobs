package store.cadera.cdrjobs.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import store.cadera.cdrjobs.service.LivingProfessionsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Thin command decorator for v1.7.0. Existing JobsCommand behavior remains untouched;
 * only /cdrjobs session is intercepted here.
 */
public final class LivingJobsCommand implements CommandExecutor, TabCompleter {
    private final JobsCommand delegate;
    private final LivingProfessionsService living;

    public LivingJobsCommand(JobsCommand delegate, LivingProfessionsService living) {
        this.delegate = delegate;
        this.living = living;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("session")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Player only");
                return true;
            }
            if (!player.hasPermission("cdrjobs.use")) return delegate.onCommand(sender, command, label, args);
            living.sendSession(player);
            return true;
        }
        return delegate.onCommand(sender, command, label, args);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> delegated = delegate.onTabComplete(sender, command, alias, args);
        if (args.length != 1) return delegated;

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> merged = new ArrayList<>();
        if (delegated != null) merged.addAll(delegated);
        if ("session".startsWith(prefix) && !merged.contains("session")) merged.add("session");
        return merged;
    }
}
