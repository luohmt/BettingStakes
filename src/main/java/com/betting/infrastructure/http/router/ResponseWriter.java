package com.betting.infrastructure.http.router;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResponseWriter {

    private ResponseWriter() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Default content type for responses
     */
    private static final String DEFAULT_CONTENT_TYPE = "text/plain;charset=UTF-8";
    
    /**
     * Default success status code
     */
    private static final int DEFAULT_SUCCESS_STATUS = 200;
    
    /**
     * Forbidden status code for invalid sessions
     */
    private static final int FORBIDDEN_STATUS = 403;
    
    /**
     * Invalid session message
     */
    private static final String INVALID_SESSION_MESSAGE = "Invalid session";


    
    public static void write(HttpExchange exchange, int status, String body) throws IOException {
        if (body == null) {
            body = "";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", DEFAULT_CONTENT_TYPE);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void write(HttpExchange exchange, Object result) throws IOException {
        int status = DEFAULT_SUCCESS_STATUS;
        String body = result == null ? "" : result.toString();
        if (INVALID_SESSION_MESSAGE.equals(body)) {
            status = FORBIDDEN_STATUS;
        }
        write(exchange, status, body);
    }
}

