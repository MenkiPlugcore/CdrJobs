package store.cadera.cdrjobs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import store.cadera.cdrjobs.model.JobType;

public final class ProfessionAwakeningEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final JobType profession;
    private final String skillId;

    public ProfessionAwakeningEvent(Player player, JobType profession, String skillId) {
        this.player = player;
        this.profession = profession;
        this.skillId = skillId;
    }

    public Player getPlayer() { return player; }
    public JobType getProfession() { return profession; }
    public String getSkillId() { return skillId; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
