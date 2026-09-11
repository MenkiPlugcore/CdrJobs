package store.cadera.cdrjobs.data;

import org.bukkit.Location;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public final class ProfessionStore implements AutoCloseable {
    private final String url;
    private Connection connection;

    public ProfessionStore(File dataFolder) {
        this.url = "jdbc:sqlite:" + new File(dataFolder, "cdrjobs.db").getAbsolutePath();
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(url);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            st.execute("CREATE TABLE IF NOT EXISTS profession_counters (uuid TEXT NOT NULL, job TEXT NOT NULL, metric TEXT NOT NULL, value INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(uuid,job,metric))");
            st.execute("CREATE TABLE IF NOT EXISTS profession_flags (uuid TEXT NOT NULL, job TEXT NOT NULL, flag TEXT NOT NULL, value INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(uuid,job,flag))");
            st.execute("CREATE TABLE IF NOT EXISTS reward_locations (activity TEXT NOT NULL, world TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, ready_at INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(activity,world,x,y,z))");
        }
    }

    public synchronized int getSkillRank(UUID uuid, String skillKey) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT rank FROM player_skills WHERE uuid=? AND skill=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, skillKey);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read profession skill", e);
        }
    }

    public synchronized void setSkillRank(UUID uuid, String skillKey, int rank) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO player_skills(uuid,skill,rank) VALUES(?,?,?) ON CONFLICT(uuid,skill) DO UPDATE SET rank=excluded.rank")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, skillKey);
            ps.setInt(3, rank);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save profession skill", e);
        }
    }

    public synchronized long incrementCounter(UUID uuid, String job, String metric, long amount) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO profession_counters(uuid,job,metric,value) VALUES(?,?,?,?) ON CONFLICT(uuid,job,metric) DO UPDATE SET value=value+excluded.value")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, job);
            ps.setString(3, metric);
            ps.setLong(4, amount);
            ps.executeUpdate();
            return getCounter(uuid, job, metric);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to increment profession counter", e);
        }
    }

    public synchronized long getCounter(UUID uuid, String job, String metric) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM profession_counters WHERE uuid=? AND job=? AND metric=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, job);
            ps.setString(3, metric);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read profession counter", e);
        }
    }

    public synchronized boolean getFlag(UUID uuid, String job, String flag) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM profession_flags WHERE uuid=? AND job=? AND flag=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, job);
            ps.setString(3, flag);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) != 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read profession flag", e);
        }
    }

    public synchronized void setFlag(UUID uuid, String job, String flag, boolean value) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO profession_flags(uuid,job,flag,value) VALUES(?,?,?,?) ON CONFLICT(uuid,job,flag) DO UPDATE SET value=excluded.value")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, job);
            ps.setString(3, flag);
            ps.setInt(4, value ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save profession flag", e);
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

    public synchronized boolean tryClaimLocation(Location loc, String activity, long cooldownMillis) {
        long now = System.currentTimeMillis();
        String world = loc.getWorld().getUID().toString();
        try {
            try (PreparedStatement read = connection.prepareStatement("SELECT ready_at FROM reward_locations WHERE activity=? AND world=? AND x=? AND y=? AND z=?")) {
                read.setString(1, activity); read.setString(2, world);
                read.setInt(3, loc.getBlockX()); read.setInt(4, loc.getBlockY()); read.setInt(5, loc.getBlockZ());
                try (ResultSet rs = read.executeQuery()) {
                    if (rs.next() && rs.getLong(1) > now) return false;
                }
            }
            try (PreparedStatement write = connection.prepareStatement("INSERT INTO reward_locations(activity,world,x,y,z,ready_at) VALUES(?,?,?,?,?,?) ON CONFLICT(activity,world,x,y,z) DO UPDATE SET ready_at=excluded.ready_at")) {
                write.setString(1, activity); write.setString(2, world);
                write.setInt(3, loc.getBlockX()); write.setInt(4, loc.getBlockY()); write.setInt(5, loc.getBlockZ());
                write.setLong(6, now + Math.max(0L, cooldownMillis));
                write.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to claim activity location", e);
        }
    }

    public synchronized void resetPlayer(UUID uuid) {
        try (PreparedStatement a = connection.prepareStatement("DELETE FROM profession_counters WHERE uuid=?");
             PreparedStatement b = connection.prepareStatement("DELETE FROM profession_flags WHERE uuid=?")) {
            a.setString(1, uuid.toString()); a.executeUpdate();
            b.setString(1, uuid.toString()); b.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to reset profession extension data", e);
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) connection.close();
    }
}
