package uk.co.keirahopkins.hiam.velocity.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.velocity.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    
    private final DatabaseManager databaseManager;
    private final int maxAttempts;
    private final int lockoutMinutes;
    private final Map<String, LockoutInfo> lockoutCache;

    private record LockoutInfo(Instant lockedUntil, int failedAttempts) {}

    public RateLimiter(DatabaseManager databaseManager, int maxAttempts, int lockoutMinutes) {
        this.databaseManager = databaseManager;
        this.maxAttempts = maxAttempts;
        this.lockoutMinutes = lockoutMinutes;
        this.lockoutCache = new ConcurrentHashMap<>();
    }

    public boolean isLocked(String username) {
        LockoutInfo cached = lockoutCache.get(username);
        if (cached != null && Instant.now().isBefore(cached.lockedUntil())) {
            return true;
        }
        
        if (cached != null && Instant.now().isAfter(cached.lockedUntil())) {
            lockoutCache.remove(username);
        }
        
        int recentAttempts = getRecentFailedAttempts(username);
        return recentAttempts >= maxAttempts;
    }

    public int getRemainingAttempts(String username) {
        int failed = getRecentFailedAttempts(username);
        return Math.max(0, maxAttempts - failed);
    }

    public Instant getLockoutExpiry(String username) {
        LockoutInfo cached = lockoutCache.get(username);
        if (cached != null) {
            return cached.lockedUntil();
        }
        
        int attempts = getRecentFailedAttempts(username);
        if (attempts >= maxAttempts) {
            Instant lockoutTime = getLastAttemptTime(username);
            if (lockoutTime != null) {
                Instant expiry = lockoutTime.plus(lockoutMinutes, ChronoUnit.MINUTES);
                lockoutCache.put(username, new LockoutInfo(expiry, attempts));
                return expiry;
            }
        }
        
        return null;
    }

    public void recordAttempt(String username, String ip, boolean success) {
        String sql = "INSERT INTO login_attempts (username, ip, success) VALUES (?, ?::INET, ?)";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, ip);
            stmt.setBoolean(3, success);
            
            stmt.executeUpdate();
            
            if (!success) {
                int attempts = getRecentFailedAttempts(username);
                if (attempts >= maxAttempts) {
                    Instant lockoutUntil = Instant.now().plus(lockoutMinutes, ChronoUnit.MINUTES);
                    lockoutCache.put(username, new LockoutInfo(lockoutUntil, attempts));
                    logger.warn("Account {} locked out until {} after {} failed attempts", 
                            username, lockoutUntil, attempts);
                }
            } else {
                lockoutCache.remove(username);
            }
            
        } catch (SQLException e) {
            logger.error("Failed to record login attempt for {}", username, e);
        }
    }

    public void reset(String username) {
        lockoutCache.remove(username);
        String sql = "DELETE FROM login_attempts WHERE username = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.executeUpdate();
            logger.info("Reset rate limit for username: {}", username);
            
        } catch (SQLException e) {
            logger.error("Failed to reset rate limit for {}", username, e);
        }
    }

    private int getRecentFailedAttempts(String username) {
        String sql = "SELECT COUNT(*) FROM login_attempts " +
                     "WHERE username = ? AND success = FALSE " +
                     "AND attempt_time > ?";
        
        Instant cutoff = Instant.now().minus(lockoutMinutes, ChronoUnit.MINUTES);
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setTimestamp(2, java.sql.Timestamp.from(cutoff));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get recent attempts for {}", username, e);
        }
        
        return 0;
    }

    private Instant getLastAttemptTime(String username) {
        String sql = "SELECT MAX(attempt_time) FROM login_attempts " +
                     "WHERE username = ? AND success = FALSE";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp timestamp = rs.getTimestamp(1);
                    if (timestamp != null) {
                        return timestamp.toInstant();
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get last attempt time for {}", username, e);
        }
        
        return null;
    }

    public void cleanup() {
        String sql = "SELECT cleanup_old_login_attempts()";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
            lockoutCache.clear();
            logger.debug("Cleaned up old login attempts");
        } catch (SQLException e) {
            logger.error("Failed to cleanup old login attempts", e);
        }
    }
}
