package store.cadera.cdrjobs.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import store.cadera.cdrjobs.api.event.ProfessionActionEvent;
import store.cadera.cdrjobs.service.ContractService;

public final class ContractListener implements Listener {
    private final ContractService contracts;

    public ContractListener(ContractService contracts) {
        this.contracts = contracts;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProfessionAction(ProfessionActionEvent event) {
        contracts.record(event.getPlayer(), event.getProfession(), event.getAmount());
    }
}
