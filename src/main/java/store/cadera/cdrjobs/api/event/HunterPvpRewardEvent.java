package store.cadera.cdrjobs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class HunterPvpRewardEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player killer;
    private final Player victim;
    private final int baseXp;
    private final boolean countsTowardTrials;

    public HunterPvpRewardEvent(Player killer, Player victim, int baseXp, boolean countsTowardTrials) {
        this.killer = killer;
        this.victim = victim;
        this.baseXp = baseXp;
        this.countsTowardTrials = countsTowardTrials;
    }

    public Player getKiller() { return killer; }
    public Player getVictim() { return victim; }
    public int getBaseXp() { return baseXp; }
    public boolean countsTowardTrials() { return countsTowardTrials; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
