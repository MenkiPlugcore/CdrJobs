package store.cadera.cdrjobs.data;

import org.bukkit.Location;
import store.cadera.cdrjobs.model.JobProgress;
import store.cadera.cdrjobs.model.JobType;
import store.cadera.cdrjobs.model.MinerSkill;
import store.cadera.cdrjobs.model.MinerTrialProgress;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public final class Database implements AutoCloseable {
    private final String url;
    private Connection connection;

    public Database(File dataFolder) {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder");
        }
        this.url = "jdbc:sqlite:" + new File(dataFolder, "cdrjobs.db").getAbsolutePath();
    }

    public void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
        connection = DriverManager.getConnection(url);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            st.execute("CREATE TABLE IF NOT EXISTS player_profile (uuid TEXT PRIMARY KEY, fate_essence INTEGER NOT NULL DEFAULT 0)");
            st.execute("CREATE TABLE IF NOT EXISTS job_progress (uuid TEXT NOT NULL, job TEXT NOT NULL, level INTEGER NOT NULL DEFAULT 1, xp INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(uuid, job))");
            st.execute("CREATE TABLE IF NOT EXISTS player_skills (uuid TEXT NOT NULL, skill TEXT NOT NULL, rank INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(uuid, skill))");
            st.execute("CREATE TABLE IF NOT EXISTS placed_ores (world TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, PRIMARY KEY(world, x, y, z))");
            st.execute("CREATE TABLE IF NOT EXISTS miner_trials (uuid TEXT PRIMARY KEY, total_ores INTEGER NOT NULL DEFAULT 0, deep_ores INTEGER NOT NULL DEFAULT 0, rare_ores INTEGER NOT NULL DEFAULT 0, ancient_debris INTEGER NOT NULL DEFAULT 0, stone_complete INTEGER NOT NULL DEFAULT 0, deep_complete INTEGER NOT NULL DEFAULT 0)");
            st.execute("CREATE TABLE IF NOT EXISTS ability_cooldowns (uuid TEXT NOT NULL, ability TEXT NOT NULL, ready_at INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(uuid, ability))");
        }
    }

    private void ensureProfile(UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO player_profile(uuid, fate_essence) VALUES(?,0)")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    private void ensureJob(UUID uuid, JobType job) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO job_progress(uuid, job, level, xp) VALUES(?,?,1,0)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, job.name());
            ps.executeUpdate();
        }
    }

    private void ensureMinerTrial(UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO miner_trials(uuid) VALUES(?)")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public synchronized JobProgress getProgress(UUID uuid, JobType job) {
        try {
            ensureJob(uuid, job);
            try (PreparedStatement ps = connection.prepareStatement("SELECT level, xp FROM job_progress WHERE uuid=? AND job=?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, job.name());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return new JobProgress(rs.getInt("level"), rs.getLong("xp"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read progress", e);
        }
        return new JobProgress(1, 0);
    }

    public synchronized void setProgress(UUID uuid, JobType job, int level, long xp) {
        try {
            ensureJob(uuid, job);
            try (PreparedStatement ps = connection.prepareStatement("UPDATE job_progress SET level=?, xp=? WHERE uuid=? AND job=?")) {
                ps.setInt(1, level);
                ps.setLong(2, xp);
                ps.setString(3, uuid.toString());
                ps.setString(4, job.name());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save progress", e);
        }
    }

    public synchronized int getFateEssence(UUID uuid) {
        try {
            ensureProfile(uuid);
            try (PreparedStatement ps = connection.prepareStatement("SELECT fate_essence FROM player_profile WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read Fate Essence", e);
        }
    }

    public synchronized void addFateEssence(UUID uuid, int amount) {
        try {
            ensureProfile(uuid);
            try (PreparedStatement ps = connection.prepareStatement("UPDATE player_profile SET fate_essence = MAX(0, fate_essence + ?) WHERE uuid=?")) {
                ps.setInt(1, amount);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update Fate Essence", e);
        }
    }

    public synchronized int getSkillRank(UUID uuid, MinerSkill skill) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT rank FROM player_skills WHERE uuid=? AND skill=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, skill.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read skill rank", e);
        }
    }

    public synchronized void setSkillRank(UUID uuid, MinerSkill skill, int rank) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO player_skills(uuid, skill, rank) VALUES(?,?,?) ON CONFLICT(uuid, skill) DO UPDATE SET rank=excluded.rank")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, skill.name());
            ps.setInt(3, rank);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save skill", e);
        }
    }

    public synchronized MinerTrialProgress getMinerTrialProgress(UUID uuid) {
        try {
            ensureMinerTrial(uuid);
            try (PreparedStatement ps = connection.prepareStatement("SELECT total_ores,deep_ores,rare_ores,ancient_debris,stone_complete,deep_complete FROM miner_trials WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new MinerTrialProgress(rs.getInt("total_ores"), rs.getInt("deep_ores"), rs.getInt("rare_ores"),
                                rs.getInt("ancient_debris"), rs.getInt("stone_complete") != 0, rs.getInt("deep_complete") != 0);
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read Miner trial progress", e);
        }
        return new MinerTrialProgress(0, 0, 0, 0, false, false);
    }

    public synchronized MinerTrialProgress incrementMinerTrial(UUID uuid, boolean deep, boolean rare, boolean ancient) {
        try {
            ensureMinerTrial(uuid);
            try (PreparedStatement ps = connection.prepareStatement("UPDATE miner_trials SET total_ores=total_ores+1, deep_ores=deep_ores+?, rare_ores=rare_ores+?, ancient_debris=ancient_debris+? WHERE uuid=?")) {
                ps.setInt(1, deep ? 1 : 0);
                ps.setInt(2, rare ? 1 : 0);
                ps.setInt(3, ancient ? 1 : 0);
                ps.setString(4, uuid.toString());
                ps.executeUpdate();
            }
            return getMinerTrialProgress(uuid);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update Miner trial progress", e);
        }
    }

    public synchronized void setStoneTrialComplete(UUID uuid) {
        setTrialFlag(uuid, "stone_complete");
    }

    public synchronized void setDeepTrialComplete(UUID uuid) {
        setTrialFlag(uuid, "deep_complete");
    }

    private void setTrialFlag(UUID uuid, String column) {
        if (!column.equals("stone_complete") && !column.equals("deep_complete")) throw new IllegalArgumentException("Invalid trial flag");
        try {
            ensureMinerTrial(uuid);
            try (PreparedStatement ps = connection.prepareStatement("UPDATE miner_trials SET " + column + "=1 WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to complete Miner trial", e);
        }
    }

    public synchronized long getAbilityReadyAt(UUID uuid, String ability) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT ready_at FROM ability_cooldowns WHERE uuid=? AND ability=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ability);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read ability cooldown", e);
        }
    }

    public synchronized void setAbilityReadyAt(UUID uuid, String ability, long readyAt) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO ability_cooldowns(uuid,ability,ready_at) VALUES(?,?,?) ON CONFLICT(uuid,ability) DO UPDATE SET ready_at=excluded.ready_at")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ability);
            ps.setLong(3, readyAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save ability cooldown", e);
        }
    }

    public synchronized void resetPlayer(UUID uuid) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement a = connection.prepareStatement("DELETE FROM player_profile WHERE uuid=?");
                 PreparedStatement b = connection.prepareStatement("DELETE FROM job_progress WHERE uuid=?");
                 PreparedStatement c = connection.prepareStatement("DELETE FROM player_skills WHERE uuid=?");
                 PreparedStatement d = connection.prepareStatement("DELETE FROM miner_trials WHERE uuid=?");
                 PreparedStatement e = connection.prepareStatement("DELETE FROM ability_cooldowns WHERE uuid=?")) {
                String id = uuid.toString();
                a.setString(1, id); a.executeUpdate();
                b.setString(1, id); b.executeUpdate();
                c.setString(1, id); c.executeUpdate();
                d.setString(1, id); d.executeUpdate();
                e.setString(1, id); e.executeUpdate();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to reset player", e);
        }
    }

    public synchronized void markPlacedOre(Location loc) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO placed_ores(world,x,y,z) VALUES(?,?,?,?)")) {
            ps.setString(1, loc.getWorld().getUID().toString());
            ps.setInt(2, loc.getBlockX()); ps.setInt(3, loc.getBlockY()); ps.setInt(4, loc.getBlockZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to track placed ore", e);
        }
    }

    public synchronized boolean consumePlacedOre(Location loc) {
        String sql = "DELETE FROM placed_ores WHERE world=? AND x=? AND y=? AND z=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, loc.getWorld().getUID().toString());
            ps.setInt(2, loc.getBlockX()); ps.setInt(3, loc.getBlockY()); ps.setInt(4, loc.getBlockZ());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check placed ore", e);
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) connection.close();
    }
}
