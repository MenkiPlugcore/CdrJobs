package store.cadera.cdrjobs.api;

import org.bukkit.entity.Player;
import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.model.MinerTrialProgress;
import store.cadera.cdrjobs.service.ContractService;
import store.cadera.cdrjobs.service.FateResonanceService;
import store.cadera.cdrjobs.service.LeaderboardService;
import store.cadera.cdrjobs.service.MasteryService;
import store.cadera.cdrjobs.service.ProfileService;
import store.cadera.cdrjobs.service.ProgressionService;
import store.cadera.cdrjobs.util.JobRanks;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stable public facade for CdrJobs integrations.
 *
 * API v2 is backward-compatible with the v1 read/progression methods and adds
 * immutable snapshots for Profile, skills, Trials/statistics, Mastery,
 * Contracts, Fate Resonance and cached leaderboards.
 */
public final class CdrJobsAPI {
    public static final int API_VERSION = 2;

    private final Database database;
    private final ProfessionStore store;
    private final ProgressionService progression;
    private final ProfileService profiles;
    private final MasteryService mastery;
    private final ContractService contracts;
    private final FateResonanceService resonance;
    private final LeaderboardService leaderboards;

    public CdrJobsAPI(Database database, ProfessionStore store, ProgressionService progression,
                      ProfileService profiles, MasteryService mastery, ContractService contracts,
                      FateResonanceService resonance, LeaderboardService leaderboards) {
        this.database = database;
        this.store = store;
        this.progression = progression;
        this.profiles = profiles;
        this.mastery = mastery;
        this.contracts = contracts;
        this.resonance = resonance;
        this.leaderboards = leaderboards;
    }

    public int getApiVersion() { return API_VERSION; }

    // API v1 compatibility
    public JobProgress getProgress(UUID uuid, JobType job) { return database.getProgress(uuid, job); }
    public int getLevel(UUID uuid, JobType job) { return getProgress(uuid, job).level(); }
    public long getXp(UUID uuid, JobType job) { return getProgress(uuid, job).xp(); }
    public int getFateEssence(UUID uuid) { return database.getFateEssence(uuid); }
    public String getRankTitle(UUID uuid, JobType job) { return JobRanks.title(job, getLevel(uuid, job)); }
    public JobType getHighestProfession(UUID uuid) { return getHighestProfessions(uuid).getFirst(); }
    public JobProgress addXp(Player player, JobType job, long amount) { return progression.addXp(player, job, amount); }

    public List<JobType> getHighestProfessions(UUID uuid) {
        int highest = Integer.MIN_VALUE;
        List<JobType> result = new ArrayList<>();
        for (JobType job : JobType.values()) {
            int level = getLevel(uuid, job);
            if (level > highest) {
                highest = level;
                result.clear();
                result.add(job);
            } else if (level == highest) {
                result.add(job);
            }
        }
        return List.copyOf(result);
    }

    public PlayerSnapshot getPlayerSnapshot(UUID uuid) {
        ProfileService.ProfileSnapshot profile = profiles.snapshot(uuid);
        Map<JobType, ProfessionSnapshot> jobs = new EnumMap<>(JobType.class);
        for (JobType job : JobType.values()) {
            JobProgress progress = profile.progress().get(job);
            MasteryService.State state = mastery.state(uuid, job);
            jobs.put(job, new ProfessionSnapshot(
                    job,
                    progress.level(),
                    progress.xp(),
                    JobRanks.title(job, progress.level()),
                    profile.activities().getOrDefault(job, 0L),
                    toMastery(state)
            ));
        }
        return new PlayerSnapshot(
                uuid,
                profile.totalLevel(),
                List.copyOf(getHighestProfessions(uuid)),
                profile.fateEssence(),
                profile.completedTrials(),
                profile.unlockedSkills(),
                List.copyOf(profile.awakenedJobs()),
                mastery.totalMasteryTiers(uuid),
                mastery.totalMasteryXp(uuid),
                resonance.unlockedCount(uuid),
                resonance.harmonizedCount(uuid),
                resonance.score(uuid),
                Map.copyOf(jobs)
        );
    }

    public Map<String, Integer> getSkillRanks(UUID uuid) {
        return Map.copyOf(database.getSkillRanks(uuid));
    }

    public int getSkillRank(UUID uuid, String skillId) {
        if (skillId == null || skillId.isBlank()) return 0;
        return Math.max(0, store.getSkillRank(uuid, skillId));
    }

    public Map<String, Long> getStatistics(UUID uuid, JobType job) {
        return Map.copyOf(store.getCounters(uuid, job.name()));
    }

    public long getStatistic(UUID uuid, JobType job, String metric) {
        if (metric == null || metric.isBlank()) return 0L;
        return Math.max(0L, store.getCounter(uuid, job.name(), metric));
    }

    public Map<String, Boolean> getFlags(UUID uuid, JobType job) {
        return Map.copyOf(store.getFlags(uuid, job.name()));
    }

    public boolean getFlag(UUID uuid, JobType job, String flag) {
        return flag != null && !flag.isBlank() && store.getFlag(uuid, job.name(), flag);
    }

