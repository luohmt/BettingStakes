package com.betting.infrastructure.config;

import com.betting.util.Logger;

/**
 * Simplified configuration class for the betting application
 * 
 * This class provides essential configuration parameters for the betting service.
 * Only includes configurations that are actually used in the application.
 */
public class BettingConfig {
    
    // Essential server configuration
    private final int port;
    private final int threadPoolSize;
    
    public BettingConfig() {
        // Default values with system property override
        this.port = getIntProperty("betting.port", 8001);
        this.threadPoolSize = getIntProperty("betting.thread.pool.size", 
            Runtime.getRuntime().availableProcessors() * 2);
    }
    
    // Getters
    public int getPort() { return port; }
    public int getThreadPoolSize() { return threadPoolSize; }
    
    // Helper method for property reading
    private int getIntProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            // Try environment variable as fallback
            value = System.getenv(key.toUpperCase().replace('.', '_'));
        }
        
        if (value == null) return defaultValue;
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Logger.warn("Warning: Invalid integer value for " + key + ": " + value + ", " + "using default: " + defaultValue);
            return defaultValue;
        }
    }
}