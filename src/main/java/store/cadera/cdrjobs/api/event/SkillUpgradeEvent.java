package store.cadera.cdrjobs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import store.cadera.cdrjobs.model.JobType;

public final class SkillUpgradeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final JobType profession;
    private final String skillId;
    private final int newRank;
    private final int essenceCost;

    public SkillUpgradeEvent(Player player, JobType profession, String skillId, int newRank, int essenceCost) {
        this.player = player;
        this.profession = profession;
        this.skillId = skillId;
        this.newRank = newRank;
        this.essenceCost = essenceCost;
    }

    public Player getPlayer() { return player; }
    public JobType getProfession() { return profession; }
    public String getSkillId() { return skillId; }
    public int getNewRank() { return newRank; }
    public int getEssenceCost() { return essenceCost; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
