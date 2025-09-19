package com.betting.infrastructure.http.interceptor;

import com.sun.net.httpserver.HttpExchange;

/**
 * Interceptor interface for handling pre-processing, post-processing, and exception handling
 * around HTTP request processing.
 */

public interface RequestInterceptor {
    void preHandle(HttpExchange exchange);

    void postHandle(HttpExchange exchange, Object result);

    void afterException(HttpExchange exchange, Throwable e);
}
