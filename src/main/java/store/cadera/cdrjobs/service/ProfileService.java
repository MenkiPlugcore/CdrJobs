package store.cadera.cdrjobs.service;

import store.cadera.cdrjobs.data.Database;
import store.cadera.cdrjobs.data.ProfessionStore;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.model.MinerTrialProgress;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProfileService {
    private static final long CACHE_MILLIS = 1_000L;
    private final Database database;
    private final ProfessionStore store;
    private final Map<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

    public ProfileService(Database database, ProfessionStore store) {
        this.database = database;
        this.store = store;
    }

    public ProfileSnapshot snapshot(UUID uuid) {
        long now = System.currentTimeMillis();
        CacheEntry cached = cache.get(uuid);
        if (cached != null && cached.expiresAt() > now) return cached.snapshot();
        ProfileSnapshot snapshot = load(uuid);
        if (cache.size() > 512) cache.clear();
        cache.put(uuid, new CacheEntry(snapshot, now + CACHE_MILLIS));
        return snapshot;
    }

    public void invalidate(UUID uuid) {
        cache.remove(uuid);
    }

    private ProfileSnapshot load(UUID uuid) {
        int totalLevel = 0;
        JobType highestJob = JobType.MINER;
        int highestLevel = -1;
        Map<JobType, JobProgress> progress = new EnumMap<>(JobType.class);
        Map<JobType, Long> activities = new EnumMap<>(JobType.class);

        for (JobType job : JobType.values()) {
            JobProgress value = database.getProgress(uuid, job);
            progress.put(job, value);
            totalLevel += value.level();
            if (value.level() > highestLevel) {
                highestLevel = value.level();
                highestJob = job;
            }
        }

        MinerTrialProgress miner = database.getMinerTrialProgress(uuid);
        activities.put(JobType.MINER, (long) miner.totalOres());
        activities.put(JobType.FARMER, store.getCounter(uuid, JobType.FARMER.name(), "harvests"));
        activities.put(JobType.HUNTER,
                store.getCounter(uuid, JobType.HUNTER.name(), "mob_kills")
                        + store.getCounter(uuid, JobType.HUNTER.name(), "pvp_kills"));
        activities.put(JobType.LUMBERJACK, store.getCounter(uuid, JobType.LUMBERJACK.name(), "logs"));
        activities.put(JobType.FISHER, store.getCounter(uuid, JobType.FISHER.name(), "catches"));

        int completedTrials = 0;
        if (miner.stoneComplete()) completedTrials++;
        if (miner.deepComplete()) completedTrials++;
        completedTrials += flag(uuid, JobType.FARMER, "trial_seed") + flag(uuid, JobType.FARMER, "trial_gaia");
        completedTrials += flag(uuid, JobType.HUNTER, "trial_fang") + flag(uuid, JobType.HUNTER, "trial_moon");
        completedTrials += flag(uuid, JobType.LUMBERJACK, "trial_timber") + flag(uuid, JobType.LUMBERJACK, "trial_grove");
        completedTrials += flag(uuid, JobType.FISHER, "trial_tide") + flag(uuid, JobType.FISHER, "trial_abyss");

        Map<String, Integer> skillRanks = database.getSkillRanks(uuid);
        int unlockedSkills = (int) skillRanks.values().stream().filter(rank -> rank != null && rank > 0).count();
        List<JobType> awakened = new ArrayList<>();
        if (rank(skillRanks, "HEART_OF_MOUNTAIN") > 0) awakened.add(JobType.MINER);
        if (rank(skillRanks, "FARMER_VERDANT_DOMINION") > 0) awakened.add(JobType.FARMER);
        if (rank(skillRanks, "HUNTER_APEX_PREDATOR") > 0) awakened.add(JobType.HUNTER);
        if (rank(skillRanks, "LUMBERJACK_OATH_YGGDRASIL") > 0) awakened.add(JobType.LUMBERJACK);
        if (rank(skillRanks, "FISHER_KING_OF_TIDES") > 0) awakened.add(JobType.FISHER);

        return new ProfileSnapshot(
                totalLevel, highestJob, highestLevel, database.getFateEssence(uuid),
                completedTrials, unlockedSkills, List.copyOf(awakened),
                Map.copyOf(progress), Map.copyOf(activities)
        );
    }

    private int flag(UUID uuid, JobType job, String flag) {
        return store.getFlag(uuid, job.name(), flag) ? 1 : 0;
    }

    private int rank(Map<String, Integer> skills, String key) {
        return Math.max(0, skills.getOrDefault(key, 0));
    }

    private record CacheEntry(ProfileSnapshot snapshot, long expiresAt) {}

    public record ProfileSnapshot(
            int totalLevel,
            JobType highestJob,
            int highestLevel,
            int fateEssence,
            int completedTrials,
            int unlockedSkills,
            List<JobType> awakenedJobs,
            Map<JobType, JobProgress> progress,
            Map<JobType, Long> activities
    ) {
        public String awakenedNames() {
            if (awakenedJobs.isEmpty()) return "None";
            return awakenedJobs.stream().map(JobType::displayName).reduce((a, b) -> a + ", " + b).orElse("None");
        }
    }
}
