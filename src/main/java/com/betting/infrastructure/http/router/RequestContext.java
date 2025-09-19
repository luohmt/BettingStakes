package com.betting.infrastructure.http.router;


import java.util.Map;

/**
 * Request context containing all request-related data
 */
public record RequestContext(
        String path,
        String method,
        Map<String, String> pathParams,
        Map<String, String> queryParams,
        String body
) {
}

