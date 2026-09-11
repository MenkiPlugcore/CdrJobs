package store.cadera.cdrjobs.service;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import store.cadera.cdrjobs.CdrJobsPlugin;
import store.cadera.cdrjobs.model.JobType;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LeaderboardService implements AutoCloseable {
    private static final int PROFESSION_COUNT = JobType.values().length;

    private final CdrJobsPlugin plugin;
    private final Connection connection;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public LeaderboardService(CdrJobsPlugin plugin, File dataFolder) throws SQLException {
        this.plugin = plugin;
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + new File(dataFolder, "cdrjobs.db").getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=NORMAL");
            statement.execute("PRAGMA busy_timeout=3000");
        }
    }

    public List<LeaderboardRow> profession(JobType job) {
        return cached("profession:" + job.name(), () -> queryProfession(job));
    }

    public List<LeaderboardRow> totalLevel() {
        return cached("total", this::queryTotalLevel);
    }

    public List<LeaderboardRow> hunterPvp() {
        return cached("hunter:pvp", () -> queryCounter(JobType.HUNTER.name(), "pvp_kills"));
    }

    public List<LeaderboardRow> activity(JobType job) {
        return cached("activity:" + job.name(), () -> queryActivity(job));
    }

    public List<LeaderboardRow> mastery(JobType job) {
        return cached("mastery:" + job.name(), () -> queryCounter(job.name(), MasteryService.METRIC));
    }

    public List<LeaderboardRow> masteryTotal() {
        return cached("mastery:total", this::queryMasteryTotal);
    }

    public void invalidateAll() {
        cache.clear();
    }

    public int limit() {
        return Math.max(3, Math.min(20, plugin.getConfig().getInt("leaderboards.limit", 10)));
    }

    private long ttlMillis() {
        long seconds = Math.max(5L, Math.min(300L, plugin.getConfig().getLong("leaderboards.cache-seconds", 30L)));
        return seconds * 1000L;
    }

    private List<LeaderboardRow> cached(String key, Loader loader) {
        long now = System.currentTimeMillis();
        CacheEntry existing = cache.get(key);
        if (existing != null && existing.expiresAt() > now) return existing.rows();
        List<LeaderboardRow> rows = List.copyOf(loader.load());
        cache.put(key, new CacheEntry(rows, now + ttlMillis()));
        return rows;
    }

    private synchronized List<LeaderboardRow> queryProfession(JobType job) {
        String sql = "SELECT uuid,level,xp FROM job_progress WHERE job=? ORDER BY level DESC,xp DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, job.name());
            ps.setInt(2, limit());
            try (ResultSet rs = ps.executeQuery()) {
                List<LeaderboardRow> rows = new ArrayList<>();
                while (rs.next()) rows.add(row(rs.getString(1), rs.getLong(2), rs.getLong(3)));
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load profession leaderboard", e);
        }
    }

    private synchronized List<LeaderboardRow> queryTotalLevel() {
        String sql = "SELECT uuid,(SUM(level) + (? - COUNT(*))) AS total_level,SUM(xp) AS total_xp "
                + "FROM job_progress GROUP BY uuid ORDER BY total_level DESC,total_xp DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, PROFESSION_COUNT);
            ps.setInt(2, limit());
            try (ResultSet rs = ps.executeQuery()) {
                List<LeaderboardRow> rows = new ArrayList<>();
                while (rs.next()) rows.add(row(rs.getString(1), rs.getLong(2), rs.getLong(3)));
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load total-level leaderboard", e);
        }
    }

    private synchronized List<LeaderboardRow> queryCounter(String job, String metric) {
        String sql = "SELECT uuid,value FROM profession_counters WHERE job=? AND metric=? AND value>0 ORDER BY value DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, job);
            ps.setString(2, metric);
            ps.setInt(3, limit());
            try (ResultSet rs = ps.executeQuery()) {
                List<LeaderboardRow> rows = new ArrayList<>();
                while (rs.next()) rows.add(row(rs.getString(1), rs.getLong(2), 0L));
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load activity leaderboard", e);
        }
    }

    private synchronized List<LeaderboardRow> queryMasteryTotal() {
        String sql = "SELECT uuid,SUM(value) AS total_mastery_xp FROM profession_counters "
                + "WHERE metric=? GROUP BY uuid HAVING total_mastery_xp>0 ORDER BY total_mastery_xp DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, MasteryService.METRIC);
            ps.setInt(2, limit());
            try (ResultSet rs = ps.executeQuery()) {
                List<LeaderboardRow> rows = new ArrayList<>();
                while (rs.next()) rows.add(row(rs.getString(1), rs.getLong(2), 0L));
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load total Mastery leaderboard", e);
        }
    }

    private synchronized List<LeaderboardRow> queryActivity(JobType job) {
        if (job == JobType.MINER) {
            String sql = "SELECT uuid,total_ores FROM miner_trials ORDER BY total_ores DESC LIMIT ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, limit());
                try (ResultSet rs = ps.executeQuery()) {
                    List<LeaderboardRow> rows = new ArrayList<>();
                    while (rs.next()) rows.add(row(rs.getString(1), rs.getLong(2), 0L));
                    return rows;
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to load Miner activity leaderboard", e);
            }
        }

        if (job == JobType.HUNTER) {
            String sql = "SELECT uuid,SUM(value) AS rewarded_kills FROM profession_counters "
                    + "WHERE job='HUNTER' AND metric IN ('mob_kills','pvp_kills') "
                    + "GROUP BY uuid ORDER BY rewarded_kills DESC LIMIT ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, limit());
                try (ResultSet rs = ps.executeQuery()) {
                    List<LeaderboardRow> rows = new ArrayList<>();
                    while (rs.next()) rows.add(row(rs.getString(1), rs.getLong(2), 0L));
                    return rows;
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to load Hunter activity leaderboard", e);
            }
        }

        String metric = switch (job) {
            case FARMER -> "harvests";
            case LUMBERJACK -> "logs";
            case FISHER -> "catches";
            default -> throw new IllegalArgumentException("Unsupported activity leaderboard: " + job);
        };
        return queryCounter(job.name(), metric);
    }

    private LeaderboardRow row(String rawUuid, long value, long secondary) {
        UUID uuid = UUID.fromString(rawUuid);
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        if (name == null || name.isBlank()) name = uuid.toString().substring(0, 8);
        return new LeaderboardRow(uuid, name, value, secondary);
    }

    @Override
    public void close() throws SQLException {
        cache.clear();
        if (!connection.isClosed()) connection.close();
    }

    @FunctionalInterface
    private interface Loader {
        List<LeaderboardRow> load();
    }

    private record CacheEntry(List<LeaderboardRow> rows, long expiresAt) {}

    public record LeaderboardRow(UUID uuid, String name, long value, long secondary) {}
}
