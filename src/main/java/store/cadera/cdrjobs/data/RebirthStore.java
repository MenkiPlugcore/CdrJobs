package store.cadera.cdrjobs.data;

import store.cadera.cdrjobs.model.JobType;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RebirthStore implements AutoCloseable {
    private final String url;
    private Connection connection;

    public RebirthStore(File folder) {
        this.url = "jdbc:sqlite:" + new File(folder, "cdrjobs.db").getAbsolutePath();
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=NORMAL");
        }
    }

    public synchronized long cooldownReadyAt(UUID uuid, JobType job) {
        String key = cooldownKey(job);
        try (PreparedStatement ps = connection.prepareStatement("SELECT ready_at FROM ability_cooldowns WHERE uuid=? AND ability=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read Rebirth cooldown", e);
        }
    }

    public synchronized Result rebirth(UUID uuid, JobType job, Map<String, Integer> skillCosts,
                                       int refundPercent, int fateFee, long cooldownSeconds,
                                       boolean bypassCooldown) {
        String id = uuid.toString();
        long now = System.currentTimeMillis();
        String cooldownKey = cooldownKey(job);
        int safeRefundPercent = Math.max(0, Math.min(100, refundPercent));
        int safeFateFee = Math.max(0, fateFee);
        long safeCooldownSeconds = Math.max(0L, cooldownSeconds);

        try {
            connection.setAutoCommit(false);
            ensureProfile(id);

            if (!bypassCooldown) {
                long readyAt = readCooldown(id, cooldownKey);
                if (readyAt > now) {
                    connection.rollback();
                    return new Result(Status.COOLDOWN, 0, 0, 0, readyAt);
                }
            }

            int currentFate = readFate(id);
            if (currentFate < safeFateFee) {
                connection.rollback();
                return new Result(Status.NOT_ENOUGH_FATE, 0, 0, 0, 0L);
            }

            long totalSpent = 0L;
            int investedNodes = 0;
            int investedRanks = 0;
            for (Map.Entry<String, Integer> entry : skillCosts.entrySet()) {
                int rank = readSkillRank(id, entry.getKey());
                if (rank <= 0) continue;
                investedNodes++;
                investedRanks = Math.addExact(investedRanks, rank);
                totalSpent = Math.addExact(totalSpent, Math.multiplyExact((long) rank, Math.max(0, entry.getValue())));
            }

            if (investedRanks == 0) {
                connection.rollback();
                return new Result(Status.NO_SKILLS, 0, 0, 0, 0L);
            }

            long refundLong = totalSpent * safeRefundPercent / 100L;
            int refund = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, refundLong));

            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM player_skills WHERE uuid=? AND skill=?")) {
                for (String skill : skillCosts.keySet()) {
                    delete.setString(1, id);
                    delete.setString(2, skill);
                    delete.addBatch();
                }
                delete.executeBatch();
            }

            long newFateLong = (long) currentFate - safeFateFee + refund;
            int newFate = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, newFateLong));
            try (PreparedStatement update = connection.prepareStatement("UPDATE player_profile SET fate_essence=? WHERE uuid=?")) {
                update.setInt(1, newFate);
                update.setString(2, id);
                update.executeUpdate();
            }

            long readyAt = 0L;
            if (!bypassCooldown && safeCooldownSeconds > 0L) {
                readyAt = safeAddMillis(now, safeCooldownSeconds);
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO ability_cooldowns(uuid,ability,ready_at) VALUES(?,?,?) " +
                                "ON CONFLICT(uuid,ability) DO UPDATE SET ready_at=excluded.ready_at")) {
                    ps.setString(1, id);
                    ps.setString(2, cooldownKey);
                    ps.setLong(3, readyAt);
                    ps.executeUpdate();
                }
            }

            connection.commit();
            return new Result(Status.SUCCESS, refund, investedNodes, investedRanks, readyAt);
        } catch (ArithmeticException | SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            throw new IllegalStateException("Failed to execute Rite of Rebirth", e);
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    private void ensureProfile(String uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO player_profile(uuid,fate_essence) VALUES(?,0)")) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        }
    }

    private int readFate(String uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT fate_essence FROM player_profile WHERE uuid=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int readSkillRank(String uuid, String skill) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT rank FROM player_skills WHERE uuid=? AND skill=?")) {
            ps.setString(1, uuid);
            ps.setString(2, skill);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Math.max(0, rs.getInt(1)) : 0;
            }
        }
    }

    private long readCooldown(String uuid, String ability) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT ready_at FROM ability_cooldowns WHERE uuid=? AND ability=?")) {
            ps.setString(1, uuid);
            ps.setString(2, ability);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private String cooldownKey(JobType job) {
        return "rebirth_" + job.name().toLowerCase(Locale.ROOT);
    }

    private long safeAddMillis(long now, long seconds) {
        try {
            return Math.addExact(now, Math.multiplyExact(seconds, 1000L));
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) connection.close();
    }

    public enum Status { SUCCESS, NO_SKILLS, COOLDOWN, NOT_ENOUGH_FATE }

    public record Result(Status status, int refund, int investedNodes, int investedRanks, long readyAt) {}
}