    public long getTrialProgress(UUID uuid, JobType job, String metric) {
        if (metric == null || metric.isBlank()) return 0L;
        return Math.max(0L, store.getTrialProgress(uuid, job.name(), metric));
    }

    public MinerTrialProgress getMinerTrialProgress(UUID uuid) {
        return database.getMinerTrialProgress(uuid);
    }

    public long getAbilityReadyAt(UUID uuid, String abilityId) {
        if (abilityId == null || abilityId.isBlank()) return 0L;
        return Math.max(0L, store.getAbilityReadyAt(uuid, abilityId));
    }

    public long getAbilityCooldownSeconds(UUID uuid, String abilityId) {
        long remaining = getAbilityReadyAt(uuid, abilityId) - System.currentTimeMillis();
        return remaining <= 0L ? 0L : (remaining + 999L) / 1000L;
    }

    public MasterySnapshot getMastery(UUID uuid, JobType job) {
        return toMastery(mastery.state(uuid, job));
    }

    public ContractSnapshot getContract(UUID uuid, String cadence) {
        ContractService.Cadence parsed = ContractService.Cadence.parse(cadence);
        if (parsed == null) throw new IllegalArgumentException("cadence must be daily or weekly");
        ContractService.ContractView view = contracts.view(uuid, parsed);
        ContractService.ContractDefinition def = view.definition();
        return new ContractSnapshot(
                parsed.id(), view.cycle(), def.id(), def.name(), def.job(), view.progress(), def.target(),
                view.complete(), view.claimed(), def.rewardXp(), def.rewardFate(),
                view.usedRerolls(), view.rerollLimit()
        );
    }

    public List<ResonanceSnapshot> getResonances(UUID uuid) {
        return resonance.states(uuid).stream().map(state -> new ResonanceSnapshot(
                state.definition().id(),
                state.definition().name(),
                state.definition().title(),
                state.definition().badge(),
                state.definition().first(),
                state.definition().second(),
                state.definition().minLevel(),
                state.definition().harmonizedMasteryTier(),
                state.unlocked(),
                state.harmonized(),
                state.firstLevel(),
                state.secondLevel(),
                state.firstMastery(),
                state.secondMastery()
        )).toList();
    }

    public int getResonanceScore(UUID uuid) { return resonance.score(uuid); }

    public List<LeaderboardEntry> getLeaderboard(LeaderboardType type, JobType job) {
        List<LeaderboardService.LeaderboardRow> rows = switch (type) {
            case PROFESSION -> leaderboards.profession(requireJob(type, job));
            case TOTAL_LEVEL -> leaderboards.totalLevel();
            case HUNTER_PVP -> leaderboards.hunterPvp();
            case ACTIVITY -> leaderboards.activity(requireJob(type, job));
            case MASTERY -> leaderboards.mastery(requireJob(type, job));
            case MASTERY_TOTAL -> leaderboards.masteryTotal();
        };
        return rows.stream().map(row -> new LeaderboardEntry(row.uuid(), row.name(), row.value(), row.secondary())).toList();
    }

    private JobType requireJob(LeaderboardType type, JobType job) {
        if (job == null) throw new IllegalArgumentException(type + " leaderboard requires a profession");
        return job;
    }

    private MasterySnapshot toMastery(MasteryService.State state) {
        return new MasterySnapshot(state.tier(), state.xp(), state.requiredXp(), state.totalXp(), state.title(), state.badge(), state.maxed());
    }

    public enum LeaderboardType {
        PROFESSION, TOTAL_LEVEL, HUNTER_PVP, ACTIVITY, MASTERY, MASTERY_TOTAL
    }

    public record PlayerSnapshot(UUID uuid, int totalLevel, List<JobType> highestProfessions,
                                 int fateEssence, int completedTrials, int unlockedSkills,
                                 List<JobType> awakenedProfessions, int totalMasteryTiers,
                                 long totalMasteryXp, int resonanceUnlocked,
                                 int resonanceHarmonized, int resonanceScore,
                                 Map<JobType, ProfessionSnapshot> professions) {}

    public record ProfessionSnapshot(JobType profession, int level, long xp, String rankTitle,
                                     long activity, MasterySnapshot mastery) {}

    public record MasterySnapshot(int tier, long xp, long requiredXp, long totalXp,
                                  String title, String badge, boolean maxed) {}

    public record ContractSnapshot(String cadence, long cycle, String id, String name,
                                   JobType profession, long progress, long target,
                                   boolean complete, boolean claimed, long rewardXp,
                                   int rewardFate, int usedRerolls, int rerollLimit) {}

    public record ResonanceSnapshot(String id, String name, String title, String badge,
                                    JobType firstProfession, JobType secondProfession,
                                    int minLevel, int harmonizedMasteryTier,
                                    boolean unlocked, boolean harmonized,
                                    int firstLevel, int secondLevel,
                                    int firstMasteryTier, int secondMasteryTier) {}

    public record LeaderboardEntry(UUID uuid, String name, long value, long secondary) {}
}
