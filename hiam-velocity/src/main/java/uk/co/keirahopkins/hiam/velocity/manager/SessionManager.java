package uk.co.keirahopkins.hiam.velocity.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.velocity.database.SessionRepository;
import uk.co.keirahopkins.hiam.velocity.model.Session;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    
    private final SessionRepository sessionRepository;
    private final Map<UUID, Session> sessionCache;
    private final Map<UUID, UUID> accountSessionMap;

    public SessionManager(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
        this.sessionCache = new ConcurrentHashMap<>();
        this.accountSessionMap = new ConcurrentHashMap<>();
    }

    public Session createSession(UUID accountId, String ip, String userAgent, boolean isPremium, int ttlMinutes) {
        Session session = sessionRepository.create(accountId, ip, userAgent, isPremium, ttlMinutes);
        sessionCache.put(session.getSessionId(), session);
        accountSessionMap.put(accountId, session.getSessionId());
        logger.debug("Cached session {} for account {}", session.getSessionId(), accountId);
        return session;
    }

    public void cacheSession(UUID accountId, Session session) {
        if (session == null || session.getSessionId() == null) {
            return;
        }

        UUID accountKey = accountId != null ? accountId : session.getAccountId();
        sessionCache.put(session.getSessionId(), session);

        if (accountKey != null) {
            accountSessionMap.put(accountKey, session.getSessionId());
            logger.debug("Cached session {} for account {}", session.getSessionId(), accountKey);
        } else {
            logger.debug("Cached session {} with no account mapping", session.getSessionId());
        }
    }

    public Optional<Session> getSession(UUID sessionId) {
        Session cachedSession = sessionCache.get(sessionId);
        if (cachedSession != null) {
            if (cachedSession.isExpired()) {
                invalidateSession(sessionId);
                return Optional.empty();
            }
            return Optional.of(cachedSession);
        }
        
        Optional<Session> session = sessionRepository.findById(sessionId);
        session.ifPresent(s -> {
            if (!s.isExpired()) {
                sessionCache.put(s.getSessionId(), s);
                accountSessionMap.put(s.getAccountId(), s.getSessionId());
            }
        });
        
        return session;
    }

    public Optional<Session> getSessionByAccountId(UUID accountId) {
        UUID cachedSessionId = accountSessionMap.get(accountId);
        if (cachedSessionId != null) {
            return getSession(cachedSessionId);
        }
        
        List<Session> sessions = sessionRepository.findByAccountId(accountId);
        if (!sessions.isEmpty()) {
            Session latestSession = sessions.get(0);
            sessionCache.put(latestSession.getSessionId(), latestSession);
            accountSessionMap.put(accountId, latestSession.getSessionId());
            return Optional.of(latestSession);
        }
        
        return Optional.empty();
    }

    public void invalidateSession(UUID sessionId) {
        Session session = sessionCache.remove(sessionId);
        if (session != null) {
            accountSessionMap.remove(session.getAccountId());
            logger.debug("Invalidated cached session {}", sessionId);
        }
        sessionRepository.delete(sessionId);
    }

    public void invalidateAllSessionsForAccount(UUID accountId) {
        UUID sessionId = accountSessionMap.remove(accountId);
        if (sessionId != null) {
            sessionCache.remove(sessionId);
        }
        sessionRepository.deleteAllForAccount(accountId);
        logger.info("Invalidated all sessions for account {}", accountId);
    }

    public boolean validateSession(UUID sessionId, String ip, boolean requireIpMatch) {
        Optional<Session> sessionOpt = getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        
        Session session = sessionOpt.get();
        
        if (session.isExpired()) {
            invalidateSession(sessionId);
            return false;
        }
        
        if (requireIpMatch && !session.getIp().equals(ip)) {
            logger.warn("Session {} IP mismatch: expected {}, got {}", sessionId, session.getIp(), ip);
            return false;
        }
        
        return true;
    }

    public void cleanupExpiredSessions() {
        int cleaned = sessionRepository.cleanupExpired();
        sessionCache.values().removeIf(session -> {
            if (session.isExpired()) {
                accountSessionMap.remove(session.getAccountId());
                return true;
            }
            return false;
        });
        logger.debug("Cleaned up {} expired sessions", cleaned);
    }

    public void clearCache() {
        sessionCache.clear();
        accountSessionMap.clear();
        logger.info("Session cache cleared");
    }
}
