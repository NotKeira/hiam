package uk.co.keirahopkins.hiam.velocity.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.velocity.model.CredentialType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class CredentialRepository {
    private static final Logger logger = LoggerFactory.getLogger(CredentialRepository.class);

    private final DatabaseManager databaseManager;

    public CredentialRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<String> getPasswordHash(UUID accountId) {
        String sql = "SELECT secret FROM account_credentials WHERE account_id = ? AND type = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, accountId);
            stmt.setString(2, CredentialType.PASSWORD.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("secret"));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load password hash for account {}", accountId, e);
        }
        return Optional.empty();
    }

    public void upsertPasswordHash(UUID accountId, String passwordHash) {
        String sql = "INSERT INTO account_credentials (account_id, type, secret) VALUES (?, ?, ?) " +
                     "ON CONFLICT (account_id, type) DO UPDATE SET secret = EXCLUDED.secret";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, accountId);
            stmt.setString(2, CredentialType.PASSWORD.name());
            stmt.setString(3, passwordHash);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to upsert password hash for account {}", accountId, e);
            throw new RuntimeException("Failed to update password hash", e);
        }
    }

    public void clearPassword(UUID accountId) {
        deleteCredential(accountId, CredentialType.PASSWORD);
    }

    public Optional<UUID> getPremiumUuid(UUID accountId) {
        String sql = "SELECT premium_uuid FROM account_credentials WHERE account_id = ? AND type = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, accountId);
            stmt.setString(2, CredentialType.PREMIUM_UUID.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable((UUID) rs.getObject("premium_uuid"));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load premium UUID for account {}", accountId, e);
        }
        return Optional.empty();
    }

    public void upsertPremiumUuid(UUID accountId, UUID premiumUuid) {
        String sql = "INSERT INTO account_credentials (account_id, type, premium_uuid) VALUES (?, ?, ?) " +
                     "ON CONFLICT (account_id, type) DO UPDATE SET premium_uuid = EXCLUDED.premium_uuid";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, accountId);
            stmt.setString(2, CredentialType.PREMIUM_UUID.name());
            stmt.setObject(3, premiumUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to upsert premium UUID for account {}", accountId, e);
            throw new RuntimeException("Failed to update premium UUID", e);
        }
    }

    public void clearPremiumUuid(UUID accountId) {
        deleteCredential(accountId, CredentialType.PREMIUM_UUID);
    }

    public boolean hasPassword(UUID accountId) {
        return getPasswordHash(accountId).isPresent();
    }

    public boolean hasPremium(UUID accountId) {
        return getPremiumUuid(accountId).isPresent();
    }

    private void deleteCredential(UUID accountId, CredentialType type) {
        String sql = "DELETE FROM account_credentials WHERE account_id = ? AND type = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, accountId);
            stmt.setString(2, type.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to delete credential {} for account {}", type, accountId, e);
            throw new RuntimeException("Failed to delete credential", e);
        }
    }
}
