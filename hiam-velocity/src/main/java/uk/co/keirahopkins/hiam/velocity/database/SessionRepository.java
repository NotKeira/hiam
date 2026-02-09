package uk.co.keirahopkins.hiam.velocity.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.velocity.model.Session;

import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SessionRepository {
    private static final Logger logger = LoggerFactory.getLogger(SessionRepository.class);
    
    private final DatabaseManager databaseManager;

    public SessionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Session create(UUID accountId, String ip, String userAgent, boolean isPremium, int ttlMinutes) {
        String sql = "INSERT INTO sessions (account_id, ip, user_agent, auth_type, is_premium, expires_at) " +
                 "VALUES (?, ?::INET, ?, ?, ?, ?) " +
                     "RETURNING session_id, issued_at, expires_at";
        
        Instant expiresAt = Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES);
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, accountId);
            stmt.setString(2, ip);
            stmt.setString(3, userAgent);
            stmt.setNull(4, Types.VARCHAR);
            stmt.setBoolean(5, isPremium);
            stmt.setTimestamp(6, Timestamp.from(expiresAt));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Session session = new Session();
                    session.setSessionId((UUID) rs.getObject("session_id"));
                    session.setAccountId(accountId);
                    session.setIssuedAt(rs.getTimestamp("issued_at").toInstant());
                    session.setExpiresAt(rs.getTimestamp("expires_at").toInstant());
                    session.setIp(ip);
                    session.setUserAgent(userAgent);
                    session.setPremium(isPremium);
                    
                    logger.info("Created session {} for account {}", session.getSessionId(), accountId);
                    return session;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to create session for account: {}", accountId, e);
            throw new RuntimeException("Failed to create session", e);
        }
        
        throw new RuntimeException("Failed to create session - no ID returned");
    }

    public Optional<Session> findById(UUID sessionId) {
        String sql = "SELECT session_id, account_id, issued_at, expires_at, ip, user_agent, auth_type, is_premium " +
                     "FROM sessions WHERE session_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, sessionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find session by ID: {}", sessionId, e);
        }
        
        return Optional.empty();
    }

    public List<Session> findByAccountId(UUID accountId) {
        String sql = "SELECT session_id, account_id, issued_at, expires_at, ip, user_agent, auth_type, is_premium " +
                     "FROM sessions WHERE account_id = ? AND expires_at > CURRENT_TIMESTAMP " +
                     "ORDER BY issued_at DESC";
        
        List<Session> sessions = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, accountId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find sessions for account: {}", accountId, e);
        }
        
        return sessions;
    }

    public void delete(UUID sessionId) {
        String sql = "DELETE FROM sessions WHERE session_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, sessionId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Deleted session: {}", sessionId);
            }
        } catch (SQLException e) {
            logger.error("Failed to delete session: {}", sessionId, e);
            throw new RuntimeException("Failed to delete session", e);
        }
    }

    public void deleteAllForAccount(UUID accountId) {
        String sql = "DELETE FROM sessions WHERE account_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, accountId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Deleted {} session(s) for account {}", rowsAffected, accountId);
            }
        } catch (SQLException e) {
            logger.error("Failed to delete sessions for account: {}", accountId, e);
            throw new RuntimeException("Failed to delete sessions", e);
        }
    }

    public int cleanupExpired() {
        String sql = "DELETE FROM sessions WHERE expires_at <= CURRENT_TIMESTAMP";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Cleaned up {} expired session(s)", rowsAffected);
            }
            return rowsAffected;
        } catch (SQLException e) {
            logger.error("Failed to cleanup expired sessions", e);
            return 0;
        }
    }

    public void save(Session session) {
        if (session.getSessionId() == null) {
            session.setSessionId(UUID.randomUUID());
        }

        String updateSql = "UPDATE sessions SET account_id = ?, issued_at = ?, expires_at = ?, ip = ?::INET, " +
                   "user_agent = ?, auth_type = ?, is_premium = ? WHERE session_id = ? " +
                           "RETURNING session_id, issued_at, expires_at";
        String insertSql = "INSERT INTO sessions (session_id, account_id, issued_at, expires_at, ip, user_agent, auth_type, is_premium) " +
                   "VALUES (?, ?, ?, ?, ?::INET, ?, ?, ?) RETURNING session_id, issued_at, expires_at";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            int index = bindSessionFields(updateStmt, session, 1);
            updateStmt.setObject(index, session.getSessionId());

            try (ResultSet rs = updateStmt.executeQuery()) {
                if (rs.next()) {
                    applySaveResult(session, rs);
                    return;
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setObject(1, session.getSessionId());
                bindSessionFields(insertStmt, session, 2);

                try (ResultSet rs = insertStmt.executeQuery()) {
                    if (rs.next()) {
                        applySaveResult(session, rs);
                        return;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to save session: {}", session.getSessionId(), e);
            throw new RuntimeException("Failed to save session", e);
        }

        throw new RuntimeException("Failed to save session - no ID returned");
    }

    private Session mapResultSetToSession(ResultSet rs) throws SQLException {
        Session session = new Session();
        session.setSessionId((UUID) rs.getObject("session_id"));
        session.setAccountId((UUID) rs.getObject("account_id"));
        session.setIssuedAt(rs.getTimestamp("issued_at").toInstant());
        session.setExpiresAt(rs.getTimestamp("expires_at").toInstant());
        session.setIp(rs.getString("ip"));
        session.setUserAgent(rs.getString("user_agent"));
        session.setAuthType(rs.getString("auth_type"));
        session.setPremium(rs.getBoolean("is_premium"));
        return session;
    }

    private int bindSessionFields(PreparedStatement stmt, Session session, int startIndex) throws SQLException {
        int index = startIndex;

        if (session.getAccountId() != null) {
            stmt.setObject(index++, session.getAccountId());
        } else {
            stmt.setNull(index++, Types.OTHER);
        }

        if (session.getIssuedAt() != null) {
            stmt.setTimestamp(index++, Timestamp.from(session.getIssuedAt()));
        } else {
            stmt.setNull(index++, Types.TIMESTAMP);
        }

        if (session.getExpiresAt() != null) {
            stmt.setTimestamp(index++, Timestamp.from(session.getExpiresAt()));
        } else {
            stmt.setNull(index++, Types.TIMESTAMP);
        }

        if (session.getIp() != null) {
            stmt.setString(index++, session.getIp());
        } else {
            stmt.setNull(index++, Types.OTHER);
        }

        if (session.getUserAgent() != null) {
            stmt.setString(index++, session.getUserAgent());
        } else {
            stmt.setNull(index++, Types.VARCHAR);
        }

        if (session.getAuthType() != null) {
            stmt.setString(index++, session.getAuthType());
        } else {
            stmt.setNull(index++, Types.VARCHAR);
        }

        stmt.setBoolean(index++, session.isPremium());

        return index;
    }

    private void applySaveResult(Session session, ResultSet rs) throws SQLException {
        session.setSessionId((UUID) rs.getObject("session_id"));

        Timestamp issuedAt = rs.getTimestamp("issued_at");
        if (issuedAt != null) {
            session.setIssuedAt(issuedAt.toInstant());
        }

        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            session.setExpiresAt(expiresAt.toInstant());
        }
    }
}
