package store.cadera.cdrjobs.data;

import org.bukkit.Location;

import java.io.File;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProfessionStore implements AutoCloseable {
    private static final String TRIAL_BASE_PREFIX = "_trial_base_";

    private final String url;
    private Connection connection;

    public ProfessionStore(File folder) {
        this.url = "jdbc:sqlite:" + new File(folder, "cdrjobs.db").getAbsolutePath();
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(url);
        try (Statement s = connection.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
            s.execute("PRAGMA synchronous=NORMAL");
            s.execute("CREATE TABLE IF NOT EXISTS profession_counters (uuid TEXT NOT NULL, job TEXT NOT NULL, metric TEXT NOT NULL, value INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(uuid,job,metric))");
            s.execute("CREATE TABLE IF NOT EXISTS profession_flags (uuid TEXT NOT NULL, job TEXT NOT NULL, flag TEXT NOT NULL, value INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(uuid,job,flag))");
            s.execute("CREATE TABLE IF NOT EXISTS reward_locations (activity TEXT NOT NULL, world TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, ready_at INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(activity,world,x,y,z))");
            s.execute("CREATE TABLE IF NOT EXISTS placed_job_blocks (job TEXT NOT NULL, world TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, PRIMARY KEY(job,world,x,y,z))");
            s.execute("CREATE TABLE IF NOT EXISTS schema_meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
        }
    }

    public synchronized void setSchemaVersion(int version) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO schema_meta(key,value) VALUES('schema_version',?) ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
            ps.setString(1, String.valueOf(version));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public synchronized int getSchemaVersion() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM schema_meta WHERE key='schema_version'")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Integer.parseInt(rs.getString(1)) : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public synchronized int getSkillRank(UUID uuid, String key) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT rank FROM player_skills WHERE uuid=? AND skill=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public synchronized void setSkillRank(UUID uuid, String key, int rank) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO player_skills(uuid,skill,rank) VALUES(?,?,?) ON CONFLICT(uuid,skill) DO UPDATE SET rank=excluded.rank")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            ps.setInt(3, rank);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
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
            throw new IllegalStateException(e);
        }
    }

    public synchronized void setCounter(UUID uuid, String job, String metric, long value) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO profession_counters(uuid,job,metric,value) VALUES(?,?,?,?) ON CONFLICT(uuid,job,metric) DO UPDATE SET value=excluded.value")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, job);
            ps.setString(3, metric);
            ps.setLong(4, Math.max(0L, value));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
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
            throw new IllegalStateException(e);
        }
    }

    public synchronized long getTrialProgress(UUID uuid, String job, String metric) {
        long total = getCounter(uuid, job, metric);
        long baseline = getCounter(uuid, job, TRIAL_BASE_PREFIX + metric);
        return Math.max(0L, total - baseline);
    }

    public synchronized Map<String, Long> getCounters(UUID uuid, String job) {
        Map<String, Long> out = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT metric,value FROM profession_counters WHERE uuid=? AND job=? AND metric NOT LIKE ? ORDER BY metric")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, job);
            ps.setString(3, TRIAL_BASE_PREFIX + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getLong(2));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return out;
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
            throw new IllegalStateException(e);
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
            throw new IllegalStateException(e);
        }
    }

    public synchronized Map<String, Boolean> getFlags(UUID uuid, String job) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT flag,value FROM profession_flags WHERE uuid=? AND job=? ORDER BY flag")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, job);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getInt(2) != 0);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    public synchronized long getAbilityReadyAt(UUID uuid, String ability) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT ready_at FROM ability_cooldowns WHERE uuid=? AND ability=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ability);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public synchronized void setAbilityReadyAt(UUID uuid, String ability, long readyAt) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO ability_cooldowns(uuid,ability,ready_at) VALUES(?,?,?) ON CONFLICT(uuid,ability) DO UPDATE SET ready_at=excluded.ready_at")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ability);
            ps.setLong(3, readyAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public synchronized boolean tryClaimLocation(Location location, String activity, long cooldown) {
        long now = System.currentTimeMillis();
        String world = location.getWorld().getUID().toString();
        try {
            try (PreparedStatement ps = connection.prepareStatement("SELECT ready_at FROM reward_locations WHERE activity=? AND world=? AND x=? AND y=? AND z=?")) {
                ps.setString(1, activity);
                ps.setString(2, world);
                ps.setInt(3, location.getBlockX());
                ps.setInt(4, location.getBlockY());
                ps.setInt(5, location.getBlockZ());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getLong(1) > now) return false;
                }
            }
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO reward_locations(activity,world,x,y,z,ready_at) VALUES(?,?,?,?,?,?) ON CONFLICT(activity,world,x,y,z) DO UPDATE SET ready_at=excluded.ready_at")) {
                ps.setString(1, activity);
                ps.setString(2, world);
                ps.setInt(3, location.getBlockX());
                ps.setInt(4, location.getBlockY());
                ps.setInt(5, location.getBlockZ());
                ps.setLong(6, now + Math.max(0, cooldown));
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public synchronized void markPlacedJobBlock(Location location, String job) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO placed_job_blocks(job,world,x,y,z) VALUES(?,?,?,?,?)")) {
            ps.setString(1, job);
            ps.setString(2, location.getWorld().getUID().toString());
            ps.setInt(3, location.getBlockX());
            ps.setInt(4, location.getBlockY());
            ps.setInt(5, location.getBlockZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public synchronized boolean consumePlacedJobBlock(Location location, String job) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM placed_job_blocks WHERE job=? AND world=? AND x=? AND y=? AND z=?")) {
            ps.setString(1, job);
            ps.setString(2, location.getWorld().getUID().toString());
            ps.setInt(3, location.getBlockX());
            ps.setInt(4, location.getBlockY());
            ps.setInt(5, location.getBlockZ());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public synchronized void resetProfessionData(UUID uuid, String job) {
        try (PreparedStatement counters = connection.prepareStatement("DELETE FROM profession_counters WHERE uuid=? AND job=?");
             PreparedStatement flags = connection.prepareStatement("DELETE FROM profession_flags WHERE uuid=? AND job=?")) {
            counters.setString(1, uuid.toString());
            counters.setString(2, job);
            counters.executeUpdate();
            flags.setString(1, uuid.toString());
            flags.setString(2, job);
            flags.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to reset profession extension data", e);
        }
    }

    public synchronized void resetTrialData(UUID uuid, String job) {
        List<String> metrics = switch (job.toUpperCase()) {
            case "FARMER" -> List.of("harvests", "rare_harvests");
            case "HUNTER" -> List.of("kills", "night_kills", "dangerous_kills");
            case "LUMBERJACK" -> List.of("logs", "elder_logs");
            case "FISHER" -> List.of("catches", "treasures", "ocean_catches");
            default -> List.of();
        };

        for (String metric : metrics) {
            setCounter(uuid, job, TRIAL_BASE_PREFIX + metric, getCounter(uuid, job, metric));
        }

        try (PreparedStatement flags = connection.prepareStatement("DELETE FROM profession_flags WHERE uuid=? AND job=? AND flag LIKE 'trial_%'")) {
            flags.setString(1, uuid.toString());
            flags.setString(2, job);
            flags.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to reset trial flags", e);
        }
    }

    public synchronized void resetPlayer(UUID uuid) {
        try (PreparedStatement counters = connection.prepareStatement("DELETE FROM profession_counters WHERE uuid=?");
             PreparedStatement flags = connection.prepareStatement("DELETE FROM profession_flags WHERE uuid=?")) {
            counters.setString(1, uuid.toString());
            counters.executeUpdate();
            flags.setString(1, uuid.toString());
            flags.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) connection.close();
    }
}
