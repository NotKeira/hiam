package uk.co.keirahopkins.hiam.velocity.model;

import java.time.Instant;
import java.util.UUID;

public class AccountCredential {
    private UUID id;
    private UUID accountId;
    private CredentialType type;
    private String secret;
    private UUID premiumUuid;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public CredentialType getType() {
        return type;
    }

    public void setType(CredentialType type) {
        this.type = type;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public UUID getPremiumUuid() {
        return premiumUuid;
    }

    public void setPremiumUuid(UUID premiumUuid) {
        this.premiumUuid = premiumUuid;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
