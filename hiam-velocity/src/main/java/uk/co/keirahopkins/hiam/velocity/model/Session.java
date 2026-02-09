package uk.co.keirahopkins.hiam.velocity.model;

import java.time.Instant;
import java.util.UUID;

public class Session {
    private UUID sessionId;
    private UUID accountId;
    private Instant issuedAt;
    private Instant expiresAt;
    private String ip;
    private String userAgent;
    private String authType;
    private boolean isPremium;

    public Session() {}

    public Session(UUID sessionId, UUID accountId, Instant issuedAt, Instant expiresAt,
                   String ip, String userAgent, String authType, boolean isPremium) {
        this.sessionId = sessionId;
        this.accountId = accountId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.ip = ip;
        this.userAgent = userAgent;
        this.authType = authType;
        this.isPremium = isPremium;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isExpired();
    }
}
