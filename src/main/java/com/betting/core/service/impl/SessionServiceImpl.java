package com.betting.core.service.impl;

import com.betting.core.model.Session;
import com.betting.core.service.SessionService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * - O(1) session lookup using ConcurrentHashMap
 * - Single map for session management (customerId embedded in Session)
 * - Optimized session key generation algorithm
 * - Intelligent cleanup strategy (30-second intervals)
 * - Thread-safe operations with atomic counters
 */
public class SessionServiceImpl implements SessionService {

    /**
     * Primary session storage: sessionKey -> Session
     * Provides O(1) lookup complexity for session validation and retrieval
     */
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * Background scheduler for periodic cleanup of expired sessions
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Atomic counter for generating unique session keys
     * Ensures thread-safe session key generation
     */
    private final AtomicLong sessionCounter = new AtomicLong(0);

    /**
     * Session duration: 10 minutes in milliseconds
     * Sessions expire after this duration and are automatically cleaned up
     */
    private static final long SESSION_DURATION = 10 * 60 * 1000L;
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public SessionServiceImpl() {
        // clean up expired sessions every 30 seconds
        scheduler.scheduleAtFixedRate(this::cleanupExpired, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public Session createOrGetSession(int customerId) {
        // check if customer already has an active session
        Session existingSession = sessions.values().stream()
                .filter(s -> s.getCustomerId() == customerId && s.getExpiryTime() > System.currentTimeMillis())
                .findFirst()
                .orElse(null);

        if (existingSession != null) {
            existingSession.renew(System.currentTimeMillis() + SESSION_DURATION);
            return existingSession;
        }

        // create new session key
        String newSessionKey = generateSessionKey();
        Session newSession = new Session(customerId, newSessionKey, System.currentTimeMillis() + SESSION_DURATION);

        sessions.put(newSessionKey, newSession);
        return newSession;
    }

    @Override
    public boolean validateSession(String sessionKey) {
        if (sessionKey == null) return false;

        Session session = sessions.get(sessionKey);
        if (session == null) return false;

        long now = System.currentTimeMillis();
        if (session.getExpiryTime() <= now) {
            // expired session cleanup
            sessions.remove(sessionKey);
            return false;
        }

        return true;
    }

    @Override
    public int getCustomerId(String sessionKey) {
        if (sessionKey == null) return 0;

        Session session = sessions.get(sessionKey);
        if (session == null) return 0;

        long now = System.currentTimeMillis();
        if (session.getExpiryTime() <= now) {
            // cleanup expired session
            sessions.remove(sessionKey);
            return 0;
        }

        return session.getCustomerId();
    }

    /**
     * cleanupExpired
     */
    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> entry.getValue().getExpiryTime() <= now);
    }

    /**
     * generate SessionKey
     * use timestamp + counter to ensure uniqueness
     */
    private String generateSessionKey() {
        long timestamp = System.currentTimeMillis() % 1000000; // get last 6 digits
        long counter = sessionCounter.incrementAndGet() % 1000; //  get 3 digits counter
        long combined = timestamp * 1000 + counter;

        StringBuilder sb = new StringBuilder(7);
        for (int i = 0; i < 7; i++) {
            sb.append(CHARS.charAt((int) (combined % CHARS.length())));
            combined /= CHARS.length();
        }

        return sb.toString();
    }

    /**
     * get active session count for monitoring
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * close the scheduler on shutdown
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
