package store.cadera.cdrjobs.service;

import org.bukkit.configuration.ConfigurationSection;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.model.JobType;

import java.util.*;

/**
 * Derived cross-profession progression. Resonance stores no player state: unlocks are
 * calculated from canonical profession levels and Mastery, so resets/restarts cannot
 * desynchronise a separate resonance ledger.
 */
public final class FateResonanceService {
    private final CdrJobsPlugin plugin;
    private final Database database;
    private final MasteryService mastery;

    public FateResonanceService(CdrJobsPlugin plugin, Database database, MasteryService mastery) {
        this.plugin = plugin;
        this.database = database;
        this.mastery = mastery;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("fate-resonance.enabled", true);
    }

    public List<Definition> definitions() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("fate-resonance.definitions");
        if (root == null) return List.of();
        List<Definition> out = new ArrayList<>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) continue;
            List<String> jobs = section.getStringList("jobs");
            if (jobs.size() != 2) continue;
            JobType first = parseJob(jobs.get(0));
            JobType second = parseJob(jobs.get(1));
            if (first == null || second == null || first == second) continue;
            int minLevel = Math.max(1, section.getInt("min-level", 50));
            int harmonizedMastery = Math.max(0, section.getInt("harmonized-mastery-tier", 1));
            String name = section.getString("name", prettify(id));
            String title = section.getString("title", name);
            String badge = section.getString("badge", "✦");
            out.add(new Definition(id.toLowerCase(Locale.ROOT), name, title, badge, first, second, minLevel, harmonizedMastery));
        }
        return List.copyOf(out);
    }

    public Optional<Definition> definition(String id) {
        return definitions().stream().filter(def -> def.id().equalsIgnoreCase(id)).findFirst();
    }

    public State state(UUID uuid, Definition def) {
        int firstLevel = database.getProgress(uuid, def.first()).level();
        int secondLevel = database.getProgress(uuid, def.second()).level();
        int firstMastery = mastery.state(uuid, def.first()).tier();
        int secondMastery = mastery.state(uuid, def.second()).tier();
        boolean unlocked = enabled() && firstLevel >= def.minLevel() && secondLevel >= def.minLevel();
        boolean harmonized = unlocked && firstMastery >= def.harmonizedMasteryTier()
                && secondMastery >= def.harmonizedMasteryTier();
        return new State(def, unlocked, harmonized, firstLevel, secondLevel, firstMastery, secondMastery);
    }

    public List<State> states(UUID uuid) {
        return definitions().stream().map(def -> state(uuid, def)).toList();
    }

    public int unlockedCount(UUID uuid) {
        return (int) states(uuid).stream().filter(State::unlocked).count();
    }

    public int harmonizedCount(UUID uuid) {
        return (int) states(uuid).stream().filter(State::harmonized).count();
    }

    public int score(UUID uuid) {
        int score = 0;
        for (State state : states(uuid)) {
            if (state.unlocked()) score++;
            if (state.harmonized()) score++;
        }
        return score;
    }

    public String unlockedNames(UUID uuid) {
        List<String> names = states(uuid).stream().filter(State::unlocked).map(s -> s.definition().name()).toList();
        return names.isEmpty() ? "None" : String.join(", ", names);
    }

    public String harmonizedNames(UUID uuid) {
        List<String> names = states(uuid).stream().filter(State::harmonized).map(s -> s.definition().name()).toList();
        return names.isEmpty() ? "None" : String.join(", ", names);
    }

    public String display(State state) {
        if (!state.unlocked()) return "Locked";
        return state.definition().badge() + " " + state.definition().title()
                + (state.harmonized() ? " §6[Harmonized]" : " §7[Resonant]");
    }

    private JobType parseJob(String raw) {
        if (raw == null) return null;
        try { return JobType.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private String prettify(String id) {
        String[] words = id.replace('-', '_').split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    public record Definition(String id, String name, String title, String badge,
                             JobType first, JobType second, int minLevel, int harmonizedMasteryTier) {}

    public record State(Definition definition, boolean unlocked, boolean harmonized,
                        int firstLevel, int secondLevel, int firstMastery, int secondMastery) {}
}
