package com.betting.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple logging utility for the betting application
 * Provides structured logging with timestamps and log levels
 */
public class Logger {
    
    /**
     * Date time formatter for log timestamps
     */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Log level constants
     */
    private static final String INFO_LEVEL = "INFO";
    private static final String WARN_LEVEL = "WARN";
    private static final String ERROR_LEVEL = "ERROR";
    
    /**
     * Log format template
     */
    private static final String LOG_FORMAT = "[%s] %s - %s";
    
    private Logger() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Logs an info message with timestamp
     * 
     * @param msg the message template
     * @param args the message arguments
     */
    public static void info(String msg, Object... args) {
        String formattedMessage = formatMessage(msg, args);
        System.out.printf((LOG_FORMAT) + "%n", INFO_LEVEL, getTimestamp(), formattedMessage);
    }

    

    /**
     * Logs a warning message with timestamp
     * 
     * @param msg the message template
     * @param args the message arguments
     */
    public static void warn(String msg, Object... args) {
        String formattedMessage = formatMessage(msg, args);
        System.out.printf((LOG_FORMAT) + "%n", WARN_LEVEL, getTimestamp(), formattedMessage);
    }

    /**
     * Logs an error message with timestamp and stack trace
     * 
     * @param msg the message template
     * @param t the throwable to log
     * @param args the message arguments
     */
    public static void error(String msg, Throwable t, Object... args) {
        String formattedMessage = formatMessage(msg, args);
        System.err.printf((LOG_FORMAT) + "%n", ERROR_LEVEL, getTimestamp(), formattedMessage);
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }
    
    /**
     * Formats a message with arguments
     * 
     * @param msg the message template
     * @param args the arguments
     * @return the formatted message
     */
    private static String formatMessage(String msg, Object... args) {
        if (args == null || args.length == 0) {
            return msg;
        }
        try {
            return String.format(msg, args);
        } catch (Exception e) {
            // Fallback to simple concatenation if formatting fails
            return msg + " " + String.join(" ", java.util.Arrays.toString(args));
        }
    }
    
    /**
     * Gets the current timestamp
     * 
     * @return formatted timestamp string
     */
    private static String getTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }
}

