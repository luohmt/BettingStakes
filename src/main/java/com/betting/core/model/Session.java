package com.betting.core.model;

public class Session {

    private final int customerId;
    /**
     * session key : a "reasonably" unique - letters and digits only string identifying the session
     * example: "QWER12A"
     */
    private final String sessionKey;
    private volatile long expiryTime;

    public Session(int customerId, String sessionKey, long expiryTime) {
        this.customerId = customerId;
        this.sessionKey = sessionKey;
        this.expiryTime = expiryTime;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    /**
     * Renews the session with a new expiry time
     * Thread-safe operation using volatile write
     */
    public void renew(long newExpiry) {
        this.expiryTime = newExpiry;
    }

    /**
     * Checks if the session is expired
     * Thread-safe read operation
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    @Override
    public String toString() {
        return String.format("Session{customerId=%d, sessionKey='%s', expiryTime=%d, valid=%s}",
                customerId, sessionKey, expiryTime, !isExpired());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Session session = (Session) obj;
        return sessionKey.equals(session.sessionKey);
    }

    @Override
    public int hashCode() {
        return sessionKey.hashCode();
    }
}
