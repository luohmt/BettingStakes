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
 * - Single authoritative map: sessionKey -> Session
 * - Secondary index cache: customerId -> sessionKey (lazy repair if stale)
 * - Optimized session key generation algorithm
 * - Intelligent cleanup strategy (30-second intervals)
 * - Thread-safe operations with atomic counters
 */
public class SessionServiceImpl implements SessionService {

    /** 主存：sessionKey -> Session */
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    /** 索引缓存：customerId -> sessionKey（懒惰修复，不保证强一致） */
    private final ConcurrentHashMap<Integer, String> customerIndex = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong sessionCounter = new AtomicLong(0);

    private static final long SESSION_DURATION = 10 * 60 * 1000L;
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public SessionServiceImpl() {
        scheduler.scheduleAtFixedRate(this::cleanupExpired, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public Session createOrGetSession(int customerId) {
        // 优先走索引缓存
        String existingSessionKey = customerIndex.get(customerId);
        Session existingSession = existingSessionKey != null ? sessions.get(existingSessionKey) : null;

        if (existingSession != null && existingSession.getExpiryTime() > System.currentTimeMillis()) {
            existingSession.renew(System.currentTimeMillis() + SESSION_DURATION);
            return existingSession;
        }

        // fallback：从 sessions 扫描一次（修复缓存）
        existingSession = sessions.values().stream()
                .filter(s -> s.getCustomerId() == customerId && s.getExpiryTime() > System.currentTimeMillis())
                .findFirst()
                .orElse(null);

        if (existingSession != null) {
            existingSession.renew(System.currentTimeMillis() + SESSION_DURATION);
            customerIndex.put(customerId, existingSession.getSessionKey());
            return existingSession;
        }

        // create new session key
        String newSessionKey = generateSessionKey();
        Session newSession = new Session(customerId, newSessionKey, System.currentTimeMillis() + SESSION_DURATION);

        sessions.put(newSessionKey, newSession);
        customerIndex.put(customerId, newSessionKey);

        return newSession;
    }

    @Override
    public boolean validateSession(String sessionKey) {
        if (sessionKey == null) return false;

        Session session = sessions.get(sessionKey);
        if (session == null) return false;

        long now = System.currentTimeMillis();
        if (session.getExpiryTime() <= now) {
            sessions.remove(sessionKey);
            customerIndex.remove(session.getCustomerId(), sessionKey); // 只移除匹配的索引，避免误删
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
            sessions.remove(sessionKey);
            customerIndex.remove(session.getCustomerId(), sessionKey);
            return 0;
        }

        return session.getCustomerId();
    }

    /**
     * cleanupExpired
     */
    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> {
            Session session = entry.getValue();
            if (session.getExpiryTime() <= now) {
                customerIndex.remove(session.getCustomerId(), entry.getKey());
                return true;
            }
            return false;
        });
    }

    /** 生成 sessionKey */
    private String generateSessionKey() {
        long timestamp = System.currentTimeMillis() % 1000000;
        long counter = sessionCounter.incrementAndGet() % 1000;
        long combined = timestamp * 1000 + counter;

        StringBuilder sb = new StringBuilder(7);
        for (int i = 0; i < 7; i++) {
            sb.append(CHARS.charAt((int) (combined % CHARS.length())));
            combined /= CHARS.length();
        }
        return sb.toString();
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

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
