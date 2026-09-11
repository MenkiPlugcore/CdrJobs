package store.cadera.cdrjobs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import store.cadera.cdrjobs.model.JobType;

public final class ContractClaimEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String cadence;
    private final String contractId;
    private final JobType profession;
    private final long rewardXp;
    private final int rewardFate;

    public ContractClaimEvent(Player player, String cadence, String contractId, JobType profession, long rewardXp, int rewardFate) {
        this.player = player;
        this.cadence = cadence;
        this.contractId = contractId;
        this.profession = profession;
        this.rewardXp = rewardXp;
        this.rewardFate = rewardFate;
    }

    public Player getPlayer() { return player; }
    public String getCadence() { return cadence; }
    public String getContractId() { return contractId; }
    public JobType getProfession() { return profession; }
    public long getRewardXp() { return rewardXp; }
    public int getRewardFate() { return rewardFate; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
