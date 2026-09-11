package store.cadera.cdrjobs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import store.cadera.cdrjobs.model.JobType;

public final class ContractCompleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String cadence;
    private final String contractId;
    private final JobType profession;
    private final long target;

    public ContractCompleteEvent(Player player, String cadence, String contractId, JobType profession, long target) {
        this.player = player;
        this.cadence = cadence;
        this.contractId = contractId;
        this.profession = profession;
        this.target = target;
    }

    public Player getPlayer() { return player; }
    public String getCadence() { return cadence; }
    public String getContractId() { return contractId; }
    public JobType getProfession() { return profession; }
    public long getTarget() { return target; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
