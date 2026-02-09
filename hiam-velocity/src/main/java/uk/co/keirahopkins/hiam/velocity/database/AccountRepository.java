package uk.co.keirahopkins.hiam.velocity.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.velocity.model.Account;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class AccountRepository {
    private static final Logger logger = LoggerFactory.getLogger(AccountRepository.class);
    
    private final DatabaseManager databaseManager;

    public AccountRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<Account> findByUsername(String username) {
        String sql = "SELECT id, username, created_at, updated_at, last_ip, last_login_at " +
                 "FROM accounts WHERE username = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find account by username: {}", username, e);
        }
        
        return Optional.empty();
    }

    public Optional<Account> findById(UUID id) {
        String sql = "SELECT id, username, created_at, updated_at, last_ip, last_login_at " +
                     "FROM accounts WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find account by ID: {}", id, e);
        }
        
        return Optional.empty();
    }

    public Account create(String username) {
        String sql = "INSERT INTO accounts (username) VALUES (?) RETURNING id, created_at, updated_at";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Account account = new Account();
                    account.setId((UUID) rs.getObject("id"));
                    account.setUsername(username);
                    account.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                    account.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
                    
                    logger.info("Created new account: {}", username);
                    return account;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to create account: {}", username, e);
            throw new RuntimeException("Failed to create account", e);
        }
        
        throw new RuntimeException("Failed to create account - no ID returned");
    }

    public void updateLastLogin(UUID accountId, String ip) {
        String sql = "UPDATE accounts SET last_login_at = CURRENT_TIMESTAMP, last_ip = ?::INET WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ip);
            stmt.setObject(2, accountId);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update last login for account ID: {}", accountId, e);
        }
    }

    public void delete(UUID accountId) {
        String sql = "DELETE FROM accounts WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, accountId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Deleted account ID: {}", accountId);
            }
        } catch (SQLException e) {
            logger.error("Failed to delete account ID: {}", accountId, e);
            throw new RuntimeException("Failed to delete account", e);
        }
    }

    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId((UUID) rs.getObject("id"));
        account.setUsername(rs.getString("username"));
        account.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        account.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        account.setLastIp(rs.getString("last_ip"));
        
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        if (lastLogin != null) {
            account.setLastLoginAt(lastLogin.toInstant());
        }
        
        return account;
    }

    public void save(Account account) {
        String updateSql = "UPDATE accounts SET username = ?, last_ip = ?::INET, last_login_at = ? WHERE username = ? " +
                           "RETURNING id, created_at, updated_at";
        String insertSql = "INSERT INTO accounts (username, last_ip, last_login_at) " +
                           "VALUES (?, ?::INET, ?) RETURNING id, created_at, updated_at";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            int index = bindAccountFields(updateStmt, account, 1);
            updateStmt.setString(index, account.getUsername());

            try (ResultSet rs = updateStmt.executeQuery()) {
                if (rs.next()) {
                    applySaveResult(account, rs);
                    return;
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                bindAccountFields(insertStmt, account, 1);

                try (ResultSet rs = insertStmt.executeQuery()) {
                    if (rs.next()) {
                        applySaveResult(account, rs);
                        return;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to save account: {}", account.getUsername(), e);
            throw new RuntimeException("Failed to save account", e);
        }

        throw new RuntimeException("Failed to save account - no ID returned");
    }

    private int bindAccountFields(PreparedStatement stmt, Account account, int startIndex) throws SQLException {
        int index = startIndex;
        stmt.setString(index++, account.getUsername());

        if (account.getLastIp() != null) {
            stmt.setString(index++, account.getLastIp());
        } else {
            stmt.setNull(index++, Types.OTHER);
        }

        Instant lastLoginAt = account.getLastLoginAt();
        if (lastLoginAt != null) {
            stmt.setTimestamp(index++, Timestamp.from(lastLoginAt));
        } else {
            stmt.setNull(index++, Types.TIMESTAMP);
        }

        return index;
    }

    private void applySaveResult(Account account, ResultSet rs) throws SQLException {
        account.setId((UUID) rs.getObject("id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            account.setCreatedAt(createdAt.toInstant());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            account.setUpdatedAt(updatedAt.toInstant());
        }
    }
}
