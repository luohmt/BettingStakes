package com.betting.infrastructure.http.resolver;

import com.betting.util.Logger;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpExchangeResolver implements ParamResolver {
    @Override
    public boolean supports(Parameter parameter) {
        return parameter.getType() == HttpExchange.class;
    }

    @Override
    public Object resolve(Parameter parameter,
                          HttpExchange exchange,
                          Map<String, String> pathParams,
                          Map<String, String> queryParams,
                          String body) {
        return exchange;
    }

    /**
     * @param exchange   exchange
     * @param statusCode HTTP status code
     * @param body       body
     */
    public static void send(HttpExchange exchange, int statusCode, String body) {
        try {
            byte[] bytes = body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
        } catch (IOException e) {
            Logger.error("Error sending response", e, e.getMessage());
        } finally {
            try {
                exchange.getResponseBody().close();
            } catch (IOException ignored) {
                Logger.warn("Error closing response body");
            }
        }
    }
}
