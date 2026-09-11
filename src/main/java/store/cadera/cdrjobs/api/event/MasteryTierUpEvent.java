package store.cadera.cdrjobs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import store.cadera.cdrjobs.model.JobType;

public final class MasteryTierUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final JobType profession;
    private final int oldTier;
    private final int newTier;
    private final long totalXp;

    public MasteryTierUpEvent(Player player, JobType profession, int oldTier, int newTier, long totalXp) {
        this.player = player;
        this.profession = profession;
        this.oldTier = oldTier;
        this.newTier = newTier;
        this.totalXp = totalXp;
    }

    public Player getPlayer() { return player; }
    public JobType getProfession() { return profession; }
    public int getOldTier() { return oldTier; }
    public int getNewTier() { return newTier; }
    public long getTotalXp() { return totalXp; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
