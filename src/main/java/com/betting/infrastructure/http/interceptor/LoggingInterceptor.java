package com.betting.infrastructure.http.interceptor;

import com.betting.infrastructure.http.router.ResponseWriter;
import com.betting.util.Logger;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class LoggingInterceptor implements RequestInterceptor {

    @Override
    public void preHandle(HttpExchange exchange) {
        Logger.info("[Request] %s %s", exchange.getRequestMethod(), exchange.getRequestURI());
    }

    @Override
    public void postHandle(HttpExchange exchange, Object result) {
        Logger.info("[Response] %s %s -> %s",
                exchange.getRequestMethod(),
                exchange.getRequestURI(),
                result == null ? "" : result.toString());
    }

    @Override
    public void afterException(HttpExchange exchange, Throwable e) {
        Logger.error("[Error] %s %s", e,
                exchange.getRequestMethod(),
                exchange.getRequestURI());
        try {
            ResponseWriter.write(exchange, 500, "Internal Server Error");
        } catch (IOException ioEx) {
            Logger.error("Failed to write error response", ioEx);
        }
    }
}

