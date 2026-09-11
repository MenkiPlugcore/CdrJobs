package store.cadera.cdrjobs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import store.cadera.cdrjobs.model.JobType;

public final class RebirthEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final JobType profession;
    private final int refundEssence;
    private final int investedNodes;
    private final int investedRanks;
    private final boolean forced;

    public RebirthEvent(Player player, JobType profession, int refundEssence, int investedNodes, int investedRanks, boolean forced) {
        this.player = player;
        this.profession = profession;
        this.refundEssence = refundEssence;
        this.investedNodes = investedNodes;
        this.investedRanks = investedRanks;
        this.forced = forced;
    }

    public Player getPlayer() { return player; }
    public JobType getProfession() { return profession; }
    public int getRefundEssence() { return refundEssence; }
    public int getInvestedNodes() { return investedNodes; }
    public int getInvestedRanks() { return investedRanks; }
    public boolean isForced() { return forced; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
