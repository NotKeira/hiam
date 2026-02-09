package uk.co.keirahopkins.hiam.velocity.model;

import java.time.Instant;
import java.util.UUID;

public class Account {
    private UUID id;
    private String username;
    private Instant createdAt;
    private Instant updatedAt;
    private String lastIp;
    private Instant lastLoginAt;

    public Account() {}

    public Account(UUID id, String username, Instant createdAt, Instant updatedAt, String lastIp, Instant lastLoginAt) {
        this.id = id;
        this.username = username;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastIp = lastIp;
        this.lastLoginAt = lastLoginAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

}
