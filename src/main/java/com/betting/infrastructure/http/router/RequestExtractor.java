package com.betting.infrastructure.http.router;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.betting.infrastructure.http.router.Router.*;

public class RequestExtractor {

    /**
     * Extracts request context from HTTP exchange
     *
     * @param exchange the HTTP exchange
     * @return request context containing path, method, and parameters
     * @throws IOException if there's an error reading the request body
     */
    public RequestContext extractRequestContext(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        Map<String, String> pathParams = new HashMap<>();
        Map<String, String> queryParams = parseQueryParameters(exchange.getRequestURI().getQuery());
        String body = readRequestBody(exchange);

        return new RequestContext(path, method, pathParams, queryParams, body);
    }

    /**
     * Parses query parameters from the query string
     *
     * @param queryString the raw query string
     * @return map of parameter names to values
     */
    private Map<String, String> parseQueryParameters(String queryString) {
        Map<String, String> parameters = new HashMap<>();

        if (queryString == null || queryString.trim().isEmpty()) {
            return parameters;
        }

        String[] paramPairs = queryString.split(QUERY_PARAM_SEPARATOR);
        for (String paramPair : paramPairs) {
            if (paramPair.trim().isEmpty()) {
                continue;
            }

            String[] keyValue = paramPair.split(QUERY_KEY_VALUE_SEPARATOR, MAX_KEY_VALUE_PARTS);
            if (keyValue.length == MAX_KEY_VALUE_PARTS && !keyValue[0].trim().isEmpty()) {
                parameters.put(keyValue[0].trim(), keyValue[1]);
            }
        }

        return parameters;
    }

    /**
     * Reads the request body from the HTTP exchange
     *
     * @param exchange the HTTP exchange
     * @return the request body as a string
     * @throws IOException if there's an error reading the body
     */
    private String readRequestBody(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getRequestHeaders();
        String contentLengthHeader = headers != null ? headers.getFirst(CONTENT_LENGTH_HEADER) : null;
        int contentLength = parseContentLength(contentLengthHeader);

        if (contentLength == 0) {
            return "";
        }

        byte[] bodyBytes = exchange.getRequestBody().readNBytes(contentLength);
        return new String(bodyBytes, StandardCharsets.UTF_8);
    }

    /**
     * Safely parses the Content-Length header
     *
     * @param contentLengthHeader the Content-Length header value
     * @return parsed content length or 0 if invalid
     */
    private int parseContentLength(String contentLengthHeader) {
        if (contentLengthHeader == null || contentLengthHeader.trim().isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(contentLengthHeader.trim());
        } catch (NumberFormatException e) {
            return 0; // Return 0 for invalid content length
        }
    }
}
